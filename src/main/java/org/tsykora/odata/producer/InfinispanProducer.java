package org.tsykora.odata.producer;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.core.Response;

import org.apache.log4j.Logger;
import org.core4j.Func;
import org.core4j.Func1;
import org.infinispan.AdvancedCache;
import org.infinispan.Cache;
import org.infinispan.context.Flag;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.query.CacheQuery;
import org.infinispan.query.SearchManager;
import org.odata4j.core.OEntity;
import org.odata4j.core.OEntityId;
import org.odata4j.core.OEntityKey;
import org.odata4j.core.OExtension;
import org.odata4j.core.OFunctionParameter;
import org.odata4j.core.OSimpleObject;
import org.odata4j.edm.EdmComplexType;
import org.odata4j.edm.EdmDataServices;
import org.odata4j.edm.EdmDecorator;
import org.odata4j.edm.EdmDocumentation;
import org.odata4j.edm.EdmEntityContainer;
import org.odata4j.edm.EdmEntitySet;
import org.odata4j.edm.EdmEntityType;
import org.odata4j.edm.EdmFunctionImport;
import org.odata4j.edm.EdmFunctionParameter;
import org.odata4j.edm.EdmGenerator;
import org.odata4j.edm.EdmSchema;
import org.odata4j.edm.EdmSimpleType;
import org.odata4j.edm.EdmType;
import org.odata4j.exceptions.NotImplementedException;
import org.odata4j.producer.BaseResponse;
import org.odata4j.producer.CountResponse;
import org.odata4j.producer.EntitiesResponse;
import org.odata4j.producer.EntityIdResponse;
import org.odata4j.producer.EntityQueryInfo;
import org.odata4j.producer.EntityResponse;
import org.odata4j.producer.ODataContext;
import org.odata4j.producer.ODataProducer;
import org.odata4j.producer.QueryInfo;
import org.odata4j.producer.Responses;
import org.odata4j.producer.edm.MetadataProducer;
import org.odata4j.producer.inmemory.BeanBasedPropertyModel;
import org.odata4j.producer.inmemory.EnumsAsStringsPropertyModelDelegate;
import org.odata4j.producer.inmemory.InMemoryComplexTypeInfo;
import org.odata4j.producer.inmemory.InMemoryTypeMapping;
import org.odata4j.producer.inmemory.PropertyModel;

/**
 * ODataProducer implementation with direct access to the Infinispan's caches.
 *
 * TODO: FIND OUT AUTHOR OF THE InMemoryProducerExample and give a proper credit to him (them).
 *
 * @author Tomas Sykora <tomas@infinispan.org>
 *
 * this class is based on the InMemoryProducer -- author
 */
public class InfinispanProducer implements ODataProducer {

    private static final boolean DUMP = false;
    private static final Logger log = Logger.getLogger(InfinispanProducer.class.getName());

    private static void dump(Object msg) {
        if (DUMP) {
            log.trace(msg);
        }
    }

    public static final String ID_PROPNAME = "EntityId";
    private final String namespace;
    private final String containerName;
    private final int maxResults;

    // preserve the order of registration
    private final Map<String, InMemoryEntityInfo<?>> eis = new LinkedHashMap<String, InMemoryEntityInfo<?>>();

    private final Map<String, InMemoryComplexTypeInfo<?>> complexTypes = new LinkedHashMap<String, InMemoryComplexTypeInfo<?>>();
    private EdmDataServices metadata;
    private final EdmDecorator decorator;
    private final MetadataProducer metadataProducer;
    private final InMemoryTypeMapping typeMapping;
    private final boolean flattenEdm;
    private static final int DEFAULT_MAX_RESULTS = 100;

    private DefaultCacheManager defaultCacheManager = null;
    // for faster cache access
    private HashMap<String, AdvancedCache> caches = new HashMap<String, AdvancedCache>();


    /**
     * Creates a new instance of an in-memory POJO producer.
     *
     * @param namespace the namespace of the schema registrations
     */
    public InfinispanProducer(String namespace, String ispnConfigFile) {
        this(namespace, DEFAULT_MAX_RESULTS, ispnConfigFile);
    }

    /**
     * Creates a new instance of an in-memory POJO producer.
     *
     * @param namespace  the namespace of the schema registrations
     * @param maxResults the maximum number of entities to return in a single call
     */
    public InfinispanProducer(String namespace, int maxResults, String ispnConfigFile) {
        this(namespace, null, maxResults, null, null, ispnConfigFile);
    }

    /**
     * Creates a new instance of an in-memory POJO producer.
     *
     * @param namespace     the namespace of the schema registrations
     * @param containerName the container name for generated metadata
     * @param maxResults    the maximum number of entities to return in a single call
     * @param decorator     a decorator to use for edm customizations
     * @param typeMapping   optional mapping between java types and edm types, null for default
     */
    public InfinispanProducer(String namespace, String containerName, int maxResults, EdmDecorator decorator, InMemoryTypeMapping typeMapping,
                              String ispnConfigFile) {
        this(namespace, containerName, maxResults, decorator, typeMapping,
                true, ispnConfigFile); // legacy: flatten edm
    }

    /**
     * Do everything important here while creating new producer instance.
     */
    public <TEntity, TKey> InfinispanProducer(String namespace, String containerName, int maxResults,
                                              EdmDecorator decorator, InMemoryTypeMapping typeMapping,
                                              boolean flattenEdm, String ispnConfigFile) {
        this.namespace = namespace;
        this.containerName = containerName != null && !containerName.isEmpty() ? containerName : "Container";
        this.maxResults = maxResults;
        this.decorator = decorator;
        this.metadataProducer = new MetadataProducer(this, decorator);
        this.typeMapping = typeMapping == null ? InMemoryTypeMapping.DEFAULT : typeMapping;
        this.flattenEdm = flattenEdm;

        // TODO: implement running in -DtestMode=true (pre-fill up cache with 10 JSON entries)
        // TODO add possibility for passing configurations (global, local)
        try {
            // true = start it + start defined caches

            log.info("Infinispan config file: " + ispnConfigFile);

            defaultCacheManager = new DefaultCacheManager(ispnConfigFile, true);

            // default cache is not included in this Set
            Set<String> cacheNames = defaultCacheManager.getCacheNames();

            // TODO: add default cache explicitly? check whether exist? created every time?
//            cacheNames.add("default");

            for (String cacheName : cacheNames) {
                log.info("Registering cache with name " + cacheName + " in OData InfinispanProducer...");
                // cacheName = entitySetName
                eis.put(cacheName, null);
            }

        } catch (IOException e) {
            log.error("PROBLEMS WITH CREATING DEFAULT CACHE MANAGER!");
            e.printStackTrace();
        }
    }

    /**
     * Look into global map for registered cache. Avoiding multiple asking CacheManager.
     * <p/>
     * If there is no cache with the given name, get it from CacheManager and put.
     *
     * @param cacheName
     * @return
     */
    private AdvancedCache getCache(String cacheName) {
        if (caches.get(cacheName) != null) {
            return this.caches.get(cacheName);
        } else {
            try {
                log.info("Starting cache with name " + cacheName +
                        " on defaultCacheManager inside of OData InfinispanProducer");
                defaultCacheManager.startCache(cacheName);
                log.info("Cache " + cacheName + " started!");
                Cache cache = defaultCacheManager.getCache(cacheName);
                this.caches.put(cacheName, cache.getAdvancedCache());
                return cache.getAdvancedCache();
            } catch (Exception e) {
                log.error(" \n\n ***** ERROR DURING STARTING CACHE ***** \n\n" + e.getMessage());
                e.printStackTrace();
            }
        }
        return this.caches.get(cacheName);
    }

    @Override
    public EdmDataServices getMetadata() {
        if (metadata == null) {
            metadata = newEdmGenerator(namespace, typeMapping, ID_PROPNAME, eis, complexTypes).generateEdm(decorator).build();
        }
        return metadata;
    }

    protected InMemoryEdmGenerator newEdmGenerator(String namespace, InMemoryTypeMapping typeMapping, String idPropName, Map<String, InMemoryEntityInfo<?>> eis,
                                                   Map<String, InMemoryComplexTypeInfo<?>> complexTypesInfo) {
        return new InMemoryEdmGenerator(namespace, containerName, typeMapping, ID_PROPNAME, eis, complexTypesInfo, this.flattenEdm);
    }

    @Override
    public MetadataProducer getMetadataProducer() {
        return metadataProducer;
    }




    /**
     * HTTP POST request accepted, issued on service/cacheName_put?params...&$filter=... URI
     *
     * @return
     */
    private BaseResponse callFunctionPut(String setNameWhichIsCacheName, String entryKey, CachedValue cachedValue,
                                         boolean ignoreReturnValues) {
        log.trace("Putting into " + setNameWhichIsCacheName + " cache, entryKey: " + entryKey + " value: " + cachedValue.toString());

        // TODO -- what to do in PUT when cache has indexing disabled??? Does it even reach JACKson and/or field bridge?
        // TODO -- or when indexing disabled, this is just ignored?

        if (ignoreReturnValues) {
            getCache(setNameWhichIsCacheName).withFlags(Flag.IGNORE_RETURN_VALUES).put(entryKey, cachedValue);
            log.trace("Put with IGNORE_RETURN_VALUES");
            return Responses.infinispanResponse(null, null, null, Response.Status.CREATED);
        } else {
            getCache(setNameWhichIsCacheName).put(entryKey, cachedValue);
            CachedValue resultOfPutForResponse = (CachedValue) getCache(setNameWhichIsCacheName).get(entryKey);
            log.trace("Put function, ignoring return values false, returning full get after put");
            return Responses.infinispanResponse(EdmSimpleType.STRING, "jsonValue", standardizeJSONresponse(
                    new StringBuilder(resultOfPutForResponse.getJsonValueWrapper().getJson())).toString(), Response.Status.CREATED);
        }
    }


    public BaseResponse callFunctionGet(String setNameWhichIsCacheName, String entryKey,
                                        QueryInfo queryInfo) {

        List<Object> queryResult = null;

        if (entryKey != null) {
            // ignore query and return value directly
            CachedValue value = (CachedValue) getCache(setNameWhichIsCacheName).get(entryKey);
            if (value != null) {
                log.trace("CallFunctionGet entry with key " + entryKey + " was found. Returning response with status 200.");
                return Responses.infinispanResponse(EdmSimpleType.STRING, "jsonValue", standardizeJSONresponse(
                        new StringBuilder(value.getJsonValueWrapper().getJson())).toString(), Response.Status.OK);
            } else {
                // no results found, clients will get 404 response
                log.trace("CallFunctionGet entry with key " + entryKey + " was not found. Returning response with status 404.");
                return Responses.infinispanResponse(null, null, null, Response.Status.NOT_FOUND);
            }

        } else {
            // NO ENTRY KEY -- query on document store expected

            // I need some filter
            // TODO: don't process filters when cache is not Queryable (Log warning + serve only direct gets) !!! perf+

            if (queryInfo.filter == null) {
                return Responses.error(new OErrorImpl("Parameter 'key' is not specified, therefore we want to get entries using query filter." +
                        " \n However, $filter is not specified as well."));
            }

            log.trace("Query report for $filter " + queryInfo.filter.toString());

            SearchManager searchManager = org.infinispan.query.Search.getSearchManager(getCache(setNameWhichIsCacheName));
            MapQueryExpressionVisitor mapQueryExpressionVisitor =
                    new MapQueryExpressionVisitor(searchManager.buildQueryBuilderForClass(CachedValue.class).get());
            mapQueryExpressionVisitor.visit(queryInfo.filter);

            // Query cache here adn get results based on constructed Lucene query
            CacheQuery queryFromVisitor = searchManager.getQuery(mapQueryExpressionVisitor.getBuiltLuceneQuery(),
                    CachedValue.class);
            // pass query result to the function final response
            queryResult = queryFromVisitor.list();

            log.trace(" \n\n SEARCH RESULTS GOT BY VISITOR!!! APPROACH: size:" + queryResult.size() + ":");

            for (Object one_result : queryResult) {
                log.trace(one_result);
            }

            // *********************************************************************************
            // We have set queryResult object containing list of results from querying the cache
            // Now apply other filters/order by/top/skip etc. requests

            if (queryInfo.top != null) {
                // client wants to only a specified count of first "top" results
                // use it for collections of objects??
            }

            if (queryInfo.skip != null) {
                // client wants to skip particular number of results -- skip them and return only what's requested
                // use it for collections of objects??

            }

            if (queryInfo.orderBy != null) {
                // TODO: I hope this can be definitely added into LUCENE QUERY builder and we can get back
                // already ordered objects
            }
        }

        int resultsCount = queryResult.size();
        if (resultsCount > 0) {
            StringBuilder sb = new StringBuilder();
            // build response

            // [ODATA SPEC]
            // signs marked as "---" are standardized by standardizeJSONresponse() function
            // part market as "/****/", "/" including will be passed to standardizeJSONresponse() function
            // result of standardizeJSONresponse() will be directly returned to clients

            // for more results, create array
            // --------/*************************/-
            // { "d" : [{ ... }, { ... }, { ... }] }

            // for one result, return just one stored JSON document
            // --------/*****/-
            // { "d" : { ... }}

            if (resultsCount > 1) {
                sb.append("["); // start array of results
            }


            int counter = 0;
            for (Object one_result : queryResult) {
                counter++;
                // stack more JSON strings responses if needed
                CachedValue cv = (CachedValue) one_result;
                sb.append(cv.getJsonValueWrapper().getJson());

//                sb.append("\n"); // for better readability?

                if ((resultsCount > 1) && (resultsCount > counter)) {
                    // delimit results inside of an array, don't add "," after the last one JSON
                    sb.append(", \n");
                }
            }

            if (resultsCount > 1) {
                sb.append("]"); // end array of results
            }

            log.trace("CallFunctionGet method... returning query results in JSON format: " + standardizeJSONresponse(sb).toString());
            return Responses.infinispanResponse(EdmSimpleType.STRING, "jsonValue", standardizeJSONresponse(sb).toString(), Response.Status.OK);
        } else {
            // no results found, clients will get 404 response
            return Responses.infinispanResponse(null, null, null, Response.Status.NOT_FOUND);
        }
    }

    public BaseResponse callFunctionRemove(String setNameWhichIsCacheName, String entryKey) {
        // TODO: later, avoid returning by using flag (or add option to call uri)
        CachedValue removed = (CachedValue) getCache(setNameWhichIsCacheName).remove(entryKey);
        return Responses.infinispanResponse(EdmSimpleType.STRING, "jsonValue", removed.getJsonValueWrapper().getJson(), Response.Status.OK);
    }

    public BaseResponse callFunctionReplace(String setNameWhichIsCacheName, String entryKey, CachedValue cachedValue) {

        log.trace("Replacing in " + setNameWhichIsCacheName + " cache, entryKey: " + entryKey + " value: " + cachedValue.toString());
        getCache(setNameWhichIsCacheName).replace(entryKey, cachedValue);

        // TODO: avoid this when FLAG don't return when put = true
//        if (flag) {
//            TODO: prepare BaseResponse? and FunctionResource.java for it!
//            return null; // clients will get NO_CONTENT response (It is successful kind of response!)
//        }
        return callFunctionGet(setNameWhichIsCacheName, entryKey, null);
    }

    /**
     * The heart of our producer.
     * We can go the way of having cacheName_get/put/delete/update and call particular aforementioned methods
     * (i.e. create, delete, update, get entity)
     * <p/>
     * TODO: implement cache management related functions like: Start, Stop etc. probably cacheName_stop
     * <p/>
     * <p/>
     * Use it like: http://localhost:8887/ODataInfinispanEndpoint.svc/mySpecialNamedCache_get?key=%27jsonKey1%27"
     * Use HTTP POST for create / PUT entity into the cache
     *
     * @param context
     * @param function
     * @param params
     * @param queryInfo
     * @return
     */
    @Override
    public BaseResponse callFunction(ODataContext context, EdmFunctionImport function, Map<String, OFunctionParameter> params,
                                     QueryInfo queryInfo) {


        // every function call need to have key or queryInfo specified
        // TODO: modify condition to not only .filter (but also other constraints?) queryInfo != null is not enough
        // TODO: it's not null for every case I think

        if (params.get("key") != null || queryInfo.filter != null) {

            String setNameWhichIsCacheName = function.getEntitySet().getName();
            CachedValue cachedValue = null;

            String entryKey = null;
            if (params.get("key") != null) {
                entryKey = params.get("key").getValue().toString();
            }

            // Extract client payload in case of POST(ispn put) and PUT(ispn replace)
            boolean extractClientPayload = (function.getHttpMethod().equals("POST") && function.getName().endsWith("_put")) ||
                    ((function.getHttpMethod().equals("PUT") && function.getName().endsWith("_replace")));

            InputStream jsonInputStream = null;

            if (extractClientPayload) {
                // raw JSON data
                OSimpleObject payloadOSimpleObject = (OSimpleObject) params.get("payload").getValue();

                jsonInputStream = (InputStream) payloadOSimpleObject.getValue();
                BufferedReader br = new BufferedReader(new InputStreamReader(new BufferedInputStream(jsonInputStream)));
                StringBuilder sb = new StringBuilder();
                String readLine;

                try {
                    while (((readLine = br.readLine()) != null)) {
                        sb.append(readLine);
                    }

                    log.trace("Client payload extracted for put or replace: " + sb.toString());
                    cachedValue = new CachedValue(sb.toString());

                } catch (Exception e) {
                    return Responses.error(new OErrorImpl("Problems with extracting jsonValue from payload. " + e.getMessage()));
                } finally {
                    try {
                        if (jsonInputStream != null) jsonInputStream.close();
                        if (br != null) br.close();
                    } catch (IOException e) {
                        log.error("Closing streams in InfinispanProducer failed. Method callFunction()." + e.getMessage());
                        e.printStackTrace();
                    }
                }
            }


            if (function.getHttpMethod().equals("POST") && function.getName().endsWith("_put")) {

                boolean ignoreReturnValues = false;
                if (params.get("IGNORE_RETURN_VALUES") != null) {
                    // still can be set to false (by user in URI)
                    log.trace("IGNORE_RETURN_VALUES value from URI parameter: " + params.get("IGNORE_RETURN_VALUES").getValue().toString());
                    ignoreReturnValues = Boolean.parseBoolean(params.get("IGNORE_RETURN_VALUES").getValue().toString());
                }
                log.trace("put, IGNORE_RETURN_VALUES set to: " + ignoreReturnValues);
                return callFunctionPut(setNameWhichIsCacheName, entryKey, cachedValue, ignoreReturnValues);
            }

            if (function.getHttpMethod().equals("GET") && function.getName().endsWith("_get")) {
                return callFunctionGet(setNameWhichIsCacheName, entryKey, queryInfo);
            }

            if (function.getHttpMethod().equals("DELETE") && function.getName().endsWith("_remove")) {
                return callFunctionRemove(setNameWhichIsCacheName, entryKey);
            }

            if (function.getHttpMethod().equals("PUT") && function.getName().endsWith("_replace")) {
                callFunctionReplace(setNameWhichIsCacheName, entryKey, cachedValue);
            }

            return Responses.error(new OErrorImpl(
                    " HTTP GET method AND cache method ending _get,\n" +
                            " HTTP POST method AND cache method ending _put,\n" +
                            " HTTP DELETE method AND cache method ending _remove\n" +
                            " OR HTTP PUT method AND cache method ending _replace was expected.\n" +
                            " Function name was: " + function.getName() + " HTTP method was: " + function.getHttpMethod()));

        }

        return Responses.error(new OErrorImpl("Parameter 'key' or $filter needs to be specified."));
    }

    @Override
    public <TExtension extends OExtension<ODataProducer>> TExtension findExtension(Class<TExtension> clazz) {
        return null;
    }


    /**
     * TODO: Do we really need this? Can we find another (more simple) way of registering entities and drop this?
     *
     * @param <TEntity>
     */
    public class InMemoryEntityInfo<TEntity> {

        // we are maintaining collection of these entities - they are mapped to EntitySetName in [eis] hash map
        String entitySetName;
        String entityTypeName;
        String[] keys;
        Class<TEntity> entityClass;
        Func<Iterable<TEntity>> get; //returning defined apply()

        Func1<Object, HashMap<String, Object>> id;
        PropertyModel properties;
        boolean hasStream;

        public String getEntitySetName() {
            return entitySetName;
        }

        public String getEntityTypeName() {
            return entityTypeName;
        }

        public String[] getKeys() {
            return keys;
        }

        public Class<TEntity> getEntityClass() {
            return entityClass;
        }

        public Func<Iterable<TEntity>> getGet() {
            return get;
        }

        public Func1<Object, HashMap<String, Object>> getId() {
            return id;
        }

        public PropertyModel getPropertyModel() {
            return properties;
        }

        public boolean getHasStream() {
            return hasStream;
        }

        public Class<?> getSuperClass() {
            return entityClass.getSuperclass() != null && !entityClass.getSuperclass().equals(Object.class) ? entityClass.getSuperclass() : null;
        }
    }


    /**
     * TODO: Can we simplify this even more?
     * <p/>
     * There is a workaround in method toEdmProperties(). Key and Value entity properties are directly considered as
     * byte[].class.
     */
    public class InMemoryEdmGenerator implements EdmGenerator {

        private static final boolean DUMP = false;
        //      private static void dump(String msg) { if (DUMP) System.out.println(msg); }
        private final Logger log = Logger.getLogger(InMemoryEdmGenerator.class.getName());
        private final String namespace;
        private final String containerName;
        protected final InMemoryTypeMapping typeMapping;
        protected final Map<String, InMemoryEntityInfo<?>> eis; // key: EntitySet name
        protected final Map<String, InMemoryComplexTypeInfo<?>> complexTypeInfo; // key complex type edm type name
        protected final List<EdmComplexType.Builder> edmComplexTypes = new ArrayList<EdmComplexType.Builder>();
        // Note, assumes each Java type will only have a single Entity Set defined for it.
        protected final Map<Class<?>, String> entitySetNameByClass = new HashMap<Class<?>, String>();
        // build these as we go now.
        protected Map<String, EdmEntityType.Builder> entityTypesByName = new HashMap<String, EdmEntityType.Builder>();
        protected Map<String, EdmEntitySet.Builder> entitySetsByName = new HashMap<String, EdmEntitySet.Builder>();
        protected final boolean flatten;

        public InMemoryEdmGenerator(String namespace, String containerName, InMemoryTypeMapping typeMapping,
                                    String idPropertyName, Map<String, InfinispanProducer.InMemoryEntityInfo<?>> eis,
                                    Map<String, InMemoryComplexTypeInfo<?>> complexTypes) {
            this(namespace, containerName, typeMapping, idPropertyName, eis, complexTypes, true);
        }

        public InMemoryEdmGenerator(String namespace, String containerName, InMemoryTypeMapping typeMapping,
                                    String idPropertyName, Map<String, InfinispanProducer.InMemoryEntityInfo<?>> eis,
                                    Map<String, InMemoryComplexTypeInfo<?>> complexTypes, boolean flatten) {
            this.namespace = namespace;
            this.containerName = containerName;
            this.typeMapping = typeMapping;
            this.eis = eis;
            this.complexTypeInfo = complexTypes;

            // if not registering, this is null
//         for (Map.Entry<String, InfinispanProducer.InMemoryEntityInfo<?>> e : eis.entrySet()) {
//            // e.getValue().entityClass = MyInternalCacheEntry , e.getKey() = "CacheEntries"
//            // e.getValue() = InMemoryEntityInfo
//            entitySetNameByClass.put(e.getValue().entityClass, e.getKey());
//         }
            this.flatten = flatten;
        }

        @Override
        public EdmDataServices.Builder generateEdm(EdmDecorator decorator) {

            List<EdmSchema.Builder> schemas = new ArrayList<EdmSchema.Builder>();
            List<EdmEntityContainer.Builder> containers = new ArrayList<EdmEntityContainer.Builder>();

//            createComplexTypes(decorator, edmComplexTypes);

            // creates id other basic SUPPORTED_TYPE properties(structural) entities
            createStructuralEntities(decorator);

            // TODO handle back references too
            // create hashmaps from sets

//            createNavigationProperties(associations, associationSets,
//                    entityTypesByName, entitySetsByName, entitySetNameByClass);

            EdmEntityContainer.Builder container = EdmEntityContainer.newBuilder().
                    setName(containerName).setIsDefault(true).
                    addEntitySets(entitySetsByName.values());

            // I need EntityType for EntitySet for EdmxFormatWriter

//         EdmSchema.Builder schema = EdmSchema.newBuilder().setNamespace(namespace).
//               addEntityTypes(entityTypesByName.values()).addAssociations(associations).
//               addEntityContainers(containers).addComplexTypes(edmComplexTypes);

            // fictional entity type to satisfy EdmxFormatWriter & EdmxFormatParser
//         EdmEntityType.Builder d = EdmEntityType.newBuilder().setBaseType("java.lang.String");
//            EdmEntityType.Builder eet = EdmEntityType.newBuilder().setNamespace(namespace).
//                    setName("java.lang.String").setHasStream(false);
            // java.lang.IllegalArgumentException: Root types must have keys
//         at org.odata4j.edm.EdmEntityType.<init>(EdmEntityType.java:54)


            // I don't have entity types, nor associations... however I need to register containers
//         EdmSchema.Builder schema = EdmSchema.newBuilder().setNamespace(namespace).addComplexTypes(edmComplexTypes).addEntityTypes(eet);
            EdmSchema.Builder schema = EdmSchema.newBuilder().setNamespace(namespace).addComplexTypes(edmComplexTypes);

            addFunctions(container);

            // FIXED *****************************************
            // FIXED add container after function registration / functions are added directly into container object
            containers.add(container);

            if (decorator != null) {
                schema.setDocumentation(decorator.getDocumentationForSchema(namespace));
                schema.setAnnotations(decorator.getAnnotationsForSchema(namespace));
            }

            // FIXED ********************************
            // FIXED add containers into schema so I can get it in Metadata and properly find function imports in EdmDataServices
            schema.addEntityContainers(containers);
            schemas.add(schema);
            EdmDataServices.Builder rt = EdmDataServices.newBuilder().addSchemas(schemas);
            if (decorator != null) {
                rt.addNamespaces(decorator.getNamespaces());
            }
            return rt;
        }


        // TODO: use decorator in other way if needed
        private void createStructuralEntities(EdmDecorator decorator) {
            // eis contains all of the registered entity sets.
            for (String entitySetName : eis.keySet()) {

                // TODO !!! RETURN BACK check in odata4j libs! See diff and return it back a resolve problem here!!!

                // TODO: it is necessary to set EdmEntityType for metadata to work

                EdmEntityType.Builder eet = EdmEntityType.newBuilder().setNamespace(namespace).
                        setName("JsonDocument").setBaseType("Edm.String").setHasStream(false);

                // Root types mush have keys, add keys
                // TODO: find out how to link/get entities with dependence on this key
                List<String> keysForRootEdmType = new ArrayList<String>();
                keysForRootEdmType.add("rootTypeKey");
                eet.addKeys(keysForRootEdmType);

                EdmEntitySet.Builder ees = EdmEntitySet.newBuilder().setName(entitySetName).setEntityType(eet);

                ees.setDocumentation(new EdmDocumentation("This EDM EntitySet represents Infinispan cache." +
                        " Cache is ready for storing JSON documents. " +
                        "Enable indexing in Infinispan configuration XML file for searching capabilities. " +
                        "Base EdmEntityType is Edm.STRING (being able to return JSON entities).",

                        "org.infinispan.odata.producer.CachedValue objects " +
                        "are stored into the Infinispan cache. CachedValue instance encapsulates " +
                                "org.infinispan.odata.producer.JsonValueWrapper instance, which wraps " +
                                "JSON entity as a String."));

//                EdmEntitySet.Builder ees = EdmEntitySet.newBuilder().setName(entitySetName);
                entitySetsByName.put(ees.getName(), ees);
            }
        }

        protected InfinispanProducer.InMemoryEntityInfo<?> findEntityInfoForClass(Class<?> clazz) {
            for (InfinispanProducer.InMemoryEntityInfo<?> typeInfo : this.eis.values()) {
                if (typeInfo.entityClass.equals(clazz)) {
                    return typeInfo;
                }
            }
            return null;
        }







        // TODO: WE PROBABLY DON'T NEED THESE 2 CLASSES below
        /*
        * contains all generated InMemoryEntityInfos that get created as we walk
        * up the inheritance hierarchy and find Java types that are not registered.
        */
        private Map<Class<?>, InfinispanProducer.InMemoryEntityInfo<?>> unregisteredEntityInfo =
                new HashMap<Class<?>, InfinispanProducer.InMemoryEntityInfo<?>>();

        protected InfinispanProducer.InMemoryEntityInfo<?> getUnregisteredEntityInfo(Class<?> clazz, InfinispanProducer.InMemoryEntityInfo<?> subclass) {
            InfinispanProducer.InMemoryEntityInfo<?> ei = unregisteredEntityInfo.get(clazz);
            if (ei == null) {
                ei = new InfinispanProducer.InMemoryEntityInfo();
                ei.entityTypeName = clazz.getSimpleName();
                ei.keys = subclass.keys;
                ei.entityClass = (Class) clazz;
                ei.properties = new EnumsAsStringsPropertyModelDelegate(
                        new BeanBasedPropertyModel(ei.entityClass, this.flatten));
            }
            return ei;
        }

        protected EdmEntityType.Builder createStructuralType(EdmDecorator decorator, InfinispanProducer.InMemoryEntityInfo<?> entityInfo) {

            Class<?> superClass = flatten ? null : entityInfo.getSuperClass();


            EdmEntityType.Builder eet = EdmEntityType.newBuilder().setNamespace(namespace).
                    setName(entityInfo.entityTypeName).setHasStream(entityInfo.hasStream);

            if (superClass == null) {
                eet.addKeys(entityInfo.keys);
            }

            if (decorator != null) {
                eet.setDocumentation(decorator.getDocumentationForEntityType(namespace, entityInfo.entityTypeName));
                eet.setAnnotations(decorator.getAnnotationsForEntityType(namespace, entityInfo.entityTypeName));
            }
            entityTypesByName.put(eet.getName(), eet);

            EdmEntityType.Builder superType = null;
            if (!this.flatten && entityInfo.entityClass.getSuperclass() != null && !entityInfo.entityClass.getSuperclass().equals(Object.class)) {
                InfinispanProducer.InMemoryEntityInfo<?> entityInfoSuper = findEntityInfoForClass(entityInfo.entityClass.getSuperclass());
                // may have created it along another branch in the hierarchy
                if (entityInfoSuper == null) {
                    // synthesize...
                    entityInfoSuper = getUnregisteredEntityInfo(entityInfo.entityClass.getSuperclass(), entityInfo);
                }

                superType = entityTypesByName.get(entityInfoSuper.entityTypeName);
                if (superType == null) {
                    superType = createStructuralType(decorator, entityInfoSuper);
                }
            }

            eet.setBaseType(superType);
            return eet;
        }


        /**
         * Function definitions it defines and add functions into EDM Schema these functions are callable as GET HTTP
         * operations
         * <p/>
         * <p/>
         * TODO: Define cache operations: stop, start etc.
         * <p/>
         * provides an override point for applications to add application specific EdmFunctions to their producer.
         * <p/>
         * note: if function getReturnType returns null it returns nothing in ConsumerFunctionCallRequest
         *
         * @param container the EdmEntityContainer.Builder
         */
        protected void addFunctions(EdmEntityContainer.Builder container) {

            List<EdmFunctionImport.Builder> funcImports = new LinkedList<EdmFunctionImport.Builder>();


            for (int i = 0; i<container.getEntitySets().size(); i++) {
                // define functions for each entity set (= each cache)

                String entitySetNameCacheName = container.getEntitySets().get(i).getName();
                List<EdmFunctionParameter.Builder> funcParameters = new LinkedList<EdmFunctionParameter.Builder>();
                List<EdmFunctionParameter.Builder> flagsFuncParameters = new LinkedList<EdmFunctionParameter.Builder>();

                EdmFunctionParameter.Builder pbKey = new EdmFunctionParameter.Builder();
                EdmFunctionParameter.Builder pbIgnoreReturnValues = new EdmFunctionParameter.Builder();

                // for POST, GET, DELETE and PUT method
                // It is needed to add function parameter for all values (FLAGS) which users need to pass through URI
                pbKey.setName("key").setType(EdmType.getSimple("String")).setNullable(true).build();
                funcParameters.add(pbKey);

                // TODO: add other FLAGS (ASYNC calls)
                pbIgnoreReturnValues.setName("IGNORE_RETURN_VALUES").setType(EdmType.getSimple("String")).setNullable(true).build();
                flagsFuncParameters.add(pbIgnoreReturnValues);


                // only cache name function for method POST requests
                EdmFunctionImport.Builder fbPut = new EdmFunctionImport.Builder();
                EdmFunctionImport.Builder fbGet = new EdmFunctionImport.Builder();
                EdmFunctionImport.Builder fbRemove = new EdmFunctionImport.Builder();
                EdmFunctionImport.Builder fbReplace = new EdmFunctionImport.Builder();

                // TODO: add basic cache operations (start, stop)
                // These will be whole function imports defaultCache_start (HTTP GET on this will start it)


                // OData spec. HINT
//                IsBindable - 'true' indicates that the first parameter is the binding parameter
//                IsSideEffecting - 'true' defines an action rather than a function
//                IsAlwaysBindable - 'false' defines that the binding can be conditioned to the entity state.

                // IMPORTANT TASK perf+
                // Parent TODO: implement also async variants + maybe do it with advanced cache
                // TODO: and expect some flags during calls in special parameters /cache_put?key='key1'&flags='IGNORE_RETURN_VALUE,ASYNC'

                // TODO: do it like iteration through enum GET POST DELETE PUT and change Http method inside!!
                // TODO: not 4 imports, duplicate code



                // for HTTP POST (gather and emulates POST request for createEntity)
                fbPut.setName(entitySetNameCacheName + "_put")
                        .setEntitySet(container.getEntitySets().get(i))
                        .setEntitySetName(entitySetNameCacheName)
                        .setReturnType(EdmSimpleType.STRING)
                                // by specifying http method, we make from this "function" a SERVICE OPERATION kind of a "function"
                        .setHttpMethod("POST")
                        .setBindable(false)
                        .setSideEffecting(true)  // true for Action (POST)
                        .setAlwaysBindable(false)
                        .addParameters(funcParameters)
                        .addParameters(flagsFuncParameters)
                        .build();

                fbPut.setDocumentation(new EdmDocumentation("Use this function for putting JSON documents into the Infinispan cache. " +
                        "Specify the key of the entry and set BODY of the HTTP POST.",
                        "FLAG parameter IGNORE_RETURN_VALUES can be set up. " +
                                "Usage: serviceUri.svc/" + entitySetNameCacheName + "_put?IGNORE_RETURN_VALUES='false'&key='key1'"));



                fbGet.setName(entitySetNameCacheName + "_get")
                                .setEntitySet(container.getEntitySets().get(i))
                                .setEntitySetName(entitySetNameCacheName)
                                .setReturnType(EdmSimpleType.STRING)
                                .setHttpMethod("GET")
                                .setBindable(false)
                                .setSideEffecting(true)  // true for Action (POST)
                                .setAlwaysBindable(false)
                                .addParameters(funcParameters).build();

                fbGet.setDocumentation(new EdmDocumentation("Use this function for getting JSON documents from the Infinispan cache. " +
                        "Specify the key of the entry OR $filter parameter for querying documents across JSON fields.",
                        "Usage: serviceUri.svc/" + entitySetNameCacheName + "_get?key='key1' or " +
                                "serviceUri.svc/" + entitySetNameCacheName + "_get?$filter name eq 'John'"));


                fbRemove.setName(entitySetNameCacheName + "_remove")
                        .setEntitySet(container.getEntitySets().get(i))
                        .setEntitySetName(entitySetNameCacheName)
                        .setReturnType(EdmSimpleType.STRING)
                        .setHttpMethod("DELETE")
                        .setBindable(false)
                        .setSideEffecting(true)  // true for Action (POST)
                        .setAlwaysBindable(false)
                        .addParameters(funcParameters).build();

                fbReplace.setName(entitySetNameCacheName + "_replace")
                        .setEntitySet(container.getEntitySets().get(i))
                        .setEntitySetName(entitySetNameCacheName)
                        .setReturnType(EdmSimpleType.STRING)
                        .setHttpMethod("PUT")
                        .setBindable(false)
                        .setSideEffecting(true)  // true for Action (POST)
                        .setAlwaysBindable(false)
                        .addParameters(funcParameters)
                        .addParameters(flagsFuncParameters)
                        .build();

                funcImports.add(fbPut);
                funcImports.add(fbGet);
                funcImports.add(fbRemove);
                funcImports.add(fbReplace);
            }

            dump("Functions import ok...");
            container.addFunctionImports(funcImports);
        }
    }


    /**
     * [ODATA STANDARD]
     *
     * Encapsulates JSON string into OData standard format for service returns.
     * JSON values are coming RAW (puts):
     * <p/>
     * {"entityClass":"org.my.domain.person","gender":"MALE",
     * "verified":false,"age":24,"firstname":"Neo","lastname":"Matrix McMaster"}
     * <p/>
     * and have to be returned (gets) in standard format ("d" stands for "data"):
     * <p/>
     * { "d" :
     * {"entityClass":"org.my.domain.person","gender":"MALE",
     * "verified":false,"age":26,"firstname":"Neo","lastname":"Matrix McMaster"}
     * }
     *
     * <p/>
     * Pattern for collection of returned entries (array of entries):
     * // { "d" : [{ ... }, { ...}, { ... }]}
     *
     *
     * @param value -- StringBuilder containing raw JSON string or array of JSON strings encapsulated in []
     * @return standardized StringBuilder object for return to clients
     */
    private StringBuilder standardizeJSONresponse(StringBuilder value) {
        StringBuilder sb = new StringBuilder();
        sb.append("{ \"d\" : ");
        sb.append(value.toString());
        sb.append("}");
        return sb;
    }




    // ***********************
    // NOT SUPPORTED FUNCTIONS
    // ***********************

    @Override
    public void close() {
    }

    // Not supported -- use defined OData functions
    @Override
    public EntitiesResponse getEntities(ODataContext context, String entitySetName, final QueryInfo queryInfo) {
        // returning all entities from set/cache
        // calling like
        // http://localhost:8887/ODataInfinispanEndpoint.svc/mySpecialNamedCache
        throw new NotImplementedException("getEntities not yet implemented. Use service operations as defined in $metadata.");
    }


    // Not supported -- use defined OData functions
    @Override
    public CountResponse getEntitiesCount(ODataContext context, final String entitySetName, final QueryInfo queryInfo) {
        throw new NotImplementedException("getEntitiesCount not yet implemented. Use service operations as defined in $metadata.");
    }

    // Not supported -- use defined OData functions
    @Override
    public EntityResponse getEntity(ODataContext context, String entitySetName, OEntityKey entityKey, EntityQueryInfo queryInfo) {
//        // Is this faster than return simple get through function?
//        // No, odata4j is not closing streams somewhere as fast as we need.

//        // http://localhost:8887/ODataInfinispanEndpoint.svc/mySpecialNamedCache('something') <--- entity key

//        String entryKey = entityKey.toKeyStringWithoutParentheses().replace("'", "");
//        log.info("\n\n getEntity -- entryKey set to " + entryKey + " \n\n");
//        String setNameWhichIsCacheName = entitySetName;
//        EdmEntityType.Builder eet = EdmEntityType.newBuilder().setNamespace(namespace).
//                setName("JsonDocument").setBaseType("Edm.String").setHasStream(false);
//        List<String> keysForRootEdmType = new ArrayList<String>();
//        keysForRootEdmType.add("rootTypeKey");
//        eet.addKeys(keysForRootEdmType);
//        EdmEntitySet.Builder ees = EdmEntitySet.newBuilder().setName(entitySetName).setEntityType(eet);
//        // get
//        final CachedValue cachedValue = (CachedValue) getCache(setNameWhichIsCacheName).get(entryKey);
//        final List<OProperty<?>> properties = new ArrayList<OProperty<?>>();
//        properties.add(new OProperty<Object>() {
//            @Override
//            public EdmType getType() {
//                return EdmType.getSimple("Edm.String");
//            }
//
//            @Override
//            public Object getValue() {
//                return cachedValue.getJsonValueWrapper().getJson().toString();
//            }
//
//            @Override
//            public String getName() {
//                return "jsonValue";
//            }
//        });
//        final Map<String, Object> keyKVPair = new HashMap<String, Object>();
//        keyKVPair.put("rootTypeKey", entryKey);
////      ??  EdmEntityType edmEntityType = (EdmEntityType) this.getMetadata().findEdmEntityType(namespace + "." + entitySetName);
//        OEntityKey oekey = OEntityKey.create(keyKVPair);
//        OEntity oe = OEntities.create(ees.build(), eet.build(), oekey, properties, null);
//        // Todo return error if anything went wrong (parsing the key?)
//        return Responses.entity(oe);

        throw new NotImplementedException("getEntity not yet implemented. Use service operations as defined in $metadata.");
    }

    // Not supported -- use defined OData functions
    @Override
    public void mergeEntity(ODataContext context, String entitySetName, OEntity entity) {
        throw new NotImplementedException("mergeEntity not yet implemented. Use service operations as defined in $metadata.");
    }

    // Not supported -- use defined OData functions
    @Override
    public void updateEntity(ODataContext context, String entitySetName, OEntity entity) {
        throw new NotImplementedException("updateEntity not yet implemented. Use service operations as defined in $metadata.");
    }


    // Not supported -- use defined OData functions
    @Override
    public void deleteEntity(ODataContext context, String entitySetName, OEntityKey entityKey) {
        throw new NotImplementedException("deleteEntity not yet implemented. Use service operations as defined in $metadata.");
    }

    // Not supported -- use defined OData functions
    @Override
    public EntityResponse createEntity(ODataContext context, String entitySetName, final OEntity entity) {
        throw new NotImplementedException("createEntity not yet implemented. Use service operations as defined in $metadata.");
    }

    // Not supported -- use defined OData functions
    @Override
    public EntityResponse createEntity(ODataContext context, String entitySetName, OEntityKey entityKey, String navProp, OEntity entity) {
        throw new NotImplementedException("createEntity (with navProp) not yet implemented. Use service operations as defined in $metadata.");
    }

    // Not supported (How to navigate entities inside NOSQL, schema-less store?)
    @Override
    public BaseResponse getNavProperty(ODataContext context, String entitySetName, OEntityKey entityKey, String navProp, QueryInfo queryInfo) {
        throw new NotImplementedException("getNavProperty not yet implemented. Use service operations as defined in $metadata.");
    }

    // Not supported (How to navigate entities inside NOSQL, schema-less store?)
    @Override
    public CountResponse getNavPropertyCount(ODataContext context, String entitySetName, OEntityKey entityKey, String navProp, QueryInfo queryInfo) {
        throw new NotImplementedException("getNavPropertyCount not yet implemented. Use service operations as defined in $metadata.");
    }

    // Not supported (How to navigate entities inside NOSQL, schema-less store? Any links here? Check OData and confirm.)
    @Override
    public EntityIdResponse getLinks(ODataContext context, OEntityId sourceEntity, String targetNavProp) {
        throw new NotImplementedException("getLinks not yet implemented. Use service operations as defined in $metadata.");
    }

    // Not supported (How to navigate entities inside NOSQL, schema-less store? Any links here? Check OData and confirm.)
    @Override
    public void createLink(ODataContext context, OEntityId sourceEntity, String targetNavProp, OEntityId targetEntity) {
        throw new NotImplementedException("createLink not yet implemented. Use service operations as defined in $metadata.");
    }

    // Not supported (How to navigate entities inside NOSQL, schema-less store? Any links here? Check OData and confirm.)
    @Override
    public void updateLink(ODataContext context, OEntityId sourceEntity, String targetNavProp, OEntityKey oldTargetEntityKey, OEntityId newTargetEntity) {
        throw new NotImplementedException("updateLink not yet implemented. Use service operations as defined in $metadata.");
    }

    // Not supported (How to navigate entities inside NOSQL, schema-less store? Any links here? Check OData and confirm.)
    @Override
    public void deleteLink(ODataContext context, OEntityId sourceEntity, String targetNavProp, OEntityKey targetEntityKey) {
        throw new NotImplementedException("deleteLink not yet implemented. Use service operations as defined in $metadata.");
    }
}






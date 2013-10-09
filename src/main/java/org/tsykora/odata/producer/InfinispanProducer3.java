package org.tsykora.odata.producer;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.core4j.Func;
import org.core4j.Func1;
import org.infinispan.Cache;
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
import org.tsykora.odata.common.CacheObjectSerializationAble;

/**
 * ODataProducer with implemented direct access to Infinispan Cache.
 */
public class InfinispanProducer3 implements ODataProducer {

    private static final boolean DUMP = false;
    private final Logger log = Logger.getLogger(InfinispanProducer3.class.getName());

    private static void dump(Object msg) {
        if (DUMP) {
            System.out.println(msg);
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

    // TODO: properly decide: not static??? - cache instance is running with producer instance
    private DefaultCacheManager defaultCacheManager;
    // for faster cache access
    private HashMap<String, Cache> caches = new HashMap<String, Cache>();


    /**
     * Creates a new instance of an in-memory POJO producer.
     *
     * @param namespace the namespace of the schema registrations
     */
    public InfinispanProducer3(String namespace, String ispnConfigFile) {
        this(namespace, DEFAULT_MAX_RESULTS, ispnConfigFile);
    }

    /**
     * Creates a new instance of an in-memory POJO producer.
     *
     * @param namespace  the namespace of the schema registrations
     * @param maxResults the maximum number of entities to return in a single call
     */
    public InfinispanProducer3(String namespace, int maxResults, String ispnConfigFile) {
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
    public InfinispanProducer3(String namespace, String containerName, int maxResults, EdmDecorator decorator, InMemoryTypeMapping typeMapping,
                               String ispnConfigFile) {
        this(namespace, containerName, maxResults, decorator, typeMapping,
                true, ispnConfigFile); // legacy: flatten edm
    }

    /**
     * Do everything important here while creating new producer instance.
     */
    public <TEntity, TKey> InfinispanProducer3(String namespace, String containerName, int maxResults,
                                               EdmDecorator decorator, InMemoryTypeMapping typeMapping,
                                               boolean flattenEdm, String ispnConfigFile) {
        this.namespace = namespace;
        this.containerName = containerName != null && !containerName.isEmpty() ? containerName : "Container";
        this.maxResults = maxResults;
        this.decorator = decorator;
        this.metadataProducer = new MetadataProducer(this, decorator);
        this.typeMapping = typeMapping == null ? InMemoryTypeMapping.DEFAULT : typeMapping;
        this.flattenEdm = flattenEdm;


        // TODO add possibility for passing configurations (global, local)
        try {
            // true = start it + start defined caches
            defaultCacheManager = new DefaultCacheManager(ispnConfigFile, true);


            Set<String> cacheNames = defaultCacheManager.getCacheNames();

            for (String cacheName : cacheNames) {
//            dump("Starting cache with name " + cacheName + " on defaultCacheManager...");
//            defaultCacheManager.startCache(cacheName);

                dump("Registering cache with name " + cacheName + " in Producer...");
                // cacheName = entitySetName
                eis.put(cacheName, null);
            }

        } catch (IOException e) {
            System.out.println(" PROBLEMS WITH CREATING DEFAULT CACHE MANAGER! ");
            e.printStackTrace();
        }


    }

    /**
     * Look into global map for registered cache. Avoiding multiple asking CacheManager.
     * <p/>
     * If there is no cache with given name, get it from CacheManager and store.
     *
     * @param cacheName
     * @return
     */
    private Cache getCache(String cacheName) {
        if (caches.get(cacheName) != null) {

            return this.caches.get(cacheName);

        } else {

            try {
                System.out.println("Starting cache with name " + cacheName + " on defaultCacheManager...");
                defaultCacheManager.startCache(cacheName);
                System.out.println("Cache started!....");
                Cache cache = defaultCacheManager.getCache(cacheName);

                cache.put("simpleKey1", "simpleValue1"); // starts cache
                dump("Cache " + cacheName + " status: " + cache.getStatus());

                System.out.println(" simpleKey1\", \"simpleValue1 ------ PUTTED INTO CACHE, now some json stuff:");


//                // put this into the cache
//                // TODO: how about that -- "d" : { -- // and these rules for OData (input, output)
//                String json = "{\n" +
//                        "  \"name\" : { \"first\" : \"Neo\", \"last\" : \"Matrix McMaster\" },\n" +
//                        "  \"gender\" : \"MALE\",\n" +
//                        "  \"verified\" : false,\n" +
//                        "  \"age\" : 24,\n" +
//                        "  \"firstname\" : \"Neo\",\n" +
//                        "  \"lastname\" : \"Matrix McMaster\"" +
//                        "}";
//                // try query stuff here
//                CachedValue neo = new CachedValue(json);
//                // all entries are "under" simple String key (that's similar to REST)
//                cache.put("theFirst", neo);
//
//
//                // get the search manager from the cache:
//                SearchManager searchManager = org.infinispan.query.Search.getSearchManager(cache);
//                QueryBuilder queryBuilder = searchManager.buildQueryBuilderForClass(CachedValue.class).get();
//                Query luceneQuery = queryBuilder.phrase()
//                        .onField("gender")
//                        .sentence("MALE")
//                        .createQuery();
//                CacheQuery query = searchManager.getQuery(luceneQuery, CachedValue.class);
//                List<Object> objectList = query.list();
//                System.out.println(" \n\n SEARCH RESULTS: size:" + objectList.size() + ":");
//                for (Object b : objectList) {
//                    System.out.println(b);
//                }


                this.caches.put(cacheName, cache);
                return cache;
            } catch (Exception e) {
                System.out.println(" \n\n ***** ERROR DURING STARTING CACHE __ DURING EXPERIMENTS WITH INDEXING ***** \n\n");
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

    @Override
    public void close() {
    }


    /**
     * Is returning all entries from local cache.
     * TODO:? Really? Performance bottleneck? cache.values() --> into looong JSON (restrict number of returned entries)
     *
     * @param entitySetName - cache name
     * @param queryInfo     - other special restrictions??
     * @return
     */
    @Override
    public EntitiesResponse getEntities(ODataContext context, String entitySetName, final QueryInfo queryInfo) {
        throw new NotImplementedException();
    }


    /**
     * TODO: Easy - support this. Return JSON. (details: return SimpleResponse --> INT --> goes to JSON)
     *
     * @param context
     * @param entitySetName
     * @param queryInfo
     * @return
     */
    @Override
    public CountResponse getEntitiesCount(ODataContext context, String entitySetName, final QueryInfo queryInfo) {
        throw new NotImplementedException();
    }

    /**
     * I need probably need to support this as it is HTTP GET for a particular cache entry.
     * Key should be simple String and CacheValue's JSON should be returned to the client.
     * <p/>
     * This should EDM.String SimpleResponse with application/json setting.
     * <p/>
     * TODO? make entityKey corresponds with Key of entry in the cache?
     */
    @Override
    public EntityResponse getEntity(ODataContext context, final String entitySetName, final OEntityKey entityKey, final EntityQueryInfo queryInfo) {
        throw new NotImplementedException();
    }

    /**
     * TODO - find ISPN equivalent support for it or decide about not supporting this operation at all.
     */
    @Override
    public void mergeEntity(ODataContext context, String entitySetName, OEntity entity) {
        // merge - what is equal to merge in ISPN?
        throw new NotImplementedException();
    }

    /**
     * Simple update of cached entry.
     * This is HTTP UPDATE call. (It has to contain "the load" as well as POST for create entity.)
     * <p/>
     * TODO: we definitely need to support this.
     *
     * @param context
     * @param entitySetName
     * @param entity
     */
    @Override
    public void updateEntity(ODataContext context, String entitySetName, OEntity entity) {
        // simple update entry and re-call
        throw new NotImplementedException();
    }


    /**
     * Simple delete of cached entry.
     * This is HTTP DELETE call, based only on cached entry related simple String key.
     * <p/>
     * TODO: we definitely need to support this.
     *
     * @param context
     * @param entitySetName
     * @param entityKey
     */
    @Override
    public void deleteEntity(ODataContext context, String entitySetName, OEntityKey entityKey) {
        // simple remove entry and re-call
        throw new NotImplementedException();
    }

    /**
     * TODO: support this!
     * <p/>
     * This is HTTP POST with given "load". The "load" is JSON format.
     * Given JSON is encapsulated into CachedValue under JsonValueWrapper field and the whole entry
     * is putted into the Infinispan cache "under" given simple String key.
     *
     * @param entitySetName - cache name identifier
     * @param entity
     * @return
     */
    @Override
    public EntityResponse createEntity(ODataContext context, String entitySetName, final OEntity entity) {

        System.out.println(" \n\n !!!!!!!!!!!!! CREATE ENTITY ECHO: ");
        System.out.println("context: " + context);
        System.out.println("entitySetName: " + context);
        System.out.println(" OEntity: " + entity);

        // whats carried inside of entity?

        BaseResponse baseResponse = Responses.simple(EdmSimpleType.STRING,
                "Status of entity creation", "Entry was put into the cache");
        return (EntityResponse) baseResponse;
    }

    // Not supported
    @Override
    public EntityResponse createEntity(ODataContext context, String entitySetName, OEntityKey entityKey, String navProp, OEntity entity) {

        System.out.println(" \n\n CREATE ENTITY INCLUDING ENTITY KEY ECHO: ");
        System.out.println("context: " + context);
        System.out.println("entitySetName: " + context);
        System.out.println(" OEntity: " + entity);

        // whats carried inside of entity?

        BaseResponse baseResponse = Responses.simple(EdmSimpleType.STRING,
                "Status of entity creation", "Entry was put into the cache");
        return (EntityResponse) baseResponse;
    }

    // Not supported (How to navigate entities inside NOSQL, schema-less store?)
    @Override
    public BaseResponse getNavProperty(ODataContext context, String entitySetName, OEntityKey entityKey, String navProp, QueryInfo queryInfo) {
        throw new NotImplementedException();
    }

    // Not supported (How to navigate entities inside NOSQL, schema-less store?)
    @Override
    public CountResponse getNavPropertyCount(ODataContext context, String entitySetName, OEntityKey entityKey, String navProp, QueryInfo queryInfo) {
        throw new NotImplementedException();
    }

    // Not supported (How to navigate entities inside NOSQL, schema-less store? Any links here? Check OData and confirm.)
    @Override
    public EntityIdResponse getLinks(ODataContext context, OEntityId sourceEntity, String targetNavProp) {
        throw new NotImplementedException();
    }

    // Not supported (How to navigate entities inside NOSQL, schema-less store? Any links here? Check OData and confirm.)
    @Override
    public void createLink(ODataContext context, OEntityId sourceEntity, String targetNavProp, OEntityId targetEntity) {
        throw new NotImplementedException();
    }

    // Not supported (How to navigate entities inside NOSQL, schema-less store? Any links here? Check OData and confirm.)
    @Override
    public void updateLink(ODataContext context, OEntityId sourceEntity, String targetNavProp, OEntityKey oldTargetEntityKey, OEntityId newTargetEntity) {
        throw new NotImplementedException();
    }

    // Not supported (How to navigate entities inside NOSQL, schema-less store? Any links here? Check OData and confirm.)
    @Override
    public void deleteLink(ODataContext context, OEntityId sourceEntity, String targetNavProp, OEntityKey targetEntityKey) {
        throw new NotImplementedException();
    }

    /**
     * The heart of our producer.
     * We can go the way of having cacheName_get/put/delete/update and call particular aforementioned methods
     * (i.e. create, delete, update, get entity)
     * <p/>
     * TODO: implement real - cache related functions like: Start, Stop etc.
     * <p/>
     * <p/>
     * Use it like: http://localhost:8887/ODataInfinispanEndpoint.svc/mySpecialNamedCache?key=%27jsonKey1%27"
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

        System.out.println(" Echo from callFunction() in InfinispanProducer... Yup, I'm here... ");

        long startCallFunctionProducerInside = System.currentTimeMillis();

        OEntityKey oentityKey = null;
        CacheObjectSerializationAble keyObject = null;
        CacheObjectSerializationAble valueObject = null;
        String simpleKey = null;
        String simpleValue = null;

        String setNameWhichIsCacheName = function.getEntitySet().getName();

        // for getting cache name (function is bindable to this entity set (which is collection type))
        // function.getEntitySet();

        dump("Params passed into callFunction method in Producer:");
        for (String paramKey : params.keySet()) {
            if (params.get(paramKey) != null) {
                dump(paramKey + "=" + params.get(paramKey).getValue() + " of type: " + params.get(paramKey).getType());
            } else {
                dump(paramKey + "=" + params.get(paramKey) + " is null");
            }
        }


        // TODO HERE!
        // THIS IS POST REQUEST TO URI: http://localhost:8887/ODataInfinispanEndpoint.svc/mySpecialNamedCache?key=%27jsonKey1%27"
        // and we need to extract body/content/entity of POST, containing JSON
        // why is this JSON encoded into some bytes?
        if (params.get("key") != null || queryInfo.filter != null) {


            // *********************** POST - PUT entry *******************
            // *********************** POST - PUT entry *******************
            // *********************** POST - PUT entry *******************

            if (function.getHttpMethod().equals("POST") && function.getName().endsWith("_put")) {
                // need key + payload
                String jsonValue = "";

                // Now I have passed directly ByteArrayInputStream payload in function parameter
                // FunctionResources were trying to parse it (TODO: disable it later for perf. not do this redundant!!)
                // and parse it here...

                // or avoid encoding into ByteArrayInputStream?
                // or check JSON format...

                System.out.println("Parameter key is not null -- this looks like POST http request. " +
                        "Process it in callFunction inside of IspnProducer3...");


                OSimpleObject payloadOSimpleObject = (OSimpleObject) params.get("payload").getValue();
                if (payloadOSimpleObject.getType() != EdmSimpleType.BINARY) {
                    System.out.println(" ERROR !!! I expected BINARY stuff here in payload !!! ");
                    // TODO!! Error messages! Something like:
                    // Implement ERROR INTERFACE!!! for returning respective error responses
//                    return ErrorResponse;
                }


                // decode it for a string and store it into the cache
                try {


                    ByteArrayInputStream inputStream = (ByteArrayInputStream) payloadOSimpleObject.getValue();

                    BufferedReader rd = new BufferedReader(new InputStreamReader(inputStream));
                    String result = "";
                    String line;
                    while ((line = rd.readLine()) != null) {
                        result += line;
                    }
                    System.out.println("I'VE JUST READ payload in CALL FUNCTION in PRODUCER.... it is payload from HTTP POST request ");
                    System.out.println(result);
                    rd.close();

                    jsonValue = result;

                    CachedValue cachedValue = new CachedValue(jsonValue);

                    // put wrapped json into the cache and let Lucene to index its fields
                    System.out.println(" THIS IS POST request, inside of callFunction() in producer: ");
                    String entryKey = params.get("key").getValue().toString();
                    System.out.println("Putting into " + setNameWhichIsCacheName + " cache, entryKey: " + entryKey + " value: " + cachedValue.toString());
                    getCache(setNameWhichIsCacheName).put(entryKey, cachedValue);


                } catch (Exception e) {
                    System.out.println(" ERROR !!! Exception " + e.getMessage());
                    e.printStackTrace();
                }

                BaseResponse baseResponse = Responses.simple(EdmSimpleType.STRING, "jsonValue", jsonValue);

                // Whole callFunction time measurement:
                long stopCallFunctionProducerInside = System.currentTimeMillis();
                System.out.println("Whole inside of CallFunction in producer before response took: " +
                        (stopCallFunctionProducerInside - startCallFunctionProducerInside) + " millis.");

                return baseResponse;
            }



            // ***************** GET *******************
            // ***************** GET *******************
            // ***************** GET *******************

            if (function.getHttpMethod().equals("GET") && function.getName().endsWith("_get")) {
                // need only key, return jsonValue

                BaseResponse response = null;

                CachedValue value = null;
                List<Object> queryResult = null;

                // parameter key was specified, ignore query and return value directly
                String key = params.get("key").getValue().toString();
                if (key != null) {

                    long start = System.currentTimeMillis();

                    value = (CachedValue) getCache(setNameWhichIsCacheName).get(key);

                    long end = System.currentTimeMillis();
                    System.out.println("Direct get from " + setNameWhichIsCacheName + " according to key parameter took: "
                            + (end - start) + " millis.");

                } else {
                    // I need some filter
                    // TODO: don't process filters when cache is not Queryable (Log warning + serve only direct gets) !!! perf+
                    if (queryInfo.filter != null) {

                        System.out.println("Query report for $filter " + queryInfo.filter.toString());

                        SearchManager searchManager = org.infinispan.query.Search.getSearchManager(getCache(setNameWhichIsCacheName));

                        MapQueryExpressionVisitor mapQueryExpressionVisitor =
                                new MapQueryExpressionVisitor(searchManager.buildQueryBuilderForClass(CachedValue.class).get());

                        mapQueryExpressionVisitor.visit(queryInfo.filter);

                        // Query cache here adn get results based on constructed Lucene query
                        CacheQuery queryFromVisitor = searchManager.getQuery(mapQueryExpressionVisitor.getBuiltLuceneQuery(),
                                CachedValue.class);

                        // pass query result to the function final response
                        queryResult = queryFromVisitor.list();

                        System.out.println(" \n\n SEARCH RESULTS GOT BY VISITOR!!! APPROACH: size:" + queryResult.size() + ":");
                        for (Object one_result : queryResult) {
                            System.out.println(one_result);
                        }

                    } else {
                        System.out.println("WARNING -- INCONSISTENT STATE: simpleKey NOR queryInfo.filter is not defined!!! ");
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


                long startBuildResponse = System.currentTimeMillis();

                // TODO: redundant -- don't create base responses here, create them immediately and return them once decided ^
                BaseResponse baseResponse = null;
                if (value != null) {
                    // it was requested by key directly
                    baseResponse = Responses.simple(EdmSimpleType.STRING, "jsonValue", value.getJsonValueWrapper().getJson());
                } else {
                    if (queryResult != null) {
                        StringBuilder sb = new StringBuilder();
                        // build response
                        for (Object one_result : queryResult) {
                            CachedValue cv = (CachedValue) one_result;
                            sb.append(cv.getJsonValueWrapper().getJson());
                            sb.append("\n");
                        }
                        // stack more json strings responses
                        baseResponse = Responses.simple(EdmSimpleType.STRING, "jsonValue", sb.toString());
                    }
                }


                long stopBuildResponse = System.currentTimeMillis();
                System.out.println("Building base response in the end of call function took: " +
                        (stopBuildResponse - startBuildResponse) + " millis.");


                // Whole callFunction time measurement:
                long stopCallFunctionProducerInside = System.currentTimeMillis();
                System.out.println("Whole inside of CallFunction in producer before response took: " +
                        (stopCallFunctionProducerInside - startCallFunctionProducerInside) + " millis.");

                // TODO: remove this redundancy (see ^)
                response = baseResponse;


                return response;
            }






            // ***************** DELETE *******************
            // ***************** DELETE *******************
            // ***************** DELETE *******************

            if (function.getHttpMethod().equals("DELETE") && function.getName().endsWith("_remove")) {
                // need only key, delete from cache

                String key = params.get("key").getValue().toString();
                if (key != null) {

                    long start = System.currentTimeMillis();

                    // TODO: later, avoid returning by using flag (or add option to call uri)
                    getCache(setNameWhichIsCacheName).remove(key);

                    long end = System.currentTimeMillis();
                    System.out.println("Direct get from " + setNameWhichIsCacheName + " according to key parameter took: "
                            + (end - start) + " millis.");

                    // TODO: create some types of successful message responses
                    return Responses.simple(EdmSimpleType.STRING, "SUCCESS",
                            "Entry specified by key " + key + " was deleted from cache: " + setNameWhichIsCacheName);

                } else {

                    return Responses.simple(EdmSimpleType.STRING, "ERROR",
                            "Parameter 'key' need to be specified for removing entry. HTTP DELETE method. ");
                }

            }



            // **************** PUT - UPDATE ***************
            // **************** PUT - UPDATE ***************
            // **************** PUT - UPDATE ***************

            if (function.getHttpMethod().equals("PUT") && function.getName().endsWith("_replace")) {
                String jsonValue = "";

                OSimpleObject payloadOSimpleObject = (OSimpleObject) params.get("payload").getValue();
                if (payloadOSimpleObject.getType() != EdmSimpleType.BINARY) {
                    System.out.println(" ERROR !!! I expected BINARY stuff here in payload !!! ");
                    // TODO!! Error messages! Something like:
                    // Implement ERROR INTERFACE!!! for returning respective error responses
//                    return ErrorResponse;
                }

                // decode it for a string and store it into the cache
                try {

                    ByteArrayInputStream inputStream = (ByteArrayInputStream) payloadOSimpleObject.getValue();

                    BufferedReader rd = new BufferedReader(new InputStreamReader(inputStream));
                    String result = "";
                    String line;
                    while ((line = rd.readLine()) != null) {
                        result += line;
                    }
                    System.out.println("I'VE JUST READ payload in CALL FUNCTION in PRODUCER.... it is payload from HTTP PUT request ");
                    System.out.println(result);
                    rd.close();

                    jsonValue = result;

                    CachedValue cachedValue = new CachedValue(jsonValue);

                    // put wrapped json into the cache and let Lucene to index its fields
                    System.out.println(" THIS IS POST request, inside of callFunction() in producer: ");
                    String entryKey = params.get("key").getValue().toString();
                    System.out.println("Replacing in " + setNameWhichIsCacheName + " cache, entryKey: " + entryKey + " value: " + cachedValue.toString());
                    getCache(setNameWhichIsCacheName).replace(entryKey, cachedValue);


                } catch (Exception e) {
                    System.out.println(" ERROR !!! Exception " + e.getMessage());
                    e.printStackTrace();
                }

                BaseResponse baseResponse = Responses.simple(EdmSimpleType.STRING, "jsonValue", jsonValue);

                // Whole callFunction time measurement:
                long stopCallFunctionProducerInside = System.currentTimeMillis();
                System.out.println("Whole inside of CallFunction in producer before response took: " +
                        (stopCallFunctionProducerInside - startCallFunctionProducerInside) + " millis.");

                return baseResponse;

            }

        }

        // TODO: exceptions need improvements!
        return Responses.simple(EdmSimpleType.STRING, "ERROR", "Error: GET, POST, DELETE or PUT Http method was expected. " +
                " OR: parameter key or $filter needs to be specified. " +
                "But it was: " + function.getHttpMethod());
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
                                    String idPropertyName, Map<String, InfinispanProducer3.InMemoryEntityInfo<?>> eis,
                                    Map<String, InMemoryComplexTypeInfo<?>> complexTypes) {
            this(namespace, containerName, typeMapping, idPropertyName, eis, complexTypes, true);
        }

        public InMemoryEdmGenerator(String namespace, String containerName, InMemoryTypeMapping typeMapping,
                                    String idPropertyName, Map<String, InfinispanProducer3.InMemoryEntityInfo<?>> eis,
                                    Map<String, InMemoryComplexTypeInfo<?>> complexTypes, boolean flatten) {
            this.namespace = namespace;
            this.containerName = containerName;
            this.typeMapping = typeMapping;
            this.eis = eis;
            this.complexTypeInfo = complexTypes;

            // if not registering, this is null
//         for (Map.Entry<String, InfinispanProducer3.InMemoryEntityInfo<?>> e : eis.entrySet()) {
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
            EdmEntityType.Builder eet = EdmEntityType.newBuilder().setNamespace(namespace).
                    setName("java.lang.String").setHasStream(false);
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

//            InfinispanProducer3.InMemoryEntityInfo<?> entityInfo = eis.get(entitySetName);
//
//          // do we have this type yet? -- yes we need this for decision in EntitiesRequestResource
                // (Boolean.TRUE.equals(entitySet.getType().getHasStream())) getType can't be null here!!

//                TODO: SOLVE THIS!!! I need set entity type.... do I need to register it somehow.
//                TODO: register it as CachedValue? Or so? Like former?

//            EdmEntityType.Builder eet = entityTypesByName.get(entityInfo.entityTypeName);
//            EdmEntityType.Builder eet = entityTypesByName.get("String");
//            if (eet == null) {
//               eet = createStructuralType(decorator, entityInfo);
//            }

                // workaround for complex registration? does it work?
//                EdmEntityType.Builder eet = EdmEntityType.newBuilder().setNamespace(namespace).
//                        setName("CachedEntry").setHasStream(false);
//                List<String> keys = new LinkedList<String>();
//                keys.add("CacheEntryKey");
//                eet.addKeys(keys);

                // I don't need EntityType now
                // Correction: I need entitySetType for EntitiesRequestResource

//                EdmEntitySet.Builder ees = EdmEntitySet.newBuilder().setName(entitySetName).setEntityType(eet);
                EdmEntitySet.Builder ees = EdmEntitySet.newBuilder().setName(entitySetName);
                entitySetsByName.put(ees.getName(), ees);
            }
        }

        protected InfinispanProducer3.InMemoryEntityInfo<?> findEntityInfoForClass(Class<?> clazz) {
            for (InfinispanProducer3.InMemoryEntityInfo<?> typeInfo : this.eis.values()) {
                if (typeInfo.entityClass.equals(clazz)) {
                    return typeInfo;
                }
            }
            return null;
        }

        /*
        * contains all generated InMemoryEntityInfos that get created as we walk
        * up the inheritance hierarchy and find Java types that are not registered.
        */
        private Map<Class<?>, InfinispanProducer3.InMemoryEntityInfo<?>> unregisteredEntityInfo =
                new HashMap<Class<?>, InfinispanProducer3.InMemoryEntityInfo<?>>();

        protected InfinispanProducer3.InMemoryEntityInfo<?> getUnregisteredEntityInfo(Class<?> clazz, InfinispanProducer3.InMemoryEntityInfo<?> subclass) {
            InfinispanProducer3.InMemoryEntityInfo<?> ei = unregisteredEntityInfo.get(clazz);
            if (ei == null) {
                ei = new InfinispanProducer3.InMemoryEntityInfo();
                ei.entityTypeName = clazz.getSimpleName();
                ei.keys = subclass.keys;
                ei.entityClass = (Class) clazz;
                ei.properties = new EnumsAsStringsPropertyModelDelegate(
                        new BeanBasedPropertyModel(ei.entityClass, this.flatten));
            }
            return ei;
        }

        protected EdmEntityType.Builder createStructuralType(EdmDecorator decorator, InfinispanProducer3.InMemoryEntityInfo<?> entityInfo) {

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
                InfinispanProducer3.InMemoryEntityInfo<?> entityInfoSuper = findEntityInfoForClass(entityInfo.entityClass.getSuperclass());
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
         * Define functions: ispn_get, ispn_put, ispn_remove, ispn_update
         * <p/>
         * TODO: Define cache operations: stop, start etc. (we need to support this) We will support operations with caches.
         * <p/>
         * provides an override point for applications to add application specific EdmFunctions to their producer.
         * <p/>
         * note: if function getReturnType returns null it returns nothing in ConsumerFunctionCallRequest
         *
         * @param container the EdmEntityContainer.Builder
         */
        protected void addFunctions(EdmEntityContainer.Builder container) {

            List<EdmFunctionImport.Builder> funcImports = new LinkedList<EdmFunctionImport.Builder>();

            int i = 0;
            while (i < container.getEntitySets().size()) {
                // define functions for each entity set (each cache)

                List<EdmFunctionParameter.Builder> funcParametersBinary = new LinkedList<EdmFunctionParameter.Builder>();
                List<EdmFunctionParameter.Builder> funcParametersSimpleString = new LinkedList<EdmFunctionParameter.Builder>();
                List<EdmFunctionParameter.Builder> funcParametersOnlyCacheName = new LinkedList<EdmFunctionParameter.Builder>();

                EdmFunctionParameter.Builder pb = new EdmFunctionParameter.Builder();
                EdmFunctionParameter.Builder pb2 = new EdmFunctionParameter.Builder();
                EdmFunctionParameter.Builder pb3 = new EdmFunctionParameter.Builder();
                EdmFunctionParameter.Builder pb4 = new EdmFunctionParameter.Builder();
                EdmFunctionParameter.Builder pbKey = new EdmFunctionParameter.Builder();

                // setMode(IN)
                pb.setName("keyBinary").setType(EdmType.getSimple("Binary")).setNullable(true).build();
                pb2.setName("valueBinary").setType(EdmType.getSimple("Binary")).setNullable(true).build();
                pb3.setName("keyString").setType(EdmType.getSimple("String")).setNullable(true).build();
                pb4.setName("valueString").setType(EdmType.getSimple("String")).setNullable(true).build();

                // for POST, GET, DELETE and PUT method
                pbKey.setName("key").setType(EdmType.getSimple("String")).setNullable(true).build();

                funcParametersBinary.add(pb);
                funcParametersBinary.add(pb2);
                funcParametersSimpleString.add(pb3);
                funcParametersSimpleString.add(pb4);


                funcParametersOnlyCacheName.add(pbKey);


                String entitySetNameCacheName = container.getEntitySets().get(i).getName();

                // binary
                EdmFunctionImport.Builder fb = new EdmFunctionImport.Builder();
                EdmFunctionImport.Builder fb2 = new EdmFunctionImport.Builder();
                // simple string
                EdmFunctionImport.Builder fb3 = new EdmFunctionImport.Builder();
                EdmFunctionImport.Builder fb4 = new EdmFunctionImport.Builder();

                // only cache name function for method POST requests
                EdmFunctionImport.Builder fb5 = new EdmFunctionImport.Builder();
                EdmFunctionImport.Builder fb6 = new EdmFunctionImport.Builder();
                EdmFunctionImport.Builder fb7 = new EdmFunctionImport.Builder();
                EdmFunctionImport.Builder fb8 = new EdmFunctionImport.Builder();

                // HINT
//          IsBindable - 'true' indicates that the first parameter is the binding parameter
//          IsSideEffecting - 'true' defines an action rather than a function
//          m:IsAlwaysBindable - 'false' defines that the binding can be conditioned to the entity state.

//                fb.setName(entitySetNameCacheName + "_put")
//                        .setEntitySet(container.getEntitySets().get(i))
//                        .setEntitySetName(entitySetNameCacheName)
////                 .setReturnType(null)
////                 .setHttpMethod("GET")
////                  .setBindable(true)
//                        .setBindable(false)
//                        .setSideEffecting(false)  // true for Action (POST)
//                        .setAlwaysBindable(false)
//                        .addParameters(funcParametersBinary).build();
//
//                fb2.setName(entitySetNameCacheName + "_get")
//                        .setEntitySet(container.getEntitySets().get(i))
//                        .setEntitySetName(entitySetNameCacheName)
//                                // let return type to null to be able to directly access response
//                        .setReturnType(EdmSimpleType.BINARY)
////                 .setHttpMethod("GET")
////                  .setBindable(true)
//                        .setBindable(false)
//                        .setSideEffecting(false)  // true for Action (POST)
//                        .setAlwaysBindable(false)
//                        .addParameters(funcParametersBinary).build();
//
//
//                fb3.setName(entitySetNameCacheName + "_putString")
//                        .setEntitySet(container.getEntitySets().get(i))
//                        .setEntitySetName(entitySetNameCacheName)
////                 .setReturnType(null)
////                 .setHttpMethod("GET")
////                  .setBindable(true)
//                        .setBindable(false)
//                        .setSideEffecting(false)  // true for Action (POST)
//                        .setAlwaysBindable(false)
//                        .addParameters(funcParametersSimpleString).build();
//
//                fb4.setName(entitySetNameCacheName + "_getString")
//                        .setEntitySet(container.getEntitySets().get(i))
//                        .setEntitySetName(entitySetNameCacheName)
//                                // let return type to null to be able to directly access response
//                        .setReturnType(EdmSimpleType.STRING)
////                 .setHttpMethod("GET")
////                  .setBindable(true)
//                        .setBindable(false)
//                        .setSideEffecting(false)  // true for Action (POST)
//                        .setAlwaysBindable(false)
//                        .addParameters(funcParametersSimpleString).build();




                // IMPORTANT TASK perf+
                // Parent TODO: implement also async variants + maybe do it with advanced cache
                // TODO: and expect some flags during calls in special parameters /cache_put?key='key1'&flags='IGNORE_RETURN_VALUE,ASYNC'

                // TODO: do it like iteration through enum GET POST DELETE PUT and change Http method inside!!
                // TODO: not 4 imports, duplicate code

                // for HTTP POST (gather and emulates POST request for createEntity)
                fb5.setName(entitySetNameCacheName + "_put")
                        .setEntitySet(container.getEntitySets().get(i))
                        .setEntitySetName(entitySetNameCacheName)
                        // let return type to null to be able to directly access response
                        .setReturnType(EdmSimpleType.STRING)
                        // by specifying http method, we make from this "function" a SERVICE OPERATION kind of a "function"
                        .setHttpMethod("POST")
                        .setBindable(false)
                        .setSideEffecting(false)  // true for Action (POST)
                        .setAlwaysBindable(false)
                        .addParameters(funcParametersOnlyCacheName).build();

                // TODO: maybe change return type to something like JSON VALUE
                // TODO: so we can avoid some ByteArrayInputStream -> string,json transformations (possible?)
                fb6.setName(entitySetNameCacheName + "_get")
                        .setEntitySet(container.getEntitySets().get(i))
                        .setEntitySetName(entitySetNameCacheName)
                                // let return type to null to be able to directly access response
                        .setReturnType(EdmSimpleType.STRING)
                        .setHttpMethod("GET")
                        .setBindable(false)
                        .setSideEffecting(false)  // true for Action (POST)
                        .setAlwaysBindable(false)
                        .addParameters(funcParametersOnlyCacheName).build();

                fb7.setName(entitySetNameCacheName + "_remove")
                        .setEntitySet(container.getEntitySets().get(i))
                        .setEntitySetName(entitySetNameCacheName)
                                // let return type to null to be able to directly access response
                        .setReturnType(EdmSimpleType.STRING)
                        .setHttpMethod("DELETE")
                        .setBindable(false)
                        .setSideEffecting(false)  // true for Action (POST)
                        .setAlwaysBindable(false)
                        .addParameters(funcParametersOnlyCacheName).build();

                fb8.setName(entitySetNameCacheName + "_replace")
                        .setEntitySet(container.getEntitySets().get(i))
                        .setEntitySetName(entitySetNameCacheName)
                                // let return type to null to be able to directly access response
                        .setReturnType(EdmSimpleType.STRING)
                        .setHttpMethod("PUT")
                        .setBindable(false)
                        .setSideEffecting(false)  // true for Action (POST)
                        .setAlwaysBindable(false)
                        .addParameters(funcParametersOnlyCacheName).build();

                // complex
//                funcImports.add(fb);
//                funcImports.add(fb2);
                // simple
//                funcImports.add(fb3);
//                funcImports.add(fb4);


                // only cacheName for POST

                funcImports.add(fb5);
                funcImports.add(fb6);
                funcImports.add(fb7);
                funcImports.add(fb8);

                i++;
            }

            dump("Functions import ok...");
            container.addFunctionImports(funcImports);
        }
    }
}


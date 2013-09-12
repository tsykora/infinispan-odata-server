package org.tsykora.odata.producer;

import org.core4j.Enumerable;
import org.core4j.Func;
import org.core4j.Func1;
import org.core4j.Funcs;
import org.core4j.Predicate1;
import org.infinispan.Cache;
import org.infinispan.manager.DefaultCacheManager;
import org.odata4j.core.OAtomStreamEntity;
import org.odata4j.core.OEntity;
import org.odata4j.core.OEntityId;
import org.odata4j.core.OEntityKey;
import org.odata4j.core.OExtension;
import org.odata4j.core.OFunctionParameter;
import org.odata4j.core.OStructuralObject;
import org.odata4j.edm.*;
import org.odata4j.exceptions.NotImplementedException;
import org.odata4j.expression.BoolCommonExpression;
import org.odata4j.expression.OrderByExpression;
import org.odata4j.expression.OrderByExpression.Direction;
import org.odata4j.producer.*;
import org.odata4j.producer.edm.MetadataProducer;
import org.odata4j.producer.inmemory.BeanBasedPropertyModel;
import org.odata4j.producer.inmemory.EntityIdFunctionPropertyModelDelegate;
import org.odata4j.producer.inmemory.EnumsAsStringsPropertyModelDelegate;
import org.odata4j.producer.inmemory.InMemoryComplexTypeInfo;
import org.odata4j.producer.inmemory.InMemoryEvaluation;
import org.odata4j.producer.inmemory.InMemoryTypeMapping;
import org.odata4j.producer.inmemory.PropertyModel;
import org.tsykora.odata.common.CacheObjectSerializationAble;
import org.tsykora.odata.common.Utils;
import org.tsykora.odata.producer.InMemoryProducerExample.MyInternalCacheEntry;
import org.tsykora.odata.producer.InMemoryProducerExample.MyInternalCacheEntrySimple;
import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

//import org.odata4j.producer.inmemory.InMemoryProducer.RequestContext.RequestType;

/**
 * InMemoryProducer with implemented direct access to Infinispan Cache.
 *
 * Let's cut big producer to the smallest one
 */
public class InfinispanProducer3 implements ODataProducer {

    private static final boolean DUMP = true;

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
    private boolean includeNullPropertyValues = true;
    private final boolean flattenEdm;
    private static final int DEFAULT_MAX_RESULTS = 100;
    private final BASE64Decoder decoder = new BASE64Decoder();
    private final BASE64Encoder encoder = new BASE64Encoder();
    // not static - cache instance is running with producer instance
    private static DefaultCacheManager defaultCacheManager;
    private Map<String, Class> cacheNames = null;

    /**
     * Creates a new instance of an in-memory POJO producer.
     *
     * @param namespace the namespace of the schema registrations
     */
    public InfinispanProducer3(String namespace, Map<String, Class> cacheNames, String ispnConfigFile) {
        this(namespace, DEFAULT_MAX_RESULTS, cacheNames, ispnConfigFile);
    }

    /**
     * Creates a new instance of an in-memory POJO producer.
     *
     * @param namespace  the namespace of the schema registrations
     * @param maxResults the maximum number of entities to return in a single call
     */
    public InfinispanProducer3(String namespace, int maxResults, Map<String, Class> cacheNames, String ispnConfigFile) {
        this(namespace, null, maxResults, null, null, cacheNames, ispnConfigFile);
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
                               Map<String, Class> cacheNames, String ispnConfigFile) {
        this(namespace, containerName, maxResults, decorator, typeMapping,
                true, cacheNames, ispnConfigFile); // legacy: flatten edm
    }

    /**
     * Do everything important here while creating new producer instance.
     */
    public <TEntity, TKey> InfinispanProducer3(String namespace, String containerName, int maxResults,
                                               EdmDecorator decorator, InMemoryTypeMapping typeMapping,
                                               boolean flattenEdm, Map<String, Class> cacheNames, String ispnConfigFile) {
        this.namespace = namespace;
        this.containerName = containerName != null && !containerName.isEmpty() ? containerName : "Container";
        this.maxResults = maxResults;
        this.decorator = decorator;
        this.metadataProducer = new MetadataProducer(this, decorator);
        this.typeMapping = typeMapping == null ? InMemoryTypeMapping.DEFAULT : typeMapping;
        this.flattenEdm = flattenEdm;
        this.cacheNames = cacheNames;

        // TODO add possibility for passing configurations (global, local)
        try {
            // true = start it + start defined caches
            defaultCacheManager = new DefaultCacheManager(ispnConfigFile, true);
//         defaultCacheManager.start();
            dump("Default cache manager started.");

        } catch (IOException e) {
            dump(" PROBLEM WITH CREATING DEFAULT CACHE MANAGER !!!!!!!!!! ");
            System.out.println(" PROBLEM WITH CREATING DEFAULT CACHE MANAGER !!!!!!!!!! ");
            e.printStackTrace();
        }

        for (String cacheName : cacheNames.keySet()) {
//         defaultCacheManager.startCache(cacheName);
//         dump("Cache with name " + cacheName + " started.");
            dump("Registering cache with name " + cacheName + " in Producer...");

            // TODO: IDEA -- if not registered yet -- register it during first put
            // TODO just for Producer2 -- NOW - only register my EDM entity set
            // TODO - check class of KEY and the last Funcs.method (try to use simple strings for key or Object.getId()??
            // TODO -- move this registration into PRODUCER and find how to register it properly and easily

            if (cacheNames.get(cacheName) == MyInternalCacheEntry.class) {
                // register entity set with name of cache
                register(cacheNames.get(cacheName), cacheNames.get(cacheName), cacheName, new Func<Iterable<MyInternalCacheEntry>>() {
                    // TODO - can I skip this registration? Can I do it inside of producer while starting new cache?
                    // TODO - while starting service? while creating new cache from builder? or according to xml?
                    // TODO - register entrySet for new cache after it starts.
                    public Iterable<MyInternalCacheEntry> apply() {
//               // TODO - can I skip this registration? Can I do it inside of producer while starting new cache?
//               // TODO - while starting service? while creating new cache from builder? or according to xml?
//               // TODO - register entrySet for new cache after it starts.
                        List<MyInternalCacheEntry> firstEntryForRegister = new ArrayList<MyInternalCacheEntry>();
                        return firstEntryForRegister;
                    }
                }, Funcs.method(cacheNames.get(cacheName), cacheNames.get(cacheName), "toString"));
            }

            if (cacheNames.get(cacheName) == MyInternalCacheEntrySimple.class) {
                // register entity set with name of cache
                register(cacheNames.get(cacheName), cacheNames.get(cacheName), cacheName, new Func<Iterable<MyInternalCacheEntrySimple>>() {
                    public Iterable<MyInternalCacheEntrySimple> apply() {
                        List<MyInternalCacheEntrySimple> firstEntryForRegister = new ArrayList<MyInternalCacheEntrySimple>();
                        return firstEntryForRegister;
                    }
                }, Funcs.method(cacheNames.get(cacheName), cacheNames.get(cacheName), "toString"));
            }
        }
    }

    private static Cache getCache(String cacheName) {
        return defaultCacheManager.getCache(cacheName);
    }

    @Override
    public EdmDataServices getMetadata() {
        if (metadata == null) {
            metadata = newEdmGenerator(namespace, typeMapping, ID_PROPNAME, eis, complexTypes).generateEdm(decorator).build();
        }
        return metadata;
    }

    public String getContainerName() {
        return containerName;
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
     * Registers a new entity based on a POJO, with support for composite keys.
     *
     * @param entityClass   the class of the entities that are to be stored in the set
     * @param entitySetName the alias the set will be known by; this is what is used in the OData url
     * @param get           a function to iterate over the elements in the set
     * @param keys          one or more keys for the entity
     */
    public <TEntity> void register(Class<TEntity> entityClass, String entitySetName, Func<Iterable<TEntity>> get, String... keys) {
        register(entityClass, entitySetName, entitySetName, get, keys);
    }

    /**
     * Registers a new entity based on a POJO, with support for composite keys.
     *
     * @param entityClass    the class of the entities that are to be stored in the set
     * @param entitySetName  the alias the set will be known by; this is what is used in the OData url
     * @param entityTypeName type name of the entity
     * @param get            a function to iterate over the elements in the set
     * @param keys           one or more keys for the entity
     */
    public <TEntity> void register(Class<TEntity> entityClass, String entitySetName,
                                   String entityTypeName, Func<Iterable<TEntity>> get, String... keys) {
        PropertyModel model = new BeanBasedPropertyModel(entityClass, this.flattenEdm);
        model = new EnumsAsStringsPropertyModelDelegate(model);
        register(entityClass, model, entitySetName, entityTypeName, get, keys);
    }

    /**
     * Registers a new entity set based on a POJO type using the default property model.
     */
    public <TEntity, TKey> void register(Class<TEntity> entityClass, Class<TKey> keyClass,
                                         String entitySetName, Func<Iterable<TEntity>> get, Func1<TEntity, TKey> id) {
        PropertyModel model = new BeanBasedPropertyModel(entityClass, this.flattenEdm);
        model = new EnumsAsStringsPropertyModelDelegate(model);
        model = new EntityIdFunctionPropertyModelDelegate<TEntity, TKey>(model, ID_PROPNAME, keyClass, id);
        register(entityClass, model, entitySetName, get, ID_PROPNAME);
    }

    /**
     * Registers a new entity set based on a POJO type and a property model.
     *
     * @param entityClass   the class of the entities that are to be stored in the set
     * @param propertyModel a way to get/set properties on the POJO
     * @param entitySetName the alias the set will be known by; this is what is used in the ODATA URL
     * @param get           a function to iterate over the elements in the set
     * @param keys          one or more keys for the entity
     */
    public <TEntity, TKey> void register(
            Class<TEntity> entityClass,
            PropertyModel propertyModel,
            String entitySetName,
            Func<Iterable<TEntity>> get,
            String... keys) {
        register(entityClass, propertyModel, entitySetName, entitySetName, get, keys);
    }

    public <TEntity> void register(
            final Class<TEntity> entityClass,
            final PropertyModel propertyModel,
            final String entitySetName,
            final String entityTypeName,
            final Func<Iterable<TEntity>> get,
            final String... keys) {
        register(entityClass, propertyModel, entitySetName, entityTypeName,
                get, null, keys);
    }

    public <TEntity> void register(
            final Class<TEntity> entityClass,
            final PropertyModel propertyModel,
            final String entitySetName,
            final String entityTypeName,
            final Func<Iterable<TEntity>> get,
            final Func1<RequestContext, Iterable<TEntity>> getWithContext,
            final String... keys) {

        InMemoryEntityInfo<TEntity> ei = new InMemoryEntityInfo<TEntity>();
        ei.entitySetName = entitySetName;
        ei.entityTypeName = entityTypeName;
        ei.properties = propertyModel;
        ei.get = get;
        ei.getWithContext = getWithContext;
        ei.keys = keys;
        ei.entityClass = entityClass;
        ei.hasStream = OAtomStreamEntity.class.isAssignableFrom(entityClass);

        ei.id = new Func1<Object, HashMap<String, Object>>() {
            @Override
            public HashMap<String, Object> apply(Object input) {
                HashMap<String, Object> values = new HashMap<String, Object>();
                for (String key : keys) {
                    values.put(key, eis.get(entitySetName).properties.getPropertyValue(input, key));
                }
                return values;
            }
        };

        eis.put(entitySetName, ei);
        metadata = null;
    }


    private static Predicate1<Object> filterToPredicate(final BoolCommonExpression filter, final PropertyModel properties) {
        return new Predicate1<Object>() {
            public boolean apply(Object input) {
                return InMemoryEvaluation.evaluate(filter, input, properties);
            }
        };
    }

    /**
     * Is returning all entries from local cache.
     *
     * @param entitySetName - cache name
     * @param queryInfo     - other special restrictions??
     * @return
     */
    @Override
    public EntitiesResponse getEntities(ODataContext context, String entitySetName, final QueryInfo queryInfo) {

//        // go f

       throw new NotImplementedException();
    }


    @Override
    public CountResponse getEntitiesCount(ODataContext context, String entitySetName, final QueryInfo queryInfo) {

       throw new NotImplementedException();
    }

    private Enumerable<Object> orderBy(Enumerable<Object> iter, List<OrderByExpression> orderBys, final PropertyModel properties) {
        for (final OrderByExpression orderBy : Enumerable.create(orderBys).reverse()) {
            iter = iter.orderBy(new Comparator<Object>() {
                @SuppressWarnings({"unchecked", "rawtypes"})
                public int compare(Object o1, Object o2) {
                    Comparable lhs = (Comparable) InMemoryEvaluation.evaluate(orderBy.getExpression(), o1, properties);
                    Comparable rhs = (Comparable) InMemoryEvaluation.evaluate(orderBy.getExpression(), o2, properties);
                    return (orderBy.getDirection() == Direction.ASCENDING ? 1 : -1) * lhs.compareTo(rhs);
                }
            });
        }
        return iter;
    }

    /**
     * This will probably need performance tunning!!
     * <p/>
     * If there is a getEntity() call on consumer then this getEntity() method on producer is called
     * <p/>
     * entityKey corresponds with Key of entry in the cache. entityKey expects deserialized object (so it can directly
     * access ispn cache)
     */
    @Override
    public EntityResponse getEntity(ODataContext context, final String entitySetName, final OEntityKey entityKey, final EntityQueryInfo queryInfo) {

       throw new NotImplementedException();
    }

    @Override
    public void mergeEntity(ODataContext context, String entitySetName, OEntity entity) {
        // merge - what is equal to merge in ISPN?
        throw new NotImplementedException();
    }

    @Override
    public void updateEntity(ODataContext context, String entitySetName, OEntity entity) {
        // simple update entry and re-call
        throw new NotImplementedException();
    }

    @Override
    public void deleteEntity(ODataContext context, String entitySetName, OEntityKey entityKey) {
        // simple remove entry and re-call
        throw new NotImplementedException();
    }

    /**
     * Puts entry given in entity into the Infinispan (in-memory) cache specified by entitySetName
     *
     * @param entitySetName
     * @param entity
     * @return
     */
    @Override
    public EntityResponse createEntity(ODataContext context, String entitySetName, final OEntity entity) {
       throw new NotImplementedException();
    }

    @Override
    public EntityResponse createEntity(ODataContext context, String entitySetName, OEntityKey entityKey, String navProp, OEntity entity) {
        dump("THIS IS SECOND createEntity METHOD CALL -- NOT IMPLEMENTED YET!!!");
        throw new NotImplementedException();
    }

    @Override
    public BaseResponse getNavProperty(ODataContext context, String entitySetName, OEntityKey entityKey, String navProp, QueryInfo queryInfo) {


       throw new NotImplementedException();
    }

    @Override
    public CountResponse getNavPropertyCount(ODataContext context, String entitySetName, OEntityKey entityKey, String navProp, QueryInfo queryInfo) {
        throw new NotImplementedException();
    }

    @Override
    public EntityIdResponse getLinks(ODataContext context, OEntityId sourceEntity, String targetNavProp) {
        throw new NotImplementedException();
    }

    @Override
    public void createLink(ODataContext context, OEntityId sourceEntity, String targetNavProp, OEntityId targetEntity) {
        throw new NotImplementedException();
    }

    @Override
    public void updateLink(ODataContext context, OEntityId sourceEntity, String targetNavProp, OEntityKey oldTargetEntityKey, OEntityId newTargetEntity) {
        throw new NotImplementedException();
    }

    @Override
    public void deleteLink(ODataContext context, OEntityId sourceEntity, String targetNavProp, OEntityKey targetEntityKey) {
        throw new NotImplementedException();
    }

    @Override
    public BaseResponse callFunction(ODataContext context, EdmFunctionImport function, Map<String, OFunctionParameter> params, QueryInfo queryInfo) {

        // if complex issue put with serialized encoded objects
        boolean complex = true;
        OEntityKey oentityKey = null;
        CacheObjectSerializationAble keyObject = null;
        CacheObjectSerializationAble valueObject = null;
        String simpleKey = null;
        String simpleValue = null;

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

        if (params.get("keyEncodedSerializedObject") != null || params.get("valueEncodedSerializedObject") != null) {
            dump("Working with SERIALIZED and ENCODED OBJECT KEY and VALUE");

            try {
                String keyEncodedString = params.get("keyEncodedSerializedObject").getValue().toString();
                byte[] keyDecodedBytes = decoder.decodeBuffer(keyEncodedString);
                dump("PRODUCER, key decoded bytes for deserialization: " + keyDecodedBytes);
                Object keyDeserializedObject = Utils.deserialize(keyDecodedBytes);
                dump("PRODUCER, key deserialized object: " + keyDeserializedObject.toString());
                keyObject = (CacheObjectSerializationAble) keyDeserializedObject;

                // when calling _get value is not defined of course
                if (function.getName().endsWith("_put")) {
                    String valueEncodedString = params.get("valueEncodedSerializedObject").getValue().toString();
                    byte[] valueDecodedBytes = decoder.decodeBuffer(valueEncodedString);
                    dump("PRODUCER, value decoded bytes for deserialization: " + valueDecodedBytes);
                    Object valueDeserializedObject = Utils.deserialize(valueDecodedBytes);
                    dump("PRODUCER, value deserialized object: " + valueDeserializedObject.toString());
                    valueObject = (CacheObjectSerializationAble) valueDeserializedObject;
                }
            } catch (Exception e) {
                dump("EXCEPTION: " + e.getMessage() + " " + e.getCause().getMessage());
                e.printStackTrace();
            }

        } else {
            complex = false;

            // Working with only simple String KEY and VALUE
            dump("Working with only simple String KEY and VALUE");
            simpleKey = params.get("keySimpleString").getValue().toString();
            // set simpleValue later because when calling _get valueSimpleString is not defined
        }


        String setNameWhichIsCacheName = function.getEntitySet().getName();

        // returning just right putted entity in this case
        BaseResponse response = null;

        if (function.getName().endsWith("_put")) {
            dump("Putting into " + setNameWhichIsCacheName + " cache....... ");

            if (complex) {
                // TODO!! FIX THIS!!! up ->> key object, put whole key object and pass whole keyObject as a entityKey
                // TODO: or simply return nothing when putting? but it returns... (flag it if wanna nothing?)
                dump("TODO... FIX THIS!!! in callFunction put branch. " +
                        "There is a put not of a whole object but only String as a Key!");

                getCache(setNameWhichIsCacheName).put(keyObject.getKeyx(), valueObject);

            } else {
                simpleValue = params.get("valueSimpleString").getValue().toString();
                getCache(setNameWhichIsCacheName).put(simpleKey, simpleValue);
            }


            // TODO: this will depend on the function name (for PUT no return type, for GET yes)
            // TODO: WHEN USER WANTS SOMETHING RETURNED WHEN PUTTING, YOU CAN RETURN WHOLE ENTITY LIKE THIS:
//         response = getEntity(context, setNameWhichIsCacheName,
//                              oentityKey, null);
            // **** !!! ****
            // set return type for put as EDM.STRING and call only put here and encode + serialize value -> return

            // otherwise when put return nothing
            // dealing with this as a Status.NO_CONTENT (it is successful for functions)
            response = null;
        }


        if (function.getName().endsWith("_get")) {
            if (complex) {
                // TODO change this to keyObject only!!
                dump("_get call from callFunction, FIX cache.get(keyObject.getKeyx()) to keyObject only");
                Object value = getCache(setNameWhichIsCacheName).get(keyObject.getKeyx());
                byte[] serializedValue = Utils.serialize(value);
                String encodedValue = encoder.encode(serializedValue);
                response = Responses.simple(EdmSimpleType.STRING, "valueEncodedSerializedObject", encodedValue);
            } else {
                String value = (String) getCache(setNameWhichIsCacheName).get(simpleKey);
                response = Responses.simple(EdmSimpleType.STRING, "valueSimpleString", value);
            }
        }


        // need to pass the right parameters
//           try {
//              Method m = c.getClass().getMethod("put", null); // tady mam metody
//              try {
//                 m.invoke(c, new Object());
//              } catch (IllegalAccessException e) {
//                 e.printStackTrace();
//              } catch (InvocationTargetException e) {
//                 e.printStackTrace();
//              }
//           } catch (NoSuchMethodException e) {
//              e.printStackTrace();
//           }


        return response;
    }

    @Override
    public <TExtension extends OExtension<ODataProducer>> TExtension findExtension(Class<TExtension> clazz) {
        return null;
    }

    public static class RequestContext {

        public enum RequestType {

            GetEntity, GetEntities, GetEntitiesCount, GetNavProperty
        }

        ;
        public final RequestType requestType;
        private final String entitySetName;
        private EdmEntitySet entitySet;
        private final String navPropName;
        private final OEntityKey entityKey;
        private final QueryInfo queryInfo;
        private final PropertyPathHelper pathHelper;
        private final Object ispnCacheKey;
        private final boolean isSimpleStringKeyValue;

        public boolean isSimpleStringKeyValue() {
            return isSimpleStringKeyValue;
        }

        public Object getIspnCacheKey() {
            return ispnCacheKey;
        }

        public RequestType getRequestType() {
            return requestType;
        }

        public String getEntitySetName() {
            return entitySetName;
        }

        public EdmEntitySet getEntitySet() {
            return entitySet;
        }

        public String getNavPropName() {
            return navPropName;
        }

        public OEntityKey getEntityKey() {
            return entityKey;
        }

        public QueryInfo getQueryInfo() {
            return queryInfo;
        }

        public PropertyPathHelper getPathHelper() {
            return pathHelper;
        }

        public static Builder newBuilder(RequestType requestType) {
            return new Builder().requestType(requestType);
        }

        public static class Builder {

            private RequestType requestType;
            private String entitySetName;
            private EdmEntitySet entitySet;
            private String navPropName;
            private OEntityKey entityKey;
            private QueryInfo queryInfo;
            private PropertyPathHelper pathHelper;
            private Object ispnCacheKey;
            private boolean isSimpleStringKeyValue;

            public Builder isSimpleStringKeyValue(boolean value) {
                this.isSimpleStringKeyValue = value;
                return this;
            }

            public Builder ispnCacheKey(Object value) {
                this.ispnCacheKey = value;
                return this;
            }

            public Builder requestType(RequestType value) {
                this.requestType = value;
                return this;
            }

            public Builder entitySetName(String value) {
                this.entitySetName = value;
                return this;
            }

            public Builder entitySet(EdmEntitySet value) {
                this.entitySet = value;
                return this;
            }

            public Builder navPropName(String value) {
                this.navPropName = value;
                return this;
            }

            public Builder entityKey(OEntityKey value) {
                this.entityKey = value;
                return this;
            }

            public Builder queryInfo(QueryInfo value) {
                this.queryInfo = value;
                return this;
            }

            public Builder pathHelper(PropertyPathHelper value) {
                this.pathHelper = value;
                return this;
            }

            public RequestContext build() {
                return new RequestContext(requestType, entitySetName, entitySet, navPropName, entityKey, queryInfo,
                        pathHelper, ispnCacheKey, isSimpleStringKeyValue);
            }
        }

        private RequestContext(RequestType requestType, String entitySetName, EdmEntitySet entitySet,
                               String navPropName, OEntityKey entityKey, QueryInfo queryInfo, PropertyPathHelper pathHelper,
                               Object ispnCacheKey, boolean simpleStringKeyValue) {
            this.requestType = requestType;
            this.entitySetName = entitySetName;
            this.entitySet = entitySet;
            this.navPropName = navPropName;
            this.entityKey = entityKey;
            this.queryInfo = queryInfo;
            this.pathHelper = pathHelper;
            this.ispnCacheKey = ispnCacheKey;
            this.isSimpleStringKeyValue = simpleStringKeyValue;
        }
    }

    /**
     * TODO - document THIS PROPERLY? Change in the future? Given an entity set and an entity key, returns the pojo that
     * is that entity instance. The default implementation iterates over the entire set of pojos to find the desired
     * instance.
     *
     * @param rc the current ReqeustContext, may be valuable to the ei.getWithContext impl
     * @return the pojo
     */
    @SuppressWarnings("unchecked")
    protected Object getEntityPojo(final RequestContext rc) {

        // I need to transfer cache entry to MyInternalCacheEntry
        // because this is Entity and I have EntityInfo about this class
        // RequestContext is my internal class here and it has rc.getEntityKey()

        // Citation EntityKey DOC: "The string representation of an entity-key is wrapped with parentheses" ('foo')

        MyInternalCacheEntry mice = null;
        MyInternalCacheEntrySimple miceSimple = null;

        // entry exists?

        System.out.println("\n\n\n");
        System.out.println("rc.getIspnCacheKey (have to be the same as the first entry later: " + rc.getIspnCacheKey());
        System.out.println("Cache KEYSET before creating MICE object in getEntityPojo!!! " + getCache(rc.getEntitySetName()).keySet().toString());
        System.out.println("Cache KEYSET before creating MICE object in getEntityPojo the first entry " +
                getCache(rc.getEntitySetName()).keySet().toArray()[0]);
        System.out.println(" rc.getIspnCacheKey class " + rc.getIspnCacheKey().getClass());
        System.out.println(" key from keyset class: " + getCache(rc.getEntitySetName()).keySet().toArray()[0].getClass());

        System.out.println("EQUALS???: " + getCache(rc.getEntitySetName()).keySet().toArray()[0].equals(rc.getIspnCacheKey()));

//        Object value = getCache(rc.getEntitySetName()).get(getCache(rc.getEntitySetName()).keySet().toArray()[0]);
        Object value = getCache(rc.getEntitySetName()).get(rc.getIspnCacheKey()); // I need this to work


        System.out.println("value: " + value + " toString: " + value.toString());
        System.out.println("\n\n\n");

        if (value != null) {
            dump("Found value : " + value + " for key: " + rc.getIspnCacheKey() + " in cache: " + rc.getEntitySetName());

            if (rc.isSimpleStringKeyValue()) {
                dump("getEntityPojo(): NO serialization of response. Simple String value key only.");
                // Calling (String, String) constructor here, so response is OK, no need of serialization
                // setting of Simple attributes in MyInternalCacheEntry is ok a supposed to be filled by String values
                miceSimple = new MyInternalCacheEntrySimple(rc.getIspnCacheKey().toString(), value.toString());
            } else {
                // we are dealing with complex objects, do proper serialization

                // IMPORTANT
                // now I have OBJECTS here -- (which are strings for example)
                // but they can't be cast to byte[]
                // this mice is put into OEntity and I need to put there properties in byte[] => in edm.binary format
                // so I need to serialize these objects here

                // NOPE: NEW!!! approach
                // rc.IspnCacheKey is OBJECT here. No serialization needed

                dump("getEntityPojo(): Serializing response.");
                mice = new MyInternalCacheEntry(Utils.serialize(rc.getIspnCacheKey()), Utils.serialize(value));
            }

        } else {
            dump("Value NOT FOUND for key: " + rc.getIspnCacheKey() + " in cache: " + rc.getEntitySetName());
        }

        if (mice == null) {
            return miceSimple;
        } else {
            return mice;
        }
    }

    private enum TriggerType {
        Before, After
    }

    ;

    protected void fireUnmarshalEvent(Object pojo, OStructuralObject sobj, TriggerType ttype)
            throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        try {
            Method m = pojo.getClass().getMethod(ttype == TriggerType.Before ? "beforeOEntityUnmarshal" : "afterOEntityUnmarshal", OStructuralObject.class);
            if (m != null) {
                m.invoke(pojo, sobj);
            }
        } catch (NoSuchMethodException ex) {
        }
    }


    public class InMemoryEntityInfo<TEntity> {

        // we are maintaining collection of these entities - they are mapped to EntitySetName in [eis] hash map
        String entitySetName;
        String entityTypeName;
        String[] keys;
        Class<TEntity> entityClass;
        Func<Iterable<TEntity>> get; //returning defined apply()
        Func1<RequestContext, Iterable<TEntity>> getWithContext;
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

        public Func1<RequestContext, Iterable<TEntity>> getGetWithContext() {
            dump("Call from getGetWithContext method - return type was changed!!!");
            return getWithContext;
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

            for (Map.Entry<String, InfinispanProducer3.InMemoryEntityInfo<?>> e : eis.entrySet()) {
                // e.getValue().entityClass = MyInternalCacheEntry , e.getKey() = "CacheEntries"
                // e.getValue() = InMemoryEntityInfo
                entitySetNameByClass.put(e.getValue().entityClass, e.getKey());
            }
            this.flatten = flatten;
        }

        @Override
        public EdmDataServices.Builder generateEdm(EdmDecorator decorator) {

            List<EdmSchema.Builder> schemas = new ArrayList<EdmSchema.Builder>();
            List<EdmEntityContainer.Builder> containers = new ArrayList<EdmEntityContainer.Builder>();
            List<EdmAssociation.Builder> associations = new ArrayList<EdmAssociation.Builder>();
            List<EdmAssociationSet.Builder> associationSets = new ArrayList<EdmAssociationSet.Builder>();

//            createComplexTypes(decorator, edmComplexTypes);

            // creates id other basic SUPPORTED_TYPE properties(structural) entities
            createStructuralEntities(decorator);

            // TODO handle back references too
            // create hashmaps from sets

//            createNavigationProperties(associations, associationSets,
//                    entityTypesByName, entitySetsByName, entitySetNameByClass);

            EdmEntityContainer.Builder container = EdmEntityContainer.newBuilder().
                    setName(containerName).setIsDefault(true).
                    addEntitySets(entitySetsByName.values()).addAssociationSets(associationSets);

            EdmSchema.Builder schema = EdmSchema.newBuilder().setNamespace(namespace).
                    addEntityTypes(entityTypesByName.values()).addAssociations(associations).
                    addEntityContainers(containers).addComplexTypes(edmComplexTypes);

            addFunctions(schema, container);

            // FIXED *****************************************
            // FIXED add container after function registration
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


        private void createStructuralEntities(EdmDecorator decorator) {

            // eis contains all of the registered entity sets.
            for (String entitySetName : eis.keySet()) {
                InfinispanProducer3.InMemoryEntityInfo<?> entityInfo = eis.get(entitySetName);

                // do we have this type yet?
                EdmEntityType.Builder eet = entityTypesByName.get(entityInfo.entityTypeName);
                if (eet == null) {
                    eet = createStructuralType(decorator, entityInfo);
                }

                EdmEntitySet.Builder ees = EdmEntitySet.newBuilder().setName(entitySetName).setEntityType(eet);
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
         * get the Edm namespace
         *
         * @return the Edm namespace
         */
        public String getNamespace() {
            return namespace;
        }

        /**
         * Function definitions it defines and add functions into EDM Schema these functions are callable as GET HTTP
         * operations
         * <p/>
         * Define functions: ispn_get, ispn_put, ispn_remove, ispn_update
         * <p/>
         * <p/>
         * provides an override point for applications to add application specific EdmFunctions to their producer.
         * <p/>
         * note: if function getReturnType returns null it returns nothing in ConsumerFunctionCallRequest
         *
         * @param schema    the EdmSchema.Builder
         * @param container the EdmEntityContainer.Builder
         */
        protected void addFunctions(EdmSchema.Builder schema, EdmEntityContainer.Builder container) {

            List<EdmFunctionImport.Builder> funcImports = new LinkedList<EdmFunctionImport.Builder>();

            // TODO: improve structure (don't call X-times same things)
            int i = 0;
            while (i < container.getEntitySets().size()) {
                // define functions for each entity set (each cache)

                List<EdmFunctionParameter.Builder> funcParameters = new LinkedList<EdmFunctionParameter.Builder>();

                EdmFunctionParameter.Builder pb = new EdmFunctionParameter.Builder();
                EdmFunctionParameter.Builder pb2 = new EdmFunctionParameter.Builder();
                EdmFunctionParameter.Builder pb3 = new EdmFunctionParameter.Builder();
                EdmFunctionParameter.Builder pb4 = new EdmFunctionParameter.Builder();
                EdmFunctionParameter.Builder pb5 = new EdmFunctionParameter.Builder();
//         EdmFunctionParameter.Builder pb6 = new EdmFunctionParameter.Builder();

                // TODO: do it as some complex type
                // causing performance problems (getEntities) in every function call
//            EdmCollectionType collectionType = new EdmCollectionType(EdmProperty.CollectionKind.Collection,
//                                                                     schema.getEntityTypes().get(i).build());

                // setMode(IN)
//            pb.setName("cacheName").setType(collectionType).setNullable(false).setBound(true).build();
                pb2.setName("keyEncodedSerializedObject").setType(EdmType.getSimple("String")).setNullable(true).build();
                pb3.setName("valueEncodedSerializedObject").setType(EdmType.getSimple("String")).setNullable(true).build();
                pb4.setName("keySimpleString").setType(EdmType.getSimple("String")).setNullable(true).build();
                pb5.setName("valueSimpleString").setType(EdmType.getSimple("String")).setNullable(true).build();

//            funcParameters.add(pb);
                funcParameters.add(pb2);
                funcParameters.add(pb3);
                funcParameters.add(pb4);
                funcParameters.add(pb5);

                String entitySetNameCacheName = container.getEntitySets().get(i).getName();

                EdmFunctionImport.Builder fb = new EdmFunctionImport.Builder();
                EdmFunctionImport.Builder fb2 = new EdmFunctionImport.Builder();

                // HINT
//          IsBindable - 'true' indicates that the first parameter is the binding parameter
//          IsSideEffecting - 'true' defines an action rather than a function
//          m:IsAlwaysBindable - 'false' defines that the binding can be conditioned to the entity state.

                fb.setName(entitySetNameCacheName + "_put")
                        .setEntitySet(container.getEntitySets().get(i))
                        .setEntitySetName(entitySetNameCacheName)
//                 .setReturnType(null)
//                 .setHttpMethod("GET")
//                  .setBindable(true)
                        .setBindable(false)
                        .setSideEffecting(false)  // true for Action (POST)
                        .setAlwaysBindable(false)
                        .addParameters(funcParameters).build();

                // TODO?
                // returning string as a decoded byte array of value for deserialization?
                fb2.setName(entitySetNameCacheName + "_get")
                        .setEntitySet(container.getEntitySets().get(i))
                        .setEntitySetName(entitySetNameCacheName)
                                // let return type to null to be able to directly access response
                        .setReturnType(EdmSimpleType.STRING)
//                 .setHttpMethod("GET")
//                  .setBindable(true)
                        .setBindable(false)
                        .setSideEffecting(false)  // true for Action (POST)
                        .setAlwaysBindable(false)
                        .addParameters(funcParameters).build();

                funcImports.add(fb);
                funcImports.add(fb2);
                i++;
            }

            dump("Functions import ok...");
            container.addFunctionImports(funcImports);
        }
    }
}

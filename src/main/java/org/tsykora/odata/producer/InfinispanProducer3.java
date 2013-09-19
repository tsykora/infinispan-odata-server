package org.tsykora.odata.producer;

import org.core4j.Enumerable;
import org.core4j.Func;
import org.core4j.Func1;
import org.core4j.Predicate1;
import org.infinispan.Cache;
import org.infinispan.manager.DefaultCacheManager;
import org.odata4j.core.OEntity;
import org.odata4j.core.OEntityId;
import org.odata4j.core.OEntityKey;
import org.odata4j.core.OExtension;
import org.odata4j.core.OFunctionParameter;
import org.odata4j.core.OSimpleObject;
import org.odata4j.core.OStructuralObject;
import org.odata4j.edm.*;
import org.odata4j.exceptions.NotImplementedException;
import org.odata4j.expression.BoolCommonExpression;
import org.odata4j.expression.OrderByExpression;
import org.odata4j.expression.OrderByExpression.Direction;
import org.odata4j.producer.*;
import org.odata4j.producer.edm.MetadataProducer;
import org.odata4j.producer.inmemory.BeanBasedPropertyModel;
import org.odata4j.producer.inmemory.EnumsAsStringsPropertyModelDelegate;
import org.odata4j.producer.inmemory.InMemoryComplexTypeInfo;
import org.odata4j.producer.inmemory.InMemoryEvaluation;
import org.odata4j.producer.inmemory.InMemoryTypeMapping;
import org.odata4j.producer.inmemory.PropertyModel;
import org.tsykora.odata.common.CacheObjectSerializationAble;
import org.tsykora.odata.common.Utils;

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
import java.util.Set;
import java.util.logging.Logger;

//import org.odata4j.producer.inmemory.InMemoryProducer.RequestContext.RequestType;

/**
 * InMemoryProducer with implemented direct access to Infinispan Cache.
 * <p/>
 * Let's cut big producer to the smallest one
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
         dump("Starting cache with name " + cacheName + " on defaultCacheManager...");
         defaultCacheManager.startCache(cacheName);

         Cache cache = defaultCacheManager.getCache(cacheName);
         cache.put("key1", "value1");
         dump("Cache " + cacheName + " status: " + cache.getStatus());

             this.caches.put(cacheName, cache);
            return cache;
         } catch (Exception e) {
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

      long startCallFunctionProducerInside = System.currentTimeMillis();

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

      if (params.get("keyBinary") != null || params.get("valueBinary") != null) {
//         dump("Working with SERIALIZED OBJECT KEY and VALUE");

         try {

            OSimpleObject simpleObject = (OSimpleObject) params.get("keyBinary").getValue();
            byte[] keyBytes = (byte[]) simpleObject.getValue();

//            dump("PRODUCER, key bytes for deserialization: " + keyBytes);
            Object keyDeserializedObject = Utils.deserialize(keyBytes);
//            dump("PRODUCER, key deserialized object: " + keyDeserializedObject.toString());
            keyObject = (CacheObjectSerializationAble) keyDeserializedObject;

            // when calling _get value is not defined of course
            if (function.getName().endsWith("_put")) {
               simpleObject = (OSimpleObject) params.get("valueBinary").getValue();
               byte[] valueBytes = (byte[]) simpleObject.getValue();

//               dump("PRODUCER, value bytes for deserialization: " + valueBytes);
               Object valueDeserializedObject = Utils.deserialize(valueBytes);
//               dump("PRODUCER, value deserialized object: " + valueDeserializedObject.toString());
               valueObject = (CacheObjectSerializationAble) valueDeserializedObject;
            }
         } catch (Exception e) {
            dump("EXCEPTION: " + e.getMessage() + " " + e.getCause().getMessage());
            e.printStackTrace();
         }

      } else {

         // Working with only simple String KEY and VALUE
         dump("Working with only simple String KEY and VALUE");
         simpleKey = params.get("keyString").getValue().toString();
         // set simpleValue later because when calling _get valueSimpleString is not defined
      }


      String setNameWhichIsCacheName = function.getEntitySet().getName();

      // returning just right putted entity in this case
      BaseResponse response = null;

      if (function.getName().endsWith("_put")) {
         dump("Putting into " + setNameWhichIsCacheName + " cache....... ");
         long start = System.currentTimeMillis();
         getCache(setNameWhichIsCacheName).put(keyObject, valueObject);
         long end = System.currentTimeMillis();
         System.out.println("Put into " + setNameWhichIsCacheName + " took: " + (end-start) + " millis.");

         // put should return value too (as Infinispan itself)
         // dealing with this as a Status.NO_CONTENT (it is successful for functions)
         response = null;
      }

      if (function.getName().endsWith("_putString")) {
         dump("Putting into " + setNameWhichIsCacheName + " cache....... ");

         simpleValue = params.get("valueString").getValue().toString();
         long start = System.currentTimeMillis();
         getCache(setNameWhichIsCacheName).put(simpleKey, simpleValue);
         long end = System.currentTimeMillis();
         System.out.println("Put into " + setNameWhichIsCacheName + " took: " + (end-start) + " millis.");

         response = null;
      }


      if (function.getName().endsWith("_get")) {
         long start = System.currentTimeMillis();
         Object value = getCache(setNameWhichIsCacheName).get(keyObject);
         long end = System.currentTimeMillis();
         System.out.println("Get from " + setNameWhichIsCacheName + " took: " + (end-start) + " millis.");

         byte[] serializedValue = Utils.serialize(value);

         BaseResponse baseResponse = Responses.simple(EdmSimpleType.BINARY, "valueBinary", serializedValue);

         long stopCallFunctionProducerInside = System.currentTimeMillis();
         System.out.println("Whole inside of CallFunction in producer before response took: " +
                                  (startCallFunctionProducerInside-stopCallFunctionProducerInside) + " millis.");

         response = baseResponse;
      }

      if (function.getName().endsWith("_getString")) {
         long start = System.currentTimeMillis();
         String value = (String) getCache(setNameWhichIsCacheName).get(simpleKey);
         long end = System.currentTimeMillis();
         System.out.println("Get from " + setNameWhichIsCacheName + " took: " + (end-start) + " millis.");

         BaseResponse baseResponse = Responses.simple(EdmSimpleType.STRING, "valueString", value);

         long stopCallFunctionProducerInside = System.currentTimeMillis();
         System.out.println("Whole inside of CallFunction in producer before response took: " +
                                  (startCallFunctionProducerInside-stopCallFunctionProducerInside) + " millis.");

         response = baseResponse;
      }


//           need to pass the right parameters
//           try {
//              Method m = c.getClass().getMethod("put", null);
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


//   /**
//    * TODO - document THIS PROPERLY? Change in the future? Given an entity set and an entity key, returns the pojo that
//    * is that entity instance. The default implementation iterates over the entire set of pojos to find the desired
//    * instance.
//    *
//    * @param rc the current ReqeustContext, may be valuable to the ei.getWithContext impl
//    * @return the pojo
//    */
//   @SuppressWarnings("unchecked")
//   protected Object getEntityPojo(final RequestContext rc) {
//
//      // I need to transfer cache entry to MyInternalCacheEntry
//      // because this is Entity and I have EntityInfo about this class
//      // RequestContext is my internal class here and it has rc.getEntityKey()
//
//      // Citation EntityKey DOC: "The string representation of an entity-key is wrapped with parentheses" ('foo')
//
//      MyInternalCacheEntry mice = null;
//      MyInternalCacheEntrySimple miceSimple = null;
//
//      // entry exists?
//
//      System.out.println("\n\n\n");
//      System.out.println("rc.getIspnCacheKey (have to be the same as the first entry later: " + rc.getIspnCacheKey());
//      System.out.println("Cache KEYSET before creating MICE object in getEntityPojo!!! " + getCache(rc.getEntitySetName()).keySet().toString());
//      System.out.println("Cache KEYSET before creating MICE object in getEntityPojo the first entry " +
//                               getCache(rc.getEntitySetName()).keySet().toArray()[0]);
//      System.out.println(" rc.getIspnCacheKey class " + rc.getIspnCacheKey().getClass());
//      System.out.println(" key from keyset class: " + getCache(rc.getEntitySetName()).keySet().toArray()[0].getClass());
//
//      System.out.println("EQUALS???: " + getCache(rc.getEntitySetName()).keySet().toArray()[0].equals(rc.getIspnCacheKey()));
//
////        Object value = getCache(rc.getEntitySetName()).get(getCache(rc.getEntitySetName()).keySet().toArray()[0]);
//      Object value = getCache(rc.getEntitySetName()).get(rc.getIspnCacheKey()); // I need this to work
//
//
//      System.out.println("value: " + value + " toString: " + value.toString());
//      System.out.println("\n\n\n");
//
//      if (value != null) {
//         dump("Found value : " + value + " for key: " + rc.getIspnCacheKey() + " in cache: " + rc.getEntitySetName());
//
//         if (rc.isSimpleStringKeyValue()) {
//            dump("getEntityPojo(): NO serialization of response. Simple String value key only.");
//            // Calling (String, String) constructor here, so response is OK, no need of serialization
//            // setting of Simple attributes in MyInternalCacheEntry is ok a supposed to be filled by String values
//            miceSimple = new MyInternalCacheEntrySimple(rc.getIspnCacheKey().toString(), value.toString());
//         } else {
//            // we are dealing with complex objects, do proper serialization
//
//            // IMPORTANT
//            // now I have OBJECTS here -- (which are strings for example)
//            // but they can't be cast to byte[]
//            // this mice is put into OEntity and I need to put there properties in byte[] => in edm.binary format
//            // so I need to serialize these objects here
//
//            // NOPE: NEW!!! approach
//            // rc.IspnCacheKey is OBJECT here. No serialization needed
//
//            dump("getEntityPojo(): Serializing response.");
//            mice = new MyInternalCacheEntry(Utils.serialize(rc.getIspnCacheKey()), Utils.serialize(value));
//         }
//
//      } else {
//         dump("Value NOT FOUND for key: " + rc.getIspnCacheKey() + " in cache: " + rc.getEntitySetName());
//      }
//
//      if (mice == null) {
//         return miceSimple;
//      } else {
//         return mice;
//      }
//   }

   private enum TriggerType {
      Before, After
   }

   ;

   protected void fireUnmarshalEvent(Object pojo, OStructuralObject sobj, TriggerType ttype)
         throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
      try {
         Method m = pojo.getClass().getMethod(ttype == TriggerType.Before ? "beforeOEntityUnmarshal" :
                                                    "afterOEntityUnmarshal", OStructuralObject.class);
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
//            // do we have this type yet?
//            EdmEntityType.Builder eet = entityTypesByName.get(entityInfo.entityTypeName);
//            if (eet == null) {
//               eet = createStructuralType(decorator, entityInfo);
//            }

            // I don't need EntityType now
//            EdmEntitySet.Builder ees = EdmEntitySet.newBuilder().setName(entitySetName).setEntityType(eet);
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
       * Define cache operations: stop, start etc.
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

            EdmFunctionParameter.Builder pb = new EdmFunctionParameter.Builder();
            EdmFunctionParameter.Builder pb2 = new EdmFunctionParameter.Builder();
            EdmFunctionParameter.Builder pb3 = new EdmFunctionParameter.Builder();
            EdmFunctionParameter.Builder pb4 = new EdmFunctionParameter.Builder();


            // setMode(IN)
            pb.setName("keyBinary").setType(EdmType.getSimple("Binary")).setNullable(true).build();
            pb2.setName("valueBinary").setType(EdmType.getSimple("Binary")).setNullable(true).build();
            pb3.setName("keyString").setType(EdmType.getSimple("String")).setNullable(true).build();
            pb4.setName("valueString").setType(EdmType.getSimple("String")).setNullable(true).build();

            funcParametersBinary.add(pb);
            funcParametersBinary.add(pb2);
            funcParametersSimpleString.add(pb3);
            funcParametersSimpleString.add(pb4);

            String entitySetNameCacheName = container.getEntitySets().get(i).getName();

            // binary
            EdmFunctionImport.Builder fb = new EdmFunctionImport.Builder();
            EdmFunctionImport.Builder fb2 = new EdmFunctionImport.Builder();
            // simple string
            EdmFunctionImport.Builder fb3 = new EdmFunctionImport.Builder();
            EdmFunctionImport.Builder fb4 = new EdmFunctionImport.Builder();

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
                  .addParameters(funcParametersBinary).build();

            fb2.setName(entitySetNameCacheName + "_get")
                  .setEntitySet(container.getEntitySets().get(i))
                  .setEntitySetName(entitySetNameCacheName)
                        // let return type to null to be able to directly access response
                  .setReturnType(EdmSimpleType.BINARY)
//                 .setHttpMethod("GET")
//                  .setBindable(true)
                  .setBindable(false)
                  .setSideEffecting(false)  // true for Action (POST)
                  .setAlwaysBindable(false)
                  .addParameters(funcParametersBinary).build();


            fb3.setName(entitySetNameCacheName + "_putString")
                  .setEntitySet(container.getEntitySets().get(i))
                  .setEntitySetName(entitySetNameCacheName)
//                 .setReturnType(null)
//                 .setHttpMethod("GET")
//                  .setBindable(true)
                  .setBindable(false)
                  .setSideEffecting(false)  // true for Action (POST)
                  .setAlwaysBindable(false)
                  .addParameters(funcParametersSimpleString).build();

            fb4.setName(entitySetNameCacheName + "_getString")
                  .setEntitySet(container.getEntitySets().get(i))
                  .setEntitySetName(entitySetNameCacheName)
                        // let return type to null to be able to directly access response
                  .setReturnType(EdmSimpleType.STRING)
//                 .setHttpMethod("GET")
//                  .setBindable(true)
                  .setBindable(false)
                  .setSideEffecting(false)  // true for Action (POST)
                  .setAlwaysBindable(false)
                  .addParameters(funcParametersSimpleString).build();


            // complex
            funcImports.add(fb);
            funcImports.add(fb2);
            // simple
            funcImports.add(fb3);
            funcImports.add(fb4);
            i++;
         }

         dump("Functions import ok...");
         container.addFunctionImports(funcImports);
      }
   }
}

package org.tsykora.odata.producer;

import org.odata4j.core.NamespacedAnnotation;
import org.odata4j.core.OCollection;
import org.odata4j.core.OCollections;
import org.odata4j.core.OComplexObject;
import org.odata4j.core.OComplexObjects;
import org.odata4j.core.OProperties;
import org.odata4j.core.OProperty;
import org.odata4j.core.OSimpleObjects;
import org.odata4j.core.PrefixedNamespace;
import org.odata4j.edm.*;
import org.odata4j.producer.PropertyPath;
import org.odata4j.producer.resources.DefaultODataProducerProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * @author tsykora
 */
public class InMemoryProducerExample extends AbstractExample {

   public static void main(String[] args) {
      InMemoryProducerExample example = new InMemoryProducerExample();
      example.run(args);
   }

   @SuppressWarnings({"unchecked", "rawtypes"})
   public void run(String[] args) {

//        String endpointUri = "http://localhost:8887/ODataInfinispanEndpoint.svc/";
      String endpointUri = args[0];

      // final InMemoryProducer producer = new InMemoryProducer("InMemoryProducerExample", null, 100, new MyEdmDecorator(), null);

      // later do it based on infinispan.xml file
      String containerName;
      String configFile;
      containerName = "InMemoryProducerExample";
//      configFile = "infinispan-dist.xml";
      configFile = "infinispan-local.xml";

      // the first parameter is containerName
      final InfinispanProducer3 producerBig =
            new InfinispanProducer3(containerName, null, 100, new MyEdmDecorator(), null, configFile);
//        final LightweightInfinispanProducer producer = new LightweightInfinispanProducer("InMemoryProducerExample", null, 100, null, null);


// <editor-fold defaultstate="collapsed" desc="other producer's registrations">
//    // expose this jvm's thread information (Thread instances) as an entity-set called "Threads"
//    producer.register(Thread.class, "Threads", new Func<Iterable<Thread>>() {
//      public Iterable<Thread> apply() {
//        ThreadGroup tg = Thread.currentThread().getThreadGroup();
//        while (tg.getParent() != null)
//          tg = tg.getParent();
//        Thread[] threads = new Thread[1000];
//        int count = tg.enumerate(threads, true);
//        return Enumerable.create(threads).take(count);
//      }
//    }, "Id");
//
//    // expose current system properties (Map.Entry instances) as an entity-set called "SystemProperties"
//    producer.register(Entry.class, "SystemProperties", new Func<Iterable<Entry>>() {
//      public Iterable<Entry> apply() {
//        return (Iterable<Entry>) (Object) System.getProperties().entrySet();
//      }
//    }, "Key");
//
//    // expose current environment variables (Map.Entry instances) as an entity-set called "EnvironmentVariables"
//    producer.register(Entry.class, "EnvironmentVariables", new Func<Iterable<Entry>>() {
//      public Iterable<Entry> apply() {
//        return (Iterable<Entry>) (Object) System.getenv().entrySet();
//      }
//    }, "Key");
//
//    // expose this producer's entity-types (EdmEntityType instances) as an entity-set called "EdmEntityTypes"
//    producer.register(EdmEntityType.class, "EdmEntityTypes", new Func<Iterable<EdmEntityType>>() {
//      public Iterable<EdmEntityType> apply() {
//        return producer.getMetadata().getEntityTypes();
//      }
//    }, "FullyQualifiedTypeName");
//
//    // expose a current listing of exchange traded funds sourced from an external csv (EtfInfo instances) as an entity-set called "ETFs"
//    producer.register(EtfInfo.class, "ETFs", Funcs.wrap(new ThrowingFunc<Iterable<EtfInfo>>() {
//      public Iterable<EtfInfo> apply() throws Exception {
//        return getETFs();
//      }
//    }), "Symbol");

      // expose an large list of integers as an entity-set called "Integers"
//      producerBig.register(Integer.class, Integer.class, "Integers", new Func<Iterable<Integer>>() {
//         public Iterable<Integer> apply() {
//            return Enumerable.range(0, 150);
//         }        
//      }, Funcs.method(Integer.class, Integer.class, "toString"));

//      producer.register(String.class, String.class, "CacheKeys", new Func<Iterable<String>>() {
//         public Iterable<String> apply() {
//            return c.keySet();
//         }
//      }, Funcs.method(String.class, String.class, "toString"));
//      producer.register(MyInternalCacheEntry.class, MyInternalCacheEntry.class, "CacheEntries", new Func<Iterable<MyInternalCacheEntry>>() {
//         public Iterable<MyInternalCacheEntry> apply() {
//            return returnInternalCacheEntrySet();
//         }
//      }, Funcs.method(MyInternalCacheEntry.class, MyInternalCacheEntry.class, "toString"));
// </editor-fold>

// <editor-fold defaultstate="collapsed" desc="2 entity creations from PRODUCER here">        

      // NOTES:
      // calling put and through some visitor? transferer? I will build OEntity for request here
      // this will be sent to the server side as an OEntity and there put in remote cache (via OData)

//        byte[] serializedKey = null;
//        try {
//            serializedKey = Utils.serialize("key8");
//        } catch (IOException ex) {
//            Logger.getLogger(InMemoryProducerExample.class.getName()).log(Level.SEVERE, null, ex);
//        }
//        
//        byte[] serializedValue = null;
//        try {
//            serializedValue = Utils.serialize("value8");
//        } catch (IOException ex) {
//            Logger.getLogger(InMemoryProducerExample.class.getName()).log(Level.SEVERE, null, ex);
//        }
//
//        Map<String, Object> entityKeysValues = new HashMap<String, Object>();
//        entityKeysValues.put("Key", serializedKey);
//
//        // based on real entry -> transfer it into OEntity by this (via properties, entrySetName is cache name etc.)
//        List<OProperty<?>> p = new ArrayList<OProperty<?>>();
//        
//        p.add(OProperties.binary("Key", serializedKey));
//        p.add(OProperties.binary("Value", serializedValue));
//
//        // entity KEY HAVE TO BE key8 !!!! Create new entity key with requested key! (Cache key)
//        // it needs to ask cache about get("key8"); and not "('key');
//        OEntity entityForPut = OEntities.create(producerBig.getMetadata().getEdmEntitySet("CacheEntries"),
//                OEntityKey.create(entityKeysValues.values()), p, null);
//
//        // CREATE
//        EntityResponse response = producerBig.createEntity("CacheEntries", entityForPut);
//
//        OEntity createdRightNow = response.getEntity();
//        reportEntity("\n\n\n This is response from producer (InMemoryProducerExample), recently created OEntity: \n ", createdRightNow);


//        entityKeysValues = new HashMap<String, Object>();
//        entityKeysValues.put("key", "key55");
//
//        // based on real entry -> transfer it into OEntity by this (via properties, entrySetName is cache name etc.)
//        p = new ArrayList<OProperty<?>>();
//        p.add(OProperties.string("key", "key55"));
//        p.add(OProperties.string("value", "value55"));
//
//
//        // why was value = null?
//
//        // entity KEY HAVE TO BE key8 !!!! Create new entity key with requested key! (Cache key)
//        // it needs to ask cache about get("key8"); and not "('key');
//        entityForPut = OEntities.create(producerBig.getMetadata().getEdmEntitySet("CacheEntries"),
//                OEntityKey.create(entityKeysValues.values()), p, null);
//
//
//        // CREATE
//        response = producerBig.createEntity("CacheEntries", entityForPut);
//
//        createdRightNow = response.getEntity();
//        reportEntity("\n\n\n This is response from producer (InMemoryProducerExample), recently created OEntity: \n ", createdRightNow);


      //      </editor-fold>


      // START ODATA SERVER
      // register the producer as the static instance, then launch the http server

      DefaultODataProducerProvider.setInstance(producerBig);
      this.rtFacde.hostODataServer(endpointUri);
   }

//   public static Set<MyInternalCacheEntry> returnInternalCacheEntrySet() {
//      Set<MyInternalCacheEntry> setOfEntries = new HashSet<MyInternalCacheEntry>();
//      for (Map.Entry<String, String> m : c.entrySet()) {
//         setOfEntries.add(new MyInternalCacheEntry(m.getKey(), m.getValue()));
//      }
//      return setOfEntries;
//   }


   public static class MyInternalCacheEntry {

      private Object key;
      private Object value;

      public MyInternalCacheEntry(Object key, Object value) {
         this.key = key;
         this.value = value;
      }

      public Object getKey() {
         return key;
      }

      public Object getValue() {
         return value;
      }

      public void setKey(Object key) {
         this.key = key;
      }

      public void setValue(Object value) {
         this.value = value;
      }

      @Override
      public boolean equals(Object obj) {
         if (obj == null) {
            return false;
         }
         if (getClass() != obj.getClass()) {
            return false;
         }
         final MyInternalCacheEntry other = (MyInternalCacheEntry) obj;
         if ((this.key == null) ? (other.key != null) : !this.key.equals(other.key)) {
            return false;
         }
         return true;
      }

      @Override
      public int hashCode() {
         int hash = 5;
         return hash;
      }
   }

   public static class MyInternalCacheEntrySimple {

      private String simpleStringKey;
      private String simpleStringValue;

      public MyInternalCacheEntrySimple(String simpleStringKey, String simpleStringValue) {
         this.simpleStringKey = simpleStringKey;
         this.simpleStringValue = simpleStringValue;
      }

      public String getSimpleStringKey() {
         return simpleStringKey;
      }

      public void setSimpleStringKey(String simpleStringKey) {
         this.simpleStringKey = simpleStringKey;
      }

      public String getSimpleStringValue() {
         return simpleStringValue;
      }

      public void setSimpleStringValue(String simpleStringValue) {
         this.simpleStringValue = simpleStringValue;
      }
   }

   public static class MyEdmDecorator implements EdmDecorator {

      public static final String namespace = "http://infinispan.org";
      public static final String prefix = "inmem";
      private final List<PrefixedNamespace> namespaces = new ArrayList<PrefixedNamespace>(1);
      private final EdmComplexType schemaInfoType;

      public MyEdmDecorator() {
         namespaces.add(new PrefixedNamespace(namespace, prefix));
         this.schemaInfoType = createSchemaInfoType().build();
      }

      @Override
      public List<PrefixedNamespace> getNamespaces() {
         return namespaces;
      }

      @Override
      public EdmDocumentation getDocumentationForSchema(String namespace) {
         return new EdmDocumentation("InMemoryProducerExample", "This schema exposes cache entries stored in Infinispan cache.");
      }

      private EdmComplexType.Builder createSchemaInfoType() {
         List<EdmProperty.Builder> props = new ArrayList<EdmProperty.Builder>();

         EdmProperty.Builder ep = EdmProperty.newBuilder("Author").setType(EdmSimpleType.STRING);
         props.add(ep);

         ep = EdmProperty.newBuilder("SeeAlso").setType(EdmSimpleType.STRING);
         props.add(ep);

         return EdmComplexType.newBuilder().setNamespace(namespace).setName("SchemaInfo").addProperties(props);

      }

      @Override
      public List<EdmAnnotation<?>> getAnnotationsForSchema(String namespace) {
         List<EdmAnnotation<?>> annots = new ArrayList<EdmAnnotation<?>>();
         annots.add(new EdmAnnotationAttribute(namespace, prefix, "Version", "1.0 early experience pre-alpha"));

         List<OProperty<?>> p = new ArrayList<OProperty<?>>();
         p.add(OProperties.string("Author", "Tomas Sykora"));
         p.add(OProperties.string("SeeAlso", "InMemoryProducerExample.java"));

         annots.add(EdmAnnotation.element(namespace, prefix, "SchemaInfo", OComplexObject.class,
                                          OComplexObjects.create(schemaInfoType, p)));

         annots.add(EdmAnnotation.element(namespace, prefix, "Tags", OCollection.class,
                                          OCollections.newBuilder(EdmSimpleType.STRING).add(OSimpleObjects.create(EdmSimpleType.STRING, "tag1")).add(OSimpleObjects.create(EdmSimpleType.STRING, "tag2")).build()));
         return annots;
      }

      @Override
      public EdmDocumentation getDocumentationForEntityType(String namespace, String typeName) {
         return null;
      }

      @Override
      public List<EdmAnnotation<?>> getAnnotationsForEntityType(String namespace, String typeName) {
         return null;
      }

      @Override
      public Object resolveStructuralTypeProperty(EdmStructuralType st, PropertyPath path) throws IllegalArgumentException {
         return null;
      }

      @Override
      public EdmDocumentation getDocumentationForProperty(String namespace, String typename, String propName) {
         return null;
      }

      @Override
      public List<EdmAnnotation<?>> getAnnotationsForProperty(String namespace, String typename, String propName) {
         return null;
      }

      @Override
      public Object resolvePropertyProperty(EdmProperty st, PropertyPath path) throws IllegalArgumentException {
         return null;
      }

      @Override
      public Object getAnnotationValueOverride(EdmItem item, NamespacedAnnotation<?> annot, boolean flatten, Locale locale, Map<String, String> options) {
         return null;
      }

      @Override
      public void decorateEntity(EdmEntitySet entitySet, EdmItem item, EdmItem originalQueryItem, List<OProperty<?>> props, boolean flatten, Locale locale, Map<String, String> options) {
         // no-op
      }
   }
}

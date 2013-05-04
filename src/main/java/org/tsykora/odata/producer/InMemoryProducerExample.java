package org.tsykora.odata.producer;

import org.core4j.Enumerables;
import org.core4j.Func;
import org.core4j.Func1;
import org.core4j.Funcs;
import org.odata4j.core.*;
import org.odata4j.edm.*;
import org.odata4j.producer.EntityResponse;
import org.odata4j.producer.PropertyPath;
import org.odata4j.producer.resources.DefaultODataProducerProvider;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * @author tsykora
 */
public class InMemoryProducerExample extends AbstractExample {

    // Infinispan stuff
//   private static Cache<String, String> c = new DefaultCacheManager().getCache();
    public static void main(String[] args) {
        InMemoryProducerExample example = new InMemoryProducerExample();
        example.run(args);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public void run(String[] args) {

        String endpointUri = "http://localhost:8887/InMemoryProducerExample.svc/";

        // feed it
//      c.put("key1", "value1");
//      c.put("key2", "value2");
//      c.put("key3", "value3");
//      c.put("key4", "value4");
//      c.put("key5", "value5");

        // InMemoryProducer is a readonly odata provider that serves up POJOs as entities using bean properties
        // call InMemoryProducer.register to declare a new entity-set, providing a entity source function and a propertyname to serve as the key
//      final InMemoryProducer producer = new InMemoryProducer("InMemoryProducerExample", null, 100, new MyEdmDecorator(), null);

        // Call own infinispan producer which implements properly all methods
        // There will be probably more producers?
        // Is there need to do my own consumer? How it will be different from ODataJerseyConsumer for example.

        final InfinispanProducer2 producerBig = new InfinispanProducer2("InMemoryProducerExample", null, 100, null, null);
//        final LightweightInfinispanProducer producer = new LightweightInfinispanProducer("InMemoryProducerExample", null, 100, null, null);


        // now - simulation (I need to find out how to generate metadata lightly):
        // register CacheEntries entity set and it should be given by metadata then...
//        // TODO: reveal magic here and REGISTER this entitySet lightweightly


        // TODO just for Producer2 -- NOW - only register my EDM entity set
        producerBig.register(MyInternalCacheEntry.class, MyInternalCacheEntry.class, "CacheEntries", new Func<Iterable<MyInternalCacheEntry>>() {

            public Iterable<MyInternalCacheEntry> apply() {

                // IMPORTANT!!!
                // TODO udelat funci apply() tak, aby pracovala, vracela to, co je aktualne v nejake cachi

                // Do I need to properly register the first entry to provide some model for another creating from consumer?
                List<MyInternalCacheEntry> firstEntryForRegister = new ArrayList<MyInternalCacheEntry>();
                firstEntryForRegister.add(new MyInternalCacheEntry("key1", "value1"));
                return firstEntryForRegister;
            }
        }, Funcs.method(MyInternalCacheEntry.class, MyInternalCacheEntry.class, "toString"));





        // temporary usage of metadata given by Heavy InMemory Producer (all decorators etc. and LightEdmGenerator!!)
//        final LightweightInfinispanProducer producer = new LightweightInfinispanProducer(producerBig.getMetadata());

        // TODO:
        // I want to LightProducer to create metadata itself (EDMGenerator, register EntitySet names - during creation? / entry creation?)


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
// </editor-fold>


        // expose an large list of integers as an entity-set called "Integers"
//      producer.register(Integer.class, Integer.class, "Integers", new Func<Iterable<Integer>>() {
//         public Iterable<Integer> apply() {
//            return Enumerable.range(0, 150);
//         }
//      }, Funcs.method(Integer.class, Integer.class, "intValue"));
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


        
        
        
        
        
        // NOTES:
        // calling put and through some visitor? transferer? I will build OEntity for request here
        // this will be sent to the server side as an OEntity and there put in remote cache (via OData)

//        Map<String, Object> entityKeysValues = new HashMap<String, Object>();
//        entityKeysValues.put("key", "key8");
//
//        // based on real entry -> transfer it into OEntity by this (via properties, entrySetName is cache name etc.)
//        List<OProperty<?>> p = new ArrayList<OProperty<?>>();
//        p.add(OProperties.string("key", "key8"));
//        p.add(OProperties.string("value", "value8"));
//
//
//        // why was value = null?
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



        
        
        
        
        
//
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
        
        
        

        // for lightweight
//        EntityResponse response = producer.createEntity("CacheEntries",
//                OEntities.create(producer.getEntitySet("CacheEntries"), OEntityKey.create(entityKeysValues),
//                p, null));

        // IMPORTANT!!!

        // IMPORTANT!!!

        // IMPORTANT!!!

        // ENTRIES (InternalCacheEntries - are stored in InMemoryEntityInfo class in get function!!!!)
        // get.apply() -- how does it exactly work??


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

        private final String key;
        private final String value;

        public MyInternalCacheEntry(String key, String value) {
            this.key = key;
            this.value = value;
        }

        public String getKey() {
            return key;
        }

        public String getValue() {
            return value;
        }

        public String toString() {
            return getKey();
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

    private static Iterable<EtfInfo> getETFs() throws Exception {
        return Enumerables.lines(new URL("http://www.masterdata.com/HelpFiles/ETF_List_Downloads/AllETFs.csv")).select(new Func1<String, EtfInfo>() {

            public EtfInfo apply(String csvLine) {
                return EtfInfo.parse(csvLine);
            }
        }).skip(1); // skip header line
    }

    public static class EtfInfo {

        private final String name;
        private final String symbol;
        private final String fundType;

        private EtfInfo(String name, String symbol, String fundType) {
            this.name = name;
            this.symbol = symbol;
            this.fundType = fundType;
        }

        public static EtfInfo parse(String csvLine) {

            csvLine = csvLine.substring(0, csvLine.lastIndexOf(','));
            int i = csvLine.lastIndexOf(',');
            String type = csvLine.substring(i + 1);
            csvLine = csvLine.substring(0, csvLine.lastIndexOf(','));
            i = csvLine.lastIndexOf(',');
            String sym = csvLine.substring(i + 1);
            csvLine = csvLine.substring(0, csvLine.lastIndexOf(','));
            String name = csvLine;
            name = name.startsWith("\"") ? name.substring(1) : name;
            name = name.endsWith("\"") ? name.substring(0, name.length() - 1) : name;
            name = name.replace("\u00A0", " ");

            return new EtfInfo(name, sym, type);
        }

        public String getName() {
            return name;
        }

        public String getSymbol() {
            return symbol;
        }

        public String getFundType() {
            return fundType;
        }
    }

    public static class MyEdmDecorator implements EdmDecorator {

        public static final String namespace = "http://tempuri.org";
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
            return new EdmDocumentation("InMemoryProducerExample", "This schema exposes a few example types to demonstrate the InMemoryProducer");
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
            p.add(OProperties.string("Author", "Xavier S. Dumont"));
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

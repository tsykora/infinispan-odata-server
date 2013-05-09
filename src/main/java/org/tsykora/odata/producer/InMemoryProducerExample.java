package org.tsykora.odata.producer;

import com.sun.crypto.provider.DESedeCipher;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.lang.Object;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.odata4j.repack.org.apache.commons.codec.binary.Base64;
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
import org.core4j.Enumerable;
import org.hibernate.type.descriptor.BinaryStream;
import org.hibernate.type.descriptor.java.BinaryStreamImpl;
import org.tsykora.odata.common.Utils;

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

        String endpointUri = "http://localhost:8887/InMemoryProducerExample.svc/";

        // InMemoryProducer is a readonly (not now - using InfinispanProducer2)
        // odata provider that serves up POJOs as entities using bean properties

        // call InMemoryProducer.register to declare a new entity-set, providing a entity source function and a propertyname to serve as the key
        // final InMemoryProducer producer = new InMemoryProducer("InMemoryProducerExample", null, 100, new MyEdmDecorator(), null);

        // Call own infinispan producer which implements properly all methods, will there be probably more producers?
        // Is there need to do my own consumer? How it will be different from ODataJerseyConsumer for example?

        final InfinispanProducer2 producerBig = new InfinispanProducer2("InMemoryProducerExample", null, 100, new MyEdmDecorator(), null);
//        final LightweightInfinispanProducer producer = new LightweightInfinispanProducer("InMemoryProducerExample", null, 100, null, null);

        
        // TODO: reveal magic here and REGISTER this entitySet lightweightly

        // TODO just for Producer2 -- NOW - only register my EDM entity set
        // TODO - check class of KEY and the last Funcs.method (try to use simple strings for key or Object.getId()??

        producerBig.register(MyInternalCacheEntry.class, MyInternalCacheEntry.class, "CacheEntries", new Func<Iterable<MyInternalCacheEntry>>() {
            // TEMPORARY REGISTRATION of CACHE ENTRIES set name!!!
            // TODO - can I skip this registration? Can I do it inside of producer while starting new cache?
            // TODO - while starting service? while creating new cache from builder? or according to xml?
            // TODO - register entrySet fro new cache after it starts.

            public Iterable<MyInternalCacheEntry> apply() {
                List<MyInternalCacheEntry> firstEntryForRegister = new ArrayList<MyInternalCacheEntry>();
                firstEntryForRegister.add(new MyInternalCacheEntry("key8".getBytes(), "value8".getBytes()));
//                firstEntryForRegister.add(new MyInternalCacheEntry(new Pair<Object, Object>("aa", "bb"), new Pair<Object, Object>("vv", "valval")));
                return firstEntryForRegister;
            }
        }, Funcs.method(MyInternalCacheEntry.class, MyInternalCacheEntry.class, "toString"));





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
        
        byte[] serializedKey = null;
        try {
            serializedKey = Utils.serialize("key8");
        } catch (IOException ex) {
            Logger.getLogger(InMemoryProducerExample.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        byte[] serializedValue = null;
        try {
            serializedValue = Utils.serialize("value8");
        } catch (IOException ex) {
            Logger.getLogger(InMemoryProducerExample.class.getName()).log(Level.SEVERE, null, ex);
        }

        Map<String, Object> entityKeysValues = new HashMap<String, Object>();
        entityKeysValues.put("Key", serializedKey);

        // based on real entry -> transfer it into OEntity by this (via properties, entrySetName is cache name etc.)
        List<OProperty<?>> p = new ArrayList<OProperty<?>>();
        
        p.add(OProperties.binary("Key", serializedKey));
        p.add(OProperties.binary("Value", serializedValue));

        // entity KEY HAVE TO BE key8 !!!! Create new entity key with requested key! (Cache key)
        // it needs to ask cache about get("key8"); and not "('key');
        OEntity entityForPut = OEntities.create(producerBig.getMetadata().getEdmEntitySet("CacheEntries"),
                OEntityKey.create(entityKeysValues.values()), p, null);

        // CREATE
        EntityResponse response = producerBig.createEntity("CacheEntries", entityForPut);

        OEntity createdRightNow = response.getEntity();
        reportEntity("\n\n\n This is response from producer (InMemoryProducerExample), recently created OEntity: \n ", createdRightNow);
        
        





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
    // TODO: rename this parameterized type to CacheEntry or CacheKey for example...
    // so there are these types stored in EDM as a property model
    // and according to this given pattern clients communicate with cache
    public static class Pair<X, Y> {

        private X first;
        private Y second;

        public Pair(X a1, Y a2) {
            first = a1;
            second = a2;
        }

        public X getFirst() {
            return first;
        }

        public Y getSecond() {
            return second;
        }

        public void setFirst(X arg) {
            first = arg;
        }

        public void setSecond(Y arg) {
            second = arg;
        }
    }

    public static class MyInternalCacheEntry {

        // TODO: I need to use serialization
        // Transform Object into ByteArray
        // Use Stream?
        // I can't register iterable types here
        // Where can I register them?
        // In complex types?? Where can I register collections?
        // ?????????????????????
        // Method - computeCollections
        // IMPORTANT
        // Setter has to be implemented too
        // for more info see code in BeanModel.java computeCollections()
        // parameterizedTypes 
//        private Pair<Object,Object> key;
//        private Pair<Object,Object> value;
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

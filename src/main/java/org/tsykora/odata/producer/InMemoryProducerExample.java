package org.tsykora.odata.producer;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.odata4j.core.NamespacedAnnotation;
import org.odata4j.core.OCollection;
import org.odata4j.core.OCollections;
import org.odata4j.core.OComplexObject;
import org.odata4j.core.OComplexObjects;
import org.odata4j.core.OProperties;
import org.odata4j.core.OProperty;
import org.odata4j.core.OSimpleObjects;
import org.odata4j.core.PrefixedNamespace;
import org.odata4j.edm.EdmAnnotation;
import org.odata4j.edm.EdmAnnotationAttribute;
import org.odata4j.edm.EdmComplexType;
import org.odata4j.edm.EdmDecorator;
import org.odata4j.edm.EdmDocumentation;
import org.odata4j.edm.EdmEntitySet;
import org.odata4j.edm.EdmItem;
import org.odata4j.edm.EdmProperty;
import org.odata4j.edm.EdmSimpleType;
import org.odata4j.edm.EdmStructuralType;
import org.odata4j.producer.PropertyPath;
import org.odata4j.producer.resources.DefaultODataProducerProvider;

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
      configFile = "infinispan-dist.xml";
//      configFile = "infinispan-local.xml";

      // the first parameter is containerName
      final InfinispanProducer3 producerBig =
            new InfinispanProducer3(containerName, null, 100, new MyEdmDecorator(), null, configFile);
//        final LightweightInfinispanProducer producer = new LightweightInfinispanProducer("InMemoryProducerExample", null, 100, null, null);


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

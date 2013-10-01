package org.tsykora.odata.producer;

import org.apache.lucene.document.Document;
import org.hibernate.search.bridge.FieldBridge;
import org.hibernate.search.bridge.LuceneOptions;

/**
 *
 * This is the field bridge that is able to extract actual fields from the JSON and index them with Lucene.
 *
 * @author tsykora@redhat.com
 * @since 4.0
 */
public final class JsonValueWrapperFieldBridge implements FieldBridge {

//   private final Cache cache;
//
//   public JsonValueWrapperFieldBridge(Cache cache) {
//      this.cache = cache;
//   }

   @Override
   public void set(String name, Object value, Document document, LuceneOptions luceneOptions) {
      if (!(value instanceof JsonValueWrapper)) {
         throw new IllegalArgumentException("This FieldBridge can only be applied to a JsonValueWrapper");
      }
//      JsonValueWrapper valueWrapper = (JsonValueWrapper) value;


//       fieldName - The field name
//       indexedString - The value to index
//       document - the document to which to add the the new field

       // we need to move through all JSON fields structure and add them one by one to the document for indexing
       // + there will be probably needed to recognize type string, int etc. for making queries like "quantity>4"
//       valueWrapper.getJson();

       String fieldName = "jsonFieldName1";
       String indexedString = "jsonIndexedString1";

       // Do the magic here... add it to the Lucene Document
       luceneOptions.addFieldToDocument(fieldName, indexedString, document);

//      decodeAndIndex(valueWrapper.getJson(), document, luceneOptions);
   }

   private void decodeAndIndex(String json, Document document, LuceneOptions luceneOptions) {
//      SerializationContext serCtx = ProtobufMetadataManager.getSerializationContext(cache.getCacheManager());
//      Descriptors.Descriptor wrapperDescriptor = serCtx.getMessageDescriptor(WrappedMessage.PROTOBUF_TYPE_NAME);
//      try {
//         new ProtobufParser().parse(new WrappedMessageTagHandler(document, luceneOptions, serCtx), wrapperDescriptor, json);
//      } catch (IOException e) {
//         throw new CacheException(e);
//      }
   }
}

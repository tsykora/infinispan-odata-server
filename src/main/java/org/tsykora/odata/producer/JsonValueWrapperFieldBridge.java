package org.tsykora.odata.producer;

import java.io.IOException;
import java.util.Map;

import org.apache.lucene.document.Document;
import org.codehaus.jackson.map.ObjectMapper;
import org.hibernate.search.bridge.FieldBridge;
import org.hibernate.search.bridge.LuceneOptions;

/**
 * We use this field bridge for extracting actual fields from the JSON and indexing them with Lucene.
 * TODO: find out how to index numeric values (for queries like <, > etc.)
 *
 * Expected JSON format from client:
 *
 * {"d" : {"jsonValue" : "{
 * "entityClass":"org.infinispan.odata.Person",
 * "id":"person1",
 * "gender":"MALE",
 * "firstName":"John",
 * "lastName":"Smith",
 * "age":24}"
 * }}
 *
 * NOTE: only jsonValue is extracted in InfinispanProducer before put.
 * We are putting and indexing actually only this section:
 *
 * {"entityClass":"org.infinispan.odata.Person",
 * "id":"person1",
 * "gender":"MALE",
 * "firstName":"John",
 * "lastName":"Smith",
 * "age":24}
 *
 * as this is the actual entry which we need to store in the cache and index.
 *
 * @author tsykora@redhat.com
 */
public final class JsonValueWrapperFieldBridge implements FieldBridge {

    @Override
    public void set(String name, Object value, Document document, LuceneOptions luceneOptions) {
        if (!(value instanceof JsonValueWrapper)) {
            throw new IllegalArgumentException("This FieldBridge can only be applied to a JsonValueWrapper");
        }

        JsonValueWrapper valueWrapper = (JsonValueWrapper) value;
        String json = valueWrapper.getJson();

//       fieldName - The field name
//       indexedString - The value to index
//       document - the document to which to add the the new field

        // we need to move through all JSON fields structure and add them one by one to the document for indexing
        // + there will be probably needed to recognize type string, int etc. for making queries like "quantity>4"
//       valueWrapper.getJson();

        ObjectMapper mapper = new ObjectMapper();
        try {
            System.out.println("Reading by Jackson mapper, json to read: " + json);
            Map<String, Object> entryAsMap = (Map<String, Object>) mapper.readValue(json, Object.class);

            System.out.println("Reading output ---> for storing into Lucene document: ");
            for (String field : entryAsMap.keySet()) {
                System.out.println(" * Field: " + field + " *");
                System.out.println("Class: " + entryAsMap.get(field).getClass());
                System.out.println("value: " + entryAsMap.get(field));
                System.out.println("value.toString() " + entryAsMap.get(field));

                if (entryAsMap.get(field) instanceof Number) {

                    System.out.println("THIS FIELD CONTAINS NUMBER! Value is Number, " +
//                            "instance of Number ==> adding as NumericField to Lucene Document.");
                            "instance of Number ==> DOES NOT WORK YET!!!!");

                    // Like this? How do we index numbers in the Infinispan?
//                    Integer number = Integer.getInteger(entryAsMap.get(field).toString());
//                    luceneOptions.addNumericFieldToDocument(field, number, document);
                } else {

                    // not a number
                    // TODO: how to deal with lists, LinkedHashMaps? (recursive addition?) (index lists somehow?)

                    // Do the magic here... add it to the Lucene Document
                    luceneOptions.addFieldToDocument(field, entryAsMap.get(field).toString(), document);
                }
            }


//          decodeAndIndex(valueWrapper.getJson(), document, luceneOptions);

        } catch (IOException e) {
            System.out.println("EXCEPTION: in JsonValueWrapperFieldBridge.... " + e.getMessage());
            e.printStackTrace();
        }


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

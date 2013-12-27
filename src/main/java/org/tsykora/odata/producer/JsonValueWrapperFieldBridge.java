package org.tsykora.odata.producer;

import java.io.IOException;
import java.io.Serializable;
import java.util.Map;

import org.apache.log4j.Logger;
import org.apache.lucene.document.Document;
import org.codehaus.jackson.map.ObjectMapper;
import org.hibernate.search.bridge.FieldBridge;
import org.hibernate.search.bridge.LuceneOptions;

/**
 * We use this field bridge for extracting actual fields from the JSON and indexing them with Lucene.
 * TODO: find out how to index numeric values (for queries like <, > etc.)
 *
 *
 * TODO: BUG here!
 * TODO: it's expected that clients will call POST with only extracted jsonValue to pass. Without "d"
 * TODO: we need to prepare service for it.
 *
 *
 * Expected JSON format from client:
 *
 * {"d" : {"jsonValue" : {
 * "entityClass":"org.infinispan.odata.Person",
 * "id":"person1",
 * "gender":"MALE",
 * "firstName":"John",
 * "lastName":"Smith",
 * "age":24}
 * }}
 *
 * NOTE: only jsonValue is extracted in InfinispanProducer before put/replace entry into the cache.
 * We are putting and indexing actually only this section:
 *
 * {"entityClass":"org.infinispan.odata.Person",
 * "id":"person1",
 * "gender":"MALE",
 * "firstName":"John",
 * "lastName":"Smith",
 * "age":24}
 *
 * as this is the particular entry which we need to store in the cache and index.
 *
 * @author tsykora@redhat.com
 */
public final class JsonValueWrapperFieldBridge implements FieldBridge, Serializable {

    private static final Logger log = Logger.getLogger(JsonValueWrapperFieldBridge.class.getName());

    @Override
    public void set(String name, Object value, Document document, LuceneOptions luceneOptions) {
        if (!(value instanceof JsonValueWrapper)) {
            throw new IllegalArgumentException("This FieldBridge can only be applied to a JsonValueWrapper");
        }

        // TODO: what to do when I have corrupted JSON as input???

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
            log.trace("Reading by Jackson mapper, json to read: " + json);
            Map<String, Object> entryAsMap = (Map<String, Object>) mapper.readValue(json, Object.class);

            log.trace("Reading output ---> for storing into Lucene document: ");
            for (String field : entryAsMap.keySet()) {
                log.trace(" * Field: " + field + " *");
                log.trace("Class: " + entryAsMap.get(field).getClass());
                log.trace("value: " + entryAsMap.get(field));
                log.trace("value.toString() " + entryAsMap.get(field));

                if (entryAsMap.get(field) instanceof Number) {

                    log.trace("THIS FIELD CONTAINS NUMBER! Value is Number, " +
//                            "instance of Number ==> adding as NumericField to Lucene Document.");
                            "instance of Number ==> DOES NOT WORK YET!!!!");

                    // Like this? How do we index numbers in the Infinispan?
//                    Integer number = Integer.getInteger(entryAsMap.get(field).toString());
//                    luceneOptions.addNumericFieldToDocument(field, number, document);

                    // Lucene is transforming it to some strings and then doing lexicographic ordering

                } else {
                    // not a number
                    // TODO: how to deal with lists, LinkedHashMaps? (recursive addition?) (index lists somehow?)
                    // Do the magic here... add it to the Lucene Document
                    luceneOptions.addFieldToDocument(field, entryAsMap.get(field).toString(), document);
                }
            }

        } catch (IOException e) {
            log.error("EXCEPTION occurred in JsonValueWrapperFieldBridge.... " + e.getMessage());
            e.printStackTrace();
        }
    }
}

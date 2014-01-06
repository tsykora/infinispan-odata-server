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
 * Link between JSON document which is put into Infinispan cache and Lucene Document.
 *
 * This field bridge is used for extracting fields from JSON document which is being put
 * into Infinispan cache and for indexing those fields.
 *
 * TODO: find out how to index numeric values (for queries like <, > etc., lt gt in OData)
 *
 * @author Tomas Sykora <tomas@infinispan.org>
 */
public final class JsonValueWrapperFieldBridge implements FieldBridge, Serializable {

    private static final Logger log = Logger.getLogger(JsonValueWrapperFieldBridge.class.getName());

    private JsonValueWrapper valueWrapper;
    private String json;
    private ObjectMapper mapper = new ObjectMapper();

    @Override
    public void set(String name, Object value, Document document, LuceneOptions luceneOptions) {
        if (!(value instanceof JsonValueWrapper)) {
            throw new IllegalArgumentException("This FieldBridge can only be applied to a JsonValueWrapper");
        }

        valueWrapper = (JsonValueWrapper) value;
        json = valueWrapper.getJson();

        try {

            log.trace("Reading by Jackson mapper, incoming JSON document: " + json);
            Map<String, Object> entryAsMap = (Map<String, Object>) mapper.readValue(json, Object.class);

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

//                    Like this???
//                    if ( value != null ) {
//                        Long indexedValue = Long.valueOf("24");
//                        luceneOptions.addNumericFieldToDocument( name, indexedValue, document );
//                    }

                    // Lucene is transforming it to some strings and then doing lexicographic ordering

                } else {
                    luceneOptions.addFieldToDocument(field, entryAsMap.get(field).toString(), document);
                }
            }

        } catch (IOException e) {
            log.error("EXCEPTION occurred in JsonValueWrapperFieldBridge during adding fields into Lucene Document."
                    + e.getMessage());
            e.printStackTrace();
        }
    }
}

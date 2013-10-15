import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HTTP;
import org.codehaus.jackson.map.ObjectMapper;

import static org.junit.Assert.fail;

/**
 * @author tsykora
 */
public class TestingUtils {

    public static HttpResponse httpPostPutJsonEntry(String serviceUri, String cacheName, String entryKey, String jsonValue) {

        HttpClient httpClient = new DefaultHttpClient();

        String post = serviceUri + "" + cacheName + "_put?key=%27" + entryKey + "%27";
        HttpPost httpPost = new HttpPost(post);
        httpPost.setHeader("Content-Type", "application/json; charset=UTF-8");
        httpPost.setHeader("Accept", "application/json; charset=UTF-8");

        try {
            StringEntity se = new StringEntity(jsonValue.toString(), HTTP.UTF_8);
            se.setContentEncoding(new BasicHeader(HTTP.CONTENT_TYPE, "application/json; charset=UTF-8"));
            se.setContentType("application/json; charset=UTF-8");
            httpPost.setEntity(se);

            System.out.println("Executing HTTP POST...");
            return httpClient.execute(httpPost);

        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        fail("HttpResponse for return expected");
        return null;
    }


    // let this return Map<String, Object> as uniform map for representing JSON objects
    public static String createJsonPersonString(String entityClass, String id,
                                                String gender, String firstName, String lastName, int age) {

        StringBuilder sb = new StringBuilder();

//        // according do OData JSON format standard
//        sb.append("{\"d\" : {\"jsonValue\" : \"");
//        sb.append("{");
//        sb.append("\"entityClass\":\"" + entityClass + "\",\n");
//        sb.append("\"id\":\"" + id + "\",\n");
//        sb.append("\"gender\":\"" + gender + "\",\n");
//        sb.append("\"firstName\":\"" + firstName + "\",\n");
//        sb.append("\"lastName\":\"" + lastName + "\"\n");
////        sb.append("\"lastName\":\"" + lastName + "\",\n");
////        sb.append("\"age\":" + age + "\n");
//        sb.append("}");
//        sb.append("\"}}");


        // according do OData JSON format standard
        sb.append("{\"d\" : {\"jsonValue\" : ");
        sb.append("{");
        sb.append("\"entityClass\":\"" + entityClass + "\"\n");
//        sb.append("\"id\":\"" + id + "\"\n");
//        sb.append("\"gender\":\"" + gender + "\",\n");
//        sb.append("\"firstName\":\"" + firstName + "\",\n");
//        sb.append("\"lastName\":\"" + lastName + "\"\n");
//        sb.append("\"lastName\":\"" + lastName + "\",\n");
//        sb.append("\"age\":" + age + "\n");
        sb.append("}");
        sb.append("}}");


        return sb.toString();
    }


    /**
     * Standardized format
     * <p/>
     * {"d" : {"jsonValue" : "{
     * "entityClass":"org.infinispan.odata.Person",
     * "id":"person1",
     * "gender":"MALE",
     * "firstName":"John",
     * "lastName":"Smith",
     * "age":24}"
     * }}
     *
     * @param standardizedJson - obtain it like: Object standardizedJson = mapper.readValue(jsonInStream, Object.class);
     *                         where InputStream jsonInStream = httpResponse.getEntity().getContent();
     */
    public static String extractJsonValueFromStandardizedODataJsonAsString(Object standardizedJson) {

//        Object returnedJsonValue = null;
        String returnedJsonValueAsString = null;
        Map<String, Object> entryAsMap = null;

        ObjectMapper mapper = new ObjectMapper();

        if (standardizedJson instanceof Map) {
            entryAsMap = (Map<String, Object>) standardizedJson;


            System.out.println("Test: Reading JSON response from put.");
            for (String field : entryAsMap.keySet()) {
                System.out.println(" * Field: " + field + " *");
                System.out.println("Class: " + entryAsMap.get(field).getClass());
                System.out.println("value: " + entryAsMap.get(field));

                Map<String, Object> childEntry = (Map<String, Object>) entryAsMap.get(field);

                try {
                    System.out.println("FROM RESPONSE, WRITE AS STRING: " + mapper.writeValueAsString(childEntry.get("jsonValue")));
                    returnedJsonValueAsString = mapper.writeValueAsString(childEntry.get("jsonValue"));
                } catch (IOException e) {
                    e.printStackTrace();
                }
//                returnedJsonValue = childEntry;

//            returnedJsonValue = childEntry.get("jsonValue");
//            System.out.println("TestUtil: Extracted jsonValue from response: " + returnedJsonValue);
            }
        }

        if (standardizedJson instanceof String) {

            try {

                InputStream stream = new ByteArrayInputStream(standardizedJson.toString().getBytes("UTF-8"));

                // read it from stream, like reading from response
                entryAsMap = (Map<String, Object>) mapper.readValue(stream, Object.class);

                Map<String, Object> childDmap = (Map<String, Object>) entryAsMap.get("d");

                System.out.println("String test util write value as string d " + mapper.writeValueAsString(entryAsMap.get("d")));
                System.out.println("FROM ORIGINAL WRITE as string childD jsonValue " + mapper.writeValueAsString(childDmap.get("jsonValue")));
                System.out.println("FROM ORIGINAL WRITE as string childD jsonValue TWICE " +
                        mapper.writeValueAsString(mapper.writeValueAsString(childDmap.get("jsonValue"))));

//                String stringFromMapper = mapper.writeValueAsString(entryAsMap.get("d"));
                String stringFromMapper = mapper.writeValueAsString(childDmap.get("jsonValue"));
                // to simulate input from response which is whole json encapsulated like a string
                String stringFromMapper2 = mapper.writeValueAsString(stringFromMapper);

                // to convert it to json map for comparing
                // TODO: decide: maybe we should test equals on the whole string, because client wants to get whole json string back
                returnedJsonValueAsString = stringFromMapper2;
                System.out.println("returning returnedJsonValue read from writers output: " + returnedJsonValueAsString);

            } catch (IOException e) {
                e.printStackTrace();
                fail("String standardized json exception: " + e.getMessage());
            }
        }


//        return (Map<String, Object>) returnedJsonValue;
        return returnedJsonValueAsString;
    }

}

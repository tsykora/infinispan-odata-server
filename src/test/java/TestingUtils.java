import java.io.IOException;
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

import static org.junit.Assert.assertTrue;
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
            StringEntity se = new StringEntity(jsonValue, HTTP.UTF_8);
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


    /**
     * Return OData standardized JSON (represented as String)
     * This can be passed as content (StringEntity) of HTTP POST request
     *
     * @param entityClass
     * @param id
     * @param gender
     * @param firstName
     * @param lastName
     * @param age
     * @return Standardized OData JSON person entity as String.
     */
    public static String createJsonPersonString(String entityClass, String id,
                                                String gender, String firstName, String lastName, int age) {

        StringBuilder sb = new StringBuilder();

        // according do OData JSON format standard
        sb.append("{\"d\" : {\"jsonValue\" : ");
        sb.append("{");
        sb.append("\"entityClass\":\"" + entityClass + "\",\n");
        sb.append("\"id\":\"" + id + "\",\n");
        sb.append("\"gender\":\"" + gender + "\",\n");
        sb.append("\"firstName\":\"" + firstName + "\",\n");
        sb.append("\"lastName\":\"" + lastName + "\",\n");
        sb.append("\"age\":" + age + "\n");
        sb.append("}");
        sb.append("}}");

        return sb.toString();
    }


    /**
     * Standardized format
     * <p/>
     * {"d" : {"jsonValue" : {
     * "entityClass":"org.infinispan.odata.Person",
     * "id":"person1",
     * "gender":"MALE",
     * "firstName":"John",
     * "lastName":"Smith",
     * "age":24}
     * }}
     *
     * @param standardizedJson Obtain it like: Object standardizedJson = mapper.readValue(jsonInStream, Object.class);
     *                         where InputStream jsonInStream = httpResponse.getEntity().getContent(); or this can be
     *                         String which represents standardized JSON Object.
     * @return returns value of field jsonValue as String
     */
    public static String extractJsonValueFromStandardizedODataJsonAsString(Object standardizedJson) {

        String returnedJsonValueAsString = null;
        Map<String, Object> entryAsMap = null;
        ObjectMapper mapper = new ObjectMapper();

        if (standardizedJson instanceof Map) {
            // this is JSON Object from HTTP response
            System.out.println("Extraction in TestUtil: " +
                    "Directly extracting jsonValue from Object Map (was read from HTTP response InputStream).");
            entryAsMap = (Map<String, Object>) standardizedJson;
            Map<String, Object> childEntry = (Map<String, Object>) entryAsMap.get("d");
            try {
                returnedJsonValueAsString = mapper.writeValueAsString(childEntry.get("jsonValue"));
            } catch (IOException e) {
                e.printStackTrace();
                fail("Object Map standardized json exception: " + e.getMessage());
            }
        }

        if (standardizedJson instanceof String) {
            try {
                // read from String
                System.out.println("Extraction in TestUtil: Reading JSON from String for extraction...");
                entryAsMap = (Map<String, Object>) mapper.readValue(standardizedJson.toString(), Object.class);
                Map<String, Object> childMap = (Map<String, Object>) entryAsMap.get("d");
                returnedJsonValueAsString = mapper.writeValueAsString(childMap.get("jsonValue"));
            } catch (IOException e) {
                e.printStackTrace();
                fail("String standardized json exception: " + e.getMessage());
            }
        }

        assertTrue("ReturnedJsonValueAsString should not be null", returnedJsonValueAsString != null);
        System.out.println("Extraction in TestUtil: Returning returnedJsonValueAsString: " + returnedJsonValueAsString);
        return returnedJsonValueAsString;
    }
}

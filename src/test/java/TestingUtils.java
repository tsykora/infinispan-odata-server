import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
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

    public static HttpResponse httpPostPutJsonEntry(String serviceUri, String cacheName,
                                                    String entryKey, String jsonValue, boolean ignoreReturnValues) throws UnsupportedEncodingException{

        HttpClient httpClient = new DefaultHttpClient();
        String post = "";

        if (ignoreReturnValues) {
            // test IGNORE_RETURN_VALUES flag
            post = serviceUri + "" + cacheName + "_put?IGNORE_RETURN_VALUES=%27true%27&key=%27" + entryKey + "%27";
        } else {
            post = serviceUri + "" + cacheName + "_put?IGNORE_RETURN_VALUES=%27false%27&key=%27" + entryKey + "%27";
        }

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


    public static HttpResponse httpGetJsonEntryByEntryKey(String serviceUri, String cacheName, String entryKey) {

        HttpClient httpClient = new DefaultHttpClient();

        String get = serviceUri + "" + cacheName + "_get?key=%27" + entryKey + "%27";
        HttpGet httpGet = new HttpGet(get);
        httpGet.setHeader("Content-Type", "application/json; charset=UTF-8");
        httpGet.setHeader("Accept", "application/json; charset=UTF-8");

        try {
            System.out.println("Executing HTTP GET...");
            return httpClient.execute(httpGet);
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

    public static HttpResponse httpGetJsonEntryByODataQuery(String serviceUri, String cacheName, String filterQuery) {
        HttpClient httpClient = new DefaultHttpClient();

        try {
            filterQuery = URLEncoder.encode(filterQuery, "UTF-8");
            String get = serviceUri + "" + cacheName + "_get?$filter=" + filterQuery;
            HttpGet httpGet = new HttpGet(get);
            httpGet.setHeader("Content-Type", "application/json; charset=UTF-8");
            httpGet.setHeader("Accept", "application/json; charset=UTF-8");

            System.out.println("Executing HTTP GET...");
            return httpClient.execute(httpGet);
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
        sb.append("{");
        sb.append("\"entityClass\":\"" + entityClass + "\",");
        sb.append("\"id\":\"" + id + "\",");
        sb.append("\"gender\":\"" + gender + "\",");
        sb.append("\"firstName\":\"" + firstName + "\",");
        sb.append("\"lastName\":\"" + lastName + "\",");
        sb.append("\"age\":" + age);
        sb.append("}");
        return sb.toString();
    }

    /**
     * This method extracts raw jsonValue from standardized service response.
     *
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
     * @param standardizedJson - can be obtained like: <p> Object standardizedJson = mapper.readValue(jsonInStream, Object.class);
     *                         where InputStream jsonInStream = httpResponse.getEntity().getContent();
     *
     * @return returns value of field jsonValue as String (i.e. raw JSON entity which was stored into the cache)
     */
public static String extractJsonValueFromStandardizedODataJsonAsString(Object standardizedJson) {

        String returnedJsonValueAsString = null;
        Map<String, Object> entryAsMap = null;
        ObjectMapper mapper = new ObjectMapper();

        if (standardizedJson instanceof Map) {
            // this is JSON Object from HTTP response
            entryAsMap = (Map<String, Object>) standardizedJson;
            Map<String, Object> childMap = (Map<String, Object>) entryAsMap.get("d");
            try {
                returnedJsonValueAsString = mapper.writeValueAsString(childMap.get("jsonValue"));
            } catch (IOException e) {
                e.printStackTrace();
                fail("Object Map standardized json exception: " + e.getMessage());
            }
        }
        assertTrue("ReturnedJsonValueAsString should not be null", returnedJsonValueAsString != null);
        return returnedJsonValueAsString;
    }
}

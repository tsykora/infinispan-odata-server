import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HTTP;
import org.codehaus.jackson.map.ObjectMapper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Utils class for Infinispan OData server functional test suite.
 *
 * @author Tomas Sykora <tomas@infinispan.org>
 */
public class TestingUtils {

    private static ObjectMapper mapper = new ObjectMapper();

    public static HttpResponse httpPostPutJsonEntry(String serviceUri, String cacheName,
                                                    String entryKey, String jsonValue, boolean ignoreReturnValues) throws UnsupportedEncodingException {

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

    public static HttpResponse httpPutReplaceJsonEntry(String serviceUri, String cacheName,
                                                    String entryKey, String jsonValue, boolean ignoreReturnValues) throws UnsupportedEncodingException {
        HttpClient httpClient = new DefaultHttpClient();
        String put = "";

        put = serviceUri + "" + cacheName + "_replace?key=%27" + entryKey + "%27";

        HttpPut httpPut = new HttpPut(put);

        try {
            StringEntity se = new StringEntity(jsonValue, HTTP.UTF_8);
            se.setContentEncoding(new BasicHeader(HTTP.CONTENT_TYPE, "application/json; charset=UTF-8"));
            se.setContentType("application/json; charset=UTF-8");
            httpPut.setEntity(se);

            return httpClient.execute(httpPut);
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

    public static HttpResponse httpDeleteRemoveJsonEntryByEntryKey(String serviceUri, String cacheName, String entryKey) {

        HttpClient httpClient = new DefaultHttpClient();

        String delete = serviceUri + "" + cacheName + "_remove?key=%27" + entryKey + "%27";
        HttpDelete httpDelete = new HttpDelete(delete);
        httpDelete.setHeader("Content-Type", "application/json; charset=UTF-8");
        httpDelete.setHeader("Accept", "application/json; charset=UTF-8");

        try {
            return httpClient.execute(httpDelete);
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
     * <p/>
     * Standardized format
     * <p/>
     * {"d" : {
     * "entityClass":"org.infinispan.odata.Person",
     * "id":"person1",
     * "gender":"MALE",
     * "firstName":"John",
     * "lastName":"Smith",
     * "age":24}
     * }
     *
     * @param standardizedJson - can be obtained like: <p> Object standardizedJson = mapper.readValue(jsonInStream, Object.class);
     *                         where InputStream jsonInStream = httpResponse.getEntity().getContent();
     * @return returns value of field jsonValue as String (i.e. raw JSON entity which was stored into the cache)
     */
    public static String extractJsonValueFromStandardizedODataJsonAsString(Object standardizedJson) {

        String returnedJsonValueAsString = null;
        Map<String, Object> entryAsMap = null;
        ObjectMapper mapper = new ObjectMapper();

        if (standardizedJson instanceof Map) {
            // this is JSON Object from HTTP response
            entryAsMap = (Map<String, Object>) standardizedJson;
            try {
                returnedJsonValueAsString = mapper.writeValueAsString(entryAsMap.get("d"));
            } catch (IOException e) {
                e.printStackTrace();
                fail("Object Map standardized json exception: " + e.getMessage());
            }
        }
        assertTrue("ReturnedJsonValueAsString should not be null", returnedJsonValueAsString != null);
        return returnedJsonValueAsString;
    }

    /**
     * This method tests that we got back exactly the same JSON String entity (value of field jsonValue)
     * as was put earlier into the cache
     *
     * @param httpResponse - HTTP response of a service
     * @param jsonEntity   - expected entity from client point of view
     */
    public static void compareHttpResponseWithJsonEntity(HttpResponse httpResponse, String jsonEntity) {
        try {
            InputStream jsonInStream = httpResponse.getEntity().getContent();
            Object jsonObjectFromResponse = mapper.readValue(jsonInStream, Object.class);
            String jsonValueFromResponse = TestingUtils.extractJsonValueFromStandardizedODataJsonAsString(jsonObjectFromResponse);
            assertEquals("Returned jsonValue under \"d\" in JSON string is not the same as original stored JSON string.",
                    jsonEntity, jsonValueFromResponse);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * This method tests whether expected plain string was returned by the service.
     *
     * @param httpResponse        - HTTP response of a service
     * @param expectedPlainString - expected entity from client point of view
     */
    public static void compareHttpResponseWithString(HttpResponse httpResponse, String expectedPlainString) {
        BufferedReader br = null;
        try {
            StringBuilder sb = new StringBuilder();
            br = new BufferedReader(new InputStreamReader(new BufferedInputStream(httpResponse.getEntity().getContent())));
            String readLine;
            while (((readLine = br.readLine()) != null)) {
                sb.append(readLine);
            }
            assertEquals("Returned string from the service is not the same as expected.",
                    expectedPlainString, sb.toString());
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (br != null) br.close();
            } catch (IOException e) {
            }
        }
    }
}

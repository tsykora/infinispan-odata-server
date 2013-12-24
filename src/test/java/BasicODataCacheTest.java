import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;

import org.apache.http.HttpResponse;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.tsykora.odata.server.ODataInfinispanServerRunner;

import static org.junit.Assert.assertEquals;

/**
 * This is basic test suite for OData producer + server. We are using apache http client for
 * client-server interaction.
 * <p/>
 * TODO: add test for ignore_return_values
 * TODO: + follow OData standard for 201 created + our build up for return values + 201
 * <p/>
 * TODO: ensure gets are called after puts
 *
 * @author Tomas Sykora <tomas@infinispan.org>
 */
public class BasicODataCacheTest {

    private ObjectMapper mapper = new ObjectMapper();

    String serviceUri = "http://localhost:8887/ODataInfinispanEndpoint.svc/";
    String cacheName = "mySpecialNamedCache";

    public BasicODataCacheTest() {
    }

    @BeforeClass
    public static void setUpClass() {
        String[] args = {"http://localhost:8887/ODataInfinispanEndpoint.svc/", "infinispan-dist.xml"};
        ODataInfinispanServerRunner.main(args);
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }


    @Test
    public void jsonPutIgnoreReturnValues() throws UnsupportedEncodingException {

        String jsonPersonIgnoreReturn = TestingUtils.createJsonPersonString(
                "org.infinispan.odata.Person", "personIGNORE", "MALE", "JohnIGNORE", "SmithRETURN", 24);

        System.out.println("Test: About to send HTTP POST...");
        final HttpResponse httpPutResponse = TestingUtils.httpPostPutJsonEntry(
                serviceUri, cacheName, "personIGNORE", jsonPersonIgnoreReturn, true);

        int statusCode = httpPutResponse.getStatusLine().getStatusCode();
        assertEquals("Status code from POST without any flag was expected 201 - CREATED.", 201, statusCode);

        // [ODATA SPEC] -- "location" header with URL of created entry should be returned
        assertEquals("When creating new entry (putting)," +
                " location header with URI for access new entity needs to be passed.",
                serviceUri + cacheName + "_get?key='personIGNORE'", httpPutResponse.getFirstHeader("location").getValue());

        // we have no return values here -- just message from FunctionResource class, callFunction() method (odata4j)
        compareHttpResponseWithString(httpPutResponse, "Entry created -- ready for access here: " +
                serviceUri + cacheName + "_get?key='personIGNORE'");

        // check whether it's really stored in the cache
        final HttpResponse httpGetResponse = TestingUtils.httpGetJsonEntryByEntryKey(
                serviceUri, cacheName, "personIGNORE");
        statusCode = httpGetResponse.getStatusLine().getStatusCode();
        assertEquals("Status code from GET without any flag was expected 200.", 200, statusCode);
        compareHttpResponseWithJsonEntity(httpGetResponse, jsonPersonIgnoreReturn);
    }


    @Test
    // also tests explicitly disabled IGNORE_RETURN_VALUES flag
    public void basicJsonPut() throws UnsupportedEncodingException {

        String jsonPerson1 = TestingUtils.createJsonPersonString(
                "org.infinispan.odata.Person", "person1", "MALE", "John", "Smith", 24);

        System.out.println("Test: About to send HTTP POST...");
        final HttpResponse httpPutResponse = TestingUtils.httpPostPutJsonEntry(
                serviceUri, cacheName, "person1", jsonPerson1, false);

        int statusCode = httpPutResponse.getStatusLine().getStatusCode();
        assertEquals("Status code from POST without any flag was expected 201 - CREATED.", 201, statusCode);

        // [ODATA SPEC] -- "location" header with URL of created entry should be returned
        assertEquals("When creating new entry (putting)," +
                " location header with URI for access new entity needs to be passed.",
                serviceUri + cacheName + "_get?key='person1'", httpPutResponse.getFirstHeader("location").getValue());

        compareHttpResponseWithJsonEntity(httpPutResponse, jsonPerson1);

        try {
            Thread.sleep(600000);
        } catch (InterruptedException e) {
            e.printStackTrace();  // TODO: Customise this generated block
        }
    }


    @Test
    public void basicJsonGet() {

        String jsonPerson1 = TestingUtils.createJsonPersonString(
                "org.infinispan.odata.Person", "person1", "MALE", "John", "Smith", 24);

        System.out.println("Test: About to send HTTP GET with key specified...");
        final HttpResponse httpGetResponse = TestingUtils.httpGetJsonEntryByEntryKey(
                serviceUri, cacheName, "person1");

        int statusCode = httpGetResponse.getStatusLine().getStatusCode();
        assertEquals("Status code from GET without any flag was expected 200.", 200, statusCode);

        compareHttpResponseWithJsonEntity(httpGetResponse, jsonPerson1);
    }

    @Test
    public void test404notFound() {

        System.out.println("Test: About to send HTTP GET with key specified...");
        final HttpResponse httpGetResponse = TestingUtils.httpGetJsonEntryByEntryKey(
                serviceUri, cacheName, "nonexistent_person");

        int statusCode = httpGetResponse.getStatusLine().getStatusCode();
        assertEquals("Get for nonexistent_person. Status was expected 404.", 404, statusCode);
    }


    @Test
    public void basicEQQueryTest() {

        String jsonPerson1 = TestingUtils.createJsonPersonString(
                "org.infinispan.odata.Person", "person1", "MALE", "John", "Smith", 24);

        String odataQuery = "firstName eq 'John'";
        final HttpResponse httpGetResponse = TestingUtils.httpGetJsonEntryByODataQuery(
                serviceUri, cacheName, odataQuery);

        int statusCode = httpGetResponse.getStatusLine().getStatusCode();
        assertEquals("Status code from GET without any flag was expected 200.", 200, statusCode);

        compareHttpResponseWithJsonEntity(httpGetResponse, jsonPerson1);
        // TODO: consume it
//        EntityUtils.consume(httpGetResponse.getEntity());
    }





    /**
     * This method tests that we got back exactly the same JSON String entity (value of field jsonValue)
     * as was put earlier into the cache
     *
     * @param httpResponse - HTTP response of a service
     * @param jsonEntity   - expected entity from client point of view
     */
    private void compareHttpResponseWithJsonEntity(HttpResponse httpResponse, String jsonEntity) {
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
    private void compareHttpResponseWithString(HttpResponse httpResponse, String expectedPlainString) {

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
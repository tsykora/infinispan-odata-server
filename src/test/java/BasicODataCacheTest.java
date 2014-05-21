import java.io.UnsupportedEncodingException;

import org.apache.http.HttpResponse;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.infinispan.odata.server.ODataInfinispanServerRunner;

import static org.junit.Assert.assertEquals;

/**
 * This is basic test suite for OData producer + server. We are using apache http client for
 * client-server interaction.
 * <p/>
 *
 * @author Tomas Sykora <tomas@infinispan.org>
 */
public class BasicODataCacheTest {

    String serviceUri = "http://localhost:8887/ODataInfinispanEndpoint.svc/";
    //    String cacheName = "mySpecialNamedCache";
    String cacheName = "odataCache";

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
    public void testJsonPutIgnoreReturnValues() throws UnsupportedEncodingException {

        String jsonPersonIgnoreReturn = TestingUtils.createJsonPersonString(
                "org.infinispan.odata.Person", "personIGNORE", "MALE", "JohnIGNORE", "SmithRETURN", 24);

        final HttpResponse httpPutResponse = TestingUtils.httpPostPutJsonEntry(
                serviceUri, cacheName, "personIGNORE", jsonPersonIgnoreReturn, true);

        int statusCode = httpPutResponse.getStatusLine().getStatusCode();
        assertEquals("Status code from POST without any flag was expected 201 - CREATED.", 201, statusCode);

        // [ODATA SPEC] -- "location" header with URL of created entry should be returned
        assertEquals("When creating new entry (putting)," +
                " location header with URI for access new entity needs to be passed.",
                serviceUri + cacheName + "_get?key='personIGNORE'", httpPutResponse.getFirstHeader("location").getValue());

        // we have no return values here -- just message from FunctionResource class, callFunction() method (odata4j)
        TestingUtils.compareHttpResponseWithString(httpPutResponse, "Entry created -- ready for access here: " +
                serviceUri + cacheName + "_get?key='personIGNORE'");

        // check whether it's really stored in the cache
        final HttpResponse httpGetResponse = TestingUtils.httpGetJsonEntryByEntryKey(
                serviceUri, cacheName, "personIGNORE");
        statusCode = httpGetResponse.getStatusLine().getStatusCode();
        assertEquals("Status code from GET without any flag was expected 200.", 200, statusCode);
        TestingUtils.compareHttpResponseWithJsonEntity(httpGetResponse, jsonPersonIgnoreReturn);
    }

    @Test
    public void testBasicJsonPutAndGet() throws UnsupportedEncodingException {
        testBasicJsonPut();
        testBasicJsonGet();
    }



    // also tests explicitly disabled IGNORE_RETURN_VALUES flag
    public void testBasicJsonPut() throws UnsupportedEncodingException {

        String jsonPerson1 = TestingUtils.createJsonPersonString(
                "org.infinispan.odata.Person", "person1", "MALE", "John", "Smith", 24);

        final HttpResponse httpPutResponse = TestingUtils.httpPostPutJsonEntry(
                serviceUri, cacheName, "person1", jsonPerson1, false);

        int statusCode = httpPutResponse.getStatusLine().getStatusCode();
        assertEquals("Status code from POST without any flag was expected 201 - CREATED.", 201, statusCode);

        // [ODATA SPEC] -- "location" header with URL of created entry should be returned
        assertEquals("When creating new entry (putting)," +
                " location header with URI for access new entity needs to be passed.",
                serviceUri + cacheName + "_get?key='person1'", httpPutResponse.getFirstHeader("location").getValue());

        TestingUtils.compareHttpResponseWithJsonEntity(httpPutResponse, jsonPerson1);
    }

    public void testBasicJsonGet() {
        String jsonPerson1 = TestingUtils.createJsonPersonString(
                "org.infinispan.odata.Person", "person1", "MALE", "John", "Smith", 24);

        final HttpResponse httpGetResponse = TestingUtils.httpGetJsonEntryByEntryKey(
                serviceUri, cacheName, "person1");

        int statusCode = httpGetResponse.getStatusLine().getStatusCode();
        assertEquals("Status code from GET without any flag was expected 200.", 200, statusCode);

        TestingUtils.compareHttpResponseWithJsonEntity(httpGetResponse, jsonPerson1);
    }

    @Test
    public void testReplaceAndDelete() throws UnsupportedEncodingException {

        String jsonPerson1 = TestingUtils.createJsonPersonString(
                "org.infinispan.odata.Person", "person1_ForReplace", "MALE", "John", "Smith", 24);

        final HttpResponse httpPostResponse = TestingUtils.httpPostPutJsonEntry(
                serviceUri, cacheName, "person1", jsonPerson1, false);

        assertEquals("Status code from POST without any flag was expected 201 - CREATED.", 201,
                httpPostResponse.getStatusLine().getStatusCode());

        String jsonPerson_replacement = TestingUtils.createJsonPersonString(
                "org.infinispan.odata.Person", "person1_Replacement", "MALE", "John", "Smith", 24);

        // HTTP PUT = ispn replace
        final HttpResponse httpPutResponse = TestingUtils.httpPutReplaceJsonEntry(
                serviceUri, cacheName, "person1", jsonPerson_replacement, false);


        assertEquals("Status code from PUT (replace) was expected 200.", 200,
                httpPutResponse.getStatusLine().getStatusCode());
        TestingUtils.compareHttpResponseWithJsonEntity(httpPutResponse, jsonPerson_replacement);

        // get it
        final HttpResponse httpGetResponse = TestingUtils.httpGetJsonEntryByEntryKey(
                serviceUri, cacheName, "person1");
        TestingUtils.compareHttpResponseWithJsonEntity(httpGetResponse, jsonPerson_replacement);

        // delete it
        final HttpResponse httpDeleteResponse = TestingUtils.httpDeleteRemoveJsonEntryByEntryKey(
                serviceUri, cacheName, "person1");

        assertEquals("DELETE response should return NO_CONTENT.",
                204, httpDeleteResponse.getStatusLine().getStatusCode());

        // get it -- NOT FOUND 404 needed
        final HttpResponse httpGet2Response = TestingUtils.httpGetJsonEntryByEntryKey(
                serviceUri, cacheName, "person1");
        assertEquals("Get for deleted entity does not return 404. Entity person1_Replacement should not exist.",
                404, httpGet2Response.getStatusLine().getStatusCode());
    }

    @Test
    public void test404notFound() {
        final HttpResponse httpGetResponse = TestingUtils.httpGetJsonEntryByEntryKey(
                serviceUri, cacheName, "nonexistent_person");

        int statusCode = httpGetResponse.getStatusLine().getStatusCode();
        assertEquals("Get for nonexistent_person. Status was expected 404.", 404, statusCode);
    }

    @Test
    public void basicEQQueryTest() throws UnsupportedEncodingException {

        // put person1
        testBasicJsonPut();

        String jsonPerson1 = TestingUtils.createJsonPersonString(
                "org.infinispan.odata.Person", "person1", "MALE", "John", "Smith", 24);

        String odataQuery = "firstName eq 'John'";
        final HttpResponse httpGetResponse = TestingUtils.httpGetJsonEntryByODataQuery(
                serviceUri, cacheName, odataQuery);

        int statusCode = httpGetResponse.getStatusLine().getStatusCode();
        assertEquals("Status code from GET without any flag was expected 200.", 200, statusCode);

        TestingUtils.compareHttpResponseWithJsonEntity(httpGetResponse, jsonPerson1);
    }
}
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

import org.apache.http.HttpResponse;
import org.apache.log4j.Logger;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.tsykora.odata.server.ODataInfinispanServerRunner;

import static org.junit.Assert.assertEquals;

/**
 *
 * This test case tests functional side of replication ability of Infinispan OData servers.
 *
 * @author Tomas Sykora <tomas@infinispan.org>
 */
public class ReplicationODataCacheTest {

    private ObjectMapper mapper = new ObjectMapper();
    private static final Logger log = Logger.getLogger(ReplicationODataCacheTest.class.getName());

    public ReplicationODataCacheTest() {
    }

    @BeforeClass
    public static void setUpClass() {

        log.info("\n\n STARTING INFINISPAN ODATA SERVER http://localhost:8887/ODataInfinispanEndpoint.svc/ \n\n ");
        String[] args = {"http://localhost:8887/ODataInfinispanEndpoint.svc/",
                "infinispan-dist.xml"};
        ODataInfinispanServerRunner.main(args);

        log.info("\n\n STARTING INFINISPAN ODATA SERVER http://localhost:9887/ODataInfinispanEndpoint.svc/ \n\n ");
        String[] args2 = {"http://localhost:9887/ODataInfinispanEndpoint.svc/",
                "infinispan-dist.xml"};
        ODataInfinispanServerRunner.main(args2);
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

    private void compareHttpResponseWithJsonEntity(HttpResponse httpResponse, String jsonEntity) {

        try {
            InputStream jsonInStream = httpResponse.getEntity().getContent();
            Object jsonObjectFromResponse = mapper.readValue(jsonInStream, Object.class);

            // we (and clients) care about these 2 Strings. These represents content and actual data
            // which are stored in Infinispan caches and their fields are indexed and can be queried
            String jsonValueFromResponse = TestingUtils.extractJsonValueFromStandardizedODataJsonAsString(jsonObjectFromResponse);
            String originalJsonValue = TestingUtils.extractJsonValueFromStandardizedODataJsonAsString(jsonEntity);

            // This tests that we got back exactly the same JSON String entity (value of field jsonValue)
            // which we earlier putted into the cache
            assertEquals("Returned json string is not the same as original json string.",
                    originalJsonValue, jsonValueFromResponse);

        } catch (IOException e) {
            e.printStackTrace();
        }

    }



    @Test
    public void basicJsonPut() throws UnsupportedEncodingException {

        log.trace("\n\n Testing PUT on http://localhost:8887/ODataInfinispanEndpoint.svc/ \n\n ");

        String serviceUri = "http://localhost:8887/ODataInfinispanEndpoint.svc/";
        String cacheName = "mySpecialNamedCache";

        String jsonPerson1 = TestingUtils.createJsonPersonString(
                "org.infinispan.odata.Person", "person1", "MALE", "John", "Smith", 24);

        System.out.println("Test: About to send HTTP POST...");
        final HttpResponse httpPutResponse = TestingUtils.httpPostPutJsonEntry(
                serviceUri, cacheName, "person1", jsonPerson1, false);

        int statusCode = httpPutResponse.getStatusLine().getStatusCode();
        // TODO: BUG --- this should return 201
        assertEquals("Status code from POST without any flag was expected 201 - CREATED.", 200, statusCode);

        compareHttpResponseWithJsonEntity(httpPutResponse, jsonPerson1);
    }


    @Test
    public void basicJsonGet() {

        log.trace("\n\n Testing GET from http://localhost:9887/ODataInfinispanEndpoint.svc/ \n\n ");

        String serviceUri = "http://localhost:9887/ODataInfinispanEndpoint.svc/";
        String cacheName = "mySpecialNamedCache";

        String jsonPerson1 = TestingUtils.createJsonPersonString(
                "org.infinispan.odata.Person", "person1", "MALE", "John", "Smith", 24);

        System.out.println("Test: About to send HTTP GET with key specified...");
        final HttpResponse httpGetResponse = TestingUtils.httpGetJsonEntryByEntryKey(
                serviceUri, cacheName, "person1");

        int statusCode = httpGetResponse.getStatusLine().getStatusCode();
        assertEquals("Status code from GET without any flag was expected 200.", 200, statusCode);

        compareHttpResponseWithJsonEntity(httpGetResponse, jsonPerson1);
    }




}
import java.io.IOException;
import java.io.InputStream;

import org.apache.http.HttpResponse;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.tsykora.odata.producer.ODataInfinispanServerRunner;

import static org.junit.Assert.assertEquals;

/**
 * @author tsykora
 */
public class BasicODataCacheTest {

    public BasicODataCacheTest() {
    }

    @BeforeClass
    public static void setUpClass() {
        String[] args = {"http://localhost:8887/ODataInfinispanEndpoint.svc/",
                "infinispan-dist.xml"};
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
    public void basicJsonPut() {

        String serviceUri = "http://localhost:8887/ODataInfinispanEndpoint.svc/";
        String cacheName = "mySpecialNamedCache";

        String jsonPerson1 = TestingUtils.createJsonPersonString(
                    "org.infinispan.odata.Person", "person1", "MALE", "John", "Smith", 24);

        try {
            ObjectMapper mapper = new ObjectMapper();

            System.out.println("Test: About to send HTTP POST...");
            final HttpResponse httpPutResponse = TestingUtils.httpPostPutJsonEntry(
                    serviceUri, cacheName, "person1", jsonPerson1);

            int statusCode = httpPutResponse.getStatusLine().getStatusCode();
            assertEquals("Status code from POST without any flag was expected 200.", 200, statusCode);

            InputStream jsonInStream = httpPutResponse.getEntity().getContent();
            Object jsonObjectFromResponse = mapper.readValue(jsonInStream, Object.class);

            // we (and clients) care about these 2 Strings. These represents content and actual data
            // which are stored in Infinispan caches and their fields are indexed and can be queried
            String jsonValueFromResponse = TestingUtils.extractJsonValueFromStandardizedODataJsonAsString(jsonObjectFromResponse);
            String originalJsonValue = TestingUtils.extractJsonValueFromStandardizedODataJsonAsString(jsonPerson1);

            // This tests that we got back exactly the same JSON String entity (value of field jsonValue)
            // which we earlier putted into the cache
            assertEquals("Returned json string is not the same as original json string.",
                    originalJsonValue, jsonValueFromResponse);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}
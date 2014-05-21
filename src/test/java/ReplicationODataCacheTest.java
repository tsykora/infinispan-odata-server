import java.io.UnsupportedEncodingException;

import org.apache.http.HttpResponse;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.infinispan.odata.server.ODataInfinispanServerRunner;

import static org.junit.Assert.assertEquals;

/**
 *
 * This test case tests functional side of replication ability of Infinispan OData servers.
 *
 * Put data on server1, obtain data from server2
 *
 * @author Tomas Sykora <tomas@infinispan.org>
 */
public class ReplicationODataCacheTest {

    private static final Logger log = Logger.getLogger(ReplicationODataCacheTest.class.getName());
    private static final String SERVICE_URI1 = "http://localhost:9887/ODataInfinispanEndpoint.svc/";
    private static final String SERVICE_URI2 = "http://localhost:10887/ODataInfinispanEndpoint.svc/";

    private String cacheName = "mySpecialNamedCache";

    public ReplicationODataCacheTest() {
    }

    @BeforeClass
    public static void setUpClass() {

        String[] args = {SERVICE_URI1, "infinispan-dist.xml"};
        ODataInfinispanServerRunner.main(args);

        String[] args2 = {SERVICE_URI2, "infinispan-dist.xml"};
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

    @Test
    public void basicJsonPut() throws UnsupportedEncodingException {
        log.trace("\n\n Testing PUT on " + SERVICE_URI1 + " \n\n ");

        String serviceUri = "http://localhost:9887/ODataInfinispanEndpoint.svc/";
        String jsonPerson1 = TestingUtils.createJsonPersonString(
                "org.infinispan.odata.Person", "person1", "MALE", "John", "Smith", 24);

        final HttpResponse httpPutResponse = TestingUtils.httpPostPutJsonEntry(
                SERVICE_URI1, cacheName, "person1", jsonPerson1, false);

        int statusCode = httpPutResponse.getStatusLine().getStatusCode();
        assertEquals("Status code from POST without any flag was expected 201 - CREATED.", 201, statusCode);

        TestingUtils.compareHttpResponseWithJsonEntity(httpPutResponse, jsonPerson1);
    }


    @Test
    public void basicJsonGet() {
        log.trace("\n\n Testing GET from " + SERVICE_URI2 + " \n\n ");

        String jsonPerson1 = TestingUtils.createJsonPersonString(
                "org.infinispan.odata.Person", "person1", "MALE", "John", "Smith", 24);

        final HttpResponse httpGetResponse = TestingUtils.httpGetJsonEntryByEntryKey(
                SERVICE_URI2, cacheName, "person1");

        int statusCode = httpGetResponse.getStatusLine().getStatusCode();
        assertEquals("Status code from GET without any flag was expected 200.", 200, statusCode);

        TestingUtils.compareHttpResponseWithJsonEntity(httpGetResponse, jsonPerson1);
    }
}
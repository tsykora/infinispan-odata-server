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

//        HttpClient httpClient = new DefaultHttpClient();
//
//        String exampleJsonString = "{" +
//                "\"entityClass\":\"org.my.domain.person\",\n" +
//                "\"gender\":\"MALE\",\n" +
//                "\"verified\":false,\n" +
//                "\"age\":24,\n" +
//                "\"firstname\":\"Neo\",\n" +
//                "\"lastname\":\"Matrix McMaster\"" +
//                "}";
//
//        String testUrlPost = "http://localhost:8887/ODataInfinispanEndpoint.svc/" +
//                "mySpecialNamedCache_put?key=%27person1%27";
//        HttpPost httpPost = new HttpPost(testUrlPost);
//        httpPost.setHeader("Content-Type", "application/json; charset=UTF-8");
//        httpPost.setHeader("Accept", "application/json; charset=UTF-8");
//
//        try {
//            StringEntity se = new StringEntity(exampleJsonString.toString(), HTTP.UTF_8);
//            se.setContentEncoding(new BasicHeader(HTTP.CONTENT_TYPE, "application/json; charset=UTF-8"));
//            se.setContentType("application/json; charset=UTF-8");
//            httpPost.setEntity(se);
//        } catch (UnsupportedEncodingException e) {
//            e.printStackTrace();
//        }

        String jsonPerson1 = TestingUtils.createJsonPersonString(
                    "org.infinispan.odata.Person", "person1", "MALE", "John", "Smith", 24);


        try {
            // sending http POST response to producer to create entity

            ObjectMapper mapper = new ObjectMapper();
//            String jsonForPost = mapper.writeValueAsString(jsonPerson1);

            // TODO: process response in utils, compare it with person1 ^
            System.out.println("About to send HTTP POST...");
            final HttpResponse httpPutResponse = TestingUtils.httpPostPutJsonEntry(
                    serviceUri, cacheName, "person1", jsonPerson1);

            // to avoid problems with connections
            System.out.println("httpPutResponse: status code: " + httpPutResponse.getStatusLine().getStatusCode());
            System.out.println("httpPutResponse: getEntity: " + httpPutResponse.getEntity().toString());


            InputStream jsonInStream = httpPutResponse.getEntity().getContent();

            System.out.println("\n\n");
            System.out.println(" READING JSON DIRECTLY FROM RETURNED STREAM ");
            Object jsonObjectFromResponse = mapper.readValue(jsonInStream, Object.class);

            System.out.println("jsonObjectFromResponse: " + jsonObjectFromResponse);

//            Map<String, Object> extractedJsonValueFromResponse = TestingUtils.extractJsonValueFromStandardizedODataJson(jsonObjectFromResponse);
            String extractedJsonValueFromResponseAsString =
                    TestingUtils.extractJsonValueFromStandardizedODataJsonAsString(jsonObjectFromResponse);

//            System.out.println("extractedJsonValue from response: " + extractedJsonValueFromResponse);
            System.out.println("extractedJsonValue from response as string: " + extractedJsonValueFromResponseAsString);

//            Object jsonObjectFromOriginalString = mapper.readValue(jsonPerson1, Object.class);
//            Map<String, Object> extractedJsonValueOriginal = TestingUtils.extractJsonValueFromStandardizedODataJson(jsonObjectFromOriginalString);
//            Map<String, Object> extractedJsonValueOriginal = TestingUtils.extractJsonValueFromStandardizedODataJson(jsonPerson1);
            String extractedJsonValueOriginalAsString = TestingUtils.extractJsonValueFromStandardizedODataJsonAsString(jsonPerson1);
//            System.out.println("extractedJsonValue from original json string person 1: " + extractedJsonValueOriginal);
            System.out.println("extractedJsonValue from original json string person 1 as string: " + extractedJsonValueOriginalAsString);

            assertEquals("Returned json string is not the same as original json string.",
                    extractedJsonValueOriginalAsString, extractedJsonValueFromResponseAsString);

            // extractedJsonValueFromResponse & extractedJsonValueOriginal are MAPs Objects
//            for (String originalField : extractedJsonValueOriginal.keySet()) {
//                System.out.println("Field: " + originalField);
//                System.out.println("Original value: " + extractedJsonValueOriginal.get(originalField));
//                System.out.println("Returned value: " + extractedJsonValueFromResponse.get(originalField));
//
//                assertEquals("Value of field " + originalField + " does not equal returned value from response.",
//                        extractedJsonValueOriginal.get(originalField), extractedJsonValueFromResponse.get(originalField));
//            }


//            BufferedReader rd = new BufferedReader(new InputStreamReader(httpPutResponse.getEntity().getContent()));
//            String result = "";
//            String line;
//            while ((line = rd.readLine()) != null) {
//                result += line;
//            }
//            System.out.println("RESULT OF HTTP POST!!!");
//            System.out.println(result);
//            rd.close();
//            System.out.println("status of http POST:" + httpPutResponse.getStatusLine());
//
//            String json = result;


//            try {
//                System.out.println("Test: Reading by Jackson mapper, json to read: " + jsonObjectFromResponse);
////                Map<String, Object> entryAsMap = (Map<String, Object>) mapper.readValue(json, Object.class);
//                Map<String, Object> entryAsMap = (Map<String, Object>) jsonObjectFromResponse;
//
//                System.out.println("Test: Reading JSON response from put.");
//                for (String field : entryAsMap.keySet()) {
//                    System.out.println(" * Field: " + field + " *");
//                    System.out.println("Class: " + entryAsMap.get(field).getClass());
//                    System.out.println("value: " + entryAsMap.get(field));
//
//                    Map<String, Object> childEntry = (Map<String, Object>) entryAsMap.get(field);
//                    String returnedJsonValue = childEntry.get("jsonValue").toString();
//                    System.out.println("Extracted jsonValue from response: " + returnedJsonValue);
//
//
//                    // Extract jsonValue from original JSON input
//
//                    System.out.println("\n\n");
//                    System.out.println("ORIGINAL: %" + jsonPerson1 + "%");
//                    System.out.println("RETURNED: %" + childEntry.get("jsonValue") + "%");
//
//                    Map<String, Object> jsonValueAsMapOriginal = (Map<String, Object>)
//                            mapper.readValue(jsonPerson1, Object.class);
//
//                    Map<String, Object> jsonValueAsMapReturned = (Map<String, Object>)
//                            mapper.readValue(returnedJsonValue, Object.class);
//
//                    // match 2 objects
//                    System.out.println("Running matcher checker.");
//                    for (String originalField : jsonValueAsMapOriginal.keySet()) {
//                        System.out.println("Field: " + originalField);
//                        System.out.println("Original value: " + jsonValueAsMapOriginal.get(originalField));
//                        System.out.println("Returned value: " + jsonValueAsMapReturned.get(originalField));
//
//                        assertEquals("Value of field " + originalField + " does not equal returned value from response.",
//                                jsonValueAsMapOriginal.get(originalField), jsonValueAsMapReturned.get(originalField));
//                    }
//
//                    // original has \n there... it is not equal (it is somewhere lost in wrapper I think
////                    assertEquals("JSON object which was put into the cache is not the same as returned.",
////                            childEntry.get("jsonValue"), exampleJsonString);
//                }
//
//            } catch (Exception e) {
//                assertTrue(e.getMessage(), false);
//            }
//
//            EntityUtils.consumeQuietly(httpPutResponse.getEntity());

//                    inStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}
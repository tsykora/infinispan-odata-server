package org.tsykora.odata.consumer;

import java.io.InputStream;
import java.util.Scanner;

import com.sun.jersey.api.client.ClientResponse;
import org.core4j.Enumerable;
import org.odata4j.consumer.ODataConsumer;
import org.odata4j.core.OObject;
import org.odata4j.format.FormatType;
import org.odata4j.jersey.consumer.JerseyClientResponse;
import org.tsykora.odata.common.CacheObjectSerializationAble;
import org.tsykora.odata.common.Utils;
import org.tsykora.odata.producer.AbstractExample;

/**
 * @author tsykora
 */
public class ExampleConsumer extends AbstractExample {

    public static String endpointUri = "http://localhost:8887/ODataInfinispanEndpoint.svc/";
    public static String endpointUri2 = "http://localhost:9887/ODataInfinispanEndpoint.svc/";

    public static void main(String[] args) {
        ExampleConsumer example = new ExampleConsumer();
        example.run(args);
    }

    public void run(String[] args) {

        String appendix = "";

        if (args != null) {
            endpointUri = endpointUri2;
            appendix = "valueToEndpoint9887";
        }

        // ********** HINT **********
        // To dump all the HTTP trafic
        // Sends http request and/or response information to standard out.  Useful for debugging.
        // TODO: enable it for producer as well? Is it even possible?
        ODataConsumer.dump.all(false);

        // CONSUME IT
        // format null - ATOM by default?, method to tunnel null (maybe needs change in the future)
        System.out.println("Creating instance of ExampleConsumer, initializing oDataConsumer...");
//      ODataConsumer consumer = this.rtFacde.create(endpointUri, FormatType.JSON, null);
        ODataConsumer consumer = this.rtFacde.create(endpointUri, FormatType.ATOM, null);
//      ODataConsumer consumer2 = this.rtFacde.create(endpointUri2, null, null);

        System.out.println("\n\n\n\n");
        reportMetadata(consumer.getMetadata());
//      reportMetadata(consumer2.getMetadata());
        System.out.println("\n\n\n\n");

        // http://datajs.codeplex.com/discussions/391490  ??
        // NOTE/SKILL: to call it from here Producer need to implement findExtension (it can return null)
        // + it needs some successful response for ConsumerCreateEntityRequest


        // default cache is complex, not simple cache entry (String, String)

        // TODO !!!!!!!!!! FIX THIS
        // do decision in getEntity for this case + is it needed? think...
        // automatically serialize response -- but what if I call simple getEntity from browser on simple cache based entity?

//      reportEntity(" new cache entry report: ", consumer.createEntity("defaultCache").
//            properties(OProperties.binary("Key", Utils.serialize("key4"))).
//            properties(OProperties.binary("Value", Utils.serialize("value4"))).execute())


        // TODO - FIX THIS
        // this is for only one REGISTERED entry - this does not reflex cache content!! yet!!
//        Integer count = consumer.getEntitiesCount("defaultCache").execute();
//        System.out.println("\n\n\nCount of entries is defaultCache set is: " + count);


//      List<EdmFunctionImport> functionList = new ArrayList<EdmFunctionImport>();
//      for (EdmSchema schema : consumer.getMetadata().getSchemas()) {
//         for (EdmEntityContainer eec : schema.getEntityContainers()) {
//            for (EdmFunctionImport efi : eec.getFunctionImports()) {
//               System.out.println("////////////////////////////////////////////");
//               System.out.println("Function kind of function " + efi.getName() + " is: ");
//               System.out.println(efi.getFunctionKind());
//               if (efi.getFunctionKind() == EdmFunctionImport.FunctionKind.Function ||
//                     efi.getFunctionKind() == EdmFunctionImport.FunctionKind.Action ||
//                     efi.getFunctionKind() == EdmFunctionImport.FunctionKind.ServiceOperation ) {
//                  functionList.add(efi);
//               }
//            }
//         }
//      }

        String entitySetNameCacheName = "STARTING_NONSENSE";

        System.out.println("\n\n\n");
        System.out.println("PLAYING with defaultCache for serialization + encoding64 whole objects");
        System.out.println("\n\n\n");


        CacheObjectSerializationAble objectForTransfer = new CacheObjectSerializationAble("keyxx1" + appendix, "valuexx1" + appendix);
        byte[] serializedObject = Utils.serialize(objectForTransfer);
        System.out.println("serialized object into byte[]: " + serializedObject);

        entitySetNameCacheName = "defaultCache";

//      OEntity createdEntity = consumer.createEntity(entitySetNameCacheName).
//            properties(OProperties.binary("Key", serializedObject)).
//            properties(OProperties.binary("Value", serializedObject)).execute();
//
//       System.out.println("\n\n CREATED COMPLEX!! ENTITY REPORT (created by consumer.createEntity): \n");
//       System.out.println("Key: " + createdEntity.getProperty("Key").getValue());
//       System.out.println("Value: " + createdEntity.getProperty("Value").getValue());
//       System.out.println("Deserialized Value: " + Utils.deserialize((byte[]) createdEntity.getProperty("Value").getValue()));
//       System.out.println("\n\n");
//
//


//      Enumerable<OObject> results_put_empty_serialized_only = consumer.callFunction(entitySetNameCacheName + "_put")
//            .pByteArray("keyBinary", serializedObject)
//            .pByteArray("valueBinary", serializedObject)
//            .execute();
//
//      // returnType of get function is now EdmSimpleType.BINARY
//      Enumerable results_get_default = consumer.callFunction(entitySetNameCacheName + "_get")
//            .pByteArray("keyBinary", serializedObject)
//            .execute();
//
//      // PERF workaround
//      // NOW - returns ODataClientResponse.... return Enumerable of something extending Object
//      // try to retype return to ODataClientResponse
////      for(ODataClientResponse o : results_get_default) {
//      for (Object o : results_get_default) {
//
//         System.out.println("\n\n\n EXPEEEEEEEEEEEEEEEEERIMENTS ");
//         System.out.println(o);
//         JerseyClientResponse jcr = (JerseyClientResponse) o;
//         System.out.println(jcr.toString());
//         System.out.println(jcr.getHeaders().toString());
//         System.out.println(jcr.getClientResponse().getType());
//         ClientResponse clientResponse = jcr.getClientResponse();
//         String textEntity = clientResponse.getEntity(String.class);
//         System.out.println(textEntity);
//         System.out.println("Try to get byte[] class, but it's hard from text/plain of course.");
//         byte[] byteEntity = clientResponse.getEntity(byte[].class);
//         System.out.println(byteEntity);
//         System.out.println(byteEntity.toString());
//
//         // textEntity is JSON
//
////         dumpResponseBody(textEntity, clientResponse.getType());
//      }

//      for(OObject o : results_get_default) {
//
//         System.out.println("\n\n\n");
//         System.out.println("Some results here of type: " + o.getType());
//         System.out.println(o.toString());
//         String encodedSerializedString = o.toString();
//         try {
//
//            OSimpleObject simpleObject = (OSimpleObject) o;
//            byte[] valueBytes = (byte[]) simpleObject.getValue();
//
//            System.out.println("serialized in byte[]: " + valueBytes);
//            System.out.println("deserialized: " + Utils.deserialize(valueBytes).toString() + " of class: " +
//                                     Utils.deserialize(valueBytes).getClass());
//         } catch (Exception e) {
//            e.printStackTrace();
//         }
//         System.out.println();
//      }


        // mySpecialNamedCache - SIMPLE BASED

        entitySetNameCacheName = "mySpecialNamedCache";


        // working with cache entry simple class (String, String)
//      OEntity createdEntity2 = consumer.createEntity(entitySetNameCacheName).
//            properties(OProperties.string("simpleStringKey", "key7777simple" + appendix)).
//            properties(OProperties.string("simpleStringValue", "value7777simple" + appendix)).execute();

        String simpleKey = "simpleKey1" + appendix;
        String simpleValue = "simpleValue1" + appendix;

        // ispn_put is defined (in addFunctions) to have NO return type so results are null here
        Enumerable<OObject> results_put_empty2 = consumer.callFunction(entitySetNameCacheName + "_putString")
//            .bind(entitySetNameCacheName)
                // Note: when there is no definition of parameter, parameter is simply null
                .pString("keyString", simpleKey)
                .pString("valueString", simpleValue)
                .execute();

        Enumerable<OObject> results_get = consumer.callFunction(entitySetNameCacheName + "_getString")
//            .bind("mySpecialNamedCache")
                .pString("keyString", "simpleKey1" + appendix)
                .execute();

        // old with parsing client
//      for (OObject o : results_get) {
//         System.out.println("\n\n\n");
//         System.out.println("Some results here of type: " + o.getType());
//         System.out.println(o.toString());
//         System.out.println();
//      }

        for (Object o : results_get) {

            System.out.println("\n\n\n EXPEEEEEEEEEEEEEEEEERIMENTS ");
            System.out.println(o);
            JerseyClientResponse jcr = (JerseyClientResponse) o;
            System.out.println(jcr.toString());
            System.out.println(jcr.getHeaders().toString());
            System.out.println(jcr.getClientResponse().getType());
            ClientResponse clientResponse = jcr.getClientResponse();
            String textEntity = clientResponse.getEntity(String.class);
            System.out.println(textEntity);
            // textEntity is JSON

//         dumpResponseBody(textEntity, clientResponse.getType());
        }


//      String key = "simpleKey1";
//      // does not work properly currently, predicate does not match
//      String value = (String) consumer.getEntity(entitySetNameCacheName, simpleKey).execute().getProperty("valueSimpleString").getValue();
//      System.out.println("\n\n\n\n");
//      System.out.println(" SIMPLE GET ENTITY FROM CONSUMER ");
//      System.out.println("get on " + entitySetNameCacheName + " key " + key + " value: " + value);


//      ODataCache<String, String> mySpecialNamedCache = new ODataCache<String, String>(consumer, "mySpecialNamedCache");
//      mySpecialNamedCache.put();


        // URI here is only for caption
//      System.out.println("REPORT WHOLE ENTITY SET (defaultCache)");
//      reportEntities("******** " + endpointUri.concat("defaultCache"),
//                     consumer.getEntities("defaultCache").execute());
//
//      System.out.println("REPORT WHOLE ENTITY SET (mySpecialNamedCache)");
//      reportEntities("******** " + endpointUri.concat("mySpecialNamedCache"),
//                     consumer.getEntities("mySpecialNamedCache").execute());


        // <editor-fold name=Benchmark>
//      *******************************************************
//      ******************* BENCHMARK STUFF *******************
//      *******************************************************
//


        //****************** COMPLEX CACHE PUT - GET 1:1 **********************
        // Object -> serialize -> encode into String -> put -> get -> decode from String to byte[] -> deserialize


        // TODO: prepare serialized object into MAP, mapped to number
        // and in benchmark don't serialize, but only set requests and get them back

        boolean runBenchmark = true;

        if (runBenchmark) {


            int opsCount = 100;

//      long startSer = System.currentTimeMillis();
//
//      HashMap<Integer, byte[]> objects = new HashMap<Integer, byte[]>();
//      for (int i = 0; i < opsCount; i++) {
//
//         objectForTransfer = new CacheObjectSerializationAble("complexKeyBenchABCDEFGHIJKLMNOPQRSTUVWXYZ_" + i,
//                                                              "complexValueBenchABCDEFGHIJKLMNOPQRSTUVWXYZ_" + i);
//         serializedObject = Utils.serialize(objectForTransfer);
//
//         objects.put(i, serializedObject);
//      }
//      long stopSer = System.currentTimeMillis();
//      System.out.println("SERIALIZATION of " + opsCount + " objects: start:" + startSer + " stop:" + stopSer +
//                               " serialization duration (diff):" + (stopSer - startSer));
//
//      try {
//         Thread.sleep(3000);
//      } catch (InterruptedException e) {
//         e.printStackTrace();  // TODO: Customise this generated block
//      }


            System.out.println("Starting benchmark now. OpsCount: " + opsCount);

            entitySetNameCacheName = "mySpecialNamedCache";
            Enumerable<OObject> results_get_bench = null;
            long start = System.currentTimeMillis();

            System.out.println("Dump memory before benchmark...");
            long totalMemBefore = Runtime.getRuntime().totalMemory();
            long maxMemBefore = Runtime.getRuntime().maxMemory();
            long freeMemBefore = Runtime.getRuntime().freeMemory();
            System.out.println("Memory total: " + totalMemBefore);
            System.out.println("Memory max: " + maxMemBefore);
            System.out.println("Memory free: " + freeMemBefore);

            entitySetNameCacheName = "defaultCache";


//      for (int i = 0; i < opsCount; i++) {
//
////         objectForTransfer = new CacheObjectSerializationAble("complexKeyBenchABCDEFGHIJKLMNOPQRSTUVWXYZ_" + i,
////                                                              "complexValueBenchABCDEFGHIJKLMNOPQRSTUVWXYZ_" + i);
////         serializedObject = Utils.serialize(objectForTransfer);
////         System.out.println("serialized object into byte[]: " + serializedObject);
//
//         consumer.callFunction(entitySetNameCacheName + "_put")
//               .pByteArray("keyBinary", objects.get(i))
//               .pByteArray("valueBinary", objects.get(i))
//               .execute();
//
//         Enumerable<OObject> results = consumer.callFunction(entitySetNameCacheName + "_get")
//               .pByteArray("keyBinary", serializedObject)
//               .execute();
//
//         for (OObject o : results) {
//            System.out.println(o.toString());
//            try {
//               OSimpleObject simpleObject = (OSimpleObject) o;
//               byte[] valueBytes = (byte[]) simpleObject.getValue();
//               System.out.println(valueBytes);
//
////               System.out.println("serialized in byte[]: " + valueBytes);
////               System.out.println("deserialized: " + Utils.deserialize(valueBytes).toString() + " of class: " +
////                                        Utils.deserialize(valueBytes).getClass());
//
//            } catch (Exception e) {
//               e.printStackTrace();
//            }
//            System.out.println();
//         }
//      }


            //****************** SIMPLE CACHE PUT - GET 1:1 **********************
            //****************** SIMPLE CACHE PUT - GET 1:1 **********************
            //****************** SIMPLE CACHE PUT - GET 1:1 **********************

            long putsTime = 0;
            long getsTime = 0;
            for (int i = 0; i < opsCount; i++) {

                long start_put = System.currentTimeMillis();
                consumer.callFunction(entitySetNameCacheName + "_putString")
                        .pString("keyString", "simpleKeyBenchABCDEFGHIJKLMNOPQRSTUVWXYZ_simpleKeyBenchABCDEFGHIJKLMNOPQRSTUVWXYZ_" + i)
                        .pString("valueString", "simpleValueBenchABCDEFGHIJKLMNOPQRSTUVWXYZ_simpleKeyBenchABCDEFGHIJKLMNOPQRSTUVWXYZ_" + i)
                        .execute();
                long stop_put = System.currentTimeMillis();
                putsTime = putsTime + (stop_put - start_put);
                System.out.println("TIME Results: start:" + start_put + " stop:" + stop_put + " test duration (diff):" + (stop_put - start_put));

                long start_get = System.currentTimeMillis();
                results_get_bench = consumer.callFunction(entitySetNameCacheName + "_getString")
                        .pString("keyString", "simpleKeyBenchABCDEFGHIJKLMNOPQRSTUVWXYZ_simpleKeyBenchABCDEFGHIJKLMNOPQRSTUVWXYZ_" + i)
                        .execute();
                long stop_get = System.currentTimeMillis();
                getsTime = getsTime + (stop_get - start_get);
                System.out.println("TIME Results: start:" + start_get + " stop:" + stop_get + " test duration (diff):" + (stop_get - start_get));

//         for(OObject o : results_get_bench) {
//            System.out.println(o.toString());
//         }


                // THIS IS PERFORMANCE EXHAUSTED!!!
                for (Object o : results_get_bench) {
                    JerseyClientResponse jcr = (JerseyClientResponse) o;
                    System.out.println(jcr.toString());
                    System.out.println(jcr.getHeaders().toString());
                    System.out.println(jcr.getClientResponse().getType());
                    ClientResponse clientResponse = jcr.getClientResponse();

                    long st = System.currentTimeMillis();

                    // PERFORMANCE BOTTLENECK!!! 40 milliseconds
//                  String textEntity = clientResponse.getEntity(String.class);

                    InputStream responseEntityInputStream = clientResponse.getEntityInputStream(); // TODO: one line

//                        InputStream in = responseEntityInputStream;
//                        InputStreamReader isr = new InputStreamReader(in);
//                        StringBuilder sb = new StringBuilder();
//                        BufferedReader br = new BufferedReader(isr);
//                        String read = br.readLine();
//
//                        while (read != null) {
//                            //System.out.println(read);
//                            sb.append(read);
//                            read = br.readLine();
//                        }
//
//                        System.out.println("\n\nREAD INPUTSTREAM and watch time after it: " + sb.toString());

                    // need proper coding
                    String inputStreamString = new Scanner(responseEntityInputStream,"UTF-8").useDelimiter("\\A").next();
                    System.out.println("InputStreamString using scanner: " + inputStreamString);

                    long sp = System.currentTimeMillis();
                    System.out.println("String textEntity = clientResponse.getEntityInputStream(); duration (diff) millis:" + (sp - st));

//               System.out.println(textEntity);

//               byte[] byteArrayEntity = clientResponse.getEntity(byte[].class);
//               System.out.println(byteArrayEntity.getClass());
//               System.out.println(byteArrayEntity);

                }


                System.out.println("Dump time: " + System.currentTimeMillis());
            }

            System.out.println("\n\n");
            System.out.println("SUMMARY TIME for puts: " + putsTime);
            System.out.println("SUMMARY TIME for gets: " + getsTime);


            long stop = System.currentTimeMillis();

            System.out.println("Dump memory after benchmark...");
            long totalMemAfter = Runtime.getRuntime().totalMemory();
            long maxMemAfter = Runtime.getRuntime().maxMemory();
            long freeMemAfter = Runtime.getRuntime().freeMemory();
            System.out.println("Memory total: " + totalMemAfter);
            System.out.println("Memory max: " + maxMemAfter);
            System.out.println("Memory free: " + freeMemAfter);

            System.out.println("DIFF Memory total: " + (totalMemAfter - totalMemBefore));
            System.out.println("DIFF Memory max: " + (maxMemAfter - maxMemBefore));
            System.out.println("DIFF Memory free (AFTER - BEFORE) = : " + (freeMemAfter - freeMemBefore));
            System.out.println("\n\n");

            System.out.println("TIME Results: start:" + start + " stop:" + stop + " test duration (diff):" + (stop - start));
            double opsPerSec = new Double(opsCount / (new Double(stop - start) / 1000));
            System.out.println("OpsCount: " + opsCount + ", Operations per second: " + opsPerSec);
            System.out.println();

            System.out.println("\n\n\n\n\n\n\n");
        }


//      OutputStreamWriter os = null;
//      try {
//         os = new OutputStreamWriter(new FileOutputStream(new File("fileForWrite.txt")));
//
//         os.write("hello");
//         os.close();
//
//      } catch (FileNotFoundException e) {
//         e.printStackTrace();  // TODO: Customise this generated block
//      } catch (IOException e) {
//         e.printStackTrace();  // TODO: Customise this generated block
//      }


        // WHAT TO DO WITH JSON?
//      System.out.println("USER DIR!");
//      System.out.println(System.getProperty("user.dir"));
//
//
//      // in = textEntity
////      InputStream in = clientResponse.getEntityInputStream();
//      InputStream in = null;
//      try {
//         in = new FileInputStream("examplejson.txt");
//      } catch (FileNotFoundException e) {
//         e.printStackTrace();  // TODO: Customise this generated block
//      }
//
//      BufferedReader streamReader = null;
//      try {
//         streamReader = new BufferedReader(new InputStreamReader(in, "UTF-8"));
//      } catch (UnsupportedEncodingException e) {
//         e.printStackTrace();
//      }
//      StringBuilder responseStrBuilder = new StringBuilder();
//
//      String inputStr;
//      try {
//         while ((inputStr = streamReader.readLine()) != null)
//            responseStrBuilder.append(inputStr);
//      } catch (IOException e) {
//         e.printStackTrace();
//      }
//
//      try {
//         JSONObject o = new JSONObject(responseStrBuilder.toString());
//         System.out.println("\n\n\n JSON OBJECT -- constructed from file !!!!!!!!!! ");
//         System.out.println(o);
//         System.out.println(o.sortedKeys().toString());
//      } catch (JSONException e) {
//         System.out.println("EXCEPTION");
//         e.printStackTrace();
//      }


        // </editor-fold>

    }
}

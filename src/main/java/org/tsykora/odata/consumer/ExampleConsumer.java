package org.tsykora.odata.consumer;

import org.core4j.Enumerable;
import org.odata4j.consumer.ODataConsumer;
import org.odata4j.core.OObject;
import org.tsykora.odata.common.CacheObjectSerializationAble;
import org.tsykora.odata.common.Utils;
import org.tsykora.odata.producer.AbstractExample;
import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

import java.io.IOException;

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

      if(args != null) {
         endpointUri = endpointUri2;
         appendix = "valueToEndpoint9887";
      }

      // ********** HINT **********
      // To dump all the HTTP trafic
      // Sends http request and/or response information to standard out.  Useful for debugging.
      // TODO: enable it for producer as well? Is it even possible?
      ODataConsumer.dump.all(true);

      // CONSUME IT
      // format null - ATOM by default?, method to tunnel null (maybe needs change in the future)
      System.out.println("Creating instance of ExampleConsumer, initializing oDataConsumer...");
      ODataConsumer consumer = this.rtFacde.create(endpointUri, null, null);
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


      BASE64Encoder encoder = new BASE64Encoder();
      BASE64Decoder decoder = new BASE64Decoder();
      CacheObjectSerializationAble objectForTransfer = new CacheObjectSerializationAble("keyxx1" + appendix, "valuexx1" + appendix);
      byte[] serializedObject = Utils.serialize(objectForTransfer);
      System.out.println("serialized object into byte[]: " + serializedObject);
      String encodedString = encoder.encode(serializedObject);
      System.out.println("encodedObject for transfer: " + encodedString);

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
//
      Enumerable<OObject> results_put_empty = consumer.callFunction(entitySetNameCacheName + "_put")
//            .bind(entitySetNameCacheName) // we are not binding this -- need to be null to pass condition for finding function
                  // Note: when there is no definition of parameter, parameter is simply null
            .pString("keyEncodedSerializedObject", encodedString)
            .pString("valueEncodedSerializedObject", encodedString)
            .execute();

      Enumerable<OObject> results_get_default = consumer.callFunction(entitySetNameCacheName + "_get")
//            .bind(entitySetNameCacheName) // we are not binding this -- need to be null to pass condition for finding function
            .pString("keyEncodedSerializedObject", encodedString)
            .execute();

      for(OObject o : results_get_default) {
         System.out.println("\n\n\n");
         System.out.println("Some results here of type: " + o.getType());
         System.out.println(o.toString());
         String encodedSerializedString = o.toString();
         try {
            byte[] serialized = decoder.decodeBuffer(encodedSerializedString);
            System.out.println("decoded: " + serialized);
            System.out.println("deserialize: " + Utils.deserialize(serialized).toString() + " of class: " + Utils.deserialize(serialized).getClass());
         } catch (IOException e) {
            e.printStackTrace();
         }
         System.out.println();
      }


      // mySpecialNamedCache - SIMPLE BASED

      entitySetNameCacheName = "mySpecialNamedCache";

      // working with cache entry simple class (String, String)
//      OEntity createdEntity2 = consumer.createEntity(entitySetNameCacheName).
//            properties(OProperties.string("simpleStringKey", "key7777simple" + appendix)).
//            properties(OProperties.string("simpleStringValue", "value7777simple" + appendix)).execute();

      String simpleKey = "simpleKey1" + appendix;
      String simpleValue = "simpleValue1" + appendix;

      // ispn_put is defined (in addFunctions) to have NO return type so results are null here
      Enumerable<OObject> results_put_empty2 = consumer.callFunction(entitySetNameCacheName + "_put")
//            .bind(entitySetNameCacheName)
            // Note: when there is no definition of parameter, parameter is simply null
            .pString("keySimpleString", simpleKey)
            .pString("valueSimpleString", simpleValue)
            .execute();


      // ispn_get is defined (in addFunctions) to have return type EdmSimpleType.STRING so results should be here
      // TODO: It would be ideal to return serialized decoded string which I can encode and deserialize then


      Enumerable<OObject> results_get = consumer.callFunction(entitySetNameCacheName + "_get")
//            .bind("mySpecialNamedCache")
            .pString("keySimpleString", "simpleKey1" + appendix)
            .execute();


      for (OObject o : results_get) {
         System.out.println("\n\n\n");
         System.out.println("Some results here of type: " + o.getType());
         System.out.println(o.toString());
         System.out.println();
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
//      int opsCount = 100;
//      System.out.println("Starting benchmark now. OpsCount: " + opsCount);
//
//      entitySetNameCacheName = "mySpecialNamedCache";
//      Enumerable<OObject> results_get_bench = null;
//      long start = System.currentTimeMillis();
//
//      System.out.println("Dump memory before benchmark...");
//      long totalMemBefore = Runtime.getRuntime().totalMemory();
//      long maxMemBefore = Runtime.getRuntime().maxMemory();
//      long freeMemBefore = Runtime.getRuntime().freeMemory();
//      System.out.println("Memory total: " + totalMemBefore);
//      System.out.println("Memory max: " + maxMemBefore);
//      System.out.println("Memory free: " + freeMemBefore);
//
//      StringBuffer sb = new StringBuffer();

      //****************** SIMPLE CACHE PUT - GET 1:1 **********************
//      for (int i = 0; i < opsCount; i++) {
//         consumer.callFunction(entitySetNameCacheName + "_put")
//               .pString("keySimpleString", "simpleKeyBenchABCDEFGHIJKLMNOPQRSTUVWXYZ_" + i)
////               .pString("keySimpleString", "simpleKey" + i)
//               .pString("valueSimpleString", "simpleValueBenchABCDEFGHIJKLMNOPQRSTUVWXYZ_" + i)
////               .pString("valueSimpleString", "simpleValue" + i)
//               .execute();
//         results_get_bench = consumer.callFunction(entitySetNameCacheName + "_get")
//               .pString("keySimpleString", "simpleKeyBenchABCDEFGHIJKLMNOPQRSTUVWXYZ_" + i)
////               .pString("keySimpleString", "simpleKey" + i)
//               .execute();
//         for(OObject o : results_get_bench) {
//            System.out.println(o.toString());
//         }
//         System.out.println("Dump time: " + System.currentTimeMillis());
//      }




      //****************** COMPLEX CACHE PUT - GET 1:1 **********************
      // Object -> serialize -> encode into String -> put -> get -> decode from String to byte[] -> deserialize
//      entitySetNameCacheName = "defaultCache";
//      for (int i = 0; i < opsCount; i++) {
//
//         objectForTransfer = new CacheObjectSerializationAble("complexKeyBenchABCDEFGHIJKLMNOPQRSTUVWXYZ_" + i,
//                                                              "complexValueBenchABCDEFGHIJKLMNOPQRSTUVWXYZ_" + i);
//         serializedObject = Utils.serialize(objectForTransfer);
//         System.out.println("serialized object into byte[]: " + serializedObject);
//         encodedString = encoder.encode(serializedObject);
//         System.out.println("encodedObject for transfer: " + encodedString);
//
//         consumer.callFunction(entitySetNameCacheName + "_put")
//               .pString("keyEncodedSerializedObject", encodedString)
//               .pString("valueEncodedSerializedObject", encodedString)
//               .execute();
//
//         Enumerable<OObject> results_get_default = consumer.callFunction(entitySetNameCacheName + "_get")
//               .pString("keyEncodedSerializedObject", encodedString)
//               .execute();
//
//         for (OObject o : results_get_default) {
//            System.out.println(o.toString());
//            String encodedSerializedString = o.toString();
//            try {
//               byte[] serialized = decoder.decodeBuffer(encodedSerializedString);
//               System.out.println("decoded: " + serialized);
//               System.out.println("deserialize: " + Utils.deserialize(serialized).toString());
//            } catch (IOException e) {
//               e.printStackTrace();
//            }
//            System.out.println();
//         }
//      }
//
//
//      long stop = System.currentTimeMillis();
//
//      System.out.println("Dump memory after benchmark...");
//      long totalMemAfter = Runtime.getRuntime().totalMemory();
//      long maxMemAfter = Runtime.getRuntime().maxMemory();
//      long freeMemAfter = Runtime.getRuntime().freeMemory();
//      System.out.println("Memory total: " + totalMemAfter);
//      System.out.println("Memory max: " + maxMemAfter);
//      System.out.println("Memory free: " + freeMemAfter);
//
//      System.out.println("DIFF Memory total: " + (totalMemAfter - totalMemBefore));
//      System.out.println("DIFF Memory max: " + (maxMemAfter - maxMemBefore));
//      System.out.println("DIFF Memory free (AFTER - BEFORE) = : " + (freeMemAfter - freeMemBefore));
//      System.out.println("\n\n");
//
//      System.out.println("TIME Results: start:" + start + " stop:" + stop + " test duration (diff):" + (stop - start));
//      double opsPerSec = new Double(opsCount / ( new Double(stop - start) / 1000));
//      System.out.println("OpsCount: " + opsCount + ", Operations per second: " + opsPerSec);
//      System.out.println();
//
//      System.out.println("\n\n\n\n\n\n\n");

      // </editor-fold>

   }
}

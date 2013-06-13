package org.tsykora.odata.consumer;

import org.core4j.Enumerable;
import org.odata4j.consumer.ODataConsumer;
import org.odata4j.core.OEntity;
import org.odata4j.core.OObject;
import org.odata4j.core.OProperties;
import org.tsykora.odata.common.CacheObjectSerializationAble;
import org.tsykora.odata.common.Utils;
import org.tsykora.odata.producer.AbstractExample;
import sun.misc.BASE64Encoder;

/**
 * @author tsykora
 */
public class ExampleConsumer extends AbstractExample {

   public static String endpointUri = "http://localhost:8887/ODataInfinispanEndpoint.svc/";

   public static void main(String[] args) {
      ExampleConsumer example = new ExampleConsumer();
      example.run(args);
   }

   public void run(String[] args) {

      // ********** HINT **********
      // To dump all the HTTP trafic
      // Sends http request and/or response information to standard out.  Useful for debugging.
      // TODO: enable it for producer as well? Is it even possible?
      ODataConsumer.dump.all(true);

      // CONSUME IT
      // format null, method to tunnel null (maybe needs change in the future)
      System.out.println("Creating instance of ExampleConsumer, initializing oDataConsumer...");
      ODataConsumer consumer = this.rtFacde.create(endpointUri, null, null);

      System.out.println("\n\n\n\n");
      reportMetadata(consumer.getMetadata());
      System.out.println("\n\n\n\n");

      // http://datajs.codeplex.com/discussions/391490  ??
      // NOTE/SKILL: to call it from here Producer need to implement findExtension (it can return null)
      // + it needs some successful response for ConsumerCreateEntityRequest


      // default cache is complex, not simple cache entry (String, String)

      // TODO !!!!!!!!!! FIX THIS
      // do decision in getEntity for this case + is it needed? think...
      // automatically serialize response -- but what if I call simple getEntity from browser on simple cache based entity?
      reportEntity(" new cache entry report: ", consumer.createEntity("defaultCache").
            properties(OProperties.binary("Key", Utils.serialize("key7"))).
            properties(OProperties.binary("Value", Utils.serialize("value7"))).execute());

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


      BASE64Encoder encoder = new BASE64Encoder();
      CacheObjectSerializationAble objectForTransfer = new CacheObjectSerializationAble("keyxx1", "valuexx1");
      byte[] serializedObject = Utils.serialize(objectForTransfer);
      System.out.println("serialized object into byte[]: " + serializedObject);
      String encodedString = encoder.encode(serializedObject);
      System.out.println("encodedObject for transfer: " + encodedString);

      String entitySetNameCacheName = "defaultCache";

      Enumerable<OObject> results_put_empty = consumer.callFunction(entitySetNameCacheName + "_put")
            .bind(entitySetNameCacheName)
                  // Note: when there is no definition of parameter, parameter is simply null
            .pString("keyEncodedSerializedObject", encodedString)
            .pString("valueEncodedSerializedObject", encodedString)
            .execute();





      // mySpecialNamedCache - SIMPLE BASED

      entitySetNameCacheName = "mySpecialNamedCache";

      // working with cache entry simple class (String, String)
      OEntity createdEntity = consumer.createEntity("mySpecialNamedCache").
            properties(OProperties.string("simpleStringKey", "key77simple")).
            properties(OProperties.string("simpleStringValue", "value77simple")).execute();

      String simpleKey = "simpleKey1";
      String simpleValue = "simpleValue1";

      // ispn_put is defined (in addFunctions) to have NO return type so results are null here
      Enumerable<OObject> results_put_empty2 = consumer.callFunction(entitySetNameCacheName + "_put")
            .bind(entitySetNameCacheName)
            // Note: when there is no definition of parameter, parameter is simply null
            .pString("keySimpleString", simpleKey)
            .pString("valueSimpleString", simpleValue)
            .execute();



      // ispn_get is defined (in addFunctions) to have return type EdmSimpleType.STRING so results should be here
      // TODO: Q: do I have string results here? or can I get entity?
      // TODO: It would be ideal to return serialized decoded string which I can encode and deserialize then

      try {
         Thread.sleep(2000);
      } catch (InterruptedException e) {
         e.printStackTrace();  // TODO: Customise this generated block
      }

      Enumerable<OObject> results_get = consumer.callFunction(entitySetNameCacheName + "_get")
            .bind("mySpecialNamedCache")
            .pString("keySimpleString", "simpleKey1")
//            .pString("valueSimpleString", simpleValue)
            .execute();

      for(OObject o : results_get) {
         System.out.println("\n\n\n\n");

         System.out.println("Some results here: ");
         System.out.println(o.toString());
         System.out.println("result type: " + o.getType());

      }


//      ODataCache<String, String> mySpecialNamedCache = new ODataCache<String, String>(consumer, "mySpecialNamedCache");
//      mySpecialNamedCache.put();



      // URI here is only for caption
      System.out.println("REPORT WHOLE ENTITY SET (defaultCache)");
      reportEntities("******** " + endpointUri.concat("defaultCache"),
                     consumer.getEntities("defaultCache").execute());

      System.out.println("REPORT WHOLE ENTITY SET (mySpecialNamedCache)");
      reportEntities("******** " + endpointUri.concat("mySpecialNamedCache"),
                     consumer.getEntities("mySpecialNamedCache").execute());
   }
}

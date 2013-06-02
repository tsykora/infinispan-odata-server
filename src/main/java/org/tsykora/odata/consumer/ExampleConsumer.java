package org.tsykora.odata.consumer;

import org.core4j.Enumerable;
import org.odata4j.consumer.ODataConsumer;
import org.odata4j.core.OEntity;
import org.odata4j.core.OObject;
import org.odata4j.core.OProperties;
import org.odata4j.edm.EdmEntityContainer;
import org.odata4j.edm.EdmFunctionImport;
import org.odata4j.edm.EdmSchema;
import org.tsykora.odata.common.Utils;
import org.tsykora.odata.producer.AbstractExample;

import java.util.ArrayList;
import java.util.List;

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
      ODataConsumer.dump.all(true);
      // TODO: enable it for producer as well? Is it even possible?


      // CONSUME IT
      // format is null, method to tunnel is null (ok?)
      System.out.println("Creating instance of ExampleConsumer, initializing oDataConsumer...");
      ODataConsumer consumer = this.rtFacde.create(endpointUri, null, null);


      System.out.println("Some simple debug outputs...");

//        try {
//            OEntity cacheKey1 = consumer.getEntity("CacheEntries", "key8").execute();
//            reportEntity("\n\nThis is key8 entity report (from ExampleConsumer): ", cacheKey1);
//
//        } catch (Exception e) {
//            System.err.println("Error while issuing getEntity for key (entry) which is not exist in cache yet." + e.getMessage());
//        }


      // TODO: some handler which will translate it into CacheEntry
      // Or I already have InternalCacheEntry... what to do with this?


//      cacheKey1.getProperty("Key").getValue();


      System.out.println("\n\n************** issuing create entity.........\n\n ");

      // http://datajs.codeplex.com/discussions/391490  ??

      // TODO: it returns key1 value1 -- this what I was consumer.getEntity ^ above (why?)
      // creates new entity in given set

      // NOTE/SKILL: to call it from here Producer need to implement findExtension (it can return null)
      // + it needs some successful response for ConsumerCreateEntityRequest


      // TODO!!
      // create full OEntity here -- later this will make handler possible - get only user get for cache and everything handles
      // entitytype a entity key is missing!

      // need client? need metadata! Do I need it? What else do I need?

      // respose.getType() is null
      // and key is null


      // CREATE ENTRY FROM CONSUMER !!!!!!!!! //
      // CREATE ENTRY FROM CONSUMER !!!!!!!!! //
      // CREATE ENTRY FROM CONSUMER !!!!!!!!! //

//      ODataClientRequest r = new ODataClientRequest(endpointUri, endpointUri, null, null, args);
//      ConsumerCreateEntityRequest ccer = new ConsumerCreateEntityRequest(null, endpointUri, EdmDataServices.EMPTY, endpointUri, null)


      reportEntity(" new cache entry report: ", consumer.createEntity("defaultCache").
            properties(OProperties.binary("Key", Utils.serialize("key7"))).
            properties(OProperties.binary("Value", Utils.serialize("value7"))).execute());


      // TODO - FIX THIS
      // this is for only one REGISTERED entry - this does not reflex cache content!! yet!!
//        Integer count = consumer.getEntitiesCount("defaultCache").execute();
//        System.out.println("\n\n\nCount of entries is defaultCache set is: " + count);


      System.out.println("\n\n\n **** reporting metadata reportMetadata(consumer.getMetadata): ***** ");
//        reportMetadata(consumer.getMetadata());

//       System.out.println("METADATA SCHEMAS SIZE: " + consumer.getMetadata().getSchemas().size());

      System.out.println("\n\n\n **** **************************** ***** \n\n ");

//        OEntity onlyGetEntity = consumer.createEntity("defaultCache").
//                properties(OProperties.string("Key", "key10")).properties(OProperties.string("Value", "value10")).get();
//        System.out.println("Entity key here should be null (is not defined yet): " + onlyGetEntity.getEntityKey());
//        


//       newProduct = c.getEntity("Products", 10).execute();
      OEntity createdEntity = consumer.createEntity("mySpecialNamedCache").
            properties(OProperties.binary("Key", Utils.serialize("key77"))).
            properties(OProperties.binary("Value", Utils.serialize("value77"))).execute();


//       Map<String, EdmFunctionImport> bindableFunctions = new HashMap<String, EdmFunctionImport>();
//       for(EdmFunctionImport efi : consumer.getMetadata().getFunctions(EdmFunctionImport.FunctionKind.Function)) {
//         bindableFunctions.put("MyFunction", efi);
//       }


      //TODO? how is it possible that I don't have a EntityContainer defined in the schema?
      List<EdmFunctionImport> functionList = new ArrayList<EdmFunctionImport>();
      for (EdmSchema schema : consumer.getMetadata().getSchemas()) {
         for (EdmEntityContainer eec : schema.getEntityContainers()) {
            for (EdmFunctionImport efi : eec.getFunctionImports()) {
               System.out.println("////////////////////////////////////////////");
               System.out.println("Function kind of function " + efi.getName() + " is: ");
               System.out.println(efi.getFunctionKind());
               if (efi.getFunctionKind() == EdmFunctionImport.FunctionKind.Function ||
                     efi.getFunctionKind() == EdmFunctionImport.FunctionKind.Action ||
                     efi.getFunctionKind() == EdmFunctionImport.FunctionKind.ServiceOperation ) {
                  functionList.add(efi);
               }
            }
         }
      }

//       Map<String, EdmFunctionImport> bindableFunctions = new HashMap<String, EdmFunctionImport>();
//       for(EdmFunctionImport efi : functionList) {
//          bindableFunctions.put(efi.getName(), efi);
//       }
//       OEntity bindableEntity = OBindableEntities.createBindableEntity(createdEntity, bindableFunctions);
//       OBindableEntity bindableEntity2 = bindableEntity.findExtension(OBindableEntity.class);
//       EdmFunctionImport myFunction = bindableEntity2.getBindableFunctions().get("MyFunction");

      System.out.println("\n\n\n /////*****************///////// ");
      System.out.println("Still in consumer --- trying to call function without any getting");
      // call function here

      // looks like to be only for bindable functions (== Function not service operation)
//       consumer.callFunction(myFunction.getName()).bind("mySpecialNamedCache").pString("cacheEntryKey", "key77").execute();

      // if its service operation I have no myFunction here, it is not bindable at all
      // for caches I will probably need to bind it to particular EntrySet(name) which is cacheName

      // calling function MyFunction on particular entity in entitySet
      // TODO: how to deal ok with parameters, why responses are not ok?
      Enumerable<OObject> result = consumer.callFunction(functionList.get(0).getName())
//            .bind("mySpecialNamedCache", OEntityKey.create("key77"))
            .bind("mySpecialNamedCache")
//            .parameter("cacheName", (OObject) createdEntity.getType()) // take type from created oentity returned from create

//             .pString("cacheName", "mySpecialNamedCache")
             .pString("cacheOperation", "PUT")
             .pString("cacheEntryKey", "key77")
             .pString("cacheEntryValue", "value77")
            .execute();

      if (result != null && result.first() != null) {
         System.out.println("\n\n\n .................... CALL FUNCTION FROM CONSUMER RESULT: \n\n\n");
         System.out.println("size of results: " + result.toList().size());
         System.out.println("FIRST type: " + result.first().getType());
         System.out.println("FIRST toString: " + result.first().toString());
         System.out.println("\n\n\n ^^^ ............ ^^^ CALL FUNCTION FROM CONSUMER RESULT: \n\n\n");
      }


//        ODataCache<Object, Integer> defaultCache = new ODataCache<Object, Integer>(consumer, "defaultCache");
//
//        System.out.println("\n\n\n CALLING PUT ON ODataCache ****************** \n");
////        defaultCache.put((Object) "key7",(Object) "value7");
//
//        System.out.println("\n\n\n CALLING GET ON ODataCache ****************** \n");
//        Object value = defaultCache.get("key7");
//        System.out.println("class: " + value.getClass());
//        System.out.println("raw: " + value);
//        System.out.println("toString: " + value.toString());
//
//        Object valueFromPut = defaultCache.put("key11", new Integer(11));
//
//        System.out.println("VFP class: " + valueFromPut.getClass());
//        System.out.println("VFP raw: " + valueFromPut);
//        System.out.println("VFP toString: " + valueFromPut.toString());
//
//
//       // URI here is only for caption
//       System.out.println("\n\n\n *********** REPORT WHOLE ENTITY SET (defaultCache) *********** \n\n\n");
//       reportEntities("******** " + endpointUri.concat("defaultCache"),
//                      consumer.getEntities("defaultCache").execute());


      // ***************************************************************
      // ***************************************************************
      // ***************************************************************

      ODataCache<String, String> mySpecialNamedCache = new ODataCache<String, String>(consumer, "mySpecialNamedCache");

//       mySpecialNamedCache.put(new Integer(1),"specCache_value1");
//       mySpecialNamedCache.put(new Integer(2),"specCache_value2");


      System.out.println("************* GET FROM SPECIAL WHICH WAS PUT VIA SERVICE OPERATION ********** ");
      System.out.println("************* GET FROM SPECIAL WHICH WAS PUT VIA SERVICE OPERATION ********** ");
      System.out.println("************* GET FROM SPECIAL WHICH WAS PUT VIA SERVICE OPERATION ********** ");
      System.out.println(mySpecialNamedCache.get("key77"));


      // URI here is only for caption
      System.out.println("\n\n\n *********** REPORT WHOLE ENTITY SET (mySpecialNamedCache) *********** \n\n\n");
      reportEntities("******** " + endpointUri.concat("mySpecialNamedCache"),
                     consumer.getEntities("mySpecialNamedCache").execute());


//      OEntity cacheKey6 = consumer.getEntity("defaultCacheNew", "key6").execute();
//      reportEntity("This is key1 entity report: ", cacheKey6);


//      consumer.updateEntity(cacheKey1).properties(OProperties.string("Value","Updated")).execute();
//      reportEntity("This is key1 entity report: ", cacheKey1);


      // new cache entry has the same EdmType as other entries
//      final EdmType edmType = cacheKey1.getType();
//      System.out.println(" \n ************** EdmType fullyQualName: " + edmType.getFullyQualifiedTypeName() +
//                               " toString() is: " + edmType.toString());

//      OProperty<String> cacheEntryProperty = new OProperty<String>() {
//         @Override
//         public EdmType getType() {
//            return ;
//         }
//
//         @Override
//         public String getValue() {
//            return "key6";
//         }
//
//         @Override
//         public String getName() {
//            return "Key";
//         }
//      };


      // creates new entity in given set
//      OEntity newCacheEntry = consumer.createEntity("defaultCache")
//            .properties(OProperties.string("Key", "key6"))
//            .properties(OProperties.string("Value", "value6")).execute();
//
//      reportEntity(" new cache entry report: ", newCacheEntry);
//
//      OEntity cacheKey6 = consumer.getEntity("defaultCacheNew", "key6").execute();
//      reportEntity("This is key1 entity report: ", cacheKey6);


//        System.out.println("cacheKey1: " + cacheKey1);
//        System.out.println("");
//        System.out.println("");
//        System.out.println("");
//        // returns OProperty[Key,EdmSimpleType[Edm.String],key1]
//        System.out.println("cacheKey1.getProperty(\"Key\"): " + cacheKey1.getProperty("Key"));
//        System.out.println("cacheKey1.getProperty(\"Key\").getValue: " + cacheKey1.getProperty("Key").getValue());
//        System.out.println("cacheKey1.getProperty(\"Key\").getType: " + cacheKey1.getProperty("Key").getType());
//        System.out.println("cacheKey1.getProperty(\"Key\").getName: " + cacheKey1.getProperty("Key").getName());
//        System.out.println("cacheKey1.getProperty(\"Key\").toString: " + cacheKey1.getProperty("Key").toString());
//        
//        
//        System.out.println("");
//        System.out.println("");
//        System.out.println("");
//        
//        
//        System.out.println("cacheKey1 entitySetName: " + cacheKey1.getEntitySetName());
//        System.out.println("cacheKey1 entityTag: " + cacheKey1.getEntityTag());
//        System.out.println("cacheKey1 toString: " + cacheKey1.toString());
//        System.out.println("cacheKey1 entityType.toString: " + cacheKey1.getEntityType().toString());
//        System.out.println("cacheKey1 entitySet.toString: " + cacheKey1.getEntitySet().toString());


//        System.out.println("List of cache keys (consumed from OData server service):");

//        Set<String> cacheKeysFromODataServer = (Set<String>) oqr.execute();
//        for (String key : cacheKeysFromODataServer) {
//            System.out.println(key);
//        }

   }
}

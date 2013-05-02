package org.tsykora.odata.consumer;

import org.odata4j.consumer.ODataConsumer;
import org.odata4j.core.OEntity;
import org.odata4j.core.OProperties;
import org.tsykora.odata.producer.AbstractExample;

/**
 * @author tsykora
 */

public class ExampleConsumer extends AbstractExample {

   public static String endpointUri = "http://localhost:8887/InMemoryProducerExample.svc/";


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

      OEntity cacheKey1 = consumer.getEntity("CacheEntries", "key1").execute();
      reportEntity("This is key1 entity report (from ExampleConsumer): ", cacheKey1);
      
      
      // TODO: some handler which will translate it into CacheEntry
      // Or I already have InternalCacheEntry... what to do with this?


//      cacheKey1.getProperty("Key").getValue();


      System.out.println("\n\n************** issuing create entity.........\n\n ");

      // http://datajs.codeplex.com/discussions/391490  ??

      // TODO: it returns key1 value1 -- this what I was consumer.getEntity ^ above (why?)
      // creates new entity in given set

      // NOTE/SKILL: to call it from here Producer need to implement findExtension (it can return null)
      // + it needs some successful response for ConsumerCreateEntityRequest
      OEntity newCacheEntry = consumer.createEntity("CacheEntries")
            .properties(OProperties.string("Key", "key6"))
            .properties(OProperties.string("Value", "value6")).execute();

      reportEntity(" new cache entry report: ", newCacheEntry);









//      OEntity cacheKey6 = consumer.getEntity("CacheEntriesNew", "key6").execute();
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
//      OEntity newCacheEntry = consumer.createEntity("CacheEntries")
//            .properties(OProperties.string("Key", "key6"))
//            .properties(OProperties.string("Value", "value6")).execute();
//
//      reportEntity(" new cache entry report: ", newCacheEntry);
//
//      OEntity cacheKey6 = consumer.getEntity("CacheEntriesNew", "key6").execute();
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

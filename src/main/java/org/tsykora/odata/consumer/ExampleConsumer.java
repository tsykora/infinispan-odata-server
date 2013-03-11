package org.tsykora.odata.consumer;

import org.odata4j.consumer.ODataConsumer;
import org.odata4j.core.OEntity;
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

      // CONSUME IT
      // format is null, method to tunnel is null (ok?)
      System.out.println("Creating instance of ExampleConsumer, initializing oDataConsumer...");
      ODataConsumer consumer = this.rtFacde.create(endpointUri, null, null);

      System.out.println("Some simple debug outputs...");

      OEntity cacheKey1 = consumer.getEntity("CacheEntries", "key1").execute();

      reportEntity("This is key1 entity report: ", cacheKey1);

      cacheKey1.getProperty("Key").getValue();


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

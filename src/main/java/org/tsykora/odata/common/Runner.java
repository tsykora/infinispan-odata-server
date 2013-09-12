package org.tsykora.odata.common;

import org.tsykora.odata.consumer.ExampleConsumer;
import org.tsykora.odata.producer.InMemoryProducerExample;

/**
 * @author tsykora
 */
public class Runner {

   public static void main(String[] args) throws InterruptedException {

      // start producer first (Jersey server exposing OData service)
      Thread producerThread8887 = new Thread() {
         public void run() {
            System.out.println("\nStarting producer...\n");
            InMemoryProducerExample producer = new InMemoryProducerExample();
            String[] args = {"http://localhost:8887/ODataInfinispanEndpoint.svc/"};
            producer.run(args);
         }
      };
      Thread producerThread9887 = new Thread() {
         public void run() {
            System.out.println("\nStarting producer...\n");
            InMemoryProducerExample producer = new InMemoryProducerExample();
            String[] args = {"http://localhost:9887/ODataInfinispanEndpoint.svc/"};
            producer.run(args);
         }
      };

      if (args == null || args[0] == null || args[0].equals("8887")) {
         producerThread8887.start();
      } else {
         producerThread9887.start();
      }


      // start consumer in different thread and work with exposed data

      if (args == null || args[0] == null || args[0].equals("8887")) {

         Thread consumerThread = new Thread() {
            public void run() {
               System.out.println("\nStarting consumer...\n");
               ExampleConsumer consumer = new ExampleConsumer();
               consumer.run(null);
            }
         };
         // wait for server starting
         System.out.println("\n\n\nWaiting some seconds before starting consumerThread.............\n\n\n");
         Thread.sleep(5000);
         consumerThread.start();

      } else {
         Thread consumerThread = new Thread() {
            public void run() {
               System.out.println("\nStarting consumer...\n");
               ExampleConsumer consumer = new ExampleConsumer();
               String[] arg = {"notNull9887port"};
               consumer.run(arg);
            }
         };
         // wait for server starting
         System.out.println("\n\n\nWaiting some seconds before starting consumerThread.............\n\n\n");
         Thread.sleep(5000);
         consumerThread.start();

      }



   }

}

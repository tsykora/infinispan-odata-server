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


      // wait until registration
//      Thread.sleep(3000);
//
//      Thread producerThread2 = new Thread() {
//         public void run() {
//            System.out.println("\nStarting producer...\n");
//            InMemoryProducerExample producer = new InMemoryProducerExample();
//            String[] args = {"http://localhost:9887/ODataInfinispanEndpoint2.svc/"};
//            producer.run(args);
//         }
//      };
//      producerThread2.start();
//      producerThread2.sleep(3000);


      // start second server (with replicated/distributed cache)
//      Thread producerThread2 = new Thread() {
//         public void run() {
//            System.out.println("\nStarting producer...\n");
//            InMemoryProducerExample2 producer2 = new InMemoryProducerExample2();
//            String[] args = {"http://localhost:9887/ODataInfinispanEndpoint2.svc/"};
//            producer2.run(args);
//         }
//      };
//      producerThread2.start();


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
         Thread.sleep(6000);
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
         Thread.sleep(6000);
         consumerThread.start();

      }



   }

}

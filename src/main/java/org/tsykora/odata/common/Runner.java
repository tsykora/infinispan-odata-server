package org.tsykora.odata.common;

import org.tsykora.odata.consumer.ExampleConsumer;
import org.tsykora.odata.producer.InMemoryProducerExample;

/**
 * @author tsykora
 */
public class Runner {

   public static void main(String[] args) throws InterruptedException {

      // start producer first (Jersey server exposing OData service)
      Thread producerThread = new Thread() {
         public void run() {
            System.out.println("\nStarting producer...\n");
            InMemoryProducerExample producer = new InMemoryProducerExample();
            producer.run(null);
         }
      };
      producerThread.start();


      // start consumer in different thread and work with exposed data
      Thread consumerThread = new Thread() {
         public void run() {
            System.out.println("\nStarting consumer...\n");
            ExampleConsumer consumer = new ExampleConsumer();
            consumer.run(null);
         }
      };

      // wait for server starting
      System.out.println("\n\n\nWaiting some seconds before starting consumerThread.............\n\n\n");
      Thread.sleep(7000);
      consumerThread.start();

   }

}

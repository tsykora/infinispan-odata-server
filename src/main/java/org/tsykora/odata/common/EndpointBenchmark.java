package org.tsykora.odata.common;

import org.odata4j.consumer.ODataConsumer;
import org.tsykora.odata.producer.AbstractExample;

/**
 * Benchmark OData Infinispan Endpoint -- OData Jersey Server
 *
 * @author tsykora
 */
public class EndpointBenchmark extends AbstractExample {

   public static String endpointUri = "http://localhost:8887/ODataInfinispanEndpoint.svc/";

   public static void main(String[] args) throws InterruptedException {

      System.out.println("Starting benchmark now.");

      // dump traffic?
      ODataConsumer.dump.all(false);

//      ODataConsumer consumer = this.rtFacde.create(endpointUri, null, null);
   }
}

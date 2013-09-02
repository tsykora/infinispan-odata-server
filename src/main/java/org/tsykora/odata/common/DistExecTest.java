//package org.tsykora.odata.common;
//
//import org.infinispan.Cache;
//import org.infinispan.configuration.cache.CacheMode;
//import org.infinispan.configuration.cache.ConfigurationBuilder;
//import org.infinispan.configuration.global.GlobalConfigurationBuilder;
//import org.infinispan.distexec.DefaultExecutorService;
//import org.infinispan.distexec.DistributedCallable;
//import org.infinispan.distexec.DistributedExecutorService;
//import org.infinispan.manager.CacheManager;
//import org.infinispan.manager.DefaultCacheManager;
//import org.infinispan.marshall.jboss.JBossMarshaller;
//
//import java.io.ByteArrayOutputStream;
//import java.io.IOException;
//import java.io.ObjectOutputStream;
//import java.io.Serializable;
//import java.util.LinkedList;
//import java.util.List;
//import java.util.Set;
//import java.util.concurrent.ExecutionException;
//import java.util.concurrent.Future;
//
///**
// * // TODO: Document this
// *
// * @author tsykora
// * @since 4.0
// */
//public class DistExecTest {
//
//   public static void main(String[] arg) throws ExecutionException, InterruptedException, IOException {
//
//      GlobalConfigurationBuilder global = GlobalConfigurationBuilder.defaultClusteredBuilder();
//      global.transport().defaultTransport();
//      global.globalJmxStatistics().enable();
//
//      ConfigurationBuilder configCache = new ConfigurationBuilder();
//      configCache.jmxStatistics().enable();
//      configCache.clustering().cacheMode(CacheMode.DIST_ASYNC);
////      configCache.storeAsBinary().enable();
//
//      // default cache is creating with that config
//      CacheManager manager = new DefaultCacheManager(global.build(), configCache.build());
//      Cache cache = manager.getCache("distExecCache");
//
//      JBossMarshaller marshaller = new JBossMarshaller();
//      for (int i = 0; i < 5; i++) {
//         String longer = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
//         for (int y = 0; y < i; y++) {
//            longer = longer + longer;
//         }
//         cache.put("key" + i, marshaller.objectToByteBuffer("value" + longer + "" + i));
//      }
//
//
////      cache.put("key1", marshaller.objectToByteBuffer("value1"));
//
//      long sumSize = 0;
//      // size of values objects in cache
//      for(Object o : cache.getAdvancedCache().getDataContainer().values()) {
//         ByteArrayOutputStream baos = new ByteArrayOutputStream();
//         ObjectOutputStream oos = new ObjectOutputStream(baos);
//         oos.writeObject(o);
//         oos.close();
//         sumSize += baos.size();
//         System.out.println("value:" + baos.size());
//      }
//
//      // + size of keys objects in cache
//      for(Object o : cache.getAdvancedCache().getDataContainer().keySet()) {
//         ByteArrayOutputStream baos = new ByteArrayOutputStream();
//         ObjectOutputStream oos = new ObjectOutputStream(baos);
//         oos.writeObject(o);
//         oos.close();
//         sumSize += baos.size();
//         System.out.println("key:" + baos.size());
//      }
//      System.out.println("sumSize: " + sumSize);
//
//
//      System.out.println("\n\n OTHER WAY");
//
//
//      ByteArrayOutputStream baos = new ByteArrayOutputStream();
//      ObjectOutputStream oos = new ObjectOutputStream(baos);
//
//      sumSize = 0;
//      // size of values objects in cache
//      for(Object o : cache.getAdvancedCache().getDataContainer().values()) {
//         oos.writeObject(o);
//         System.out.println("value:" + baos.size());
//      }
//
//      System.out.println("all value:" + baos.size());
//      sumSize += baos.size();
//      System.out.println("sumsize polocas: " + sumSize);
//
//      baos = new ByteArrayOutputStream();
//      oos = new ObjectOutputStream(baos);
//      // + size of keys objects in cache
//      for(Object o : cache.getAdvancedCache().getDataContainer().keySet()) {
//         oos.writeObject(o);
//         System.out.println("key:" + baos.size());
//      }
//      oos.close();
//      sumSize += baos.size();
//
//      System.out.println("sumSize: " + sumSize);
//
//
//
////      for (int i = 0; i < 5; i++) {
////         System.out.println("cache get key" + i + ": " + cache.get("key" + i));
////      }
//
//
//      List<Cache> caches = new LinkedList<Cache>();
//      caches.add(cache);
//
//      int numPoints = 1000000;
//      int numServers = caches.size();
//      int numberPerWorker = numPoints / numServers;
//
//      DistributedExecutorService des = new DefaultExecutorService(cache);
//
//      long start = System.currentTimeMillis();
//      CircleTest ct = new CircleTest(numberPerWorker);
//      ct.setEnvironment(cache, null);
//      List<Future<Integer>> results = des.submitEverywhere(ct);
//      int countCircle = 0;
//      for (Future<Integer> f : results) {
//         countCircle += f.get();
//      }
//      double appxPi = 4.0 * countCircle / numPoints;
//
//      System.out.println("Distributed PI appx is " + appxPi +
//                               " completed in " + (System.currentTimeMillis() - start) + " ms");
//   }
//
//   //   private static class CircleTest implements Callable<Integer>, Serializable {
//   private static class CircleTest implements DistributedCallable, Serializable {
//
//      /**
//       * The serialVersionUID
//       */
//      private static final long serialVersionUID = 3496135215525904755L;
//
//      private final int loopCount;
//      private Cache cache = null;
//
//
//      public CircleTest(int loopCount) {
//         this.loopCount = loopCount;
//      }
//
//      @Override
//      public Integer call() throws Exception {
//
////         ByteArrayOutputStream baos = new ByteArrayOutputStream();
////         ObjectOutputStream oos = new ObjectOutputStream(baos);
////         // need to be serializable
////         oos.writeObject(cache.get("key1"));
////         oos.close();
////
////         System.out.println("baos size: " + baos.size());
////
////         return baos.size();
//
//
////         int insideCircleCount = 0;
////         for (int i = 0; i < loopCount; i++) {
////            double x = Math.random();
////            double y = Math.random();
////            if (insideCircle(x, y))
////               insideCircleCount++;
////         }
////         return insideCircleCount;
//         return 5;
//      }
//
////      private boolean insideCircle(double x, double y) {
////         return (Math.pow(x - 0.5, 2) + Math.pow(y - 0.5, 2))
////               <= Math.pow(0.5, 2);
////      }
//
//      @Override
//      public void setEnvironment(Cache cache, Set set) {
//         // keySet is null now
//         this.cache = cache;
//      }
//   }
//}
//
//

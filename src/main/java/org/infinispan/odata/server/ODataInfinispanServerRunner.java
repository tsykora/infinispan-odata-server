package org.infinispan.odata.server;

import org.odata4j.producer.resources.DefaultODataProducerProvider;
import org.infinispan.odata.facades.JerseyRuntimeFacade;
import org.infinispan.odata.facades.RuntimeFacade;
import org.infinispan.odata.producer.InfinispanProducer;

/**
 * Entry point for starting Infinispan OData server.
 *
 * @author Tomas Sykora <tomas@infinispan.org>
 */
public class ODataInfinispanServerRunner {

    private final RuntimeFacade rtFacde = new JerseyRuntimeFacade();

    /**
     * Starts Infinispan OData server.
     *
     * args[0]: expecting endpoint URI, the main hosting point for service
     * For instance: http://localhost:8887/ODataInfinispanEndpoint.svc/
     *
     * args[1]: expecting name of Infinispan configuration file
     * For instance: infinispan-dist.xml (or a path can be specified)
     *
     * mvn clean compile assembly:assembly
     * java -jar odata-infinispan-server-jar-with-dependencies.jar
     *
     * @param args -- endpoint URI as the first parameter,
     *             path to desired Infinispan configuration file as the second parameter
     */
    public static void main(String[] args) {
        ODataInfinispanServerRunner oDataInfinispanServerRunner = new ODataInfinispanServerRunner();

        if(args.length < 2 || args == null || args[0] == null || args[1] == null) {
            throw new IllegalArgumentException("IllegalArgumentException: specify args[0]" +
                    " (e.g. http://localhost:8887/ODataInfinispanEndpoint.svc/)" +
                    " and args[1] (e.g. infinispan-dist.xml).");
        }

        oDataInfinispanServerRunner.run(args);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public void run(String[] args) {

        String containerName = "InfinispanODataContainer";
        String endpointUri = args[0];
        String configFile = args[1];

        final InfinispanProducer infinispanProducer =
                new InfinispanProducer(containerName, configFile);

        // START ODATA SERVER
        // register the producer as the static instance, then launch the http server
        DefaultODataProducerProvider.setInstance(infinispanProducer);
        this.rtFacde.hostODataServer(endpointUri);
    }
}

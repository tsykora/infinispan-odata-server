package org.tsykora.odata.producer;

import org.odata4j.producer.resources.DefaultODataProducerProvider;
import org.tsykora.odata.facades.JerseyRuntimeFacade;
import org.tsykora.odata.facades.RuntimeFacade;

/**
 * @author tsykora
 */
public class ODataInfinispanServerRunner {

    private final RuntimeFacade rtFacde = new JerseyRuntimeFacade();

    /**
     * args[0] expecting endpoint URI, the main hosting point for service
     * For example: http://localhost:8887/ODataInfinispanEndpoint.svc/
     *
     * args[1] expecting name of Infinispan configuration file e.g. infinispan-dist.xml
     *
     * mvn clean compile assembly:assembly
     * java -jar odata-infinispan-server-jar-with-dependencies.jar
     *
     * @param args
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

        String endpointUri = args[0];

        String containerName = "ODataInfinispanContainer";
        String configFile = args[1];

        final InfinispanProducer infinispanProducer =
                new InfinispanProducer(containerName, configFile);

        // START ODATA SERVER
        // register the producer as the static instance, then launch the http server
        DefaultODataProducerProvider.setInstance(infinispanProducer);
        this.rtFacde.hostODataServer(endpointUri);
    }

}

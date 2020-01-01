package org.realitix.dfilesearch.filesearch;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.dropwizard.Application;
import io.dropwizard.assets.AssetsBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.realitix.dfilesearch.filesearch.configuration.FileExecutorConfiguration;
import org.realitix.dfilesearch.filesearch.socket.UDPClient;
import org.realitix.dfilesearch.filesearch.util.NodeMap;
import org.realitix.dfilesearch.webservice.beans.FileResponse;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

public class FileSearchExecutor extends Application<FileExecutorConfiguration> {

    private static UDPClient udpClient;
    private FileExecutorConfiguration configuration;
    public static final NodeMap neighbourMap = new NodeMap();
    private static final Logger logger = LogManager.getLogger(FileSearchExecutor.class);

    public static void main(String[] args) throws Exception {
        new FileSearchExecutor().run(args);
    }
    @Override
    public String getName() {
        return super.getName();
    }

    @Override
    public void initialize(Bootstrap<FileExecutorConfiguration> bootstrap) {
        bootstrap.addBundle(new AssetsBundle("/assets/", "/" , "index.html"));
    }

    @Override
    public void run(FileExecutorConfiguration fileExecutorConfiguration, Environment environment) {
        BasicConfigurator.configure();
        configuration = fileExecutorConfiguration;
        registerBackendClient(fileExecutorConfiguration);
        startWebService(environment);
    }

    private void startWebService(Environment environment) {
        logger.info("Enabling Web Service..");
        environment.jersey().setUrlPattern("/api/*");
        environment.jersey().register(new FileSharingResource());
    }

    @Override
    public Class<FileExecutorConfiguration> getConfigurationClass() {
        return super.getConfigurationClass();
    }

    private static UDPClient registerBackendClient(FileExecutorConfiguration configuration) {
        final UDPClient client = UDPClient.UDPClientBuilder.newInstance()
                .setHost(configuration.getClient().getHost())
                .setPort(configuration.getClient().getPort())
                .setUsername(configuration.getClient().getUsername())
                .build(configuration);
        try {
            client.register(configuration.getBootstrapServer().getHost(), configuration.getBootstrapServer().getPort()).sync().await();
        } catch (InterruptedException e) {
            logger.error(e.getMessage());
            Thread.currentThread().interrupt();
        }
        udpClient = client;
        return client;
    }

    public static UDPClient getUdpClient() {
        return udpClient;
    }

    public FileExecutorConfiguration getConfiguration() {
        return configuration;
    }

    /**************
     * Web Service
     *************
     */
    @Path("/file")
    @Produces(MediaType.APPLICATION_JSON)
    public static class FileSharingResource {

        private final Logger logger = LogManager.getLogger(this.getClass());

        @GET
        @Path("{fileName}")
        public Response getFile(@PathParam("fileName") String fileName) {
            return Response.status(200).entity(synthesizeFile(fileName)).build();
        }

        @GET
        @Path("/map")
        public Response getNodeMap() {
            Response r = null;
            try {
                r = Response.status(200)
                        .entity((new ObjectMapper())
                                .writeValueAsString(FileSearchExecutor.neighbourMap.getNodeMap()))
                        .build();
            } catch (JsonProcessingException e) {
                logger.error(e.getMessage());
            }
            return r;
        }

        private FileResponse synthesizeFile(String fileName){
            logger.info("Synthesizing the file");
            String randomString = fileName + RandomStringUtils.randomAlphabetic(20).toUpperCase();
            int size = (int) ((Math.random() * ((10 - 2) + 1)) + 2);    // change this to a more random algorithm
            FileResponse fileResponse = FileResponse.FileResponseBuilder
                    .newInstance()
                    .setFileSize(size)
                    .setHash(DigestUtils.sha1Hex(randomString))
                    .build();
            logger.info("File synthesizing completed.");
            return fileResponse;
        }
    }
}

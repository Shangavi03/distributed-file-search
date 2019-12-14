package org.realitix.dfilesearch.webservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.log4j.Logger;
import org.realitix.dfilesearch.webservice.beans.FileResponse;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Collections;

/**
 * TODO: Get the web port from the config
 */
@SpringBootApplication
public class WebServiceExecutor {

    private static ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
    private static Logger logger = Logger.getRootLogger();

    public static void main(String[] args) {
        runServer();
    }

    public static void runServer() {
        final SpringApplication springApplication = new SpringApplication(WebServiceExecutor.class);
        springApplication
                .setDefaultProperties(Collections.<String, Object> singletonMap("server.port", 8999)
                );
        springApplication.run();
    }

    @RestController
    @RequestMapping(value = "file")
    public static class FileController {
        @GetMapping(value = "/{fileName}")
        public FileResponse getFile(@PathVariable("fileName") String fileName) {
            logger.info("Request for filename: \'" + fileName + "\' arrived." );
            return synthesizeFile(fileName);
        }

        /**
         * Randomly generates file content, with the file name as salt.
         * @param fileName name of the file
         * @return synthesized file response
         */
        private FileResponse synthesizeFile(String fileName){
            logger.info("Synthesizing the file");
            String randomString = fileName + RandomStringUtils.randomAlphabetic(20).toUpperCase();
            int size = (int) ((Math.random() * ((10 - 2) + 1)) + 2);    // change this to a more random algorithm
            FileResponse fileResponse = new FileResponse();
            fileResponse.setFileSize(size);
            fileResponse.setHash(DigestUtils.sha1Hex(randomString));
            logger.info("File synthesizing completed.");
            return fileResponse;
        }
    }
}

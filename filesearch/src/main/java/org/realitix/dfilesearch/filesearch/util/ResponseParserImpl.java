package org.realitix.dfilesearch.filesearch.util;


import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.realitix.dfilesearch.filesearch.FileSearchExecutor;
import org.realitix.dfilesearch.filesearch.beans.Node;

public class ResponseParserImpl implements ResponseParser<String>{

    private static final Logger logger = LogManager.getLogger(ResponseParserImpl.class);

    @Override
    public void parse(String s) {
        String command = s.split(" ")[1];
        switch (command) {
            case "REGOK":
                logger.info("ACK for REG received.");
                parseRegok(s);
                break;
            case "UNREGOK":
                logger.info("Response FOR UNROK received.");
                parseUnRegok(s);
                break;
            case "JOINOK":
                logger.info("JOINOK Received.");
                break;
            case "SEROK":
                logger.info("SEROK Received");
                break;
            default:
                logger.info("Passed to RequestParser.");
        }
    }

    private void parseRegok(String s) {
        String[] ips = s.split(" ");
        // first node doesn't have to do anything.
        // Second node joining has to add the first node to its map.
        if (ips.length > 3 && ips.length < 6) {
            FileSearchExecutor.neighbourMap
                    .insertNode(new Node(ips[3], Integer.parseInt(ips[4])), 1);
            logger.info("Node: [" + FileSearchExecutor.neighbourMap.getNodeMap().get(1).getIp() + ":"
                    + FileSearchExecutor.neighbourMap.getNodeMap().get(1).getPort() + "] is added to the node map.");
        } else if (ips.length > 5) {
            FileSearchExecutor.neighbourMap
                    .insertNode(new Node(ips[3], Integer.parseInt(ips[4])), 1)
                    .insertNode(new Node(ips[5], Integer.parseInt(ips[6])), 2);
            logger.info("Node: [" + FileSearchExecutor.neighbourMap.getNodeMap().get(1).getIp() + ":"
                    + FileSearchExecutor.neighbourMap.getNodeMap().get(1).getPort() + " and " +
                    FileSearchExecutor.neighbourMap.getNodeMap().get(2).getIp() + ":"
                    + FileSearchExecutor.neighbourMap.getNodeMap().get(2).getPort()+ "] is added to the node map.");
        } else {
            // in case ips.length < 3
            logger.info("This is the first node. Response from BS: " + s);
        }

    }

    private void parseUnRegok(String s) {
        // parse unregok
    }
}

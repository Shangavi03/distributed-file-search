# Distributed P2P File Sharing System

A distributed methodology to search files in a system.

## Phases

* Phase 1: Design network topology :heavy_check_mark:
* Phase 2: Design & Develop a socket-based solution to find files requested by different nodes. :x:
* Phase 3: Web service (REST API) to support file transfer. :heavy_check_mark:

<div align="center"> 
    <img src="docs/DistributedArchitecture.png" />
</div>

## Setting up in IntelliJ and running within Jetbrains environments

First, we need to set up the commandline arguments. 

1. Open `Run | Edit Configurations`, and tick `Allow Parallel Run` (for spawning a unique IDEA thread for each execution)
2. Add `server` and `configuration.yaml` as commandline arguments.

<div align="center">
    <img src="docs/conf.png" />
</div>

3. Spin up a `BootstrapServer` instance first. Then, run `FileServer` changing the configuration in `configuration.yaml` **in the root directory** of the project (i.e. `/dfilesearch` directory). Try to spin more than 3 `FileServer` nodes up, and observe the BootstrapServer console and consoles of each node (pay attention to console outputs. The response messages are logged. Check for the log message below for an instance)  by registering multiple nodes.

```
13:10:22.546 [nioEventLoopGroup-2-1] INFO org.realitix.dfilesearch.filesearch.socket.UDPClientHandler - Response message: 0042 REGOK 2 127.0.0.1 5001 127.0.0.1 5002
```
 
The above message shows that the bootstrap server has sent the `REGOK` along with the IPs and ports of the currently registered nodes when a third node has requested `REG`.

Note that if two or more nodes have already been registered, the incoming nodes after that will be responded by nodes which are connected to the BS. **The passage in the assignment says that, we should connect only to two randomly selected nodes**. But take a look at this code extracted out from the `BootstrapServer` they've issued.

```java
class BootstrapServer {
    public static void main(String[] args){
      // ......
        if (isOkay) {
            if (nodes.size() == 1) {
                reply += "1 " + nodes.get(0).getIp() + " " + nodes.get(0).getPort();
            } else if (nodes.size() == 2) {
                reply += "2 " + nodes.get(0).getIp() + " " + nodes.get(0).getPort() + " " + nodes.get(1).getIp() + " " + nodes.get(1).getPort();
            } else {
                Random r = new Random();
                int Low = 0;
                int High = nodes.size();
                int random_1 = r.nextInt(High-Low) + Low;
                int random_2 = r.nextInt(High-Low) + Low;
                while (random_1 == random_2) {
                    random_2 = r.nextInt(High-Low) + Low;
                }
                echo (random_1 + " " + random_2);
                reply += "2 " + nodes.get(random_1).getIp() + " " + nodes.get(random_1).getPort() + " " + nodes.get(random_2).getIp() + " " + nodes.get(random_2).getPort();
            }
            nodes.add(new Neighbour(ip, port, username));
        }
    // .....
    }
}
```

The `BootstrapServer` already performs the randomization of the nodes and provide us with **two random nodes**. Therefore, regardless of how many nodes you register in the `BootstrapServer` for the `REG` query you get two nodes, who are supposed to be the neighbors of that particular node. 


Consider the log message given below.

```
INFO  [2019-12-25 03:10:59,490] org.realitix.dfilesearch.filesearch.socket.UDPClientHandler: Response message: 0042 REGOK 2 127.0.0.1 5003 127.0.0.1 5001
```

This is the nature of the overlay network. The details of the two nodes responded by the bootstrap server are the neighbors to the incoming node.


## Running in independent nodes

1. Run `bootstrapserver-1.0-SNAPSHOT.jar` using `java -jar bootstrapserver-1.0-SNAPSHOT.jar`.
2. Provide the configurations to the `configuration.yaml`.
3. Build the project using `mvn clean install -DskipTests`. Note that I've included a plugin which copies an instance of `configuration.yaml` to `filesearch/target`.
3. Run the `.jar` file generated in `filesearch/target` using `java -jar filesearch-1.0-SNAPSHOT.jar server configuration.yaml`. To run multiple instances, simply change the ports (specially the HTTP port specified as `server`) in `configuration.yaml`.
4. Observe the BootstrapServer console and consoles of each node (pay attention to console outputs. The response messages are logged. Check for the log message below for an instance)  by registering multiple nodes.

`configuration.yaml` file is shown below.

```yaml
name: Distributed File Executor
server:                     # HTTP server details
  applicationConnectors:
    - type: http
      port: 8091
  adminConnectors:
    - type: http
      port: 8092
ports:                     # client details
  port: 5001
  host: 127.0.0.1
  username: 1234abcd
bootstrapServer:           # bootstrap server details
  port: 55555
  host: 127.0.0.1
```

### Netstat commands

* Check for port 9000: `netstat -tulpn | grep 9000
`

* Send messages to UDP serveer: `echo -n "hello" | nc -4u localhost 9000`


## Notes

1. After the initial handshakes and housekeeping, the client should connect to other peers (for file sharing). Thus, "host" and "port" should resemble those of that peers. This can be facilitated by giving some sort of a map. Think about it. Or, we should close the connection with the BS and initiate another connection with the peers after the initial handshakes.After the initial handshakes and housekeeping, the client should connect to other peers (for file sharing). Thus, "host" and "port" should resemble those of that peers. This can be facilitated by giving some sort of a map. Think about it. Or, we should close the connection with the BS and initiate another connection with the peers after the initial handshakes. This should be remedied in `UDPClient.java` file in its `run()` method.

2. TCP mainains reliability, flow control, order, low speed. 
3. TCP is best suited for 
    
    * World Wide Web (HTTP, HTTPS)
    * Secure Shell (SSH)
    * File Transfer Protocol (FTP)
    * Email (SMTP, IMAP/POP)

4. UDP is best suited for:

    * VPN tunneling
    * Streaming videos
    * Online games
    * Live broadcasts
    * Domain Name System (DNS)
    * Voice over Internet Protocol (VoIP)
    * Trivial File Transfer Protocol (TFTP)

5. <u>Testing the project locally should be done giving the IP `127.0.0.1`.</u> We cannot test giving another IP in the configuration file, since the UDP sockets are bound for those particular ports.

6. Each `Node` has a `Channel` instance. Upon the `JOIN` query, these channels are initiated.

7. Web interface at `/`, and api at `/api`. File is served at `/api/file/{fileName}`, and nodemap is served at `/api/file/map`.

```java
public class UDPClient {
    // code
     public Channel createChannel(String remoteIp, int remotePort, ChannelInitializer<DatagramChannel> channelInitializer) throws InterruptedException {
            Bootstrap b = new Bootstrap();
            b.group(new NioEventLoopGroup())
                    .channel(NioDatagramChannel.class)
                    .option(ChannelOption.SO_KEEPALIVE, true)
                    .remoteAddress(remoteIp, remotePort)
                    .handler(channelInitializer);
            return b.connect().channel().bind(SocketUtils.socketAddress(host, port)).sync().await().channel();
        }

    /**
     * Sends the JOIN request to the neighbors
     * @param neighbour1 first neighbour
     * @param neighbour2 second neighbour
     * @throws InterruptedException
     */
    public void join(Node neighbour1, Node neighbour2) throws InterruptedException {
        UDPJoinInitializer initializer = new UDPJoinInitializer();
        neighbour1.setChannel(createChannel(neighbour1.getIp(), neighbour1.getPort(), initializer));
        neighbour2.setChannel(createChannel(neighbour2.getIp(), neighbour2.getPort(), initializer));
        write(neighbour1.getChannel(), new JoinRequest(host, port), neighbour1.getIp(), neighbour1.getPort());
        write(neighbour2.getChannel(), new JoinRequest(host, port), neighbour2.getIp(), neighbour2.getPort());
    }


}
```


## TODOs

Updated TODOs can be found in [this link](https://github.com/uom-cse-realitix/distributed-file-search/projects/1), in a Kanban board.

## FAQ

1. Why use a separate logger, such as `Log4j`?

    *  `System.out.println` buffers the input and synchronizes its contents. Furthermore, it switches between user level and kernel level to write to the console, therefore increasing the overhead. Therefore, an application-level logger is used in most industrial applications ([Source](https://javapapers.com/core-java/system-out-println/)).
    
2. Why a separate `webservice` module?

    * The question is divided into three phases. The third explicitly asks us to develop a web service. In this modular approach, the web service is a cohesive `.jar` which can be deployed in the same machine as the node. 
    
    * However, for simplicity, I have used `jetty` in `DropWizard` to spin up an HTTP server to instantiate the web service _inside_ `filesearch` module itself. This approach is **not modular** but simple. The resource class is given below.
    
     ```java
     @Path("/file")
        @Produces(MediaType.APPLICATION_JSON)
        public static class FileSharingResource {
    
            private final Logger logger = LogManager.getLogger(this.getClass());
    
            @GET
            @Path("{fileName}")
            public Response getFile(@PathParam("fileName") String fileName) {
                return Response.status(200).entity(synthesizeFile(fileName)).build();
            }
    
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
    ```
    
3. How does the `JOIN` has to happen?

    * After initial communication between the `BootstrapServer` (BS), the establishment of the network is done.
    
    * Afterwards, we have to communicate with the neighbours returned by the BS. 
    
    * For this, we need a UDP server listening in each node, which is modeled in `UDPServer.java` itself. Inside `UDPServerHandler` the `channelRead0` event should handle `JOIN` and other requests appropriately.
    ```java
    public class UDPServer {
        private final Logger logger = LogManager.getLogger(UDPServer.class);
        private String host;
        private int port;
    
        private UDPServer(UDPServerBuilder builder) {
            this.host = builder.host;
            this.port = builder.port;
        }
    
        /**
         * Note: https://stackoverflow.com/questions/41505852/netty-closefuture-sync-channel-blocks-rest-api
         * ServerBootstrap allows many client to connect via its channel. Therefore TCP has a dedicated ServerSocketChannel.
         * Bootstrap is used to create channels for single connections. Because UDP has one channel for all clients it makes sense that only the Bootstrap is required. All clients bind to the same channel.
         * @return the udp server
         */
        public Channel listen() {
            Channel channel = null;
            EventLoopGroup group = new NioEventLoopGroup();
            Bootstrap b = new Bootstrap();
            b.group(group)
                    .channel(NioDatagramChannel.class)
                    .option(ChannelOption.SO_BROADCAST, true)
                    .handler(new UDPServerHandler());
            try {
                channel = b.bind(host, port).sync().channel(); // .sync().channel().closeFuture.await()
                logger.info("WS UDP server listening to port: " + port);
            } catch (InterruptedException e) {
                logger.error(e.getMessage());
            }
            return channel;
        }
    
        /**
         * Builder class for the server
         */
        public static class UDPServerBuilder {
            private String host;
            private int port;
    
            private UDPServerBuilder() {}
    
            public static UDPServerBuilder newInstance() {
                return new UDPServerBuilder();
            }
    
            public UDPServerBuilder setHost(String host) {
                this.host = host;
                return this;
            }
    
            public UDPServerBuilder setPort(int port) {
                this.port = port;
                return this;
            }
    
            public UDPServer build() {
                return new UDPServer(this);
            }
        }
    }
    ```    
4. Why a `configuration.yaml`? Can't we just hardcode the information?

    * Having a serializable `.yaml` is the standard way of coding a component.
    
    * Generally, each component (built as a `.jar`) is deployed alongwith this `.yaml`.
    
    * This way, we don't have to reach into the code-level (and we can have the freedom of not having the code repository cloned in each node's file system) to alter the information (since we fetch them from `.yaml`).
    
5. Why `Netty`?

    * Many organizations now use `Netty` to establish their networking infrastructure. `WSO2`, `ElasticSearch` (`Solr` successor), and plenty of others use it.
    
    * Its event-driven architecture mixed with interceptor pattern handlers gives cohesion to the code and much needed functionalities.
      <div align="center"> 
        <img src="https://netty.io/images/components.png" />
      </div>
     
    




    
   
    

 

    
package com.mapr.franz.catcher;

import com.google.protobuf.RpcController;
import com.googlecode.protobuf.pro.duplex.PeerInfo;
import com.googlecode.protobuf.pro.duplex.RpcClientChannel;
import com.googlecode.protobuf.pro.duplex.client.DuplexTcpClientBootstrap;
import com.mapr.franz.catcher.wire.Catcher;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.Executors;

/**
 * Handles connecting to a server with standard options.
 */
class RealCatcherConnection implements CatcherConnection {
    private final Logger logger = LoggerFactory.getLogger(RealCatcherConnection.class);

    private final Catcher.CatcherService.BlockingInterface catcherService;
    private final RpcController controller;
    private final DuplexTcpClientBootstrap bootstrap;
    private final RpcClientChannel channel;
    private final PeerInfo server;

    public static CatcherConnection connect(PeerInfo server) {
        final Logger logger = LoggerFactory.getLogger(RealCatcherConnection.class);
        final CatcherConnection r;
        try {
            r = new RealCatcherConnection(server);
        } catch (IOException e) {
            logger.warn("Cannot connect to {}", server, e);
            return null;
        }
        return r;
    }

    RealCatcherConnection(PeerInfo server) throws IOException {
        logger.info("Connecting to {}", server);
        this.server = server;
        PeerInfo client = new PeerInfo("clientHostname", 9999);
        bootstrap = new DuplexTcpClientBootstrap(
                client,
                new NioClientSocketChannelFactory(
                        Executors.newCachedThreadPool(),
                        Executors.newCachedThreadPool()));
        bootstrap.setCompression(false);

        bootstrap.setOption("connectTimeoutMillis", 1000);
        bootstrap.setOption("connectResponseTimeoutMillis", 1000);
        bootstrap.setOption("receiveBufferSize", 1048576);
        bootstrap.setOption("tcpNoDelay", true);

        channel = bootstrap.peerWith(server);

        catcherService = Catcher.CatcherService.newBlockingStub(channel);
        controller = channel.newRpcController();
    }

    @Override
    public Catcher.CatcherService.BlockingInterface getService() {
        return catcherService;
    }

    @Override
    public RpcController getController() {
        return controller;
    }

    @Override
    public PeerInfo getServer() {
        return server;
    }

    @Override
    public void close() {
        // these can be null in mocked versions of this class
        if (channel != null) {
            channel.close();
        }
        if (bootstrap != null) {
            bootstrap.releaseExternalResources();
        }
    }

    @Override
    public String toString() {
        return "CatcherConnection{" + "server=" + server + '}';
    }
}

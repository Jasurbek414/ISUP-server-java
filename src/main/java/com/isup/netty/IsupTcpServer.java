package com.isup.netty;

import com.isup.security.IpBanManager;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class IsupTcpServer {

    private static final Logger log = LoggerFactory.getLogger(IsupTcpServer.class);

    @Value("${isup.tcp.port:7660}")
    private int port;

    @Value("${isup.tcp.boss-threads:1}")
    private int bossThreads;

    @Value("${isup.tcp.worker-threads:8}")
    private int workerThreads;

    private final IsupMessageHandler messageHandler;
    private final IsupPacketEncoder  encoder;
    private final IsupPacketDecoder  decoder;
    private final IpBanManager       ipBanManager;

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;

    public IsupTcpServer(IsupMessageHandler messageHandler,
                         IsupPacketEncoder encoder,
                         IsupPacketDecoder decoder,
                         IpBanManager ipBanManager) {
        this.messageHandler = messageHandler;
        this.encoder        = encoder;
        this.decoder        = decoder;
        this.ipBanManager   = ipBanManager;
    }

    @PostConstruct
    public void start() throws InterruptedException {
        bossGroup   = new NioEventLoopGroup(bossThreads);
        workerGroup = new NioEventLoopGroup(workerThreads);

        ServerBootstrap bootstrap = new ServerBootstrap()
            .group(bossGroup, workerGroup)
            .channel(NioServerSocketChannel.class)
            .option(ChannelOption.SO_BACKLOG, 1024)
            .option(ChannelOption.SO_REUSEADDR, true)
            .childOption(ChannelOption.TCP_NODELAY, true)
            .childOption(ChannelOption.SO_KEEPALIVE, true)
            .childHandler(new IsupChannelInitializer(messageHandler, encoder, decoder, ipBanManager));

        bootstrap.bind(port).sync();
        log.info("ISUP TCP server listening on port {}", port);
        
        // Also listen on Alarm Port 7661 if main port is 7660
        if (port == 7660) {
            try {
                bootstrap.bind(7661).sync();
                log.info("ISUP TCP server listening on port 7661 (ALARM)");
            } catch (Exception e) {
                log.warn("Failed to bind port 7661, it might already be in use: {}", e.getMessage());
            }
        }
    }

    @PreDestroy
    public void stop() {
        log.info("Stopping ISUP TCP server...");
        if (bossGroup   != null) bossGroup.shutdownGracefully();
        if (workerGroup != null) workerGroup.shutdownGracefully();
    }
}

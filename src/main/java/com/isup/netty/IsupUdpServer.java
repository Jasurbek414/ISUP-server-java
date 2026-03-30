package com.isup.netty;

import com.isup.protocol.IsupProtocol;
import com.isup.protocol.IsupPacket;
import com.isup.protocol.MessageType;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class IsupUdpServer {
    private static final Logger log = LoggerFactory.getLogger(IsupUdpServer.class);

    @Value("${isup.udp.port:7660}")
    private int port;

    @Value("${ISUP_SERVER_IP:192.168.1.35}")
    private String serverIp;

    private EventLoopGroup group;
    private final IsupMessageHandler messageHandler;

    public IsupUdpServer(IsupMessageHandler messageHandler) {
        this.messageHandler = messageHandler;
    }

    @PostConstruct
    public void start() throws InterruptedException {
        group = new NioEventLoopGroup();
        
        int[] portsToBind = (port == 7660) ? new int[]{7660, 7661, 37020} : new int[]{port};

        for (int p : portsToBind) {
            final int currentPort = p;
            Bootstrap b = new Bootstrap();
            b.group(group)
             .channel(NioDatagramChannel.class)
             .option(ChannelOption.SO_BROADCAST, true)
             .handler(new SimpleChannelInboundHandler<DatagramPacket>() {
                 @Override
                 protected void channelRead0(ChannelHandlerContext ctx, DatagramPacket packet) {
                     ByteBuf content = packet.content();
                     if (content.readableBytes() < 5) return;
                     
                     byte[] raw = new byte[content.readableBytes()];
                     content.getBytes(content.readerIndex(), raw);
                     String rawStr = new String(raw, java.nio.charset.StandardCharsets.UTF_8);

                     if (rawStr.startsWith("<?xml")) {
                         log.info("UDP XML RECV [port={}]: from={} content={}", currentPort, packet.sender(), rawStr);
                     if (rawStr.contains("REGISTER")) {
                         String ver = "4.0";
                         String seq = "1";
                         
                         java.util.regex.Matcher mVer = java.util.regex.Pattern.compile("<Version>(.*?)</Version>").matcher(rawStr);
                         if (mVer.find()) ver = mVer.group(1);
                         
                         java.util.regex.Matcher mSeq = java.util.regex.Pattern.compile("<Sequence>(.*?)</Sequence>").matcher(rawStr);
                         if (mSeq.find()) seq = mSeq.group(1);

                         // Check if this is a response to our challenge (Check if XML has Password with hash or similar)
                         // For now, let's always challenge once to see the behavior
                         boolean isStep2 = rawStr.contains("<Password>") && rawStr.length() > 800;
                         
                         String status = "200";
                         String msg    = "OK";
                         String params = "<Status>" + status + "</Status>\n" +
                                         "<Message>" + msg + "</Message>\n" +
                                         "<ServerID>ISUP_PLATFORM</ServerID>\n" +
                                         "<AddressType>0</AddressType>\n" +
                                         "<RegisterServer>" + serverIp + "</RegisterServer>\n";

                         String responseXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                                            "<PPVSPMessage>\n" +
                                            "<Version>" + ver + "</Version>\n" +
                                            "<Sequence>" + seq + "</Sequence>\n" +
                                            "<CommandType>RESPONSE</CommandType>\n" +
                                            "<Command>REGISTER</Command>\n" +
                                            "<Params>\n" +
                                            params +
                                            "</Params>\n" +
                                            "</PPVSPMessage>";
                         ctx.writeAndFlush(new DatagramPacket(Unpooled.copiedBuffer(responseXml, java.nio.charset.StandardCharsets.UTF_8), packet.sender()));
                         log.info("UDP XML SEND: PPVSP REGISTER Success (200) to {}", packet.sender());
                     }
                     return;
                 }

                     int marker = content.readUnsignedByte();
                     int len = content.readUnsignedByte();
                     if (content.readableBytes() < len) return;
                     
                     byte[] body = new byte[len];
                     content.readBytes(body);
                     int type = body[0] & 0xFF;
                     
                     if (type == 0x13) { // KEEPALIVE_REQUEST
                         ByteBuf response = ctx.alloc().buffer(10);
                         response.writeByte(0x10);
                         response.writeByte(0x05); 
                         response.writeByte(0x14); 
                         if (len >= 5) response.writeBytes(body, 1, 4); 
                         else response.writeIntLE(0);
                         ctx.writeAndFlush(new DatagramPacket(response, packet.sender()));
                     } else if (type == 0x65) {
                         ByteBuf response = ctx.alloc().buffer(10);
                         response.writeByte(0x10);
                         response.writeByte(0x02);
                         response.writeByte(0x66); 
                         response.writeByte(0x00);
                         ctx.writeAndFlush(new DatagramPacket(response, packet.sender()));
                     }
                 }
             });

            try {
                b.bind(p).sync();
                log.info("ISUP UDP server listening on port {}", p);
            } catch (Exception e) {
                log.warn("Failed to bind UDP port {}: {}", p, e.getMessage());
            }
        }
    }

    @PreDestroy
    public void stop() {
        if (group != null) group.shutdownGracefully();
    }
}

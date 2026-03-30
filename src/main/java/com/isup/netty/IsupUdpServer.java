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

                         // Echo back the RegisterServer from the device's request (FRP public IP/domain)
                         // This ensures the device connects TCP to the correct public address
                         String regServer = serverIp;
                         java.util.regex.Matcher mReg = java.util.regex.Pattern.compile("<RegisterServer>(.*?)</RegisterServer>").matcher(rawStr);
                         if (mReg.find()) regServer = mReg.group(1);

                         // Extract DeviceID and password for ISUP key validation (optional logging)
                         java.util.regex.Matcher mDev = java.util.regex.Pattern.compile("<DeviceID>(.*?)</DeviceID>").matcher(rawStr);
                         String devId = mDev.find() ? mDev.group(1) : "unknown";
                         log.info("UDP REGISTER: device={} ver={} server={}", devId, ver, regServer);

                         // Detect register port: LocalPort from device request or default 17660
                         String regPort = "17660";
                         java.util.regex.Matcher mPort = java.util.regex.Pattern.compile("<LocalPort>(.*?)</LocalPort>").matcher(rawStr);
                         if (mPort.find()) regPort = mPort.group(1);

                         String status = "200";
                         String msg    = "OK";
                         String params = "<Status>" + status + "</Status>\n" +
                                         "<Message>" + msg + "</Message>\n" +
                                         "<ServerID>ISUP_PLATFORM</ServerID>\n" +
                                         "<AddressType>1</AddressType>\n" +
                                         "<RegisterServer>" + regServer + "</RegisterServer>\n" +
                                         "<RegisterPort>" + regPort + "</RegisterPort>\n";

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

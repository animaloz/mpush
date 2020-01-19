package cn.mpush.client;


import cn.mpush.client.handler.ClientCustomHeartbeatHandler;
import cn.mpush.core.handler.TextWebSocketFrameHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.handler.timeout.IdleStateHandler;

import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * Sends one message when a connection is open and echoes back any received
 * data to the server.  Simply put, the echo client initiates the ping-pong
 * traffic between the echo client and server by sending the first message to
 * the server.
 */
public final class EchoClient {

    static final String HOST = System.getProperty("host", "127.0.0.1");
    static final int PORT = Integer.parseInt(System.getProperty("port", "8007"));
    static final Channel channel = null;

    public static void main(String[] args) throws Exception {
    	System.out.println("EchoClient.main");
        // Configure SSL
        final SslContext sslCtx = SslContextBuilder.forClient().trustManager(InsecureTrustManagerFactory.INSTANCE).build();
        // Configure the client.
        /*创建一个Bootstrap b实例用来配置启动客户端
         * b.group指定NioEventLoopGroup来处理连接，接收数据
         * b.channel指定通道类型
         * b.option配置参数
         * b.handler客户端成功连接服务器后就会执行
         * b.connect客户端连接服务器
         * b.sync阻塞配置完成并启动
        */
        EventLoopGroup group = new NioEventLoopGroup();
        try {
            Bootstrap b = new Bootstrap();

            b.group(group)
             .channel(NioSocketChannel.class)
             //.option(ChannelOption.TCP_NODELAY, true)
             .handler(new ChannelInitializer<SocketChannel>() {
                 @Override
                 public void initChannel(SocketChannel ch) throws Exception {
                     ChannelPipeline p = ch.pipeline();
                     //websocket协议本身是基于http协议的，所以这边也要使用http解编码器
                     p.addLast(new HttpServerCodec());
                     //以块的方式来写的处理器
                     p.addLast(new ChunkedWriteHandler());
                     //netty是基于分段请求的，HttpObjectAggregator的作用是将请求分段再聚合,参数是聚合字节的最大长度
                     p.addLast(new HttpObjectAggregator(8192));

                     //ws://server:port/context_path
                     //ws://localhost:9999/ws
                     //参数指的是contex_path
                     p.addLast(new WebSocketServerProtocolHandler("/ws"));
//                     if (sslCtx != null) {
//                         // SSL
//                         p.addLast(sslCtx.newHandler(ch.alloc(), HOST, PORT));
//                     }
                     // 每隔15s 没有write请求就自动发送ping请求
//                     p.addLast(new IdleStateHandler(0, 15, 0));
//                     p.addLast(new LengthFieldBasedFrameDecoder(1024, 0, 4, -4, 0));
//                     p.addLast(new ClientCustomHeartbeatHandler("client", 4));
                     p.addLast(new TextWebSocketFrameHandler("client"));
                     p.addLast(new HttpClientCodec(), new HttpObjectAggregator(8192), handler);
                 }
             });
            // Start the client.
            System.out.println("EchoClient.main ServerBootstrap配置启动完成");
            // Wait until the connection is closed.
            ChannelFuture channelFuture = b.connect(HOST, PORT);
            Channel channel = channelFuture.sync().channel();
            //标准输入
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
            //利用死循环，不断读取客户端在控制台上的输入内容
            for (;;){
                String msg = bufferedReader.readLine();
                if ("bye".equals(msg.toLowerCase())) {
                    channel.writeAndFlush(new CloseWebSocketFrame());
                    channel.closeFuture().sync();
                    break;
                } else if ("ping".equals(msg.toLowerCase())) {
                    WebSocketFrame frame = new PingWebSocketFrame(Unpooled.wrappedBuffer(new byte[] { 8, 1, 8, 1 }));
                    channel.writeAndFlush(frame);
                } else {
                    WebSocketFrame frame = new TextWebSocketFrame(msg);
                    channel.writeAndFlush(frame);
                }
//                ByteBuf buf = channel.alloc().buffer(16);
//                buf.writeInt(5 + in.getBytes().length);
//                buf.writeByte(3);
//                buf.writeBytes(in.getBytes());
//                channel.writeAndFlush(buf);
            }
        } finally {
            // Shut down the event loop to terminate all threads.
            group.shutdownGracefully();
        }
    }
}

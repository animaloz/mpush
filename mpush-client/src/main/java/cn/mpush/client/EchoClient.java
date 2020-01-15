package cn.mpush.client;


import cn.mpush.core.handler.ClientCustomHeartbeatHandler;
import cn.mpush.core.handler.CustomHeartbeatHandler;
import cn.mpush.core.message.MessageDecoder;
import cn.mpush.core.message.MessageEncoder;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.CharsetUtil;

import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * Sends one message when a connection is open and echoes back any received
 * data to the server.  Simply put, the echo client initiates the ping-pong
 * traffic between the echo client and server by sending the first message to
 * the server.
 */
public final class EchoClient {

    static final boolean SSL = System.getProperty("ssl") != null;
    static final String HOST = System.getProperty("host", "127.0.0.1");
    static final int PORT = Integer.parseInt(System.getProperty("port", "8007"));
    static final int SIZE = Integer.parseInt(System.getProperty("size", "256"));

    public static void main(String[] args) throws Exception {
    	System.out.println("EchoClient.main");
        // Configure SSL.git
        final SslContext sslCtx;
//        if (SSL) {
            sslCtx = SslContextBuilder.forClient().trustManager(InsecureTrustManagerFactory.INSTANCE).build();
//        } else {
//            sslCtx = null;
//        }

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
                     if (sslCtx != null) {
                         // SSL
                         p.addLast(sslCtx.newHandler(ch.alloc(), HOST, PORT));
                     }
                     // 每隔15s 没有write请求就自动发送ping请求
                     p.addLast(new StringDecoder(CharsetUtil.UTF_8));
                     p.addLast(new StringEncoder(CharsetUtil.UTF_8));
                     p.addLast(new IdleStateHandler(0, 15, 0));
                     p.addLast(new LengthFieldBasedFrameDecoder(1024, 0, 4, -4, 0));
                     p.addLast(new ClientCustomHeartbeatHandler("client", 4));
                     p.addLast(new EchoClientHandler());
                 }
             });
            // Start the client.
            System.out.println("EchoClient.main ServerBootstrap配置启动完成");
            // Wait until the connection is closed.
            Channel channel = b.connect(HOST, PORT).sync().channel();
            //标准输入
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
            //利用死循环，不断读取客户端在控制台上的输入内容
            for (;;){
                String in = bufferedReader.readLine();
                System.out.println(in);
                channel.writeAndFlush(in +"\r\n");
            }
        } finally {
            // Shut down the event loop to terminate all threads.
            group.shutdownGracefully();
        }
    }
}

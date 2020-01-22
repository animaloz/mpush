package cn.mpush.server;

import cn.mpush.core.handler.WebSocketFrameHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.stream.ChunkedWriteHandler;

/**
 * Echoes back any received data from a client.
 */
public final class WebSocketServer {

    static final int PORT = Integer.parseInt(System.getProperty("port", "8007"));

    public static void main(String[] args) throws Exception {
        System.out.println("EchoServer.main start");
        // Configure SSL.
//        SelfSignedCertificate ssc = new SelfSignedCertificate();
//        final SslContext sslCtx = SslContextBuilder.forServer(ssc.certificate(), ssc.privateKey()).build();

        // Configure the server.
        /*步骤
         * 创建一个ServerBootstrap b实例用来配置启动服务器
         * b.group指定NioEventLoopGroup来接收处理新连接
         * b.channel指定通道类型
         * b.option设置一些参数
         * b.handler设置日志记录
         * b.childHandler指定连接请求，后续调用的channelHandler
         * b.bind设置绑定的端口
         * b.sync阻塞直至启动服务
         */
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, 100)
//                    .option(ChannelOption.SO_KEEPALIVE, Boolean.TRUE)
                    //.handler(new LoggingHandler(LogLevel.INFO))
                    .childHandler(new ChannelInitializer<SocketChannel>() {
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
                            //websocket定义了传递数据的6中frame类型
                            p.addLast(new WebSocketServerProtocolHandler("/wsServer"));
                            p.addLast(new WebSocketFrameHandler("server"));
                        }
                    });

            // Start the servernetty.
            ChannelFuture f = b.bind(PORT).sync();
            System.out.println("EchoServer.main ServerBootstrap配置启动完成");

            // Wait until the server socket is closed.
            f.channel().closeFuture().sync();
            System.out.println("EchoServer.main end");
        } finally {
            // Shut down all event loops to terminate all threads.
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}

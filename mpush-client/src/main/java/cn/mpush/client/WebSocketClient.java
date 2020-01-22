package cn.mpush.client;


import cn.mpush.client.handler.WebSocketClientHandler;
import cn.mpush.client.monitor.ConnectionWatchdog;
import cn.mpush.client.monitor.WebSocketConnectionWatchdog;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.util.HashedWheelTimer;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;

/**
 * Sends one message when a connection is open and echoes back any received
 * data to the server.  Simply put, the echo client initiates the ping-pong
 * traffic between the echo client and server by sending the first message to
 * the server.
 */
public final class WebSocketClient {

    public static void main(String[] args) throws Exception {
        connect("ws://127.0.0.1:8007/wsServer");
    }

    public static void connect(URI websocketUri) throws Exception {
        String scheme = websocketUri.getScheme();
        final String host = websocketUri.getHost();
        final int port = websocketUri.getPort();
        if (!"ws".equalsIgnoreCase(scheme) && !"wss".equalsIgnoreCase(scheme)) {
            System.err.println("Only WS(S) is supported.");
            return;
        }
        EventLoopGroup group = new NioEventLoopGroup();
        try {
            // websocket 支持
            Bootstrap b = new Bootstrap();
            final WebSocketClientHandler handler = new WebSocketClientHandler(
                    WebSocketClientHandshakerFactory.newHandshaker(websocketUri, WebSocketVersion.V13, null, false, new DefaultHttpHeaders()));
            HashedWheelTimer timer = new HashedWheelTimer();
            // 长连接监视
            WebSocketConnectionWatchdog connectionWatchdog = new WebSocketConnectionWatchdog(10, timer, websocketUri, handler, b);
            b.group(group).channel(NioSocketChannel.class).handler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel ch) {
                    ChannelPipeline p = ch.pipeline();
                    p.addLast(connectionWatchdog,
                            new HttpClientCodec(),
                            new HttpObjectAggregator(8192),
                            handler);
                }
            });
            Channel channel = b.connect(host, port).sync().channel();
            handler.handshakeFuture().sync();
            BufferedReader console = new BufferedReader(new InputStreamReader(System.in));
            while (true) {
                String msg = console.readLine();
                if (msg == null) {
                    break;
                } else if ("bye".equals(msg.toLowerCase())) {
                    channel.writeAndFlush(new CloseWebSocketFrame());
                    channel.closeFuture().sync();
                    break;
                } else if ("ping".equals(msg.toLowerCase())) {
                    WebSocketFrame frame = new PingWebSocketFrame();
                    channel.writeAndFlush(frame);
                } else if ("pong".equals(msg.toLowerCase())) {
                    WebSocketFrame frame = new PongWebSocketFrame();
                    channel.writeAndFlush(frame);
                } else {
                    WebSocketFrame frame = new TextWebSocketFrame(msg);
                    channel.writeAndFlush(frame);
                }
            }
        } finally {
            group.shutdownGracefully();
        }
    }

    public static void connect(String websocketUrl) throws Exception {
        URI uri = new URI(websocketUrl);
        connect(uri);
    }
}

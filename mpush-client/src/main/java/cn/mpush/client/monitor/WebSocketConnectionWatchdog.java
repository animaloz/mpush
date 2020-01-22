package cn.mpush.client.monitor;

import cn.mpush.client.WebSocketClient;
import cn.mpush.client.handler.WebSocketClientHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshakerFactory;
import io.netty.handler.codec.http.websocketx.WebSocketVersion;
import io.netty.util.Timeout;
import io.netty.util.Timer;
import io.netty.util.TimerTask;

import java.net.URI;

@ChannelHandler.Sharable
public class WebSocketConnectionWatchdog extends ConnectionWatchdog{
    //地址 端口号
    private  URI uri;

    private WebSocketClientHandler websocketChannelHandler;
    private Bootstrap bootstrap;

    public WebSocketConnectionWatchdog(int maxTime, Timer timer, URI uri, WebSocketClientHandler websocketChannelHandler, Bootstrap bootstrap) {
        super(maxTime, timer);
        this.uri = uri;
        this.websocketChannelHandler = websocketChannelHandler;
        this.bootstrap = bootstrap;
    }

    @Override
    public void run(Timeout timeout) throws Exception {
        final WebSocketClientHandler handler = new WebSocketClientHandler(
                WebSocketClientHandshakerFactory.newHandshaker(uri, WebSocketVersion.V13, null, false, new DefaultHttpHeaders()));
        ChannelFuture future = bootstrap.handler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) throws Exception {

                ch.pipeline().addLast(
                        new HttpClientCodec(),
                        new HttpObjectAggregator(8192),
                        handler);
            }
        }).connect(uri.getHost(), uri.getPort()).sync();
        handler.handshakeFuture().sync();
        future.addListener((ChannelFutureListener) channelFuture -> {
            boolean success = channelFuture.isSuccess();
            System.out.println(success);
            if(!success){
                ChannelPipeline pipeline = channelFuture.channel().pipeline();
                pipeline.fireChannelInactive();
            }
        });
    }
}

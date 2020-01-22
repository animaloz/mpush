package cn.mpush.core.handler;

import cn.mpush.core.channels.ChannelMap;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.*;

import java.time.LocalDateTime;
import java.util.Arrays;

//处理文本协议数据，处理TextWebSocketFrame类型的数据，websocket专门处理文本的frame就是TextWebSocketFrame
public class WebSocketFrameHandler extends SimpleChannelInboundHandler<WebSocketFrame> {
    private String name;

    public WebSocketFrameHandler(String name) {
        this.name = name;
    }

    //每个channel都有一个唯一的id值
    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        //打印出channel唯一值，asLongText方法是channel的id的全名
        System.out.println(name + " handlerAdded：" + ctx.channel().id().asLongText());
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        System.out.println(name + " handlerRemoved：" + ctx.channel().id().asLongText());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        String message = cause.getMessage();
        System.err.println(name + "异常发生" + message);
        ctx.disconnect();
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        System.out.println(name + " 新连接 " + ctx.channel().remoteAddress());
        ChannelMap.put(ctx.channel());
        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        System.out.println(name + " 断开连接 " + ctx.channel().remoteAddress());
        ChannelMap.clear(ctx.channel());
        super.channelInactive(ctx);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, WebSocketFrame msg) throws Exception {
        if (msg instanceof TextWebSocketFrame) {
            TextWebSocketFrame textFrame = (TextWebSocketFrame) msg;
            String text = textFrame.text();
            System.out.println("WebSocket Server received message: " + text);
            ChannelMap.put(ctx.channel());
            ChannelMap.sendMessage(text,"发给指定用户" + ctx.channel().id().asLongText());
        } else if (msg instanceof PingWebSocketFrame) {
            System.out.println("WebSocket Server received ping");
            ChannelMap.put(ctx.channel());
        } else if (msg instanceof PongWebSocketFrame) {
            System.out.println("WebSocket Server received pong");
            ChannelMap.put(ctx.channel());
        } else if (msg instanceof CloseWebSocketFrame) {
            System.out.println("WebSocket Server received closing");
            ChannelMap.clear(ctx.channel());
            ctx.channel().close();
        }
    }
}
package cn.mpush.core.handler;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;

public abstract class CustomHeartbeatHandler extends SimpleChannelInboundHandler<Object> {
    private String name;
    private int heartbeatCount = 0;

    public CustomHeartbeatHandler(String name) {
        this.name = name;
    }

    protected void sendPingMsg(ChannelHandlerContext context) {
        WebSocketFrame frame = new PingWebSocketFrame(Unpooled.wrappedBuffer(new byte[]{8, 1, 8, 1}));
        context.channel().writeAndFlush(frame);
        heartbeatCount++;
        System.out.println(name + " sent ping msg to " + context.channel().remoteAddress() + ", count: " + heartbeatCount);
    }

    protected void sendPongMsg(ChannelHandlerContext context) {
        WebSocketFrame frame = new PongWebSocketFrame(Unpooled.wrappedBuffer(new byte[]{8, 1, 8, 1}));
        context.channel().writeAndFlush(frame);
        heartbeatCount++;
        System.out.println(name + " sent pong msg to " + context.channel().remoteAddress() + ", count: " + heartbeatCount);
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        // IdleStateHandler 所产生的 IdleStateEvent 的处理逻辑.
        System.out.println("******************************************");
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent e = (IdleStateEvent) evt;
            IdleState state = e.state();
            switch (state) {
                case WRITER_IDLE:
                    handleWriterIdle(ctx);
                    break;
                case READER_IDLE:
                    handleReaderIdle(ctx);
                    break;
                case ALL_IDLE:
                default:
                    break;
            }
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        System.err.println("---" + ctx.channel().remoteAddress() + " is active---");
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        System.err.println("---" + ctx.channel().remoteAddress() + " is inactive---");
    }

    protected void handleWriterIdle(ChannelHandlerContext ctx) {
        System.out.println("---AUTO_WRITER_IDLE---");
        sendPingMsg(ctx);
    }

    protected void handleReaderIdle(ChannelHandlerContext ctx) {
        System.out.println("---AUTO_READER_IDLE---");
        sendPingMsg(ctx);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext context, Object o) throws Exception {
        if (o instanceof PingWebSocketFrame) {
//            不发送消息，进行ping pong成功时间更新
            sendPongMsg(context);
        } else if (o instanceof  PongWebSocketFrame) {
            System.out.println(name + " get pong msg from " + context.channel().remoteAddress());
        }
    }

    protected abstract void getPingMsg(ChannelHandlerContext context, ByteBuf byteBuf);
}
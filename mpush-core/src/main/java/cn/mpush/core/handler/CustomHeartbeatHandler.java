package cn.mpush.core.handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.CharsetUtil;

import java.nio.charset.Charset;

public abstract class CustomHeartbeatHandler extends SimpleChannelInboundHandler<ByteBuf> {
    protected static final byte PING_MSG = 1;
    protected static final byte PONG_MSG = 2;
    protected static final byte CUSTOM_MSG = 3;
    protected String name;
    protected int msgLength;
    private int heartbeatCount = 0;

    public CustomHeartbeatHandler(String name, int msgLength) {
        this.name = name;
        this.msgLength = msgLength;
    }

    protected void sendPingMsg(ChannelHandlerContext context) {
        ByteBuf buf = context.alloc().buffer(5);
        buf.writeInt(5);
        buf.writeByte(PING_MSG);
        buf.retain();
        context.writeAndFlush(buf);
        heartbeatCount++;
        System.out.println(name + " sent ping msg to " + context.channel().remoteAddress() + ", count: " + heartbeatCount);
    }

    protected void sendPongMsg(ChannelHandlerContext context) {
        ByteBuf buf = context.alloc().buffer(5);
        buf.writeInt(5);
        buf.writeByte(PONG_MSG);
        context.channel().writeAndFlush(buf);
        heartbeatCount++;
        System.out.println(name + " sent pong msg to " + context.channel().remoteAddress() + ", count: " + heartbeatCount);
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        // IdleStateHandler 所产生的 IdleStateEvent 的处理逻辑.
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent e = (IdleStateEvent) evt;
            if (e.state() == IdleState.WRITER_IDLE) {
                handleWriterIdle(ctx);
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

    private void handleWriterIdle(ChannelHandlerContext ctx) {
        System.out.println("---AUTO_WRITER_IDLE---");
        sendPingMsg(ctx);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext context, ByteBuf byteBuf) throws Exception {
        byte msgType = byteBuf.getByte(msgLength);
        if (msgType == PING_MSG) {
//            不发送消息，进行ping pong成功时间更新
            getPingMsg(context, byteBuf);
        } else if (msgType == PONG_MSG) {
            System.out.println(name + " get pong msg from " + context.channel().remoteAddress());
        } else if (msgType == CUSTOM_MSG) {
            String msg = byteBuf.toString(CharsetUtil.UTF_8);
            System.out.println(name + " get custom msg "+ msg +" from " + context.channel().remoteAddress());
        }
    }

    protected abstract void getPingMsg(ChannelHandlerContext context, ByteBuf byteBuf);
}
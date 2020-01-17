package cn.mpush.client.handler;

import cn.mpush.core.handler.CustomHeartbeatHandler;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

public class ClientCustomHeartbeatHandler extends CustomHeartbeatHandler {

    public ClientCustomHeartbeatHandler(String name, int msgLength) {
        super(name, msgLength);
    }

    @Override
    protected void getPingMsg(ChannelHandlerContext context, ByteBuf byteBuf) {
        sendPongMsg(context);
    }

    @Override
    protected void handleWriterIdle(ChannelHandlerContext ctx) {
        super.handleWriterIdle(ctx);
        sendPingMsg(ctx);
    }
}
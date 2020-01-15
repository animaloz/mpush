package cn.mpush.core.handler;

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

}
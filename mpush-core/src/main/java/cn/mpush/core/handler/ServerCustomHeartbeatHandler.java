package cn.mpush.core.handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;

public class ServerCustomHeartbeatHandler extends CustomHeartbeatHandler {
    public ServerCustomHeartbeatHandler(String name, int msgLength) {
        super(name, msgLength);
    }

    @Override
    protected void getPingMsg(ChannelHandlerContext context, ByteBuf byteBuf) {
        // 心跳更新
    }
}
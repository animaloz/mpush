package cn.mpush.server.handler;

import cn.mpush.core.handler.CustomHeartbeatHandler;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

public class ServerCustomHeartbeatHandler extends CustomHeartbeatHandler {
    public ServerCustomHeartbeatHandler(String name) {
        super(name);
    }

    @Override
    protected void getPingMsg(ChannelHandlerContext context, ByteBuf byteBuf) {
        // 心跳更新
        context.fireChannelReadComplete();
    }

    @Override
    protected void handleReaderIdle(ChannelHandlerContext ctx) {
        super.handleReaderIdle(ctx);
    }
}
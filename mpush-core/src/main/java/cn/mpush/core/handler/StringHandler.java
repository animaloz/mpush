package cn.mpush.core.handler;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.concurrent.EventExecutorGroup;

/**
 * @author guanxingya[OF3449]
 * company qianmi.com
 * Date 2020/1/19 17:21
 */
public class StringHandler extends SimpleChannelInboundHandler<String> {
    private String name;
    public StringHandler(String name) {
        this.name = name;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
        System.out.println(name + " received msg " + msg);
    }
}

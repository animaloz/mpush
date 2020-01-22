package cn.mpush.client.monitor;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.Timer;
import io.netty.util.TimerTask;
import lombok.Getter;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Getter
@ChannelHandler.Sharable
public abstract class ConnectionWatchdog extends ChannelInboundHandlerAdapter implements TimerTask {
    //尝试次数
    private AtomicInteger attempts = new AtomicInteger();
    //最大尝试次数
    private int maxTimes;
    //执行重连任务的调度器
    private Timer timer;

    public ConnectionWatchdog(int maxTimes, Timer timer) {
        this.maxTimes = maxTimes;
        this.timer = timer;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("当前channel以及激活,尝试次数重置为0");
        attempts.set(0);
        ctx.fireChannelActive();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("当前channel关闭");
        if (attempts.getAndIncrement() < 12) {
            int current = attempts.get();
            int timeouts = 1 << current;
            System.out.println("正在尝试重新建立连接:" + "第" + current + "次");
            timer.newTimeout(this, timeouts, TimeUnit.SECONDS);
        }
        ctx.fireChannelInactive();
    }
}

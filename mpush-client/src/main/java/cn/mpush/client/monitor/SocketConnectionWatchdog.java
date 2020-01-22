package cn.mpush.client.monitor;

import io.netty.channel.ChannelHandler;
import io.netty.util.Timeout;
import io.netty.util.Timer;

@ChannelHandler.Sharable
public class SocketConnectionWatchdog extends ConnectionWatchdog {
    //地址 端口号
    private String address;
    private int port;

    public SocketConnectionWatchdog(int maxTime, Timer timer, String address, int port) {
        super(maxTime, timer);
        this.address = address;
        this.port = port;
    }

    @Override
    public void run(Timeout timeout) throws Exception {

    }
}

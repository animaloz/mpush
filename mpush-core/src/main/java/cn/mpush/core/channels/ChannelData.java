package cn.mpush.core.channels;

import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import lombok.Data;

import java.util.Objects;

/**
 * @author guanxingya[OF3449]
 * company qianmi.com
 * Date 2020/1/20 17:47
 */
@Data
public class ChannelData {
    /**
     * channel 唯一标识符
     */
    private String channelId;
    /**
     * channel
     */
    private Channel channel;
    /**
     * 上次通信时间
     * the current time in milliseconds
     * System.currentTimeMillis()
     */
    private long lastCommunicationTime;

    public void sendMsg(WebSocketFrame frame) {
        Channel channel = this.getChannel();
        if (Objects.nonNull(channel) && channel.isActive() && channel.isOpen()) {
            channel.writeAndFlush(frame);
        }
    }
}

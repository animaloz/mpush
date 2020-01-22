package cn.mpush.core.channels;

import io.netty.channel.Channel;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.util.concurrent.GlobalEventExecutor;
import lombok.experimental.UtilityClass;
import lombok.extern.java.Log;
import lombok.extern.log4j.Log4j;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 保存 channel数据
 *
 * @author guanxingya[OF3449]
 * company qianmi.com
 * Date 2020/1/21 10:18
 */
@Slf4j
@UtilityClass
public class ChannelMap {

    private static ConcurrentHashMap<String, ChannelData> channelDataMap = new ConcurrentHashMap<>(2048);
    private static ChannelGroup channelGroup = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

    public static void put(Channel channel) {
        channelGroup.add(channel);
        String channelId = channel.id().asLongText();
        ChannelData channelData = get(channelId);
        if (Objects.isNull(channelData)) {
            channelData = new ChannelData();
            channelData.setChannelId(channelId);
            channelData.setChannel(channel);
            channelData.setLastCommunicationTime(System.currentTimeMillis());
            put(channelData);
        } else {
            channelData.setLastCommunicationTime(System.currentTimeMillis());
        }
        System.out.println(channelGroup.size());
    }

    private static void put(ChannelData channelData) {
        Channel channel = channelData.getChannel();
        if (Objects.nonNull(channel)) {
            channelDataMap.put(channel.id().asLongText(), channelData);
        }
    }

    public static ChannelData get(String channelId) {
        return channelDataMap.get(channelId);
    }

    public static void clear(Channel channel) {
        String channelId = channel.id().asLongText();
        channelDataMap.remove(channelId);
        channelGroup.remove(channel);
    }

    public static void sendMessage(String channelId, String msg) {
        Validate.validState(StringUtils.isNotBlank(channelId), "通道Id不能为空");
        Validate.validState(StringUtils.isNotBlank(msg), "消息不能为空");
        ChannelData channelData = get(channelId);
        Validate.validState(Objects.nonNull(channelData), "%s无当前channel", channelId);
        TextWebSocketFrame frame = new TextWebSocketFrame(msg);
        channelData.sendMsg(frame);
    }

    public static void sendMessage(String msg) {
        TextWebSocketFrame frame = new TextWebSocketFrame(msg);
        channelGroup.writeAndFlush(frame);
    }

    public static void sendMessage(List<String> channelIdList, String msg) {
        TextWebSocketFrame frame = new TextWebSocketFrame(msg);
        channelGroup.writeAndFlush(frame, channel -> channelIdList.contains(channel.id().asLongText()));
    }

}

package net.kaaass.zerotierfix.events;

import lombok.Value;

/**
 * 应答服务是否运行事件。
 */
@Value
public class IsServiceRunningReplyEvent {

    /**
     * 服务是否正在运行。
     */
    boolean running;
}

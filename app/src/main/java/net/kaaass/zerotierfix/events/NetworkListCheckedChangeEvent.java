package net.kaaass.zerotierfix.events;

import net.kaaass.zerotierfix.model.dto.NetworkSummary;

import lombok.Value;

/**
 * 网络列表开关变更事件。用于在后台线程控制 ZT 服务的启停。
 */
@Value
public class NetworkListCheckedChangeEvent {

    /**
     * 被操作的网络概要，不能为空。
     */
    NetworkSummary selectedNetwork;

    /**
     * 开关的新状态。
     */
    boolean checked;
}

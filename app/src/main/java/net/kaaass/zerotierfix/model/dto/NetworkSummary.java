package net.kaaass.zerotierfix.model.dto;

import lombok.Value;

/**
 * 网络概要信息，用于网络列表和跨层传递。
 */
@Value
public class NetworkSummary {

    /**
     * 网络 ID（十进制）。
     */
    long networkId;

    /**
     * 网络 ID 的 16 进制字符串，保持用户输入时的格式。
     */
    String networkIdStr;

    /**
     * 网络名称；可能为 {@code null} 或空字符串。
     */
    String networkName;

    /**
     * 是否为最近一次激活的网络。
     */
    boolean lastActivated;
}

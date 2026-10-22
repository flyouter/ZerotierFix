package net.kaaass.zerotierfix.events;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * VPN 错误事件
 */
@Data
@AllArgsConstructor
public class VPNErrorEvent {

    /**
     * 错误信息，不能为空。
     */
    private final String message;
}

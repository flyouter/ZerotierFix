package net.kaaass.zerotierfix.events;

import net.kaaass.zerotierfix.model.dto.MoonOrbitDto;

import java.util.List;

import lombok.Value;

/**
 * Moon 入轨事件。
 *
 * <p>事件发生时应实际处理入轨动作。</p>
 *
 * @author kaaass
 */
@Value
public class OrbitMoonEvent {

    /**
     * 待入轨 Moon 的信息，不能为空。
     */
    List<MoonOrbitDto> moonOrbits;
}

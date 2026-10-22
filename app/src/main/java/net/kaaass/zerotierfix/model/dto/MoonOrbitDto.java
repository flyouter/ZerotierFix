package net.kaaass.zerotierfix.model.dto;

import lombok.Value;

/**
 * Moon 入轨信息，用于界面展示与跨层传递。
 */
@Value
public class MoonOrbitDto {

    /**
     * Moon 地址（world ID）。
     */
    long moonWorldId;

    /**
     * Moon 种子节点地址。
     */
    long moonSeed;

    /**
     * 是否从本地文件导入。
     */
    boolean fromFile;

    /**
     * 本地是否已经存在对应的 moon 缓存文件。
     */
    boolean cacheFile;
}

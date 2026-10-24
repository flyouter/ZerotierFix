package net.kaaass.zerotierfix.features.join

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 图片采样倍数计算。
 */
class QrImageSamplingTest {

    @Test
    fun smallImageIsNotSampled() {
        assertEquals(1, calculateSampleSize(480, 480))
    }

    @Test
    fun mediumImageIsSampledByTwo() {
        assertEquals(2, calculateSampleSize(4032, 3024))
    }

    @Test
    fun largeImageIsSampledByFour() {
        assertEquals(4, calculateSampleSize(8000, 6000))
    }

    @Test
    fun portraitImageUsesLongestEdge() {
        assertEquals(4, calculateSampleSize(6000, 8000))
    }

    @Test
    fun hugeImageIsSampledByEight() {
        assertEquals(8, calculateSampleSize(12000, 8000))
    }

    @Test
    fun invalidSizeFallsBackToOne() {
        assertEquals(1, calculateSampleSize(0, 100))
        assertEquals(1, calculateSampleSize(100, -1))
        assertEquals(1, calculateSampleSize(100, 100, 0))
    }
}

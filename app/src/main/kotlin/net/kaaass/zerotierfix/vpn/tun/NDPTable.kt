package net.kaaass.zerotierfix.vpn.tun

import android.util.Log

import net.kaaass.zerotierfix.util.IPPacketUtils

import java.net.InetAddress
import java.nio.ByteBuffer
import java.util.HashMap

/**
 * NDP 表，维护 MAC 与 IPv6 地址映射并在超时后清理。
 */
class NDPTable {

    private val entriesMap = HashMap<Long, NDPEntry>()
    private val inetAddressToMacAddress = HashMap<InetAddress, Long>()
    private val ipEntriesMap = HashMap<InetAddress, NDPEntry>()
    private val macAddressToInetAddress = HashMap<Long, InetAddress>()

    private val timeoutThread: Thread = object : Thread("NDP Timeout Thread") {
        override fun run() {
            while (!isInterrupted) {
                try {
                    for (ndpEntry in HashMap(this@NDPTable.entriesMap).values) {
                        if (ndpEntry.time + ENTRY_TIMEOUT < System.currentTimeMillis()) {
                            synchronized(this@NDPTable.macAddressToInetAddress) {
                                this@NDPTable.macAddressToInetAddress.remove(ndpEntry.mac)
                            }
                            synchronized(this@NDPTable.inetAddressToMacAddress) {
                                this@NDPTable.inetAddressToMacAddress.remove(ndpEntry.address)
                            }
                            synchronized(this@NDPTable.entriesMap) {
                                this@NDPTable.entriesMap.remove(ndpEntry.mac)
                            }
                            synchronized(this@NDPTable.ipEntriesMap) {
                                this@NDPTable.ipEntriesMap.remove(ndpEntry.address)
                            }
                        }
                    }
                    Thread.sleep(1000)
                } catch (e: Exception) {
                    Log.d(TAG, e.toString())
                    return
                }
            }
        }
    }

    init {
        this.timeoutThread.start()
    }

    /**
     * 停止超时清理线程。
     */
    fun stop() {
        try {
            this.timeoutThread.interrupt()
            this.timeoutThread.join()
        } catch (ignored: InterruptedException) {
        }
    }

    /**
     * 记录地址与 MAC 的映射。
     *
     * @param inetAddress IPv6 地址
     * @param mac MAC 地址
     */
    fun setAddress(inetAddress: InetAddress, mac: Long) {
        synchronized(this.inetAddressToMacAddress) {
            this.inetAddressToMacAddress[inetAddress] = mac
        }
        synchronized(this.macAddressToInetAddress) {
            this.macAddressToInetAddress[mac] = inetAddress
        }
        val ndpEntry = NDPEntry(mac, inetAddress)
        synchronized(this.entriesMap) {
            this.entriesMap[mac] = ndpEntry
        }
        synchronized(this.ipEntriesMap) {
            this.ipEntriesMap[inetAddress] = ndpEntry
        }
    }

    /**
     * 判断是否存在地址对应的 MAC。
     *
     * @param inetAddress IPv6 地址
     * @return 存在返回 `true`
     */
    fun hasMacForAddress(inetAddress: InetAddress): Boolean =
        synchronized(this.inetAddressToMacAddress) {
            this.inetAddressToMacAddress.containsKey(inetAddress)
        }

    /**
     * 判断是否存在 MAC 对应的地址。
     *
     * @param mac MAC 地址
     * @return 存在返回 `true`
     */
    fun hasAddressForMac(mac: Long): Boolean =
        synchronized(this.macAddressToInetAddress) {
            this.macAddressToInetAddress.containsKey(mac)
        }

    /**
     * 查询地址对应的 MAC。
     *
     * @param inetAddress IPv6 地址
     * @return MAC 地址；不存在时返回 -1
     */
    fun getMacForAddress(inetAddress: InetAddress): Long {
        synchronized(this.inetAddressToMacAddress) {
            if (!this.inetAddressToMacAddress.containsKey(inetAddress)) {
                return -1
            }
            val longValue = this.inetAddressToMacAddress[inetAddress]!!
            updateNDPEntryTime(longValue)
            return longValue
        }
    }

    /**
     * 查询 MAC 对应的地址。
     *
     * @param mac MAC 地址
     * @return IPv6 地址；不存在时返回 `null`
     */
    fun getAddressForMac(mac: Long): InetAddress? {
        synchronized(this.macAddressToInetAddress) {
            if (!this.macAddressToInetAddress.containsKey(mac)) {
                return null
            }
            val inetAddress = this.macAddressToInetAddress[mac]!!
            updateNDPEntryTime(inetAddress)
            return inetAddress
        }
    }

    private fun updateNDPEntryTime(inetAddress: InetAddress) {
        synchronized(this.ipEntriesMap) {
            this.ipEntriesMap[inetAddress]?.updateTime()
        }
    }

    private fun updateNDPEntryTime(mac: Long) {
        synchronized(this.entriesMap) {
            this.entriesMap[mac]?.updateTime()
        }
    }

    /**
     * 构造邻居请求（NS）报文。
     *
     * @param inetAddress 源 IPv6 地址
     * @param inetAddress2 目标 IPv6 地址
     * @param mac 源 MAC
     * @return NS 报文
     */
    fun getNeighborSolicitationPacket(
        inetAddress: InetAddress,
        inetAddress2: InetAddress,
        mac: Long
    ): ByteArray {
        val packet = ByteArray(72)
        System.arraycopy(inetAddress.address, 0, packet, 0, 16)
        System.arraycopy(inetAddress2.address, 0, packet, 16, 16)
        System.arraycopy(ByteBuffer.allocate(4).putInt(32).array(), 0, packet, 32, 4)
        packet[39] = 58
        packet[40] = -121
        System.arraycopy(inetAddress2.address, 0, packet, 48, 16)
        val macBytes = ByteBuffer.allocate(8).putLong(mac).array()
        packet[64] = 1
        packet[65] = 1
        System.arraycopy(macBytes, 2, packet, 66, 6)
        System.arraycopy(
            ByteBuffer.allocate(2)
                .putShort(IPPacketUtils.calculateChecksum(packet, 0, 0, 72).toInt().toShort())
                .array(),
            0, packet, 42, 2
        )
        for (i in 0 until 40) {
            packet[i] = 0
        }
        packet[0] = 96
        System.arraycopy(ByteBuffer.allocate(2).putShort(32.toShort()).array(), 0, packet, 4, 2)
        packet[6] = 58
        packet[7] = -1
        System.arraycopy(inetAddress.address, 0, packet, 8, 16)
        System.arraycopy(inetAddress2.address, 0, packet, 24, 16)
        return packet
    }

    companion object {
        /** 日志标签。 */
        const val TAG = "NDPTable"

        private const val ENTRY_TIMEOUT = 120000L
    }
}

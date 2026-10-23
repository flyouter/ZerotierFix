package net.kaaass.zerotierfix.vpn.node

import android.util.Log

import com.zerotier.sdk.Node
import com.zerotier.sdk.PacketSender
import com.zerotier.sdk.ResultCode

import net.kaaass.zerotierfix.util.DebugLog
import net.kaaass.zerotierfix.vpn.engine.VpnRuntime

import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetSocketAddress
import java.net.SocketTimeoutException

/**
 * UDP 通信组件，负责 ZeroTier 虚拟链路的数据收发。
 */
class UdpCom(
    private val ztService: VpnRuntime,
    private val svrSocket: DatagramSocket?
) : PacketSender, Runnable {

    private lateinit var node: Node

    /**
     * 设置关联的 ZeroTier 节点。
     *
     * @param node2 节点实例
     */
    fun setNode(node2: Node) {
        this.node = node2
    }

    /**
     * 发送 SDK 请求的 UDP 数据包。
     *
     * @param j 包 ID
     * @param inetSocketAddress 目标地址
     * @param bArr 包数据
     * @param i 数据长度
     * @return 成功返回 0；失败返回 -1
     */
    override fun onSendPacketRequested(
        j: Long,
        inetSocketAddress: InetSocketAddress,
        bArr: ByteArray,
        i: Int
    ): Int {
        val socket = this.svrSocket
        if (socket == null) {
            Log.e(TAG, "Attempted to send packet on a null socket")
            return -1
        }
        try {
            val datagramPacket = DatagramPacket(bArr, bArr.size, inetSocketAddress)
            DebugLog.d(
                TAG,
                "onSendPacketRequested: Sent " + datagramPacket.length +
                    " bytes to " + inetSocketAddress.toString()
            )
            socket.send(datagramPacket)
            return 0
        } catch (unused: Exception) {
            return -1
        }
    }

    override fun run() {
        Log.d(TAG, "UDP Listen Thread Started.")
        try {
            val jArr = LongArray(1)
            val bArr = ByteArray(16384)
            while (!Thread.interrupted()) {
                jArr[0] = 0
                val datagramPacket = DatagramPacket(bArr, 16384)
                try {
                    this.svrSocket!!.receive(datagramPacket)
                    if (datagramPacket.length > 0) {
                        val bArr2 = ByteArray(datagramPacket.length)
                        System.arraycopy(
                            datagramPacket.data, 0, bArr2, 0, datagramPacket.length
                        )
                        DebugLog.d(
                            TAG,
                            "Got " + datagramPacket.length + " Bytes From: " +
                                datagramPacket.address.toString() + ":" + datagramPacket.port
                        )
                        val processWirePacket = this.node.processWirePacket(
                            System.currentTimeMillis(),
                            -1,
                            InetSocketAddress(datagramPacket.address, datagramPacket.port),
                            bArr2,
                            jArr
                        )
                        if (processWirePacket != ResultCode.RESULT_OK) {
                            Log.e(TAG, "processWirePacket returned: " + processWirePacket.toString())
                            this.ztService.shutdown()
                        }
                        this.ztService.setNextBackgroundTaskDeadline(jArr[0])
                    }
                } catch (ignored: SocketTimeoutException) {
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        Log.d(TAG, "UDP Listen Thread Ended.")
    }

    companion object {
        private const val TAG = "UdpCom"
    }
}

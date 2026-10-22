package net.kaaass.zerotierfix.util;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Locale;

/**
 * 字符串处理工具类
 *
 * @author kaaass
 */
public class StringUtils {

    private static final String VERSION_FORMAT = "%d.%d.%d";

    /**
     * 将版本号转为可读字符串
     *
     * @param major    主版本号
     * @param minor    次版本号
     * @param revision 修订版本号
     * @return 可读字符串
     */
    public static String versionString(int major, int minor, int revision) {
        return String.format(Locale.ROOT, VERSION_FORMAT, major, minor, revision);
    }

    /**
     * 将网络 ID 转为 16 位 16 进制字符串
     *
     * @param networkId 网络 ID
     * @return 16 位 16 进制字符串
     */
    public static String networkIdToString(long networkId) {
        return String.format(Locale.ROOT, "%016x", networkId);
    }

    /**
     * 将 MAC 地址转为 XX:XX:XX:XX:XX:XX 格式
     *
     * @param mac MAC 地址
     * @return 格式化后的 MAC 地址
     */
    public static String macAddressToString(long mac) {
        int[] macChars = new int[6];
        for (int i = 0; i < 6; i++) {
            macChars[i] = (int) (mac % 256);
            mac >>= 8;
        }
        return String.format(Locale.ROOT, "%02x:%02x:%02x:%02x:%02x:%02x",
                macChars[5], macChars[4], macChars[3], macChars[2], macChars[1], macChars[0]);
    }

    /**
     * 将 16 进制字符串转换为字符数组
     *
     * @param hex 16 进制字符串
     * @return 字符数组
     */
    public static byte[] hexStringToBytes(String hex) {
        int length = hex.length();
        if (length % 2 != 0) {
            throw new RuntimeException("String length must be even");
        }
        var result = new byte[length / 2];
        for (int i = 0; i < length; i += 2) {
            var highDigit = Character.digit(hex.charAt(i), 16);
            var lowDigit = Character.digit(hex.charAt(i + 1), 16);
            result[i / 2] = (byte) ((highDigit << 4) + lowDigit);
        }
        return result;
    }

    /**
     * 将 InetSocketAddress 转为 IP:Port (IPv6 则是 [IP]:Port) 格式的字符串
     */
    public static String toString(InetSocketAddress address) {
        InetAddress inetAddress = address.getAddress();
        int port = address.getPort();

        // 将 IP 地址转为字符串
        String ipString = inetAddress.getHostAddress();
        if (inetAddress instanceof Inet6Address) {
            ipString = "[" + ipString + "]";
        }

        return ipString + ":" + port;
    }
}

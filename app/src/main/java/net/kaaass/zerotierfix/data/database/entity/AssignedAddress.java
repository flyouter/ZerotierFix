package net.kaaass.zerotierfix.data.database.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverter;
import androidx.room.TypeConverters;

@Entity(tableName = "ASSIGNED_ADDRESS")
public class AssignedAddress {

    /**
     * 自增主键。
     */
    @PrimaryKey
    @ColumnInfo(name = "_id")
    private Long id;

    /**
     * 所属网络 ID（十进制）。
     */
    @ColumnInfo(name = "NETWORK_ID")
    private long networkId;

    /**
     * 地址类型。
     */
    @TypeConverters(AddressTypeConverter.class)
    @ColumnInfo(name = "TYPE")
    private AddressType type;

    /**
     * 地址的原始字节。
     */
    @ColumnInfo(name = "ADDRESS_BYTES")
    private byte[] addressBytes;

    /**
     * 地址的可读字符串。
     */
    @ColumnInfo(name = "ADDRESS_STRING")
    private String addressString;

    /**
     * 地址前缀长度。
     */
    @ColumnInfo(name = "PREFIX")
    private short prefix;

    public AssignedAddress(Long id, long networkId, AddressType type, byte[] addressBytes,
            String addressString, short prefix) {
        this.id = id;
        this.networkId = networkId;
        this.type = type;
        this.addressBytes = addressBytes;
        this.addressString = addressString;
        this.prefix = prefix;
    }

    public AssignedAddress() {
    }

    public Long getId() {
        return this.id;
    }

    public void setId(Long l) {
        this.id = l;
    }

    public AddressType getType() {
        return this.type;
    }

    public void setType(AddressType addressType) {
        this.type = addressType;
    }

    public short getPrefix() {
        return this.prefix;
    }

    public void setPrefix(short s) {
        this.prefix = s;
    }

    public long getNetworkId() {
        return this.networkId;
    }

    public void setNetworkId(long j) {
        this.networkId = j;
    }

    public byte[] getAddressBytes() {
        return this.addressBytes;
    }

    public void setAddressBytes(byte[] bArr) {
        this.addressBytes = bArr;
    }

    public String getAddressString() {
        return this.addressString;
    }

    public void setAddressString(String str) {
        this.addressString = str;
    }

    public enum AddressType {

        /**
         * 未知地址类型。
         */
        UNKNOWN(0),

        /**
         * IPv4 地址。
         */
        IPV4(1),

        /**
         * IPv6 地址。
         */
        IPV6(2);

        /**
         * 持久化用的整数值。
         */
        final int id;

        AddressType(int i) {
            this.id = i;
        }

        public String toString() {
            int i = this.id;
            if (i != 1) {
                return i != 2 ? "Unknown" : "IPv6";
            }
            return "IPv4";
        }
    }

    public static class AddressTypeConverter {
        @TypeConverter
        public static AddressType convertToEntityProperty(Integer num) {
            if (num == null) {
                return null;
            }
            AddressType[] values = AddressType.values();
            for (AddressType addressType : values) {
                if (addressType.id == num) {
                    return addressType;
                }
            }
            return AddressType.UNKNOWN;
        }

        @TypeConverter
        public static Integer convertToDatabaseValue(AddressType addressType) {
            if (addressType == null) {
                return null;
            }
            return addressType.id;
        }
    }
}

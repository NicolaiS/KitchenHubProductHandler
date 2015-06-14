package kr.kaist.resl.kitchenhubproducthandler.util;

import android.util.Log;

import kr.kaist.resl.kitchenhubproducthandler.model.DetectedProduct;

/**
 * Created by NicolaiSonne on 21-04-2015.
 * <p/>
 * EPC oriented operations
 */
public class EPCUtil {

    public static final byte HEADER_SGTIN96 = 0x30;

    /**
     * Decode SGTIN to DetectedProduct
     *
     * @param data        SGTIN in byte array format
     * @param containerId container of DetectedProduct
     * @return DetectProduct
     */
    public static DetectedProduct decodeEPC(byte[] data, Integer containerId) {
        if (data[0] != HEADER_SGTIN96) {
            Log.w(EPCUtil.class.getName(), "- Not a SGTIN-96 header: " + data[0]);
            return null;
        }

        byte filter = (byte) ((data[1] & 0xE0) >> 5);
        if (filter != 1) {
            Log.d(EPCUtil.class.getName(), "- Filter value: " + filter + ". Product ignored");
            return null;
        }
        byte partition = (byte) ((data[1] & 0x1C) >> 2);

        long data0 = 0;
        data0 |= ((long) data[1] & 0x03) << 42;
        data0 |= (data[7] & 0xC0) >> 6;

        for (int i = 2; i < 7; i++) {
            long temp = data[i] & 0xFF;
            int shift = 34 - ((i - 2) << 3);
            data0 |= temp << shift;
        }

        byte companyPrefixBitLength = getCompanyPrefixBitLength(partition);
        long companyPrefix = data0 >> (44 - companyPrefixBitLength);
        int itemRefNo = (int) (data0 & (int) (Math.pow(2, 44 - companyPrefixBitLength) - 1));

        long serial = ((long) data[7] & 0x3F) << 32;
        for (int i = 8; i < data.length; i++) {
            long temp = data[i] & 0xFF;
            int shift = 24 - ((i - 8) << 3);
            serial |= temp << shift;
        }

        String companyPrefixStr;
        String itemRefNoStr;
        try {
            companyPrefixStr = getCompanyPrefixString(companyPrefix, partition);
            itemRefNoStr = getItemRefNoString(itemRefNo, partition);
        } catch (Exception e) {
            return null;
        }

        Integer indicator = Integer.valueOf(itemRefNoStr.substring(0, 1));
        itemRefNoStr = itemRefNoStr.substring(1);

        Integer checksum = calculateChecksum(indicator + companyPrefixStr + itemRefNoStr);

        return new DetectedProduct(indicator, companyPrefixStr, itemRefNoStr, Long.toString(serial), checksum, containerId);
    }

    /**
     * Calculate modulo 10 checksom of GTIN
     *
     * @param gtin GTIN
     * @return Checksum
     */
    private static Integer calculateChecksum(String gtin) {
        Integer sum = 0;
        for (int i = 0; i < gtin.length(); i++) {
            Integer temp = Integer.valueOf(gtin.substring(gtin.length() - i - 1, gtin.length() - i));
            if ((i % 2) == 0) {
                sum += (temp * 3);
            } else {
                sum += (temp);
            }
        }
        return (sum % 10);
    }

    /**
     * Company prefix bit length from partition
     *
     * @param partition p
     * @return Company Prefix Bit length
     */
    private static byte getCompanyPrefixBitLength(byte partition) {
        switch (partition) {
            case 0:
                return 40;
            case 1:
                return 37;
            case 2:
                return 34;
            case 3:
                return 30;
            case 4:
                return 27;
            case 5:
                return 24;
            case 6:
                return 20;
            default:
                return -1;
        }
    }

    /**
     * Convert company prefix to string of length according to partition
     *
     * @param companyPrefix cp
     * @param partition     p
     * @return Company prefix string
     * @throws Exception
     */
    private static String getCompanyPrefixString(long companyPrefix, byte partition) throws Exception {
        String result = Long.toString(companyPrefix);
        int length;
        switch (partition) {
            case 0:
                length = 12;
                break;
            case 1:
                length = 11;
                break;
            case 2:
                length = 10;
                break;
            case 3:
                length = 9;
                break;
            case 4:
                length = 8;
                break;
            case 5:
                length = 7;
                break;
            case 6:
                length = 6;
                break;
            default:
                throw new Exception("Unknown partition: " + partition);
        }
        while (result.length() < length) result = "0" + result;
        return result;
    }

    /**
     * Convert item reference number to string of length according to partition
     *
     * @param itemRefNo irn
     * @param partition p
     * @return Item reference number string
     * @throws Exception
     */
    private static String getItemRefNoString(int itemRefNo, byte partition) throws Exception {
        String result = Integer.toString(itemRefNo);
        int length;
        switch (partition) {
            case 0:
                length = 1;
                break;
            case 1:
                length = 2;
                break;
            case 2:
                length = 3;
                break;
            case 3:
                length = 4;
                break;
            case 4:
                length = 5;
                break;
            case 5:
                length = 6;
                break;
            case 6:
                length = 7;
                break;
            default:
                throw new Exception("Unknown partition: " + partition);
        }
        while (result.length() < length) result = "0" + result;
        return result;
    }
}

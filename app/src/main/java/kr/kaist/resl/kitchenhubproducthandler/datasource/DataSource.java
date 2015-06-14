package kr.kaist.resl.kitchenhubproducthandler.datasource;

import android.content.Context;

import java.io.BufferedReader;
import java.util.List;
import java.util.Map;

import kr.kaist.resl.kitchenhubproducthandler.model.DetectedProduct;

/**
 * Created by NicolaiSonne on 22-05-2015.
 * <p/>
 * DataSource abstraction
 */
public abstract class DataSource {

    public abstract List<DetectedProduct> handleConnection(Context context, BufferedReader in, Map<Integer, Integer> dataSourceContainerMap) throws Exception;

    /**
     * Convert Hex String to byte array
     *
     * @param s Hex String
     * @return Byte array
     */
    protected static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4) + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }
}

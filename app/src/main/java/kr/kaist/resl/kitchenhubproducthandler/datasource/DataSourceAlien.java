package kr.kaist.resl.kitchenhubproducthandler.datasource;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import kr.kaist.resl.kitchenhubproducthandler.model.DetectedProduct;
import kr.kaist.resl.kitchenhubproducthandler.util.EPCUtil;

/**
 * Created by NicolaiSonne on 22-05-2015.
 */
public class DataSourceAlien extends DataSource {

    /**
     * @param context                context
     * @param in                     input
     * @param dataSourceContainerMap map of Datasouce to continers
     * @return List of detected products
     * @throws Exception
     */
    @Override
    public List<DetectedProduct> handleConnection(Context context, BufferedReader in, Map<Integer, Integer> dataSourceContainerMap) throws Exception {
        List<DetectedProduct> detectedProducts = new ArrayList<DetectedProduct>();

        Log.d(getClass().getName(), "- Received:");

        try {
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                inputLine = inputLine.trim();
                if (!inputLine.isEmpty()) {
                    Log.d(getClass().getName(), "-- " + inputLine);

                    String[] data = inputLine.split(" ");
                    byte[] epc = hexStringToByteArray(data[0]);
                    Integer ant = new Integer(data[1]);

                    if (dataSourceContainerMap.containsKey(ant)) {
                        Integer containerId = dataSourceContainerMap.get(ant);
                        DetectedProduct dProduct = EPCUtil.decodeEPC(epc, containerId);
                        if (dProduct != null) {
                            detectedProducts.add(dProduct);
                        }
                    } else {
                        Log.i(getClass().getName(), "--- Antenna " + ant + " is not linked to a container");
                    }
                } else {
                    break;
                }
            }
        } catch (SocketTimeoutException e) {
            Log.d(getClass().getName(), "- Connection finished");
        }

        return detectedProducts;
    }
}

package kr.kaist.resl.kitchenhubproducthandler.service;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import constants.KHBroadcasts;
import constants.ProviderConstants;
import constants.database.KHSchema;
import constants.database.KHSchemaDataSource;
import constants.database.KHSchemaDatasourceContainer;
import constants.database.KHSchemaProduct;
import enums.EnumDataSourceType;
import kr.kaist.resl.kitchenhubproducthandler.Content_URIs;
import kr.kaist.resl.kitchenhubproducthandler.datasource.DataSource;
import kr.kaist.resl.kitchenhubproducthandler.datasource.DataSourceAlien;
import kr.kaist.resl.kitchenhubproducthandler.model.DetectedProduct;
import models.DataSourceContainerRelation;

/**
 * Created by nicolais on 4/22/15.
 * <p/>
 * Open connection to data source
 */
public class ClientThread implements Runnable {

    private Context context;
    private Socket socket;
    private BufferedReader in = null;

    public ClientThread(Context context, Socket socket) {
        this.context = context;
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            String ip = socket.getInetAddress().getHostAddress();

            // Decode data source
            EnumDataSourceType type = getDataSourceType(context, ip);
            if (type == null) {
                Log.d(getClass().getName(), "- Data source with ip " + ip + " not recognised");
                return;
            }
            List<DataSourceContainerRelation> rel = getRelatedContainers(context, ip);

            // Link data source to containers
            Map<Integer, Integer> dataSourceContainerMap = new HashMap<Integer, Integer>();
            for (DataSourceContainerRelation rc : rel) {
                dataSourceContainerMap.put(rc.getAntenna(), rc.getContainer().getId());
            }

            // Close connection if inactive for 2 seconds
            socket.setSoTimeout(2000);

            // Decode type of container and handle connection
            DataSource ds;
            switch (type) {
                case ALIEN:
                    Log.d(getClass().getName(), "- Alien RFID reader connected");
                    ds = new DataSourceAlien();
                    break;
                default:
                    throw new Exception("- Unknown data source type");
            }
            List<DetectedProduct> dProducts = ds.handleConnection(context, in, dataSourceContainerMap);

            // Insert/update detected products
            if (!dProducts.isEmpty()) {
                Date date = new Date();
                resetExistingProducts(dProducts, date);
                insertOrUpdateProducts(dProducts, date);
            }

            // Send out broadcast
            Log.d(getClass().getName(), "- Sending broadcast: " + KHBroadcasts.PRODUCTS_UPDATED);
            context.sendBroadcast(new Intent(KHBroadcasts.PRODUCTS_UPDATED));
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (in != null) in.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Get container related to data source of specific IP
     *
     * @param context   context
     * @param ipAddress IP
     * @return List of DataSource Container Relations
     * @throws Exception
     */
    private List<DataSourceContainerRelation> getRelatedContainers(Context context, String ipAddress) throws Exception {
        List<DataSourceContainerRelation> results = new ArrayList<DataSourceContainerRelation>();

        ContentResolver resolver = context.getContentResolver();

        Cursor c1 = resolver.query(Content_URIs.CONTENT_URI_DATA_SOURCE, new String[]{KHSchema.CN_ID}, KHSchemaDataSource.CN_IP_ADDRESS + " = ?", new String[]{ipAddress}, null);
        if (!c1.moveToFirst()) return results;

        Integer readerId = c1.getInt(0);
        c1.close();

        Cursor c2 = resolver.query(Content_URIs.CONTENT_URI_DSC, new String[]{KHSchemaDatasourceContainer.CN_CONTAINER_ID, KHSchemaDatasourceContainer.CN_ANTENNA}, KHSchemaDatasourceContainer.CN_DATA_SOURCE_ID + " = ?", new String[]{Integer.toString(readerId)}, null);
        if (c2.moveToFirst()) {
            do {
                Integer containerId = c2.getInt(0);
                Integer antenna = c2.getInt(1);
                results.add(new DataSourceContainerRelation(null, -1, containerId, antenna));
            } while (c2.moveToNext());
        }
        c2.close();

        return results;
    }

    /**
     * Get type of Data source
     *
     * @param context   context
     * @param ipAddress ip
     * @return Data source type
     * @throws Exception
     */
    private EnumDataSourceType getDataSourceType(Context context, String ipAddress) throws Exception {
        ContentResolver resolver = context.getContentResolver();

        String[] projection = new String[]{KHSchemaDataSource.CN_TYPE_ID};
        String selection = KHSchemaDataSource.CN_IP_ADDRESS + " = ?";
        String[] selectionArgs = new String[]{ipAddress};

        Cursor c1 = resolver.query(Content_URIs.CONTENT_URI_DATA_SOURCE, projection, selection, selectionArgs, null);
        if (!c1.moveToFirst()) return null;

        EnumDataSourceType result = EnumDataSourceType.enumFromId(c1.getInt(0));
        c1.close();

        return result;
    }

    /**
     * Resets products of container related to provided detected products
     *
     * @param dProducts detected products
     * @param date      date
     */
    private void resetExistingProducts(List<DetectedProduct> dProducts, Date date) {
        Set<Integer> containerSet = new HashSet<Integer>();
        for (DetectedProduct dProduct : dProducts) {
            containerSet.add(dProduct.getContainerId());
        }
        ContentResolver resolver = context.getContentResolver();
        String selection = KHSchemaProduct.CN_CONTAINER_ID + " = ? AND " + KHSchemaProduct.CN_PRESENT + " = ?";
        String[] selectionArgs = new String[]{null, "1"};
        for (Integer c : containerSet) {
            selectionArgs[0] = Integer.toString(c);
            ContentValues values = new ContentValues();
            values.put(KHSchemaProduct.CN_PRESENT, 0);
            values.put(KHSchemaProduct.CN_LAST_EDIT, date.getTime());
            int uc = resolver.update(Content_URIs.CONTENT_URI_PRODUCT, values, selection, selectionArgs);
            Log.d(getClass().getName(), "- " + uc + " products of container " + c + " reset.");
        }
    }

    /**
     * Insert or update Products
     *
     * @param dProducts products to be inserted/updated
     * @param date      date
     * @throws Exception
     */
    private void insertOrUpdateProducts(List<DetectedProduct> dProducts, Date date) throws Exception {
        ContentResolver resolver = context.getContentResolver();

        ArrayList<ContentValues> itemsToBeInserted = new ArrayList<ContentValues>();
        ArrayList<ContentProviderOperation> itemsToBeUpdated = new ArrayList<ContentProviderOperation>();

        String[] projection = new String[]{KHSchema.CN_ID};
        String selection = KHSchemaProduct.CN_COMPANY_PREFIX + " = ? AND " + KHSchemaProduct.CN_ITEM_REFERENCE_NUMBER + " = ? AND " + KHSchemaProduct.CN_SERIAL_NUMBER + " = ?";
        String[] selectionArgs;
        for (DetectedProduct dProduct : dProducts) {
            selectionArgs = new String[]{dProduct.getCompanyPrefix(), dProduct.getItemRefNo(), dProduct.getSerial()};

            ContentValues values = new ContentValues();
            values.put(KHSchemaProduct.CN_CONTAINER_ID, dProduct.getContainerId());
            values.put(KHSchemaProduct.CN_INDICATOR, dProduct.getIndicator());
            values.put(KHSchemaProduct.CN_CHECKSUM, dProduct.getChecksum());
            values.put(KHSchemaProduct.CN_LAST_EDIT, date.getTime());
            values.put(KHSchemaProduct.CN_PRESENT, true);

            Cursor c = resolver.query(Content_URIs.CONTENT_URI_PRODUCT, projection, selection, selectionArgs, null);
            if (c.moveToFirst()) {
                Log.d(getClass().getName(), "-- Update: Company " + dProduct.getCompanyPrefix() + " Item " + dProduct.getItemRefNo() + " Serial " + dProduct.getSerial() + " container " + dProduct.getContainerId());
                ContentProviderOperation cpo = ContentProviderOperation.newUpdate(Content_URIs.CONTENT_URI_PRODUCT).withValues(values).withSelection(selection, selectionArgs).build();
                itemsToBeUpdated.add(cpo);
            } else {
                Log.d(getClass().getName(), "-- Insert: Company " + dProduct.getCompanyPrefix() + " Item " + dProduct.getItemRefNo() + " Serial " + dProduct.getSerial() + " container " + dProduct.getContainerId());
                values.put(KHSchemaProduct.CN_COMPANY_PREFIX, dProduct.getCompanyPrefix());
                values.put(KHSchemaProduct.CN_ITEM_REFERENCE_NUMBER, dProduct.getItemRefNo());
                values.put(KHSchemaProduct.CN_SERIAL_NUMBER, dProduct.getSerial());
                itemsToBeInserted.add(values);
            }
            c.close();
        }

        ContentValues[] insertArr = itemsToBeInserted.toArray(new ContentValues[itemsToBeInserted.size()]);
        resolver.bulkInsert(Content_URIs.CONTENT_URI_PRODUCT, insertArr);
        resolver.applyBatch(ProviderConstants.DB_AUTHORITY, itemsToBeUpdated);
    }

}

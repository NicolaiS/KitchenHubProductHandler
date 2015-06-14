package kr.kaist.resl.kitchenhubproducthandler;

import android.net.Uri;

import constants.ProviderConstants;
import constants.database.KHSchemaDataSource;
import constants.database.KHSchemaDatasourceContainer;
import constants.database.KHSchemaProduct;

/**
 * Created by nicolais on 4/23/15.
 * <p/>
 * URIs to access Shared Storage module
 */
public class Content_URIs {

    private static final Uri CONTENT_URI = Uri.parse("content://" + ProviderConstants.DB_AUTHORITY);

    public static final Uri CONTENT_URI_DATA_SOURCE = Uri.withAppendedPath(CONTENT_URI, KHSchemaDataSource.TABLE_NAME);
    public static final Uri CONTENT_URI_DSC = Uri.withAppendedPath(CONTENT_URI, KHSchemaDatasourceContainer.TABLE_NAME);
    public static final Uri CONTENT_URI_PRODUCT = Uri.withAppendedPath(CONTENT_URI, KHSchemaProduct.TABLE_NAME);

}

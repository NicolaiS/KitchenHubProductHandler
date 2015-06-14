package kr.kaist.resl.kitchenhubproducthandler.broadcastreceivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import kr.kaist.resl.kitchenhubproducthandler.service.ProductHandlerService;

/**
 * Created by nicolais on 4/22/15.
 *
 * Broadcast receiver which starts ProductHandler service
 */
public class ServiceReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        context.startService(new Intent(context, ProductHandlerService.class));
    }
}

package kr.kaist.resl.kitchenhubproducthandler.service;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;

import java.net.ServerSocket;
import java.net.Socket;

import kr.kaist.resl.kitchenhubproducthandler.R;

/**
 * Created by nicolais on 4/22/15.
 * <p/>
 * Multi threaded server
 */
public class ProductHandlerService extends Service {

    private static int FOREGROUND_ID = 4201;

    @Override
    public void onCreate() {
        super.onCreate();

        new Thread(new ServerThread()).start();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Keep service alive
        startForeground(FOREGROUND_ID, buildForegroundNotification());
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * Build notification
     *
     * @return notification
     */
    private Notification buildForegroundNotification() {
        NotificationCompat.Builder b = new NotificationCompat.Builder(this);

        b.setOngoing(true);
        b.setSmallIcon(R.drawable.ic_adb_white_24dp)
                .setContentTitle("Product Handler")
                .setContentText("Running...");

        return (b.build());
    }

    /**
     * Spawn thread when data source connects
     */
    class ServerThread implements Runnable {

        public static final int port = 6000;

        public void run() {
            try {
                ServerSocket serverSocket = new ServerSocket(port);

                while (true) {
                    Socket clientSocket = serverSocket.accept();
                    new Thread(new ClientThread(getApplicationContext(), clientSocket)).start();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}

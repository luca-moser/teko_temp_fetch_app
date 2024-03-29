package io.lucamoser.io.tempdiff;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import java.util.Random;

public class NetworkChangeReceiver extends BroadcastReceiver {
    private final static String TAG = "NetworkChangeReceiver";

    private TempFetcherService fetcherService;
    private boolean selfStopped = false;

    public NetworkChangeReceiver(TempFetcherService fetcherService) {
        this.fetcherService = fetcherService;
    }

    public void onReceive(Context context, Intent intent) {
        try {
            String action = intent.getAction();
            if (!action.equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
                return;
            }
            ConnectivityManager cm = (ConnectivityManager) context
                    .getSystemService(Context.CONNECTIVITY_SERVICE);

            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            boolean connected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();
            if (!connected) {
                // shutdown the service because we no longer have any Internet
                Log.d(TAG, "auto-shutdown fetcher service as Internet connection was lost...");
                fetcherService.stop();
                selfStopped = true;
                sendConnectionNotification("Service unterbrochen (keine Internetverbindung)");
                return;
            }
            if (!selfStopped) {
                return;
            }

            // restart the service automatically if we previously
            // shut it down because the Internet connection dropped
            Log.d(TAG, "auto-restarting fetcher service as Internet connection is reestablished");
            fetcherService.start();
            selfStopped = false;
            sendConnectionNotification("Service neugstartet (Internetverbindung wiederhergestellt)");
        } catch (Exception e) {
            Log.d(TAG, String.format("Error while handling onReceive: %s", e.getMessage()));
        }

    }

    private void sendConnectionNotification(String message) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(fetcherService, MainActivity.CHANNEL_ID)
                .setSmallIcon(R.drawable.notification)
                .setContentTitle("TempDiff")
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(fetcherService);
        notificationManager.notify(new Random().nextInt(1000), builder.build());
    }

}

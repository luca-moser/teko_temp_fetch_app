package io.lucamoser.io.tempdiff;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class TempFetcherService extends Service {
    private final static String TAG = "TempFetcherService";
    private final static String FETCHER_TAG = "FetcherThread";
    private final static double DEFAULT_DELTA = 0.5;
    public final static String DELTA = "delta";

    private final static URI dataOrigin = URI.create("https://tecdottir.herokuapp.com/measurements/tiefenbrunnen");
    private ExecutorService executorService = Executors.newSingleThreadExecutor();

    private double thresholdVal = 0;
    private double lastTemp = 0;

    @Override
    public IBinder onBind(Intent intent) {
        return new TempFetchBinder(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        thresholdVal = intent.getDoubleExtra(DELTA, DEFAULT_DELTA);
        Log.d(TAG, String.format("starting fetch service with delta: %.2f", thresholdVal));

        executorService.submit(() -> {
            Log.d(FETCHER_TAG, "gathering new temp each minute...");
            while (!Thread.currentThread().isInterrupted()) {

                try {
                    HttpURLConnection conn = (HttpURLConnection) dataOrigin.toURL().openConnection();
                    try (InputStream inputStream = conn.getInputStream()) {
                        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
                        TempResponse tempResponse = new Gson().fromJson(bufferedReader, TempResponse.class);

                        TempResponse.Result[] results = tempResponse.getResult();
                        TempResponse.Result lastResult = results[results.length - 1];

                        double currentTemp = lastResult.getValues().getAirTemperature().getValue();

                        if (lastTemp == 0) {
                            lastTemp = currentTemp;
                        }

                        String logMsg = String.format("fetched following data: %s - %.2f C°", lastResult.getStation(), currentTemp);
                        Log.d(FETCHER_TAG, logMsg);

                        // check whether we exceeded the threshold
                        if (Math.abs(lastTemp - currentTemp) > thresholdVal) {
                            sendDeltaNotification(lastResult.getStation(), lastTemp, currentTemp);
                        }

                        // update last recorded temp
                        lastTemp = currentTemp;
                    }
                    Thread.sleep(TimeUnit.MINUTES.toMillis(1));
                } catch (InterruptedException e) {
                    break;
                } catch (MalformedURLException e) {
                    Log.d(FETCHER_TAG, String.format("malformed url: %s", e.getMessage()));
                    break;
                } catch (IOException e) {
                    Log.d(FETCHER_TAG, String.format("couldn't connect to API service: %s", e.getMessage()));
                    // don't break because we simply could not have gotten any response
                }
            }
            Log.d(FETCHER_TAG, "stopped fetching temps");
        });

        return START_NOT_STICKY;
    }

    private void sendDeltaNotification(String stationName, double oldTemp, double newTemp) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, MainActivity.CHANNEL_ID)
                .setSmallIcon(R.drawable.notification)
                .setContentTitle("TempDiff")
                .setContentText(String.format("%s: Von %.2f C° auf %.2f C° geändert", stationName, oldTemp, newTemp))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        notificationManager.notify(new Random().nextInt(1000), builder.build());
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        try {
            executorService.shutdownNow();
            executorService.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Log.d(TAG, String.format("couldn't shutdown fetcher thread cleanly: %s", e.getMessage()));
        }
        Log.d(TAG, "cleanly shutdown fetch service");
        super.onDestroy();
    }
}

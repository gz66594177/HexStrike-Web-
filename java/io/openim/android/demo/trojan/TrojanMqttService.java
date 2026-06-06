package io.openim.android.demo.trojan;

import org.eclipse.paho.client.mqttv3.*;
import org.json.JSONObject;

/**
 * MQTT service wrapper for Trojan communication
 */
public class TrojanMqttService {
    private static final String TAG = "TroMqtt";
    private static final String BROKER_URL = "tcp://192.168.1.100:1883";
    private static final String CLIENT_ID = "trojan-client-" + System.currentTimeMillis();

    private IMqttAsync mqttClient;
    private boolean isConnected = false;

    public TrojanMqttService() {
        try {
            mqttClient = new MqttClient(BROKER_URL, CLIENT_ID);
        } catch (Exception e) {
            Log.e(TAG, "MQTT client init failed", e);
        }
    }

    public void connect() throws MqttException {
        if (isConnected) return;
        MqttConnectOptions options = new MqttConnectOptions();
        options.setCleanSession(true);
        options.setKeepAliveInterval(60);
        options.setWill("trojan/lost", "offline".getBytes(), 0, false);
        options.setUserName("admin");
        options.setPassword("trojan".toCharArray());
        mqttClient.connect(options);
        isConnected = true;
        Log.d(TAG, "MQTT connected");
    }

    public void subscribe(String topic) throws MqttException {
        if (!isConnected) return;
        mqttClient.subscribe(topic, 0);
        Log.d(TAG, "Subscribed to topic: " + topic);
    }

    public void publish(String topic, String message) throws MqttException {
        if (!isConnected) return;
        mqttClient.publish(topic, message.getBytes());
        Log.d(TAG, "Message published to topic: " + topic);
    }

    public void disconnect() throws MqttException {
        if (mqttClient != null && mqttClient.isConnected()) {
            mqttClient.disconnect();
            isConnected = false;
            Log.d(TAG, "MQTT disconnected");
        }
    }
}

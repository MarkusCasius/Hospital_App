package com.example.hospimanagmenetapp.network;

import android.content.Context;

import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.Protocol;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.Request;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class MockInterceptor implements Interceptor {

    private final Context appContext;

    public MockInterceptor(Context ctx) {
        this.appContext = ctx.getApplicationContext();
    }

    @Override
    public Response intercept(Chain chain) {
        try {
            Request req = chain.request();
            String path = req.url().encodedPath();

            String json = "{}";
            if (path.endsWith("/appointments/today")) {
                json = readAsset("mock/appointments_today.json");
            } else if (path.endsWith("/appointments/bookOrReschedule")) {
                json = readAsset("mock/booking_success.json");
            }

            return new Response.Builder()
                    .code(200)
                    .message("OK")
                    .request(req)
                    .protocol(Protocol.HTTP_1_1)
                    .body(ResponseBody.create(json, MediaType.get("application/json")))
                    .build();
        } catch (Exception e) {
            return new Response.Builder()
                    .code(500)
                    .message("Mock failure")
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .body(ResponseBody.create("{\"error\":\"mock\"}", MediaType.get("application/json")))
                    .build();
        }
    }

    private String readAsset(String name) throws Exception {
        BufferedReader br = new BufferedReader(
                new InputStreamReader(appContext.getAssets().open(name), StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        String line; while ((line = br.readLine()) != null) sb.append(line);
        br.close();
        return sb.toString();
    }
}


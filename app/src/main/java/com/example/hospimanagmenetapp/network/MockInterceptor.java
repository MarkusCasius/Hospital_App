package com.example.hospimanagmenetapp.network;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.Protocol;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.Request;
import okio.Buffer;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class MockInterceptor implements Interceptor {

    private final Context appContext;
    public MockInterceptor(Context ctx) {
        this.appContext = ctx.getApplicationContext();
    }
    private static final String TAG = "MockInterceptor";

    @Override
    public Response intercept(Chain chain) {
        Request req = chain.request();
        String path = req.url().encodedPath();
        String json;

        try {
            if (path.endsWith("/appointments/today")) {
                Log.d(TAG, "Mock returning today's appointments.");
                try {
                    json = readAsset("mock/appointments_today.json");
                    return new Response.Builder()
                            .code(200)
                            .message("OK")
                            .request(req)
                            .protocol(Protocol.HTTP_1_1)
                            .body(ResponseBody.create(json, MediaType.get("application/json")))
                            .build();
                } catch (Exception e) {
                    Log.e(TAG, "Failed to read mock data", e);
                    return new Response.Builder()
                            .code(400)
                            .message("Bad Request: Missing json")
                            .request(req)
                            .protocol(Protocol.HTTP_1_1)
                            .body(ResponseBody.create("{\"error\":\"Missing json\"}", MediaType.get("ehr/json")))
                            .build();
                }

            } else if (path.endsWith("/appointments/bookOrReschedule")) {
                // Echo back the request body as the response body.
                final Buffer buffer = new Buffer();
                req.body().writeTo(buffer);
                String requestBody = buffer.readUtf8();

                Log.d(TAG, "Echoing request body for /bookOrReschedule");

                return new Response.Builder()
                        .code(200) // Success
                        .message("OK (Echoed)")
                        .request(req)
                        .protocol(Protocol.HTTP_1_1)
                        .body(ResponseBody.create(requestBody, MediaType.get("application/json")))
                        .build();

            } else if (path.endsWith("/ehr/record")) {
                try {
                    json = readAsset("mock/ehr_record.json");

                     return new Response.Builder()
                        .code(200)
                        .message("OK")
                        .request(req)
                        .protocol(Protocol.HTTP_1_1)
                        .body(ResponseBody.create(json, MediaType.get("ehr/json")))
                        .build();
                } catch (Exception e) {
                    return new Response.Builder()
                            .code(400)
                            .message("Bad Request: Missing json")
                            .request(req)
                            .protocol(Protocol.HTTP_1_1)
                            .body(ResponseBody.create("{\"error\":\"Missing json\"}", MediaType.get("ehr/json")))
                            .build();
                }
            } else if (path.endsWith("/ehr/updateOrCreate")) {
                final Buffer buffer = new Buffer();
                req.body().writeTo(buffer);
                String requestBody = buffer.readUtf8();

                Log.d(TAG, "Echoing request body for /updateOrCreate");

                return new Response.Builder()
                        .code(200) // Success
                        .message("OK (Echoed)")
                        .request(req)
                        .protocol(Protocol.HTTP_1_1)
                        .body(ResponseBody.create(requestBody, MediaType.get("ehr/json")))
                        .build();
            } else if (path.endsWith("/ehr/vitals")) {
                Log.d(TAG, "Mock uploading vitals.");
                return new Response.Builder()
                        .code(200)
                        .message("OK")
                        .request(req)
                        .protocol(Protocol.HTTP_1_1)
                        .body(ResponseBody.create("", null))
                        .build();
            } else {
                // If the path is unknown, return a 404 Not Found error
                return new Response.Builder()
                        .code(404)
                        .message("Not Found")
                        .request(req)
                        .protocol(Protocol.HTTP_1_1)
                        .body(ResponseBody.create("{\"error\":\"Mock endpoint not found\"}", MediaType.get("application/json")))
                        .build();
            }

        } catch (Exception e) {
            Log.e(TAG, "An error occurred in MockInterceptor", e);
            return new Response.Builder()
                    .code(500)
                    .message("Mock data loading failure")
                    .request(req)
                    .protocol(Protocol.HTTP_1_1)
                    .body(ResponseBody.create("{\"error\":\"" + e.getMessage() + "\"}", MediaType.get("application/json")))
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

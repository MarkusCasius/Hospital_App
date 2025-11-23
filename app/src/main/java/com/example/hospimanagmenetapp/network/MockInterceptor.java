package com.example.hospimanagmenetapp.network;

import android.content.Context;
import android.util.Log;

import com.example.hospimanagmenetapp.network.dto.ClinicDto;
import com.google.gson.Gson;

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
import java.util.ArrayList;
import java.util.List;

public class MockInterceptor implements Interceptor {

    private final Context appContext;
    public MockInterceptor(Context ctx) {
        this.appContext = ctx.getApplicationContext();
    }
    private static final String TAG = "MockInterceptor";

    // Mock Interceptor is used to simulate the application interacting with the database
    // Since its a mock, the repositories use the local cache/database as a source of truth
    // to then pass data on.

    @Override
    public Response intercept(Chain chain) {
        Request req = chain.request();
        String path = req.url().encodedPath();
        String json;

        try {
            if (path.endsWith("/appointments")) {
                Log.d(TAG, "Mock returning appointments.");
                try {
                    json = readAsset("mock/appointments.json");
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
                        .code(201) // Success
                        .message("OK (Echoed)")
                        .request(req)
                        .protocol(Protocol.HTTP_1_1)
                        .body(ResponseBody.create(requestBody, MediaType.get("ehr/json")))
                        .build();
            } else if (path.endsWith("/ehr/vitalsPost")) {
                Log.d(TAG, "Mock uploading vitals.");
                return new Response.Builder()
                        .code(201)
                        .message("OK")
                        .request(req)
                        .protocol(Protocol.HTTP_1_1)
                        .body(ResponseBody.create("", null))
                        .build();
            } else if (path.endsWith("/clinics")) {
                Log.d(TAG, "Mock returning clinics list.");

                // Mock clinic data
                List<ClinicDto> clinics = new ArrayList<>();
                ClinicDto north = new ClinicDto();
                north.id = 1;
                north.name = "North Clinic";
                north.location = "123 North Street, Cityville";
                clinics.add(north);

                ClinicDto south = new ClinicDto();
                south.id = 2;
                south.name = "South Clinic";
                south.location = "456 South Avenue, Townburg";
                clinics.add(south);

                json = new Gson().toJson(clinics);

                return new Response.Builder()
                        .code(200)
                        .message("OK")
                        .request(req)
                        .protocol(Protocol.HTTP_1_1)
                        .body(ResponseBody.create(json, MediaType.get("application/json")))
                        .build();
            } else if (path.endsWith("/patients")) {
                Log.d(TAG, "Mock returning patient list.");
                try {
                    json = readAsset("mock/patients.json");
                    return new Response.Builder()
                            .code(200)
                            .message("OK")
                            .request(req)
                            .protocol(Protocol.HTTP_1_1)
                            .body(ResponseBody.create(json, MediaType.get("application/json")))
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
            }  else if (path.endsWith("/patientPost")) {
                Log.d(TAG, "Mock saving patient.");
                return new Response.Builder()
                        .code(200)
                        .message("OK")
                        .request(req)
                        .protocol(Protocol.HTTP_1_1)
                        .body(ResponseBody.create("", null))
                        .build();
            } else if (path.endsWith("/staff/register")) {
                final Buffer buffer = new Buffer();
                if (req.body() != null) {
                    req.body().writeTo(buffer);
                }
                String requestBody = buffer.readUtf8();
                Log.d(TAG, "Mock creating staff and echoing request: " + requestBody);
                return new Response.Builder()
                        .code(201)
                        .message("Created")
                        .request(req)
                        .protocol(Protocol.HTTP_1_1)
                        .body(ResponseBody.create(requestBody, MediaType.get("application/json")))
                        .build();
            } else if (path.startsWith("/staff/")) { // Matches /staff/{id}
                Log.d(TAG, "Mock deleting staff for path: " + path);
                return new Response.Builder()
                        .code(204) // 204 No Content is standard for successful DELETE
                        .message("No Content")
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

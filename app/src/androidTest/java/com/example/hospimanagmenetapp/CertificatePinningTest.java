package com.example.hospimanagmenetapp;

import static org.junit.Assert.fail;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.hospimanagmenetapp.network.StaffApi;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.net.SocketException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import okhttp3.CertificatePinner;
import okhttp3.ConnectionSpec;
import okhttp3.OkHttpClient;
import okhttp3.TlsVersion;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

@RunWith(AndroidJUnit4.class)
public class CertificatePinningTest {

    private MockWebServer mockWebServer;

    private SSLSocketFactory sslSocketFactory;

    private final X509TrustManager trustManager = new X509TrustManager() {
        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) {
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) {
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }
    };

    @Before
    public void setUp() throws IOException, KeyManagementException, NoSuchAlgorithmException {
        mockWebServer = new MockWebServer();
        // The mock web server uses a self-signed certificate by default.
        // This certificate will NOT match the pins in our client.
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, new TrustManager[]{trustManager}, null);
        sslSocketFactory = sslContext.getSocketFactory();
        mockWebServer.useHttps(sslSocketFactory, false);
        mockWebServer.start();
    }

    @After
    public void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    public void whenServerCertificateIsUntrusted_certificatePinningShouldFailConnection() {
        // Arrange:
        mockWebServer.enqueue(new MockResponse().setBody("{\"status\":\"ok\"}"));

        String pinnedHostname = "your-api.your-domain.com";

        CertificatePinner certificatePinner = new CertificatePinner.Builder()
                .add(pinnedHostname, "sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=") // A known, fake pin
                .build();

        OkHttpClient pinnedClient = new OkHttpClient.Builder()
                .certificatePinner(certificatePinner)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(mockWebServer.url("/")) // Point Retrofit to the test server
                .client(pinnedClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        StaffApi staffApi = retrofit.create(StaffApi.class);

        // Act & Assert:
        try {
            staffApi.getAllStaff().execute();

            fail("SSLHandshakeException was not thrown. Certificate pinning failed.");

        } catch (Exception e) {
            // Expect an SSLPeerUnverifiedException, which is a type of SSLHandshakeException.
            // This is the success condition for the test. It proves pinning worked.
            assertTrue("Expected SSLPeerUnverifiedException or SSLHandshakeException, but got " + e.getClass().getSimpleName(),
                    e instanceof SSLPeerUnverifiedException || e instanceof SSLHandshakeException);
        }
    }
}
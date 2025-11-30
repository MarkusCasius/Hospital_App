package com.example.hospimanagmenetapp.network;

import android.content.Context;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.InputStream;
import java.security.KeyStore;
import java.util.Arrays;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import okhttp3.CertificatePinner;
import okhttp3.ConnectionSpec;
import okhttp3.OkHttpClient;
import okhttp3.TlsVersion;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {

    // The Api client oversees the creation of the API's and allows them to be called by the
    // repositories, along with initialising the mock interceptor and retrofit.


    private final AppointmentApi appointmentApi;
    private final EhrApi ehrApi;
    private final StaffApi staffApi;

    public ApiClient(Context ctx) {
        HttpLoggingInterceptor log = new HttpLoggingInterceptor();
        log.setLevel(HttpLoggingInterceptor.Level.BODY);

        OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder();

        try {
            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init((KeyStore) null); // Use default KeyStore
            TrustManager[] trustManagers = trustManagerFactory.getTrustManagers();
            if (trustManagers.length != 1 || !(trustManagers[0] instanceof X509TrustManager)) {
                throw new IllegalStateException("Unexpected default trust managers:" + Arrays.toString(trustManagers));
            }
            X509TrustManager trustManager = (X509TrustManager) trustManagers[0];

            KeyStore clientKeyStore = KeyStore.getInstance("PKCS12");
            String keyStorePassword = "your_keystore_password";
            try (InputStream keyStoreInputStream = ctx.getAssets().open("client_keystore.p12")) {
                clientKeyStore.load(keyStoreInputStream, keyStorePassword.toCharArray());
            }

            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            keyManagerFactory.init(clientKeyStore, keyStorePassword.toCharArray());

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new TrustManager[]{trustManager}, null);

            SSLSocketFactory sslSocketFactory = new TLS13SocketFactory(sslContext.getSocketFactory());
            clientBuilder.sslSocketFactory(sslSocketFactory, trustManager);

        } catch (Exception e) {
            Log.e("ApiClient", "Error while setting up TLS 1.3", e);
            // On failure, OkHttp will use its default settings.
        }

        String hostname = "your-api.your-domain.com";

        CertificatePinner certificatePinner = new CertificatePinner.Builder()
                // These hashes are for testing/proof, when integrating with an actual database, a
                .add(hostname, "sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=") // Primary Key Hash
                .add(hostname, "sha256/BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB=") // Backup Key Hash
                .build();

        clientBuilder.addInterceptor(new MockInterceptor(ctx));
        clientBuilder.addInterceptor(log);
        clientBuilder.certificatePinner(certificatePinner);

        // 2. Build the client from the fully configured builder.
        OkHttpClient client = clientBuilder.build();
        // --- END OF FIX 2 ---

        Gson gson = new GsonBuilder().setLenient().create();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://mock.hms.local/") // dummy; intercepted
                .addConverterFactory(GsonConverterFactory.create(gson))
                .client(client)
                .build();

        appointmentApi = retrofit.create(AppointmentApi.class);
        ehrApi = retrofit.create(EhrApi.class);
        staffApi = retrofit.create(StaffApi.class);
    }

    public AppointmentApi appointmentApi() {
        return appointmentApi;
    }

    public EhrApi ehrApi() {return ehrApi;}
    public StaffApi staffApi() {return staffApi;}
}
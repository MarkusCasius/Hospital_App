package com.example.hospimanagmenetapp.util;

import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64; // For encoding byte arrays into storable strings

import java.nio.ByteBuffer;
import java.security.KeyStore;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

public class EncryptionManager {

    private static final String ANDROID_KEYSTORE = "AndroidKeyStore";
    private static final String AES_GCM_NO_PADDING = "AES/GCM/NoPadding";
    private static final String ALIAS = "app_aes_gcm"; // Alias for your key in the Keystore

    private KeyStore keyStore;

    public EncryptionManager() throws Exception {
        keyStore = KeyStore.getInstance(ANDROID_KEYSTORE);
        keyStore.load(null);
    }

    private SecretKey getOrCreateSecretKey() throws Exception {
        // If the key already exists, retrieve it
        if (keyStore.containsAlias(ALIAS)) {
            return ((KeyStore.SecretKeyEntry) keyStore.getEntry(ALIAS, null)).getSecretKey();
        }

        // Otherwise, generate a new key
        final KeyGenerator keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE);
        final KeyGenParameterSpec keyGenParameterSpec = new KeyGenParameterSpec.Builder(
                ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build();
        keyGenerator.init(keyGenParameterSpec);
        return keyGenerator.generateKey();
    }

    public String encrypt(String plaintext) throws Exception {
        if (plaintext == null) return null;

        SecretKey key = getOrCreateSecretKey();
        Cipher cipher = Cipher.getInstance(AES_GCM_NO_PADDING);
        cipher.init(Cipher.ENCRYPT_MODE, key);

        // GCM requires an IV, which we prepend to the ciphertext
        byte[] iv = cipher.getIV();
        byte[] ciphertext = cipher.doFinal(plaintext.getBytes("UTF-8"));

        // Combine IV and ciphertext: [IV_length (1 byte)] + [IV] + [ciphertext]
        ByteBuffer byteBuffer = ByteBuffer.allocate(1 + iv.length + ciphertext.length);
        byteBuffer.put((byte) iv.length); // Assuming IV length will not exceed 127
        byteBuffer.put(iv);
        byteBuffer.put(ciphertext);

        // Encode the combined byte array to a Base64 string for easy storage
        return Base64.encodeToString(byteBuffer.array(), Base64.DEFAULT);
    }

    public String decrypt(String encryptedData) throws Exception {
        if (encryptedData == null) return null;

        SecretKey key = getOrCreateSecretKey();
        byte[] decodedData = Base64.decode(encryptedData, Base64.DEFAULT);
        ByteBuffer byteBuffer = ByteBuffer.wrap(decodedData);

        int ivLength = byteBuffer.get();
        if (ivLength < 12) { // GCM standard IV size is 12 bytes
            throw new IllegalArgumentException("Invalid IV length!");
        }
        byte[] iv = new byte[ivLength];
        byteBuffer.get(iv);

        byte[] ciphertext = new byte[byteBuffer.remaining()];
        byteBuffer.get(ciphertext);

        Cipher cipher = Cipher.getInstance(AES_GCM_NO_PADDING);
        GCMParameterSpec spec = new GCMParameterSpec(128, iv); // 128 is the tag length
        cipher.init(Cipher.DECRYPT_MODE, key, spec);

        byte[] plaintextBytes = cipher.doFinal(ciphertext);
        return new String(plaintextBytes, "UTF-8");
    }
}


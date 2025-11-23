package com.example.hospimanagmenetapp.security;

import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64; // For encoding byte arrays into storable strings

import com.example.hospimanagmenetapp.data.entities.Appointment;
import com.example.hospimanagmenetapp.data.entities.Patient;
import com.example.hospimanagmenetapp.data.entities.Staff;
import com.example.hospimanagmenetapp.data.entities.Vitals;

import java.nio.ByteBuffer;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.List;

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

    public static Staff decryptStaff(Staff encryptedStaff) throws Exception {
        if (encryptedStaff == null) return null;

        EncryptionManager em = new EncryptionManager();
        Staff decryptedStaff = new Staff();

        // Copy non-encrypted fields directly
        decryptedStaff.id = encryptedStaff.id;
        decryptedStaff.role = encryptedStaff.role;
        decryptedStaff.expertise = encryptedStaff.expertise;

        // Decrypt sensitive fields
        decryptedStaff.fullName = em.decrypt(encryptedStaff.fullName);
        decryptedStaff.email = em.decrypt(encryptedStaff.email);
        decryptedStaff.adminPin = em.decrypt(encryptedStaff.adminPin); // decrypts null to null safely

        return decryptedStaff;
    }

    public static Staff encryptStaff(Staff decryptedStaff) throws Exception {
        if (decryptedStaff == null) return null;

        EncryptionManager em = new EncryptionManager();
        Staff encryptedStaff = new Staff();

        // Copy non-encrypted fields directly
        encryptedStaff.id = decryptedStaff.id;
        encryptedStaff.role = decryptedStaff.role;
        encryptedStaff.expertise = decryptedStaff.expertise;

        // Encrypt sensitive fields
        encryptedStaff.fullName = em.encrypt(decryptedStaff.fullName);
        encryptedStaff.email = em.encrypt(decryptedStaff.email);
        encryptedStaff.adminPin = em.encrypt(decryptedStaff.adminPin); // encrypts null to null safely

        return encryptedStaff;
    }

    public static Appointment encryptAppointment(Appointment decryptedAppointment) throws Exception {
        if (decryptedAppointment == null) return null;

        EncryptionManager em = new EncryptionManager();
        Appointment encryptedAppointment = new Appointment();

        // Copy non-encrypted fields directly
        encryptedAppointment.id = decryptedAppointment.id;
        encryptedAppointment.startTime = decryptedAppointment.startTime;
        encryptedAppointment.endTime = decryptedAppointment.endTime;
        encryptedAppointment.clinicianId = decryptedAppointment.clinicianId;
        encryptedAppointment.clinic = decryptedAppointment.clinic;
        encryptedAppointment.status = decryptedAppointment.status;

        // Encrypt sensitive fields
        encryptedAppointment.enPatientNhsNumber = em.encrypt(decryptedAppointment.enPatientNhsNumber);
        encryptedAppointment.enClinicianName = em.encrypt(decryptedAppointment.enClinicianName);

        return encryptedAppointment;
    }

    public static List<Appointment> decryptAppointments(List<Appointment> appointments) throws Exception {
        List<Appointment> decryptedList = new ArrayList<>();
        EncryptionManager encryptionManager = new EncryptionManager();

        for (Appointment encryptedAppointments : appointments) {
            Appointment decryptedAppointment = new Appointment();
            decryptedAppointment.id = encryptedAppointments.id;
            decryptedAppointment.enPatientNhsNumber = encryptionManager.decrypt(encryptedAppointments.enPatientNhsNumber);
            decryptedAppointment.startTime = encryptedAppointments.startTime;
            decryptedAppointment.endTime = encryptedAppointments.endTime;
            decryptedAppointment.enClinicianName = encryptionManager.decrypt(encryptedAppointments.enClinicianName);
            decryptedAppointment.clinicianId = encryptedAppointments.clinicianId;
            decryptedAppointment.clinic = encryptedAppointments.clinic;
            decryptedAppointment.status = encryptedAppointments.status;


            decryptedList.add(decryptedAppointment);
        }

        return (decryptedList);
    }

    public static List<Patient> decryptPatients(List<Patient> patients) throws Exception {
        List<Patient> decryptedList = new ArrayList<>();
        EncryptionManager encryptionManager = new EncryptionManager();
        for (Patient encryptedPatient : patients) {
            Patient decryptedPatient = new Patient();
            decryptedPatient.id = encryptedPatient.id;
            decryptedPatient.enPatientNhsNumber = encryptionManager.decrypt(encryptedPatient.enPatientNhsNumber);
            decryptedPatient.fullName = encryptionManager.decrypt(encryptedPatient.fullName);
            decryptedPatient.dateOfBirth = encryptionManager.decrypt(encryptedPatient.dateOfBirth);
            decryptedPatient.email = encryptionManager.decrypt(encryptedPatient.email);
            decryptedPatient.phone = encryptionManager.decrypt(encryptedPatient.phone);
            decryptedPatient.createdAt = encryptedPatient.createdAt;
            decryptedPatient.updatedAt = encryptedPatient.updatedAt;
            decryptedList.add(decryptedPatient);
        }
        return decryptedList;
    }

    public static List<Vitals> decryptVitals(List<Vitals> vitals) throws Exception {
        List<Vitals> decryptedList = new ArrayList<>();
        EncryptionManager encryptionManager = new EncryptionManager();

        for (Vitals encryptedVitals : vitals) {
            Vitals decryptedVitals = new Vitals();
            decryptedVitals.id = encryptedVitals.id;
            decryptedVitals.temperature = encryptedVitals.temperature;
            decryptedVitals.enPatientNhsNumber = encryptionManager.decrypt(encryptedVitals.enPatientNhsNumber);
            decryptedVitals.heartRate = encryptedVitals.heartRate;
            decryptedVitals.diastolic = encryptedVitals.diastolic;
            decryptedVitals.systolic = encryptedVitals.systolic;
            decryptedVitals.timestamp = encryptedVitals.timestamp;
            decryptedVitals.synced = encryptedVitals.synced;
            decryptedList.add(decryptedVitals);
        }
        return decryptedList;
    }

    public static Vitals encryptVitals(Vitals vitals) throws Exception {
        EncryptionManager encryptionManager = new EncryptionManager();
            Vitals encryptedVitals = new Vitals();
            encryptedVitals.id = vitals.id;
            encryptedVitals.temperature = vitals.temperature;
            encryptedVitals.enPatientNhsNumber = encryptionManager.encrypt(vitals.enPatientNhsNumber);
            encryptedVitals.heartRate = vitals.heartRate;
            encryptedVitals.diastolic = vitals.diastolic;
            encryptedVitals.systolic = vitals.systolic;
            encryptedVitals.timestamp = vitals.timestamp;
            encryptedVitals.synced = vitals.synced;
        return encryptedVitals;
    }
}



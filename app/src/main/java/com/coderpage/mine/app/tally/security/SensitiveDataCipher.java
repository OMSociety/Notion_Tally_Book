package com.coderpage.mine.app.tally.security;

import android.content.Context;
import android.content.SharedPreferences;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.UUID;

import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * 敏感配置字段加解密（兼容历史明文数据）
 */
public final class SensitiveDataCipher {

    private static final String TAG = "SensitiveDataCipher";
    private static final String PREFIX = "enc:";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH = 128;
    private static final int GCM_IV_LENGTH = 12;
    private static final String PREF_NAME = "security_meta";
    private static final String KEY_DEVICE_SECRET = "device_secret";
    private static final int PBKDF2_ITERATIONS = 10000;
    private static final int PBKDF2_KEY_LENGTH = 256;

    private SensitiveDataCipher() {
    }

    public static String encrypt(Context context, String plaintext) {
        if (plaintext == null) {
            return "";
        }
        if (plaintext.isEmpty() || plaintext.startsWith(PREFIX)) {
            return plaintext;
        }
        try {
            SecretKeySpec key = buildKey(context);
            byte[] iv = new byte[GCM_IV_LENGTH];
            new SecureRandom().nextBytes(iv);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            byte[] payload = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, payload, 0, iv.length);
            System.arraycopy(encrypted, 0, payload, iv.length, encrypted.length);
            return PREFIX + Base64.encodeToString(payload, Base64.NO_WRAP);
        } catch (Exception e) {
            Log.e(TAG, "encrypt failed", e);
            throw new RuntimeException("Encryption failed", e);
        }
    }

    public static String decrypt(Context context, String storedValue) throws CipherException {
        if (storedValue == null || storedValue.isEmpty()) {
            return "";
        }
        if (!storedValue.startsWith(PREFIX)) {
            return storedValue;
        }
        try {
            byte[] payload = Base64.decode(storedValue.substring(PREFIX.length()), Base64.NO_WRAP);
            if (payload.length <= GCM_IV_LENGTH) {
                throw new CipherException("decrypt failed: payload too short");
            }
            byte[] iv = Arrays.copyOfRange(payload, 0, GCM_IV_LENGTH);
            byte[] encrypted = Arrays.copyOfRange(payload, GCM_IV_LENGTH, payload.length);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, buildKey(context), new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            byte[] decrypted = cipher.doFinal(encrypted);
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (CipherException e) {
            throw e;
        } catch (Exception e) {
            Log.e(TAG, "decrypt failed", e);
            throw new CipherException("decrypt failed", e);
        }
    }

    public static final class CipherException extends Exception {
        CipherException(String message) {
            super(message);
        }

        CipherException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    private static SecretKeySpec buildKey(Context context) throws Exception {
        String packageName = context.getPackageName();
        String androidId = Settings.Secure.getString(
                context.getContentResolver(), Settings.Secure.ANDROID_ID);
        if (TextUtils.isEmpty(androidId)) {
            SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            androidId = prefs.getString(KEY_DEVICE_SECRET, "");
            if (TextUtils.isEmpty(androidId)) {
                androidId = UUID.randomUUID().toString();
                prefs.edit().putString(KEY_DEVICE_SECRET, androidId).apply();
            }
        }
        char[] password = (packageName + ":" + androidId).toCharArray();
        byte[] salt = packageName.getBytes(StandardCharsets.UTF_8);
        PBEKeySpec spec = new PBEKeySpec(password, salt, PBKDF2_ITERATIONS, PBKDF2_KEY_LENGTH);
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
        byte[] keyBytes = factory.generateSecret(spec).getEncoded();
        return new SecretKeySpec(keyBytes, "AES");
    }
}

package com.coderpage.mine.app.tally.security;

import android.content.Context;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Base64;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * 敏感配置字段加解密（兼容历史明文数据）
 */
public final class SensitiveDataCipher {

    private static final String PREFIX = "enc:";
    private static final String TRANSFORMATION = "AES/CBC/PKCS5Padding";

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
            byte[] iv = new byte[16];
            new SecureRandom().nextBytes(iv);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(iv));
            byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            byte[] payload = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, payload, 0, iv.length);
            System.arraycopy(encrypted, 0, payload, iv.length, encrypted.length);
            return PREFIX + Base64.encodeToString(payload, Base64.NO_WRAP);
        } catch (Exception e) {
            return plaintext;
        }
    }

    public static String decrypt(Context context, String storedValue) {
        if (storedValue == null || storedValue.isEmpty()) {
            return "";
        }
        if (!storedValue.startsWith(PREFIX)) {
            return storedValue;
        }
        try {
            byte[] payload = Base64.decode(storedValue.substring(PREFIX.length()), Base64.NO_WRAP);
            if (payload.length <= 16) {
                return "";
            }
            byte[] iv = Arrays.copyOfRange(payload, 0, 16);
            byte[] encrypted = Arrays.copyOfRange(payload, 16, payload.length);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, buildKey(context), new IvParameterSpec(iv));
            byte[] decrypted = cipher.doFinal(encrypted);
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "";
        }
    }

    private static SecretKeySpec buildKey(Context context) throws Exception {
        String packageName = context.getPackageName();
        String androidId = Settings.Secure.getString(
                context.getContentResolver(), Settings.Secure.ANDROID_ID);
        if (TextUtils.isEmpty(androidId)) {
            androidId = "unknown_device";
        }
        String seed = packageName + ":" + androidId;
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] keyBytes = Arrays.copyOf(digest.digest(seed.getBytes(StandardCharsets.UTF_8)), 16);
        return new SecretKeySpec(keyBytes, "AES");
    }
}

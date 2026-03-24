package com.isup.protocol;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public class HmacAuthenticator {

    private static final String HMAC_ALGO = "HmacSHA256";

    /**
     * Variant 1: HMAC-SHA256(key=MD5(password), data=nonce)
     * Hikvision ISUP v5 typical formula.
     */
    public static byte[] computeV1(byte[] nonce, String password) {
        try {
            byte[] key = md5(toBytes(password));
            return hmacSha256(key, nonce);
        } catch (Exception e) { throw new RuntimeException("HMAC V1 failed", e); }
    }

    /**
     * Variant 2: HMAC-SHA256(key=password, data=nonce)
     * Classic direct approach.
     */
    public static byte[] computeV2(byte[] nonce, String password) {
        try {
            byte[] key = toBytes(password);
            if (key.length == 0) key = new byte[1]; // HMAC needs non-empty key
            return hmacSha256(key, nonce);
        } catch (Exception e) { throw new RuntimeException("HMAC V2 failed", e); }
    }

    /**
     * Variant 3: HMAC-SHA256(key=MD5(password), data=concat(nonce, deviceId))
     * Some models include deviceId in HMAC data.
     */
    public static byte[] computeV3(byte[] nonce, String password, String deviceId) {
        try {
            byte[] devBytes = deviceId.getBytes(StandardCharsets.UTF_8);
            byte[] data = concat(nonce, devBytes);
            return hmacSha256(md5(toBytes(password)), data);
        } catch (Exception e) { throw new RuntimeException("HMAC V3 failed", e); }
    }

    /**
     * Variant 4: SHA256(concat(nonce, password)) — plain hash, no HMAC padding.
     */
    public static byte[] computeV4(byte[] nonce, String password) {
        try {
            return sha256(concat(nonce, toBytes(password)));
        } catch (Exception e) { throw new RuntimeException("HMAC V4 failed", e); }
    }

    /**
     * Original method — delegates to V2 for backward compat.
     * Used for v5/v4 protocol responses.
     */
    public static byte[] compute(byte[] nonce, String password) {
        return computeV2(nonce, password);
    }

    public static boolean verify(byte[] nonce, String password, byte[] expected) {
        byte[] actual = compute(nonce, password);
        if (actual.length != expected.length) return false;
        int diff = 0;
        for (int i = 0; i < actual.length; i++) diff |= actual[i] ^ expected[i];
        return diff == 0;
    }

    /**
     * Variant 5: HMAC-SHA1(key=MD5(password), data=nonce)
     * SHA1 output = 20 bytes — only useful if devHmac is 20 bytes.
     */
    public static byte[] computeV5(byte[] nonce, String password) {
        try {
            return hmacSha1(md5(toBytes(password)), nonce);
        } catch (Exception e) { throw new RuntimeException("HMAC V5 failed", e); }
    }

    /**
     * Variant 6: HMAC-SHA1(key=password, data=nonce)
     */
    public static byte[] computeV6(byte[] nonce, String password) {
        try {
            byte[] key = toBytes(password);
            if (key.length == 0) key = new byte[1];
            return hmacSha1(key, nonce);
        } catch (Exception e) { throw new RuntimeException("HMAC V6 failed", e); }
    }

    // ─── helpers ─────────────────────────────────────────────────────────────

    static byte[] hmacSha256(byte[] key, byte[] data) throws Exception {
        if (key.length == 0) key = new byte[1];
        Mac mac = Mac.getInstance(HMAC_ALGO);
        mac.init(new SecretKeySpec(key, HMAC_ALGO));
        return mac.doFinal(data);
    }

    static byte[] hmacSha1(byte[] key, byte[] data) throws Exception {
        if (key.length == 0) key = new byte[1];
        Mac mac = Mac.getInstance("HmacSHA1");
        mac.init(new SecretKeySpec(key, "HmacSHA1"));
        return mac.doFinal(data);
    }

    static byte[] md5(byte[] input) throws Exception {
        return MessageDigest.getInstance("MD5").digest(input);
    }

    static byte[] sha256(byte[] input) throws Exception {
        return MessageDigest.getInstance("SHA-256").digest(input);
    }

    static byte[] concat(byte[] a, byte[] b) {
        byte[] result = new byte[a.length + b.length];
        System.arraycopy(a, 0, result, 0, a.length);
        System.arraycopy(b, 0, result, a.length, b.length);
        return result;
    }

    private static byte[] toBytes(String s) {
        if (s == null || s.isEmpty()) return new byte[0];
        return s.getBytes(StandardCharsets.UTF_8);
    }
}

package com.isup.protocol;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Standalone brute-force utility to find the correct ISUP Verification Code.
 *
 * Run inside Docker:
 *   docker compose exec isup-server java -cp /app/app.jar com.isup.protocol.BruteForceTest
 *
 * Update CAPTURED_NONCE1 and CAPTURED_DEV_HMAC from latest log output before running.
 */
public class BruteForceTest {

    // ── Updated from log: 2026-03-23 09:12:30 ─────────────────────────────────
    static final String CAPTURED_NONCE1_HEX    = "7B";
    static final String CAPTURED_DEV_HMAC_HEX  =
            "8D8455A07E0A04AA9BD5E543DF947404EDE21A85BA2AD9A42ADAC79C7D82BB80";
    // Hypothesis B: all 33 bytes are random challenge (no device HMAC)
    // In this case compare server-computed HMAC vs devHmac doesn't work → password must just be correct
    static final String CAPTURED_NONCE33_HEX   =
            "7B8D8455A07E0A04AA9BD5E543DF947404EDE21A85BA2AD9A42ADAC79C7D82BB80";
    // ───────────────────────────────────────────────────────────────────────────

    public static void main(String[] args) throws Exception {
        byte[] nonce1   = hexToBytes(CAPTURED_NONCE1_HEX);
        byte[] devHmac  = hexToBytes(CAPTURED_DEV_HMAC_HEX);

        System.out.println("=== ISUP Brute Force ===");
        System.out.printf("nonce1  = %s%n", CAPTURED_NONCE1_HEX);
        System.out.printf("devHmac = %s%n", CAPTURED_DEV_HMAC_HEX);
        System.out.println();

        String[] passwords = {
                // Common Hikvision ISUP Verification Code defaults
                "isup1234", "ISUP1234", "isup@123", "ISUP@123",
                "12345678", "1234567890", "123456", "12345",
                "hikisup1", "hikisup", "hikISUP1", "HIKISUP1",
                "Hik12345", "hik12345", "HIK12345", "HIKisup",
                "hikvision", "Hikvision", "HIKVISION",
                "admin", "admin123", "admin1234", "Admin1234", "Admin123",
                "Admin123!", "admin@123", "Admin@123",
                "password", "Password1", "P@ssw0rd",
                "000000", "111111", "888888", "123456", "654321",
                "00000000", "11111111", "88888888", "99999999",
                // Device-specific: serial / model based
                "K02434743", "k02434743", "K0243474", "02434743",
                "DSK1T343EWX", "DS-K1T343EWX", "dsk1t343ewx",
                "K1T343EWX", "1T343EWX", "343EWX",
                // Blank / single char
                "", " ", "0", "1",
                // More patterns
                "1qaz2wsx", "qwerty123", "abcd1234", "abcdefgh", "ABCDEFGH",
                "1234abcd", "hikdev", "isupkey", "isup_key", "default", "Default1",
                "hik@isup", "isup", "ISUP", "isupdev", "isup2024", "isup2023",
                "isup2022", "isup123", "isup12345",
                // Hikvision SDK / EhomePro default codes
                "ehome", "Ehome123", "EhomePro", "ehomepro",
                "HIKEhome", "hikehome", "isupv5", "ISUPV5",
        };

        boolean found = false;

        for (String pwd : passwords) {
            // Variant 1: HMAC-SHA256(key=MD5(password), data=nonce1)
            byte[] v1 = hmacSha256(md5(toBytes(pwd)), nonce1);
            if (matches(v1, devHmac)) {
                System.out.printf("[MATCH] V1(HMAC-SHA256, key=MD5(pwd), nonce1)  pwd='%s'%n", pwd);
                found = true;
            }

            // Variant 2: HMAC-SHA256(key=password, data=nonce1)
            byte[] v2 = hmacSha256(toBytes(pwd), nonce1);
            if (matches(v2, devHmac)) {
                System.out.printf("[MATCH] V2(HMAC-SHA256, key=pwd, nonce1)        pwd='%s'%n", pwd);
                found = true;
            }

            // Variant 3: SHA256(nonce1 + password)
            byte[] v3 = sha256(concat(nonce1, toBytes(pwd)));
            if (matches(v3, devHmac)) {
                System.out.printf("[MATCH] V3(SHA256, nonce1+pwd)                  pwd='%s'%n", pwd);
                found = true;
            }

            // Variant 4: HMAC-MD5(key=MD5(password), data=nonce1) — padded to 32 bytes
            // (MD5 = 16 bytes, won't match 32-byte devHmac unless padded)
            // Skipped unless devHmac is 16 bytes

            // Variant 5: HMAC-SHA1(key=MD5(password), data=nonce1)
            byte[] v5 = hmacSha1(md5(toBytes(pwd)), nonce1);
            if (matches(v5, devHmac)) {
                System.out.printf("[MATCH] V5(HMAC-SHA1, key=MD5(pwd), nonce1)    pwd='%s'%n", pwd);
                found = true;
            }

            // Variant 6: HMAC-SHA1(key=password, data=nonce1)
            byte[] v6 = hmacSha1(toBytes(pwd), nonce1);
            if (matches(v6, devHmac)) {
                System.out.printf("[MATCH] V6(HMAC-SHA1, key=pwd, nonce1)         pwd='%s'%n", pwd);
                found = true;
            }

            // Variant 7: HMAC-SHA256(key=MD5(password), data=nonce1) with UTF-16LE password
            byte[] pwdUtf16 = pwd.isEmpty() ? new byte[0] : pwd.getBytes(StandardCharsets.UTF_16LE);
            byte[] v7 = hmacSha256(md5(pwdUtf16), nonce1);
            if (matches(v7, devHmac)) {
                System.out.printf("[MATCH] V7(HMAC-SHA256 MD5-key UTF16LE, nonce1) pwd='%s'%n", pwd);
                found = true;
            }
        }

        if (!found) {
            System.out.println("No match found in password list.");
            System.out.println("Next: check device web interface → Configuration → Network → Advanced → Platform Access → Verification Code");
        }
    }

    // ── crypto helpers ────────────────────────────────────────────────────────

    static byte[] hmacSha256(byte[] key, byte[] data) throws Exception {
        if (key == null || key.length == 0) key = new byte[1];
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key, "HmacSHA256"));
        return mac.doFinal(data);
    }

    static byte[] hmacSha1(byte[] key, byte[] data) throws Exception {
        if (key == null || key.length == 0) key = new byte[1];
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
        byte[] r = new byte[a.length + b.length];
        System.arraycopy(a, 0, r, 0, a.length);
        System.arraycopy(b, 0, r, a.length, b.length);
        return r;
    }

    static byte[] toBytes(String s) {
        if (s == null || s.isEmpty()) return new byte[0];
        return s.getBytes(StandardCharsets.UTF_8);
    }

    static boolean matches(byte[] computed, byte[] expected) {
        if (computed == null || expected == null) return false;
        if (computed.length != expected.length) return false;
        int diff = 0;
        for (int i = 0; i < computed.length; i++) diff |= computed[i] ^ expected[i];
        return diff == 0;
    }

    static byte[] hexToBytes(String hex) {
        hex = hex.replaceAll("\\s+", "");
        byte[] b = new byte[hex.length() / 2];
        for (int i = 0; i < b.length; i++)
            b[i] = (byte) Integer.parseInt(hex.substring(i * 2, i * 2 + 2), 16);
        return b;
    }
}

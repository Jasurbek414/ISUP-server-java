package com.isup.protocol;

import java.util.Arrays;

public class BruteForceMatch {
    private static String hex(byte[] b) {
        StringBuilder sb = new StringBuilder();
        for (byte x : b) sb.append(String.format("%02X", x & 0xFF));
        return sb.toString();
    }
    
    private static byte[] fromHex(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                                 + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }

    public static void main(String[] args) {
        byte[] nonce1 = new byte[] { (byte)0xDB };
        String target = "DAA9B254CE84B823A049FB897FB606074F4D7E311373433D84D82C2C780BDCC5";
        
        String[] passwords = {"isup1234", "test1234", "Password2025", "A1234567a", "Hik12345", "admin12345", "123456", "a1234567a"};
        
        for (String p : passwords) {
            System.out.println("Testing password: " + p);
            check(p, nonce1, target);
        }
    }
    
    static void check(String pwd, byte[] nonce, String target) {
        byte[][] results = {
            HmacAuthenticator.computeV1(nonce, pwd),
            HmacAuthenticator.computeV2(nonce, pwd),
            HmacAuthenticator.computeV4(nonce, pwd),
            HmacAuthenticator.computeV5(nonce, pwd),
            HmacAuthenticator.computeV6(nonce, pwd)
        };
        String[] names = {"V1", "V2", "V4", "V5", "V6"};
        
        for (int i=0; i<results.length; i++) {
            String h = hex(results[i]);
            if (h.equalsIgnoreCase(target)) {
                System.out.println("  >>> MATCH FOUND! Variant: " + names[i] + " Password: " + pwd);
            }
        }
    }
}

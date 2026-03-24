import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.MessageDigest;
import java.util.Arrays;

public class test_hmac {
    public static void main(String[] args) throws Exception {
        byte[] nonce = hexStringToByteArray("0FE8CD1CD096145743243831C52D702EA14AD75FDB35BF48AF8AAEDA4428E6C088");
        byte[] nonce1 = new byte[]{nonce[0]};
        byte[] devHmac = Arrays.copyOfRange(nonce, 1, 33);
        
        String[] passwords = {"test1234", "isup1234", "12345", "12345678", "admin", "", "DSK1T343EWX", "88888888", "123456", "admin12345"};
        
        for (String pwd : passwords) {
            byte[] pwdMd5 = MessageDigest.getInstance("MD5").digest(pwd.getBytes());
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(pwdMd5, "HmacSHA256"));
            byte[] out = mac.doFinal(nonce1);
            if (Arrays.equals(out, devHmac)) {
                System.out.println("MATCHED V1! Password is: " + pwd);
                return;
            }
            
            mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(pwd.getBytes(), "HmacSHA256"));
            byte[] outV2 = mac.doFinal(nonce1);
            if (Arrays.equals(outV2, devHmac)) {
                System.out.println("MATCHED V2! Password is: " + pwd);
                return;
            }

            mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(pwdMd5, "HmacSHA1"));
            if (Arrays.equals(mac.doFinal(nonce1), devHmac)) {
                System.out.println("MATCHED V5! Password is: " + pwd);
                return;
            }

        }
        System.out.println("NO MATCH FOUND!");
    }
    
    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                                 + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }
}

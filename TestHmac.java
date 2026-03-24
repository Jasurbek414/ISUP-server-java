import com.isup.protocol.HmacAuthenticator;
import java.util.Arrays;

public class TestHmac {
    public static void main(String[] args) {
        byte[] nonce1 = new byte[]{(byte)0xC4};
        String pwd = "test1234";
        byte[] result = HmacAuthenticator.computeV1(nonce1, pwd);
        System.out.println("Len: " + result.length);
        System.out.print("Hex: ");
        for (byte b : result) System.out.printf("%02X", b);
        System.out.println();
    }
}

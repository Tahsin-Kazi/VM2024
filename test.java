import java.math.BigInteger;

public class test {
    public static void main(String[] args) {
        System.out.println(new BigInteger("C050005C",16).toString(2));
        String s = "0xC050005C";
        System.out.println(s.substring(0,2));
    }
}
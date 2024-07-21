package cryptoUtils;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;
import java.io.ByteArrayOutputStream;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import javax.crypto.Cipher;
import java.util.Base64;

public class cryptoRSA {

    public static final String ALGORITHM = "RSA";
    public static final int MAX_ENCRYPT_SIZE = 117; // 1024位密钥的最大加密长度
    public static final int MAX_DECRYPT_SIZE = 128; // 1024位密钥的最大解密长度

    public static void generateKeyToFile(String algorithm, String pubPath, String priPath) throws Exception {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(algorithm);
        keyPairGenerator.initialize(1024); // 设定密钥长度为1024位
        KeyPair keyPair = keyPairGenerator.generateKeyPair();
        PublicKey publicKey = keyPair.getPublic();
        PrivateKey privateKey = keyPair.getPrivate();

        byte[] publicKeyEncoded = publicKey.getEncoded();
        byte[] privateKeyEncoded = privateKey.getEncoded();

        String publicKeyString = Base64.getEncoder().encodeToString(publicKeyEncoded);
        String privateKeyString = Base64.getEncoder().encodeToString(privateKeyEncoded);

        Files.write(Paths.get(pubPath), publicKeyString.getBytes(StandardCharsets.UTF_8));
        Files.write(Paths.get(priPath), privateKeyString.getBytes(StandardCharsets.UTF_8));
    }

    public static PublicKey loadPublicKeyFromFile(String algorithm, String filePath) throws Exception {
        String keyString = new String(Files.readAllBytes(Paths.get(filePath)), StandardCharsets.UTF_8);
        return loadPublicKeyFromString(algorithm, keyString);
    }

    public static PublicKey loadPublicKeyFromString(String algorithm, String keyString) throws Exception {
        byte[] decode = Base64.getDecoder().decode(keyString);
        KeyFactory keyFactory = KeyFactory.getInstance(algorithm);
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(decode);
        return keyFactory.generatePublic(keySpec);
    }

    public static PrivateKey loadPrivateKeyFromFile(String algorithm, String filePath) throws Exception {
        String keyString = new String(Files.readAllBytes(Paths.get(filePath)), StandardCharsets.UTF_8);
        return loadPrivateKeyFromString(algorithm, keyString);
    }

    public static PrivateKey loadPrivateKeyFromString(String algorithm, String keyString) throws Exception {
        byte[] decode = Base64.getDecoder().decode(keyString);
        KeyFactory keyFactory = KeyFactory.getInstance(algorithm);
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(decode);
        return keyFactory.generatePrivate(keySpec);
    }

    public static String encryptRSA(String algorithm, String input, Key key, int maxEncryptSize) throws Exception {
        Cipher cipher = Cipher.getInstance(algorithm);
        cipher.init(Cipher.ENCRYPT_MODE, key);
        byte[] data = input.getBytes();
        int total = data.length;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        decodeByte(maxEncryptSize, cipher, data, total, baos);
        return Base64.getEncoder().encodeToString(baos.toByteArray());
    }

    public static String decryptRSA(String algorithm, String encrypted, Key key, int maxDecryptSize) throws Exception {
        Cipher cipher = Cipher.getInstance(algorithm);
        cipher.init(Cipher.DECRYPT_MODE, key);
        byte[] data = Base64.getDecoder().decode(encrypted);
        int total = data.length;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        decodeByte(maxDecryptSize, cipher, data, total, baos);
        return baos.toString(StandardCharsets.UTF_8);
    }

    private static void decodeByte(int maxSize, Cipher cipher, byte[] data, int total, ByteArrayOutputStream baos) throws Exception {
        int offset = 0;
        byte[] buffer;
        while (total - offset > 0) {
            if (total - offset >= maxSize) {
                buffer = cipher.doFinal(data, offset, maxSize);
                offset += maxSize;
            } else {
                buffer = cipher.doFinal(data, offset, total - offset);
                offset = total;
            }
            baos.write(buffer);
        }
    }
}

package cryptoUtils;

import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
 
public class cryptoAES {
 
	public static final int KEY_LENGTH = 16;
	public static final String algorithm = "AES";
	// AES/CBC/NOPaddin
	// AES 默认模式
	// 使用CBC模式, 在初始化Cipher对象时, 需要增加参数, 初始化向量IV : IvParameterSpec iv = new
	// IvParameterSpec(key.getBytes());
	// NOPadding: 使用NOPadding模式时, 原文长度必须是8byte的整数倍
	public static final String transformation = "AES/CBC/PKCS5Padding";
    private String key; // base64存储
	private byte[] iv;  // 明文存储

	public cryptoAES(){
		this.key = null;
		this.iv = null;
	}

	public void setIV(byte[] iv) {
        if (iv.length != 16) {
            throw new IllegalArgumentException("IV length must be 16 bytes");
        }
        this.iv = iv;
    }

	public byte[] getIV() {
        return this.iv;
    }

	public static byte[] generateIV(){
		SecureRandom randomFigure = new SecureRandom();
        byte[] ivBytes = new byte[16];
		for ( int i = 0; i < 4; i++ )
			randomFigure.nextBytes(ivBytes);
		return ivBytes;
	}

	public static String AESKeyGen() {
        try {
            // 生成一个AES密钥
            KeyGenerator keyGen = KeyGenerator.getInstance("AES");
            keyGen.init(128); // 初始化密钥生成器，指定密钥长度为128位
            SecretKey secretKey = keyGen.generateKey();
            
            // 获取密钥的字节数组
            byte[] keyBytes = secretKey.getEncoded();
            
            // 将字节数组编码为Base64字符串
            return Base64.getEncoder().encodeToString(keyBytes);
        } catch (Exception e) {
            throw new RuntimeException("Error generating AES key", e);
        }
    }

	public cryptoAES(String initKey){
		this.key = initKey;
	}

    public void setKey(String newKey){
        this.key = newKey;
    }

    public String getKey(){
        return key;
    }
 
	/***
	 * 加密，得到base64编码后的密文
	 * @param plainData 需要加密的参数（注意必须是16位）
	 * @return
	 * @throws Exception
	 */
	public String encryptAES(String plainData) throws Exception {
		byte[] tmpKey = Base64.getDecoder().decode(this.key.getBytes());
        // 获取Cipher
        Cipher cipher = Cipher.getInstance(transformation);
        // 生成密钥
        SecretKeySpec keySpec = new SecretKeySpec(tmpKey, algorithm);
        // 创建初始化向量，确保长度为16字节
        IvParameterSpec iv = new IvParameterSpec(this.iv, 0, 16);
        // 指定模式(加密)和密钥
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, iv);
        byte[] bytes = cipher.doFinal(plainData.getBytes());
        return Base64.getEncoder().encodeToString(bytes); // 返回字节数组的String形式
    }
 
	/**
	 * 解密，输入base64编码的密文，返回明文
	 * @param cryptedData 需要解密的参数
	 * @return
	 * @throws Exception
	 */
	public String decryptAES(String cryptedData) throws Exception {
		byte[] tmpKey = Base64.getDecoder().decode(this.key.getBytes());
        // 获取Cipher
		Cipher cipher = Cipher.getInstance(transformation);
		// 生成密钥
		SecretKeySpec keySpec = new SecretKeySpec(tmpKey, algorithm);
		// 指定模式(解密)和密钥
		// 创建初始化向量
		IvParameterSpec iv = new IvParameterSpec(this.iv, 0, 16);
		cipher.init(Cipher.DECRYPT_MODE, keySpec, iv);
		// cipher.init(Cipher.DECRYPT_MODE, keySpec);
		// Base64解码
        byte[] decodedBytes = Base64.getDecoder().decode(cryptedData);
		// 解密
		byte[] bytes = cipher.doFinal(decodedBytes);
		return new String(bytes);
	}
 
	// public static void main(String [] args){
	// 	cryptoAES aes = new cryptoAES();
	// 	String str = "whatcanisayman?!";
	// 	try{
	// 		String enc = aes.encryptAES(str);
	// 		String dec = aes.decryptAES(enc);
	// 		byte[] encb = enc.getBytes();
	// 		byte[] decb = dec.getBytes();
	// 		System.out.println("AESKey:  "+aes.getKey().getBytes());
	// 		System.out.println("Encrypt: "+enc.getBytes()+"\nDecrpyt: "+dec.getBytes());
	// 		for(int i = 0; i < encb.length; i++ ){
	// 			System.out.print(Integer.toHexString(encb[i]));
	// 			System.out.println();
	// 		}
	// 	}catch(Exception e){
	// 		e.printStackTrace();
	// 	}
	// }

}
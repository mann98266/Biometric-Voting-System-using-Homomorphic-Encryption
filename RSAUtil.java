import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.security.*;
import java.security.spec.*;
import javax.crypto.Cipher;
import java.util.Base64;

public class RSAUtil {
    private static KeyPair keyPair;
    private static final String PUBLIC_KEY_FILE = "public.key";
    private static final String PRIVATE_KEY_FILE = "private.key";

    static {
        try {
            File publicKeyFile = new File(PUBLIC_KEY_FILE);
            File privateKeyFile = new File(PRIVATE_KEY_FILE);

            if (publicKeyFile.exists() && privateKeyFile.exists()) {
                // Load keys from files
                PublicKey publicKey = loadPublicKey(PUBLIC_KEY_FILE);
                PrivateKey privateKey = loadPrivateKey(PRIVATE_KEY_FILE);
                keyPair = new KeyPair(publicKey, privateKey);

            } else {
                // Generate a new key pair and save them
                KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
                generator.initialize(2048);
                keyPair = generator.generateKeyPair();
                saveKey(keyPair.getPublic(), PUBLIC_KEY_FILE);
                saveKey(keyPair.getPrivate(), PRIVATE_KEY_FILE);

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void saveKey(Key key, String fileName) throws Exception {
        // Save the key as a Base64 encoded string
        String keyString = Base64.getEncoder().encodeToString(key.getEncoded());
        try (FileWriter writer = new FileWriter(fileName)) {
            writer.write(keyString);
        }
    }

    private static PublicKey loadPublicKey(String fileName) throws Exception {
        String keyString = new String(Files.readAllBytes(java.nio.file.Paths.get(fileName)));
        byte[] keyBytes = Base64.getDecoder().decode(keyString);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
        KeyFactory factory = KeyFactory.getInstance("RSA");
        return factory.generatePublic(spec);
    }

    private static PrivateKey loadPrivateKey(String fileName) throws Exception {
        String keyString = new String(Files.readAllBytes(java.nio.file.Paths.get(fileName)));
        byte[] keyBytes = Base64.getDecoder().decode(keyString);
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory factory = KeyFactory.getInstance("RSA");
        return factory.generatePrivate(spec);
    }

    public static String encrypt(String data) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.ENCRYPT_MODE, keyPair.getPublic());
        return Base64.getEncoder().encodeToString(cipher.doFinal(data.getBytes()));
    }

    public static String decrypt(String encrypted) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.DECRYPT_MODE, keyPair.getPrivate());
        return new String(cipher.doFinal(Base64.getDecoder().decode(encrypted)));
    }

    public static KeyPair getKeyPair() {
        return keyPair;
    }
}

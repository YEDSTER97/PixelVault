import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;

public class CryptoManager {

    public static final String ALGORITHM = "AES";
    public static final String TRANSFORMATION = "AES/CBC/PKCS5Padding";
    public static final String PBE_ALGORITHM = "PBKDF2WithHmacSHA256";
    public static final int KEY_LENGTH = 256;
    public static final int ITERATION_COUNT = 65536;
    public static final int SALT_LENGTH = 16;
    public static final int IV_LENGTH = 16;

    private static SecretKey getKeyFromPassword(char[] password, byte[] salt)
            throws NoSuchAlgorithmException, InvalidKeySpecException {
        PBEKeySpec spec = null;
        try {
            spec = new PBEKeySpec(password, salt, ITERATION_COUNT, KEY_LENGTH);
            SecretKeyFactory factory = SecretKeyFactory.getInstance(PBE_ALGORITHM);
            SecretKey tmp = factory.generateSecret(spec);
            return new SecretKeySpec(tmp.getEncoded(), ALGORITHM);
        } finally {
            if (spec != null) {
                spec.clearPassword();
            }
        }
    }

    private static byte[] generateSalt() {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[SALT_LENGTH];
        random.nextBytes(salt);
        return salt;
    }

    private static byte[] generateIv() {
        SecureRandom random = new SecureRandom();
        byte[] iv = new byte[IV_LENGTH];
        random.nextBytes(iv);
        return iv;
    }

    public static void encryptFile(String inputFile, String outputFile, char[] password)
            throws GeneralSecurityException, IOException {
        byte[] salt = generateSalt();
        byte[] iv = generateIv();
        SecretKey secretKey = getKeyFromPassword(password, salt);

        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, new IvParameterSpec(iv));

        byte[] inputBytes;
        try {
            inputBytes = Files.readAllBytes(Paths.get(inputFile));
        } catch (OutOfMemoryError e) {
            throw new IOException("Input file too large for memory.", e);
        } catch (NoSuchFileException e) {
            throw new FileNotFoundException("Input file not found at: " + inputFile);
        } catch (IOException e) {
            throw new IOException("Failed to read input file: " + e.getMessage(), e);
        }

        byte[] encryptedBytes = cipher.doFinal(inputBytes);

        try (FileOutputStream fos = new FileOutputStream(outputFile)) {
            fos.write(salt);
            fos.write(iv);
            fos.write(encryptedBytes);
        } catch (IOException e) {
            throw new IOException("Failed to write output file: " + e.getMessage(), e);
        }
    }

    public static void decryptFile(String inputFile, String outputFile, char[] password)
            throws GeneralSecurityException, IOException {
        byte[] salt = new byte[SALT_LENGTH];
        byte[] iv = new byte[IV_LENGTH];
        byte[] encryptedBytes;

        try (InputStream fis = new BufferedInputStream(Files.newInputStream(Paths.get(inputFile)))) {
            if (fis.read(salt) != SALT_LENGTH || fis.read(iv) != IV_LENGTH) {
                throw new IOException("Could not read complete salt/IV header from file. File may be incomplete or corrupted.");
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            fis.transferTo(baos);
            encryptedBytes = baos.toByteArray();
            if (encryptedBytes.length == 0) {
                throw new IOException("No encrypted data found after salt/IV header in the file.");
            }
        } catch (NoSuchFileException e) {
            throw new FileNotFoundException("Encrypted input file not found at: " + inputFile);
        } catch (IOException e) {
            throw new IOException("Failed to read encrypted file: " + e.getMessage(), e);
        } catch (OutOfMemoryError e) {
            throw new IOException("Error reading encrypted file (file too large for memory?).", e);
        }

        SecretKey secretKey = getKeyFromPassword(password, salt);
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(iv));
        byte[] decryptedBytes = cipher.doFinal(encryptedBytes);

        try {
            Files.write(Paths.get(outputFile), decryptedBytes);
        } catch (IOException e) {
            throw new IOException("Failed to write decrypted output file: " + e.getMessage(), e);
        } catch (OutOfMemoryError e) {
            throw new IOException("Error writing decrypted file (decrypted data too large for memory?).", e);
        }
    }
}
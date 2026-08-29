package lk.jiat.globaltrade.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

public class PasswordSecurityUtil {

    private static final int SALT_BYTES = 16;

    /**
     * Generates a cryptographically secure random salt hex string.
     */
    public static String generateSalt() {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[SALT_BYTES];
        random.nextBytes(salt);
        return bytesToHex(salt);
    }

    /**
     * Computes a SHA-256 hash for plain text password concatenated with salt.
     */
    public static String hashPassword(String plainPassword, String salt) {
        if (plainPassword == null || salt == null) {
            throw new IllegalArgumentException("Password and salt must not be null");
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String saltedInput = salt + plainPassword;
            byte[] hash = digest.digest(saltedInput.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 Algorithm not available", e);
        }
    }

    /**
     * Verifies plain text password against stored salt and expected SHA-256 hash.
     */
    public static boolean verifyPassword(String plainPassword, String salt, String expectedHash) {
        if (plainPassword == null || expectedHash == null) {
            return false;
        }
        if (salt == null || salt.trim().isEmpty()) {
            // Fallback for un-salted plain text comparison
            return plainPassword.equals(expectedHash);
        }
        String computedHash = hashPassword(plainPassword, salt);
        return computedHash.equalsIgnoreCase(expectedHash);
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder(2 * bytes.length);
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
}

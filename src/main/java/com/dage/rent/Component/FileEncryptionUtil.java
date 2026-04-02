package com.dage.rent.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * 파일 내용 AES-256-GCM 암호화/복호화 (IV 12바이트, 태그 128비트)
 */
public final class FileEncryptionUtil {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final byte[] keyBytes;

    public FileEncryptionUtil(String base64Key) {
        if (base64Key == null || base64Key.isBlank()) {
            throw new IllegalArgumentException("file.encryption-key is required (Base64, 32 bytes)");
        }
        this.keyBytes = Base64.getDecoder().decode(base64Key.trim());
        if (keyBytes.length != 32) {
            throw new IllegalArgumentException("file.encryption-key must decode to 32 bytes (AES-256)");
        }
    }

    /**
     * 바이트 배열 암호화. [IV(12)][ciphertext][tag(16)] 형태로 반환.
     */
    public byte[] encrypt(byte[] plainBytes) throws Exception {
        byte[] iv = new byte[GCM_IV_LENGTH];
        RANDOM.nextBytes(iv);
        SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");
        GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec);
        byte[] encrypted = cipher.doFinal(plainBytes);
        return ByteBuffer.allocate(iv.length + encrypted.length).put(iv).put(encrypted).array();
    }

    /**
     * encrypt()로 만든 [IV][ciphertext] 형식 복호화
     */
    public byte[] decrypt(byte[] encryptedWithIv) throws Exception {
        if (encryptedWithIv == null || encryptedWithIv.length <= GCM_IV_LENGTH) {
            throw new IllegalArgumentException("Invalid encrypted data");
        }
        byte[] iv = new byte[GCM_IV_LENGTH];
        System.arraycopy(encryptedWithIv, 0, iv, 0, GCM_IV_LENGTH);
        byte[] cipherText = new byte[encryptedWithIv.length - GCM_IV_LENGTH];
        System.arraycopy(encryptedWithIv, GCM_IV_LENGTH, cipherText, 0, cipherText.length);
        SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");
        GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec);
        return cipher.doFinal(cipherText);
    }
}

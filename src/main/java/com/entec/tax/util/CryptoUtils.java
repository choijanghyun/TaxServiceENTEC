package com.entec.tax.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

/**
 * 암복호화 유틸리티 클래스
 * - AES 대칭키 암복호화
 * - SHA-256 해시
 * - Base64 인코딩/디코딩
 */
public final class CryptoUtils {

    private static final Logger log = LoggerFactory.getLogger(CryptoUtils.class);

    private CryptoUtils() {
        throw new IllegalStateException("유틸리티 클래스는 인스턴스를 생성할 수 없습니다.");
    }

    private static final String AES_ALGORITHM = "AES/CBC/PKCS5Padding";
    private static final String AES_KEY_ALGORITHM = "AES";
    private static final String SHA256_ALGORITHM = "SHA-256";

    // AES-128 키 길이: 16바이트
    private static final int AES_KEY_LENGTH = 16;

    /**
     * AES 암호화
     *
     * @param plainText 평문
     * @param secretKey 비밀키 (16자 이상)
     * @return Base64 인코딩된 암호문
     */
    public static String encryptAES(String plainText, String secretKey) {
        if (StringUtils.isBlank(plainText) || StringUtils.isBlank(secretKey)) {
            return null;
        }
        try {
            byte[] keyBytes = adjustKeyLength(secretKey);
            SecretKeySpec keySpec = new SecretKeySpec(keyBytes, AES_KEY_ALGORITHM);
            IvParameterSpec ivSpec = new IvParameterSpec(keyBytes);

            Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);

            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            log.error("[CryptoUtils] AES 암호화 실패", e);
            return null;
        }
    }

    /**
     * AES 복호화
     *
     * @param encryptedText Base64 인코딩된 암호문
     * @param secretKey 비밀키
     * @return 복호화된 평문
     */
    public static String decryptAES(String encryptedText, String secretKey) {
        if (StringUtils.isBlank(encryptedText) || StringUtils.isBlank(secretKey)) {
            return null;
        }
        try {
            byte[] keyBytes = adjustKeyLength(secretKey);
            SecretKeySpec keySpec = new SecretKeySpec(keyBytes, AES_KEY_ALGORITHM);
            IvParameterSpec ivSpec = new IvParameterSpec(keyBytes);

            Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);

            byte[] decoded = Base64.getDecoder().decode(encryptedText);
            byte[] decrypted = cipher.doFinal(decoded);
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("[CryptoUtils] AES 복호화 실패", e);
            return null;
        }
    }

    /**
     * SHA-256 해시
     *
     * @param text 원본 문자열
     * @return 16진수 해시값
     */
    public static String hashSHA256(String text) {
        if (StringUtils.isBlank(text)) {
            return null;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance(SHA256_ALGORITHM);
            byte[] hash = digest.digest(text.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (Exception e) {
            log.error("[CryptoUtils] SHA-256 해시 실패", e);
            return null;
        }
    }

    /**
     * SHA-256 해시 (Salt 포함)
     *
     * @param text 원본 문자열
     * @param salt Salt 문자열
     * @return 16진수 해시값
     */
    public static String hashSHA256WithSalt(String text, String salt) {
        if (StringUtils.isBlank(text)) {
            return null;
        }
        return hashSHA256(text + (salt != null ? salt : ""));
    }

    /**
     * Base64 인코딩
     */
    public static String encodeBase64(String text) {
        if (text == null) {
            return null;
        }
        return Base64.getEncoder().encodeToString(text.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Base64 디코딩
     */
    public static String decodeBase64(String base64Text) {
        if (base64Text == null) {
            return null;
        }
        try {
            byte[] decoded = Base64.getDecoder().decode(base64Text);
            return new String(decoded, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("[CryptoUtils] Base64 디코딩 실패", e);
            return null;
        }
    }

    /**
     * Base64 바이트 인코딩
     */
    public static String encodeBase64(byte[] data) {
        if (data == null) {
            return null;
        }
        return Base64.getEncoder().encodeToString(data);
    }

    /**
     * Base64 바이트 디코딩
     */
    public static byte[] decodeBase64ToBytes(String base64Text) {
        if (base64Text == null) {
            return null;
        }
        try {
            return Base64.getDecoder().decode(base64Text);
        } catch (Exception e) {
            log.error("[CryptoUtils] Base64 바이트 디코딩 실패", e);
            return null;
        }
    }

    /**
     * 키 길이 조정 (AES-128 기준 16바이트)
     */
    private static byte[] adjustKeyLength(String key) {
        byte[] keyBytes = new byte[AES_KEY_LENGTH];
        byte[] originalBytes = key.getBytes(StandardCharsets.UTF_8);
        System.arraycopy(originalBytes, 0, keyBytes, 0, Math.min(originalBytes.length, AES_KEY_LENGTH));
        return keyBytes;
    }

    /**
     * 바이트 배열을 16진수 문자열로 변환
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}

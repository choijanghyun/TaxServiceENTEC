package com.entec.tax.common.util;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * 암복호화 유틸리티 클래스.
 *
 * <p>세무 서비스에서 사용하는 암호화/복호화 기능을 제공합니다.</p>
 * <ul>
 *   <li>SHA-256 해시</li>
 *   <li>AES-256-CBC 암호화/복호화</li>
 *   <li>Base64 인코딩/디코딩</li>
 *   <li>JSON 체크섬 생성</li>
 * </ul>
 *
 * <p><strong>주의: AES-256은 키 길이가 32바이트(256비트)여야 합니다.</strong></p>
 *
 * @author ENTEC Tax Service
 * @since 1.0.0
 */
public final class CryptoUtil {

    /** AES 알고리즘 변환 방식 (CBC 모드, PKCS5 패딩) */
    private static final String AES_TRANSFORMATION = "AES/CBC/PKCS5Padding";

    /** AES 알고리즘 이름 */
    private static final String AES_ALGORITHM = "AES";

    /** SHA-256 알고리즘 이름 */
    private static final String SHA_256_ALGORITHM = "SHA-256";

    /** AES-256 키 길이 (바이트) */
    private static final int AES_KEY_LENGTH = 32;

    /** AES IV 길이 (바이트) */
    private static final int AES_IV_LENGTH = 16;

    /** 16진수 문자 배열 */
    private static final char[] HEX_CHARS = "0123456789abcdef".toCharArray();

    /**
     * 인스턴스 생성 방지를 위한 private 생성자.
     */
    private CryptoUtil() {
        throw new UnsupportedOperationException("유틸리티 클래스는 인스턴스화할 수 없습니다.");
    }

    /**
     * 문자열을 SHA-256으로 해시합니다.
     *
     * <p>입력 문자열을 UTF-8로 인코딩한 후 SHA-256 해시를 적용하고,
     * 결과를 16진수(Hex) 문자열로 반환합니다.</p>
     *
     * <p>예시:</p>
     * <ul>
     *   <li>"hello" → "2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824"</li>
     * </ul>
     *
     * @param data 해시할 문자열
     * @return SHA-256 해시값 (64자리 16진수 문자열, 소문자)
     * @throws IllegalArgumentException data가 null인 경우
     * @throws RuntimeException         SHA-256 알고리즘을 사용할 수 없는 경우
     */
    public static String sha256(String data) {
        if (data == null) {
            throw new IllegalArgumentException("해시할 데이터는 null일 수 없습니다.");
        }
        try {
            byte[] hashBytes = sha256Bytes(data.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hashBytes);
        } catch (Exception e) {
            throw new RuntimeException("SHA-256 해시 처리 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 바이트 배열을 SHA-256으로 해시합니다.
     *
     * <p>바이트 배열에 SHA-256 해시를 적용하여 해시된 바이트 배열을 반환합니다.</p>
     *
     * @param data 해시할 바이트 배열
     * @return SHA-256 해시된 바이트 배열 (32바이트)
     * @throws IllegalArgumentException data가 null인 경우
     * @throws RuntimeException         SHA-256 알고리즘을 사용할 수 없는 경우
     */
    public static byte[] sha256Bytes(byte[] data) {
        if (data == null) {
            throw new IllegalArgumentException("해시할 데이터는 null일 수 없습니다.");
        }
        try {
            MessageDigest digest = MessageDigest.getInstance(SHA_256_ALGORITHM);
            return digest.digest(data);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 알고리즘을 사용할 수 없습니다.", e);
        }
    }

    /**
     * AES-256-CBC 방식으로 문자열을 암호화합니다.
     *
     * <p>암호화 과정:</p>
     * <ol>
     *   <li>키를 32바이트(256비트)로 맞춤 (부족하면 0으로 패딩, 초과하면 절삭)</li>
     *   <li>IV는 키의 앞 16바이트를 사용</li>
     *   <li>AES/CBC/PKCS5Padding 방식으로 암호화</li>
     *   <li>결과를 Base64로 인코딩하여 반환</li>
     * </ol>
     *
     * <p>예시:</p>
     * <ul>
     *   <li>plainText="민감한 데이터", key="my-secret-key-1234567890123456" → Base64 암호화 문자열</li>
     * </ul>
     *
     * @param plainText 암호화할 평문 문자열
     * @param key       암호화 키 (32바이트 권장, 부족하면 0으로 패딩)
     * @return Base64로 인코딩된 암호화 문자열
     * @throws IllegalArgumentException plainText 또는 key가 null인 경우
     * @throws RuntimeException         암호화 처리 중 오류가 발생한 경우
     */
    public static String encryptAES(String plainText, String key) {
        if (plainText == null) {
            throw new IllegalArgumentException("암호화할 평문은 null일 수 없습니다.");
        }
        if (key == null) {
            throw new IllegalArgumentException("암호화 키는 null일 수 없습니다.");
        }
        try {
            byte[] keyBytes = adjustKeyLength(key.getBytes(StandardCharsets.UTF_8));
            SecretKeySpec secretKey = new SecretKeySpec(keyBytes, AES_ALGORITHM);
            IvParameterSpec iv = new IvParameterSpec(keyBytes, 0, AES_IV_LENGTH);

            Cipher cipher = Cipher.getInstance(AES_TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, iv);

            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            return encodeBase64(encrypted);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("AES 암호화 처리 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * AES-256-CBC 방식으로 암호화된 문자열을 복호화합니다.
     *
     * <p>복호화 과정:</p>
     * <ol>
     *   <li>Base64로 디코딩</li>
     *   <li>키를 32바이트(256비트)로 맞춤</li>
     *   <li>IV는 키의 앞 16바이트를 사용</li>
     *   <li>AES/CBC/PKCS5Padding 방식으로 복호화</li>
     * </ol>
     *
     * <p>예시:</p>
     * <ul>
     *   <li>cipherText="Base64암호화문자열", key="my-secret-key-1234567890123456" → "민감한 데이터"</li>
     * </ul>
     *
     * @param cipherText Base64로 인코딩된 암호화 문자열
     * @param key        복호화 키 (암호화 시 사용한 키와 동일해야 함)
     * @return 복호화된 평문 문자열
     * @throws IllegalArgumentException cipherText 또는 key가 null인 경우
     * @throws RuntimeException         복호화 처리 중 오류가 발생한 경우
     */
    public static String decryptAES(String cipherText, String key) {
        if (cipherText == null) {
            throw new IllegalArgumentException("복호화할 암호문은 null일 수 없습니다.");
        }
        if (key == null) {
            throw new IllegalArgumentException("복호화 키는 null일 수 없습니다.");
        }
        try {
            byte[] keyBytes = adjustKeyLength(key.getBytes(StandardCharsets.UTF_8));
            SecretKeySpec secretKey = new SecretKeySpec(keyBytes, AES_ALGORITHM);
            IvParameterSpec iv = new IvParameterSpec(keyBytes, 0, AES_IV_LENGTH);

            Cipher cipher = Cipher.getInstance(AES_TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, iv);

            byte[] decrypted = cipher.doFinal(decodeBase64(cipherText));
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("AES 복호화 처리 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 바이트 배열을 Base64로 인코딩합니다.
     *
     * <p>예시:</p>
     * <ul>
     *   <li>byte[]{72, 101, 108, 108, 111} → "SGVsbG8="</li>
     * </ul>
     *
     * @param data 인코딩할 바이트 배열
     * @return Base64로 인코딩된 문자열
     * @throws IllegalArgumentException data가 null인 경우
     */
    public static String encodeBase64(byte[] data) {
        if (data == null) {
            throw new IllegalArgumentException("인코딩할 데이터는 null일 수 없습니다.");
        }
        return Base64.getEncoder().encodeToString(data);
    }

    /**
     * Base64로 인코딩된 문자열을 바이트 배열로 디코딩합니다.
     *
     * <p>예시:</p>
     * <ul>
     *   <li>"SGVsbG8=" → byte[]{72, 101, 108, 108, 111}</li>
     * </ul>
     *
     * @param encoded Base64로 인코딩된 문자열
     * @return 디코딩된 바이트 배열
     * @throws IllegalArgumentException encoded가 null인 경우
     */
    public static byte[] decodeBase64(String encoded) {
        if (encoded == null) {
            throw new IllegalArgumentException("디코딩할 데이터는 null일 수 없습니다.");
        }
        return Base64.getDecoder().decode(encoded);
    }

    /**
     * JSON 문자열의 체크섬(SHA-256 해시)을 생성합니다.
     *
     * <p>JSON 문자열을 SHA-256으로 해시하여 무결성 검증에 사용할 수 있는 체크섬을 반환합니다.</p>
     *
     * <p>예시:</p>
     * <ul>
     *   <li>json="{\"name\":\"ENTEC\"}" → SHA-256 해시 문자열 (64자리 16진수)</li>
     * </ul>
     *
     * @param json 체크섬을 생성할 JSON 문자열
     * @return SHA-256 해시 체크섬 (64자리 16진수 문자열, 소문자)
     * @throws IllegalArgumentException json이 null인 경우
     */
    public static String generateChecksum(String json) {
        if (json == null) {
            throw new IllegalArgumentException("체크섬을 생성할 JSON 데이터는 null일 수 없습니다.");
        }
        return sha256(json);
    }

    /**
     * AES 키 길이를 32바이트로 조정하는 내부 메서드.
     *
     * <p>키가 32바이트보다 짧으면 0으로 패딩하고, 길면 32바이트로 절삭합니다.</p>
     *
     * @param keyBytes 원본 키 바이트 배열
     * @return 32바이트로 조정된 키 바이트 배열
     */
    private static byte[] adjustKeyLength(byte[] keyBytes) {
        byte[] adjustedKey = new byte[AES_KEY_LENGTH];
        int copyLength = Math.min(keyBytes.length, AES_KEY_LENGTH);
        System.arraycopy(keyBytes, 0, adjustedKey, 0, copyLength);
        return adjustedKey;
    }

    /**
     * 바이트 배열을 16진수 문자열로 변환하는 내부 메서드.
     *
     * @param bytes 변환할 바이트 배열
     * @return 16진수 문자열 (소문자)
     */
    private static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int i = 0; i < bytes.length; i++) {
            int v = bytes[i] & 0xFF;
            hexChars[i * 2] = HEX_CHARS[v >>> 4];
            hexChars[i * 2 + 1] = HEX_CHARS[v & 0x0F];
        }
        return new String(hexChars);
    }
}

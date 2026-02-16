package com.entec.tax.common.util;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

/**
 * 파일 유틸리티 클래스.
 *
 * <p>세무 서비스에서 사용하는 파일 관련 기능을 제공합니다.</p>
 * <ul>
 *   <li>파일 크기 검증</li>
 *   <li>파일 확장자 검증</li>
 *   <li>유니크 파일명 생성</li>
 *   <li>파일 삭제</li>
 * </ul>
 *
 * @author ENTEC Tax Service
 * @since 1.0.0
 */
public final class FileUtil {

    /**
     * 인스턴스 생성 방지를 위한 private 생성자.
     */
    private FileUtil() {
        throw new UnsupportedOperationException("유틸리티 클래스는 인스턴스화할 수 없습니다.");
    }

    /**
     * MultipartFile의 크기가 허용 범위 이내인지 검증합니다.
     *
     * <p>업로드된 파일의 크기가 지정된 최대 크기를 초과하지 않는지 확인합니다.</p>
     *
     * <p>예시:</p>
     * <ul>
     *   <li>file.size=5MB, maxSize=10MB → true</li>
     *   <li>file.size=15MB, maxSize=10MB → false</li>
     *   <li>file.size=0, maxSize=10MB → true (빈 파일도 허용)</li>
     * </ul>
     *
     * @param file    검증할 MultipartFile 객체
     * @param maxSize 허용 최대 파일 크기 (단위: 바이트)
     * @return 파일 크기가 허용 범위 이내이면 true, 초과하면 false
     * @throws IllegalArgumentException file이 null인 경우
     * @throws IllegalArgumentException maxSize가 0 이하인 경우
     */
    public static boolean validateFileSize(MultipartFile file, long maxSize) {
        if (file == null) {
            throw new IllegalArgumentException("검증할 파일은 null일 수 없습니다.");
        }
        if (maxSize <= 0) {
            throw new IllegalArgumentException("최대 파일 크기는 0보다 커야 합니다. 입력값: " + maxSize);
        }
        return file.getSize() <= maxSize;
    }

    /**
     * 파일명의 확장자가 허용된 확장자 목록에 포함되어 있는지 검증합니다.
     *
     * <p>대소문자를 구분하지 않고 비교합니다.</p>
     *
     * <p>예시:</p>
     * <ul>
     *   <li>filename="document.pdf", allowed=["pdf","xlsx"] → true</li>
     *   <li>filename="image.PNG", allowed=["png","jpg"] → true (대소문자 무시)</li>
     *   <li>filename="script.exe", allowed=["pdf","xlsx"] → false</li>
     *   <li>filename="noextension", allowed=["pdf"] → false</li>
     * </ul>
     *
     * @param filename 검증할 파일명 (확장자 포함)
     * @param allowed  허용 확장자 목록 (점(.) 없이, 예: "pdf", "xlsx")
     * @return 확장자가 허용 목록에 포함되면 true, 포함되지 않으면 false
     * @throws IllegalArgumentException filename이 null이거나 빈 값인 경우
     * @throws IllegalArgumentException allowed가 null이거나 빈 리스트인 경우
     */
    public static boolean validateFileExtension(String filename, List<String> allowed) {
        if (filename == null || filename.trim().isEmpty()) {
            throw new IllegalArgumentException("파일명은 null이거나 빈 값일 수 없습니다.");
        }
        if (allowed == null || allowed.isEmpty()) {
            throw new IllegalArgumentException("허용 확장자 목록은 null이거나 빈 리스트일 수 없습니다.");
        }

        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex < 0 || lastDotIndex == filename.length() - 1) {
            return false;
        }

        String extension = filename.substring(lastDotIndex + 1).toLowerCase();
        for (String allowedExt : allowed) {
            if (allowedExt != null && allowedExt.toLowerCase().equals(extension)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 원본 파일명을 기반으로 유니크 파일명을 생성합니다.
     *
     * <p>UUID를 사용하여 파일명 충돌을 방지하고, 원본 확장자를 유지합니다.</p>
     * <p>생성 형식: {UUID}.{원본확장자}</p>
     *
     * <p>예시:</p>
     * <ul>
     *   <li>"document.pdf" → "550e8400-e29b-41d4-a716-446655440000.pdf"</li>
     *   <li>"image.png" → "6ba7b810-9dad-11d1-80b4-00c04fd430c8.png"</li>
     *   <li>"noextension" → "550e8400-e29b-41d4-a716-446655440000"</li>
     * </ul>
     *
     * @param originalName 원본 파일명 (확장자 포함)
     * @return 유니크 파일명 (UUID 기반)
     * @throws IllegalArgumentException originalName이 null이거나 빈 값인 경우
     */
    public static String generateUniqueFileName(String originalName) {
        if (originalName == null || originalName.trim().isEmpty()) {
            throw new IllegalArgumentException("원본 파일명은 null이거나 빈 값일 수 없습니다.");
        }

        String uuid = UUID.randomUUID().toString();
        int lastDotIndex = originalName.lastIndexOf('.');
        if (lastDotIndex >= 0 && lastDotIndex < originalName.length() - 1) {
            String extension = originalName.substring(lastDotIndex + 1);
            return uuid + "." + extension;
        }
        return uuid;
    }

    /**
     * 지정된 경로의 파일을 삭제합니다.
     *
     * <p>파일이 존재하는 경우에만 삭제를 수행합니다.
     * 파일이 존재하지 않는 경우에는 false를 반환하며 예외를 발생시키지 않습니다.</p>
     *
     * <p>예시:</p>
     * <ul>
     *   <li>filePath="/uploads/document.pdf" (파일 존재) → true (삭제 성공)</li>
     *   <li>filePath="/uploads/nonexistent.pdf" (파일 없음) → false</li>
     * </ul>
     *
     * @param filePath 삭제할 파일의 절대 경로 또는 상대 경로
     * @return 파일 삭제 성공 시 true, 파일이 존재하지 않거나 삭제 실패 시 false
     * @throws IllegalArgumentException filePath가 null이거나 빈 값인 경우
     */
    public static boolean deleteFile(String filePath) {
        if (filePath == null || filePath.trim().isEmpty()) {
            throw new IllegalArgumentException("파일 경로는 null이거나 빈 값일 수 없습니다.");
        }

        try {
            Path path = Paths.get(filePath);
            if (Files.exists(path)) {
                Files.delete(path);
                return true;
            }
            return false;
        } catch (IOException e) {
            return false;
        }
    }
}

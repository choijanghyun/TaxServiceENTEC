package com.entec.tax.util;

import com.entec.tax.common.constants.CommonConstants;
import com.entec.tax.common.exception.BusinessException;
import com.entec.tax.common.exception.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.UUID;

/**
 * 파일 입출력 유틸리티 클래스
 * - 파일 업로드, 다운로드, 삭제, 확장자 검증 등
 */
public final class FileUtils {

    private static final Logger log = LoggerFactory.getLogger(FileUtils.class);

    private FileUtils() {
        throw new IllegalStateException("유틸리티 클래스는 인스턴스를 생성할 수 없습니다.");
    }

    /**
     * 파일 업로드
     *
     * @param file       MultipartFile 객체
     * @param uploadPath 업로드 기본 경로
     * @return 저장된 파일의 상대 경로
     */
    public static String uploadFile(MultipartFile file, String uploadPath) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.FILE_NOT_FOUND, "업로드할 파일이 없습니다.");
        }

        // 파일 크기 검증
        if (file.getSize() > CommonConstants.MAX_FILE_SIZE) {
            throw new BusinessException(ErrorCode.FILE_SIZE_EXCEEDED);
        }

        // 확장자 검증
        String originalFileName = file.getOriginalFilename();
        String extension = getExtension(originalFileName);
        if (!isAllowedExtension(extension)) {
            throw new BusinessException(ErrorCode.FILE_TYPE_NOT_ALLOWED);
        }

        try {
            // 날짜별 하위 디렉토리 생성
            String dateDir = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
            Path dirPath = Paths.get(uploadPath, dateDir);
            Files.createDirectories(dirPath);

            // 저장 파일명 생성 (UUID + 확장자)
            String savedFileName = UUID.randomUUID().toString() + "." + extension;
            Path filePath = dirPath.resolve(savedFileName);

            // 파일 저장
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            String relativePath = dateDir + "/" + savedFileName;
            log.info("[FileUtils] 파일 업로드 완료 - original={}, saved={}", originalFileName, relativePath);

            return relativePath;
        } catch (IOException e) {
            log.error("[FileUtils] 파일 업로드 실패 - {}", originalFileName, e);
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAIL, e);
        }
    }

    /**
     * 파일 삭제
     *
     * @param filePath 파일 경로
     * @return 삭제 성공 여부
     */
    public static boolean deleteFile(String filePath) {
        if (StringUtils.isBlank(filePath)) {
            return false;
        }
        try {
            Path path = Paths.get(filePath);
            if (Files.exists(path)) {
                Files.delete(path);
                log.info("[FileUtils] 파일 삭제 완료 - {}", filePath);
                return true;
            }
            return false;
        } catch (IOException e) {
            log.error("[FileUtils] 파일 삭제 실패 - {}", filePath, e);
            return false;
        }
    }

    /**
     * 파일 존재 여부 확인
     */
    public static boolean exists(String filePath) {
        if (StringUtils.isBlank(filePath)) {
            return false;
        }
        return Files.exists(Paths.get(filePath));
    }

    /**
     * 파일 확장자 추출
     *
     * @param fileName 파일명
     * @return 확장자 (소문자, 점 제외)
     */
    public static String getExtension(String fileName) {
        if (StringUtils.isBlank(fileName)) {
            return "";
        }
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex == -1 || lastDotIndex == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(lastDotIndex + 1).toLowerCase();
    }

    /**
     * 파일명에서 확장자 제외한 이름 추출
     */
    public static String getFileNameWithoutExtension(String fileName) {
        if (StringUtils.isBlank(fileName)) {
            return "";
        }
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex == -1) {
            return fileName;
        }
        return fileName.substring(0, lastDotIndex);
    }

    /**
     * 허용된 파일 확장자인지 검증
     */
    public static boolean isAllowedExtension(String extension) {
        if (StringUtils.isBlank(extension)) {
            return false;
        }
        return Arrays.asList(CommonConstants.ALLOWED_FILE_EXTENSIONS)
                .contains(extension.toLowerCase());
    }

    /**
     * 파일 크기를 사람이 읽을 수 있는 형식으로 변환
     * (예: 1024 -> "1 KB", 1048576 -> "1 MB")
     */
    public static String formatFileSize(long sizeInBytes) {
        if (sizeInBytes < 0) {
            return "0 B";
        }
        String[] units = {"B", "KB", "MB", "GB", "TB"};
        int unitIndex = 0;
        double size = sizeInBytes;

        while (size >= 1024 && unitIndex < units.length - 1) {
            size /= 1024;
            unitIndex++;
        }

        if (unitIndex == 0) {
            return (long) size + " " + units[unitIndex];
        }
        return String.format("%.1f %s", size, units[unitIndex]);
    }

    /**
     * 바이트 배열을 파일로 저장
     */
    public static void writeFile(byte[] data, String filePath) {
        if (data == null || StringUtils.isBlank(filePath)) {
            return;
        }
        try {
            Path path = Paths.get(filePath);
            Files.createDirectories(path.getParent());
            Files.write(path, data);
            log.info("[FileUtils] 파일 저장 완료 - {}", filePath);
        } catch (IOException e) {
            log.error("[FileUtils] 파일 저장 실패 - {}", filePath, e);
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAIL, e);
        }
    }

    /**
     * 파일을 바이트 배열로 읽기
     */
    public static byte[] readFileToBytes(String filePath) {
        if (StringUtils.isBlank(filePath)) {
            return null;
        }
        try {
            Path path = Paths.get(filePath);
            if (!Files.exists(path)) {
                throw new BusinessException(ErrorCode.FILE_NOT_FOUND);
            }
            return Files.readAllBytes(path);
        } catch (BusinessException e) {
            throw e;
        } catch (IOException e) {
            log.error("[FileUtils] 파일 읽기 실패 - {}", filePath, e);
            throw new BusinessException(ErrorCode.FILE_READ_ERROR, e);
        }
    }

    /**
     * 임시 파일 생성
     *
     * @param prefix 파일명 접두사
     * @param suffix 파일명 접미사 (확장자 포함)
     * @return 생성된 임시 파일의 경로
     */
    public static String createTempFile(String prefix, String suffix) {
        try {
            Path tempFile = Files.createTempFile(prefix, suffix);
            return tempFile.toString();
        } catch (IOException e) {
            log.error("[FileUtils] 임시 파일 생성 실패", e);
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAIL, "임시 파일 생성에 실패하였습니다.", e);
        }
    }
}

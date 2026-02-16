package com.entec.tax.common.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * JSON 유틸리티 클래스.
 *
 * <p>Jackson ObjectMapper를 사용하여 JSON 직렬화/역직렬화 기능을 제공합니다.</p>
 * <ul>
 *   <li>Object → JSON 문자열 변환</li>
 *   <li>JSON 문자열 → Object 변환</li>
 *   <li>JSON 문자열 → List 변환</li>
 *   <li>JSON 바이트 크기 계산</li>
 *   <li>JSON 스키마 검증</li>
 *   <li>JSON 필드 추출</li>
 * </ul>
 *
 * <p>ObjectMapper는 싱글턴으로 관리되며, Java 8 날짜/시간 API를 지원합니다.</p>
 *
 * @author ENTEC Tax Service
 * @since 1.0.0
 */
public final class JsonUtil {

    /**
     * Jackson ObjectMapper 싱글턴 인스턴스.
     * <p>스레드 안전하며, 다음 설정이 적용됩니다:</p>
     * <ul>
     *   <li>Java 8 날짜/시간 모듈 (JavaTimeModule) 등록</li>
     *   <li>날짜를 타임스탬프가 아닌 ISO 형식으로 직렬화</li>
     *   <li>알 수 없는 속성 무시</li>
     * </ul>
     */
    private static final ObjectMapper OBJECT_MAPPER;

    static {
        OBJECT_MAPPER = new ObjectMapper();
        OBJECT_MAPPER.registerModule(new JavaTimeModule());
        OBJECT_MAPPER.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        OBJECT_MAPPER.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    /**
     * 인스턴스 생성 방지를 위한 private 생성자.
     */
    private JsonUtil() {
        throw new UnsupportedOperationException("유틸리티 클래스는 인스턴스화할 수 없습니다.");
    }

    /**
     * 내부 ObjectMapper 인스턴스를 반환합니다.
     *
     * <p>특수한 경우에 ObjectMapper를 직접 사용해야 할 때 호출합니다.
     * 반환된 ObjectMapper의 설정을 변경하지 마십시오.</p>
     *
     * @return 공유 ObjectMapper 인스턴스
     */
    public static ObjectMapper getObjectMapper() {
        return OBJECT_MAPPER;
    }

    /**
     * 객체를 JSON 문자열로 변환합니다. (직렬화)
     *
     * <p>Jackson ObjectMapper를 사용하여 객체를 JSON 문자열로 직렬화합니다.</p>
     *
     * <p>예시:</p>
     * <ul>
     *   <li>Map("name", "ENTEC") → "{\"name\":\"ENTEC\"}"</li>
     *   <li>POJO 객체 → JSON 문자열</li>
     * </ul>
     *
     * @param obj JSON으로 변환할 객체
     * @return JSON 문자열
     * @throws IllegalArgumentException obj가 null인 경우
     * @throws RuntimeException         JSON 직렬화 중 오류가 발생한 경우
     */
    public static String toJson(Object obj) {
        if (obj == null) {
            throw new IllegalArgumentException("직렬화할 객체는 null일 수 없습니다.");
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("JSON 직렬화 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }

    /**
     * JSON 문자열을 지정된 타입의 객체로 변환합니다. (역직렬화)
     *
     * <p>Jackson ObjectMapper를 사용하여 JSON 문자열을 지정된 클래스 타입으로 역직렬화합니다.</p>
     *
     * <p>예시:</p>
     * <ul>
     *   <li>json="{\"name\":\"ENTEC\"}", type=MyDto.class → MyDto 객체</li>
     * </ul>
     *
     * @param json JSON 문자열
     * @param type 변환 대상 클래스 타입
     * @param <T>  반환 타입
     * @return 역직렬화된 객체
     * @throws IllegalArgumentException json 또는 type이 null인 경우
     * @throws RuntimeException         JSON 역직렬화 중 오류가 발생한 경우
     */
    public static <T> T fromJson(String json, Class<T> type) {
        if (json == null) {
            throw new IllegalArgumentException("역직렬화할 JSON 문자열은 null일 수 없습니다.");
        }
        if (type == null) {
            throw new IllegalArgumentException("변환 대상 타입은 null일 수 없습니다.");
        }
        try {
            return OBJECT_MAPPER.readValue(json, type);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(
                    "JSON 역직렬화 중 오류가 발생했습니다 (대상 타입: " + type.getName() + "): " + e.getMessage(), e);
        }
    }

    /**
     * JSON 문자열을 지정된 타입의 리스트로 변환합니다.
     *
     * <p>JSON 배열 문자열을 List&lt;T&gt;로 역직렬화합니다.</p>
     *
     * <p>예시:</p>
     * <ul>
     *   <li>json="[{\"name\":\"A\"},{\"name\":\"B\"}]", type=MyDto.class → List&lt;MyDto&gt;</li>
     * </ul>
     *
     * @param json JSON 배열 문자열
     * @param type 리스트 요소의 클래스 타입
     * @param <T>  리스트 요소 타입
     * @return 역직렬화된 리스트
     * @throws IllegalArgumentException json 또는 type이 null인 경우
     * @throws RuntimeException         JSON 역직렬화 중 오류가 발생한 경우
     */
    public static <T> List<T> fromJsonList(String json, Class<T> type) {
        if (json == null) {
            throw new IllegalArgumentException("역직렬화할 JSON 문자열은 null일 수 없습니다.");
        }
        if (type == null) {
            throw new IllegalArgumentException("변환 대상 타입은 null일 수 없습니다.");
        }
        try {
            return OBJECT_MAPPER.readValue(json,
                    OBJECT_MAPPER.getTypeFactory().constructCollectionType(List.class, type));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(
                    "JSON 리스트 역직렬화 중 오류가 발생했습니다 (대상 타입: List<" + type.getName() + ">): " + e.getMessage(), e);
        }
    }

    /**
     * JSON 문자열의 바이트 크기를 계산합니다.
     *
     * <p>UTF-8 인코딩 기준으로 JSON 문자열의 바이트 크기를 반환합니다.
     * API 요청 크기 제한 검증 등에 사용됩니다.</p>
     *
     * <p>예시:</p>
     * <ul>
     *   <li>json="{\"a\":1}" → 7 (바이트)</li>
     *   <li>json="{\"이름\":\"값\"}" → 한글 포함 시 바이트 수 증가</li>
     * </ul>
     *
     * @param json 바이트 크기를 계산할 JSON 문자열
     * @return JSON 문자열의 바이트 크기 (UTF-8 기준)
     * @throws IllegalArgumentException json이 null인 경우
     */
    public static int getJsonByteSize(String json) {
        if (json == null) {
            throw new IllegalArgumentException("바이트 크기를 계산할 JSON 문자열은 null일 수 없습니다.");
        }
        return json.getBytes(StandardCharsets.UTF_8).length;
    }

    /**
     * JSON 문자열이 유효한 JSON 스키마를 준수하는지 검증합니다.
     *
     * <p>클래스패스에서 스키마 파일을 로드하여 JSON 문자열의 구조를 검증합니다.
     * 이 메서드는 기본적인 JSON 파싱 가능 여부와 스키마 파일 존재 여부를 확인합니다.</p>
     *
     * <p>예시:</p>
     * <ul>
     *   <li>json="{\"name\":\"ENTEC\"}", schemaPath="/schemas/request.json" → true/false</li>
     * </ul>
     *
     * @param json       검증할 JSON 문자열
     * @param schemaPath 스키마 파일의 클래스패스 경로 (예: "/schemas/request.json")
     * @return JSON이 유효하면 true, 그렇지 않으면 false
     * @throws IllegalArgumentException json 또는 schemaPath가 null인 경우
     */
    public static boolean validateJsonSchema(String json, String schemaPath) {
        if (json == null) {
            throw new IllegalArgumentException("검증할 JSON 문자열은 null일 수 없습니다.");
        }
        if (schemaPath == null || schemaPath.trim().isEmpty()) {
            throw new IllegalArgumentException("스키마 파일 경로는 null이거나 빈 값일 수 없습니다.");
        }

        try {
            // 1단계: JSON 파싱 가능 여부 확인
            OBJECT_MAPPER.readTree(json);

            // 2단계: 스키마 파일 존재 여부 확인
            InputStream schemaStream = JsonUtil.class.getResourceAsStream(schemaPath);
            if (schemaStream == null) {
                throw new RuntimeException("스키마 파일을 찾을 수 없습니다: " + schemaPath);
            }
            schemaStream.close();

            // 3단계: 스키마의 필수 필드 검증 (기본 구조 검증)
            JsonNode schemaNode = OBJECT_MAPPER.readTree(
                    JsonUtil.class.getResourceAsStream(schemaPath));
            JsonNode jsonNode = OBJECT_MAPPER.readTree(json);

            // required 필드가 스키마에 정의된 경우 해당 필드 존재 여부 확인
            JsonNode requiredNode = schemaNode.get("required");
            if (requiredNode != null && requiredNode.isArray()) {
                for (JsonNode fieldName : requiredNode) {
                    if (!jsonNode.has(fieldName.asText())) {
                        return false;
                    }
                }
            }

            return true;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * JSON 문자열에서 지정된 필드 경로의 값을 추출합니다.
     *
     * <p>점(.)으로 구분된 필드 경로를 사용하여 중첩된 JSON 구조에서 값을 추출합니다.</p>
     *
     * <p>예시:</p>
     * <ul>
     *   <li>json="{\"a\":{\"b\":\"c\"}}", fieldPath="a.b" → "c"</li>
     *   <li>json="{\"name\":\"ENTEC\"}", fieldPath="name" → "ENTEC"</li>
     *   <li>json="{\"count\":123}", fieldPath="count" → "123"</li>
     *   <li>json="{\"a\":1}", fieldPath="b" → null (존재하지 않는 필드)</li>
     * </ul>
     *
     * @param json      JSON 문자열
     * @param fieldPath 추출할 필드 경로 (점 구분, 예: "data.items.name")
     * @return 추출된 필드 값의 문자열 표현. 필드가 존재하지 않으면 null 반환
     * @throws IllegalArgumentException json 또는 fieldPath가 null인 경우
     * @throws RuntimeException         JSON 파싱 중 오류가 발생한 경우
     */
    public static String extractField(String json, String fieldPath) {
        if (json == null) {
            throw new IllegalArgumentException("JSON 문자열은 null일 수 없습니다.");
        }
        if (fieldPath == null || fieldPath.trim().isEmpty()) {
            throw new IllegalArgumentException("필드 경로는 null이거나 빈 값일 수 없습니다.");
        }

        try {
            JsonNode rootNode = OBJECT_MAPPER.readTree(json);
            String[] paths = fieldPath.split("\\.");
            JsonNode currentNode = rootNode;

            for (String path : paths) {
                if (currentNode == null || currentNode.isMissingNode()) {
                    return null;
                }
                currentNode = currentNode.get(path);
            }

            if (currentNode == null || currentNode.isMissingNode() || currentNode.isNull()) {
                return null;
            }

            // 값 노드인 경우 텍스트 값 반환, 객체/배열인 경우 JSON 문자열 반환
            if (currentNode.isValueNode()) {
                return currentNode.asText();
            }
            return currentNode.toString();
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (JsonProcessingException e) {
            throw new RuntimeException("JSON 파싱 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }
}

# 세액공제 환급 계산 시스템 - 테스트 가이드

## 1. 테스트 케이스 구조

```
testCase/
├── generate_test_cases.py          # 테스트 케이스 자동 생성 스크립트
├── TEST_GUIDE.md                   # 본 문서
├── corp/                           # 법인(CORP) 테스트 케이스
│   ├── TC-CORP-01.xlsx / .json     # 중소기업 고용증대 세액공제
│   ├── TC-CORP-02.xlsx / .json     # 중소기업 R&D 세액공제 (신성장원천)
│   ├── TC-CORP-03.xlsx / .json     # 중소기업 투자세액공제
│   ├── TC-CORP-04.xlsx / .json     # 중견기업 복합세액공제
│   ├── TC-CORP-05.xlsx / .json     # 대기업 R&D + 최저한세
│   ├── TC-CORP-06.xlsx / .json     # 창업중소기업 감면
│   ├── TC-CORP-07.xlsx / .json     # 수도권 과밀억제권역 투자제한
│   ├── TC-CORP-08.xlsx / .json     # 인구감소지역 추가공제
│   ├── TC-CORP-09.xlsx / .json     # 이월공제 적용
│   └── TC-CORP-10.xlsx / .json     # 중복배제 검증
└── individual/                     # 개인(INC) 테스트 케이스
    ├── TC-INC-01.xlsx / .json      # 개인사업자 고용증대 기본
    ├── TC-INC-02.xlsx / .json      # 개인사업자 R&D 세액공제
    ├── TC-INC-03.xlsx / .json      # 창업감면 수도권 외
    ├── TC-INC-04.xlsx / .json      # 성실신고 대상자
    ├── TC-INC-05.xlsx / .json      # 복합세액공제 (고용+R&D)
    ├── TC-INC-06.xlsx / .json      # 간편장부 대상자
    ├── TC-INC-07.xlsx / .json      # 벤처기업 확인
    ├── TC-INC-08.xlsx / .json      # 인구감소지역 추가공제
    ├── TC-INC-09.xlsx / .json      # 경정청구 기한초과 (FAIL 기대)
    └── TC-INC-10.xlsx / .json      # 결손금 이월공제
```

## 2. 파일 형식 설명

### Excel 파일 (.xlsx)
각 Excel 파일은 5개 시트로 구성됩니다:

| 시트명 | 설명 |
|--------|------|
| **개요** | 테스트케이스 ID, 명칭, 설명, 세금유형, 신청자유형, 귀속연도 |
| **기본정보** | 신청자 기본정보 (INP_BASIC 테이블에 대응) |
| **고용정보** | 당기/전기 상시근로자 현황 (INP_EMPLOYEE 테이블에 대응) |
| **공제항목** | 신청 세액공제/감면 항목 (INP_DEDUCTION 테이블에 대응) |
| **재무정보** | 재무 관련 데이터 (INP_FINANCIAL 테이블에 대응) |

### JSON 파일 (.json)
API 요청 형태의 JSON 데이터입니다. 구조:

```json
{
  "request": { ... },      // REQ_REQUEST 테이블 데이터
  "basic": { ... },        // INP_BASIC 테이블 데이터
  "employees": [ ... ],    // INP_EMPLOYEE 테이블 데이터 (당기/전기)
  "deductions": [ ... ],   // INP_DEDUCTION 테이블 데이터
  "financial": { ... }     // INP_FINANCIAL 테이블 데이터
}
```

## 3. 테스트 시나리오 상세

### 법인 (CORP) - 10건

| ID | 시나리오 | 핵심 검증 포인트 | 기대 결과 |
|----|----------|------------------|-----------|
| TC-CORP-01 | 중소기업 고용증대 | 청년 +4, 일반 +3 증가분 공제액 계산 | 환급 발생 |
| TC-CORP-02 | R&D 신성장원천 | 신성장원천기술 30% 공제율 적용 | 환급 발생 |
| TC-CORP-03 | 투자세액공제 | 생산설비 투자 10% 공제 (수도권 외) | 환급 발생 |
| TC-CORP-04 | 복합세액공제 | 고용+R&D+투자 동시 적용, 최적 조합 산출 | 복수 조합 비교 |
| TC-CORP-05 | 대기업 R&D+최저한세 | 최저한세 17% 적용으로 공제 한도 제한 | 일부 이월 발생 |
| TC-CORP-06 | 창업중소기업 감면 | 수도권 외 벤처 창업 50~100% 감면 | 감면 적용 |
| TC-CORP-07 | 수도권과밀 투자제한 | 과밀억제권역 투자세액공제 제한 검증 | 제한 또는 감액 |
| TC-CORP-08 | 인구감소지역 추가공제 | 인구감소지역 추가 공제율 적용 | 추가공제 발생 |
| TC-CORP-09 | 이월공제 적용 | 전기 미사용 R&D 공제 이월분 당기 적용 | 이월분+당기분 합산 |
| TC-CORP-10 | 중복배제 검증 | 창업감면(§6)과 고용증대(§30의4) 중복배제 | 최적 단일 선택 |

### 개인 (INC) - 10건

| ID | 시나리오 | 핵심 검증 포인트 | 기대 결과 |
|----|----------|------------------|-----------|
| TC-INC-01 | 고용증대 기본 | 복식부기 개인사업자 고용증대 공제 | 환급 발생 |
| TC-INC-02 | R&D 세액공제 | IT업종 벤처 R&D 일반연구 25% 공제 | 환급 발생 |
| TC-INC-03 | 창업감면 수도권 외 | 수도권 외 벤처 창업 감면 (최대 100%) | 감면 적용 |
| TC-INC-04 | 성실신고 대상 | 성실신고확인대상자 특별 규칙 적용 | 성실신고 플래그 |
| TC-INC-05 | 복합세액공제 | 고용증대+R&D 복합 적용, 종소세 최저한세 | 복수 조합 비교 |
| TC-INC-06 | 간편장부 대상자 | 간편장부 대상자 세액공제 적용 가능 여부 | 제한 검증 |
| TC-INC-07 | 벤처기업 확인 | 벤처기업 확인 시 추가 혜택 적용 | 벤처 우대 적용 |
| TC-INC-08 | 인구감소지역 | 인구감소지역 개인사업자 추가공제 | 추가공제 발생 |
| TC-INC-09 | 기한초과 (FAIL) | 2018년 귀속 → 5년 경과 기한초과 | **기한초과 FAIL** |
| TC-INC-10 | 결손금 이월 | 전기 결손금 7천만원 이월 후 공제 적용 | 과세표준 감소 반영 |

## 4. 테스트 실행 방법

### 4-1. JSON 파일로 API 직접 호출

```bash
# 단건 테스트
curl -X POST http://localhost:8080/api/v1/refund/calculate \
  -H "Content-Type: application/json" \
  -d @testCase/corp/TC-CORP-01.json

# 전체 법인 테스트 일괄 실행
for f in testCase/corp/TC-CORP-*.json; do
  echo "=== $(basename $f) ==="
  curl -s -X POST http://localhost:8080/api/v1/refund/calculate \
    -H "Content-Type: application/json" \
    -d @$f | python3 -m json.tool
  echo ""
done

# 전체 개인 테스트 일괄 실행
for f in testCase/individual/TC-INC-*.json; do
  echo "=== $(basename $f) ==="
  curl -s -X POST http://localhost:8080/api/v1/refund/calculate \
    -H "Content-Type: application/json" \
    -d @$f | python3 -m json.tool
  echo ""
done
```

### 4-2. JUnit 테스트에서 활용

```java
@SpringBootTest
class RefundCalculationIntegrationTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RefundCalculationService refundCalculationService;

    @ParameterizedTest
    @ValueSource(strings = {
        "testCase/corp/TC-CORP-01.json",
        "testCase/corp/TC-CORP-02.json",
        // ... 추가 케이스
    })
    void testCorpCases(String jsonPath) throws Exception {
        String json = Files.readString(Path.of(jsonPath));
        RefundRequest request = objectMapper.readValue(json, RefundRequest.class);

        RefundResponse response = refundCalculationService.calculate(request);

        assertNotNull(response);
        assertNotNull(response.getRefundAmount());
    }
}
```

### 4-3. Excel 파일 검증용 활용

Excel 파일은 비개발자(세무사, 회계사)와의 데이터 검증 시 활용합니다:

1. Excel 파일을 열어 입력 데이터 확인
2. 수기 계산으로 기대 결과값 산출
3. API 실행 결과와 비교 검증

## 5. 테스트 케이스 재생성

테스트 데이터를 수정하거나 새로운 케이스를 추가하려면:

```bash
# generate_test_cases.py의 CORP_CASES 또는 INC_CASES 리스트 수정 후
python3 testCase/generate_test_cases.py
```

## 6. 검증 체크리스트

각 테스트 케이스 실행 후 아래 항목을 확인합니다:

### 공통 검증
- [ ] 요청 접수 및 상태 전이 (RECEIVED → PROCESSING → COMPLETED)
- [ ] CHK_ELIGIBILITY 자격 검증 결과 생성 확인
- [ ] CHK_INSPECTION_LOG 점검 로그 생성 확인
- [ ] CHK_VALIDATION_LOG 검증 로그 생성 확인
- [ ] LOG_CALCULATION 계산 로그 기록 확인

### 계산 결과 검증
- [ ] OUT_EMPLOYEE_SUMMARY 상시근로자 증감 계산 정확성
- [ ] OUT_CREDIT_DETAIL 개별 공제 항목 금액 정확성
- [ ] OUT_COMBINATION 최적 조합 산출 적절성
- [ ] OUT_EXCLUSION_VERIFY 중복배제 검증 결과
- [ ] OUT_REFUND 최종 환급액 계산 정확성
- [ ] OUT_RISK 사후관리 의무/리스크 도출

### 특수 시나리오 검증
- [ ] TC-CORP-05: 최저한세로 인한 공제 한도 제한 확인
- [ ] TC-CORP-07: 수도권 과밀억제권역 투자 제한 확인
- [ ] TC-CORP-10: 중복배제 규칙(§6 vs §30의4) 작동 확인
- [ ] TC-INC-06: 간편장부 대상자 세액공제 적용 제한 확인
- [ ] TC-INC-09: 경정청구 기한(5년) 초과 시 FAIL 응답 확인
- [ ] TC-INC-10: 결손금 이월로 과세표준 감소 반영 확인

## 7. 금액 단위

- 모든 금액은 **원(KRW)** 단위
- 상시근로자 수는 **소수점 2자리** (시간제 환산 포함)
- 공제율은 **퍼센트(%)** 단위 (예: 25.00 = 25%)

# TaxServiceENTEC 테이블 정의서

## 개요

본 문서는 **통합 경정청구 환급액 산출 시스템(TaxServiceENTEC)**의 데이터베이스 테이블 정의서입니다.
법인사업자의 **법인세** 및 개인사업자의 **종합소득세** 경정청구 시 환급액을 극대화하기 위한 AI 점검 시스템의 전체 데이터 모델을 기술합니다.

## 아키텍처

시스템은 **Request-Driven 3계층 데이터 아키텍처**를 채택합니다.

```
Client → API(req_id 발급) → INP_RAW_DATA(원시 JSON 보관)
                                  ↓
                           INP_* 요약 테이블 (파싱)
                                  ↓
                      REF_* 기준정보 참조 + 계산 엔진
                                  ↓
                         OUT_* 산출 결과 저장
                                  ↓
                    OUT_REPORT_JSON (7섹션 JSON 조립)
                                  ↓
                          Client 응답 전달
```

- **req_id**: 기준정보(REF_*) 테이블을 제외한 모든 테이블에 관통하는 요청 식별자
- **입력 원본 불변성**: INP_RAW_DATA는 INSERT ONLY (수정 시 새 req_id 발급)
- **Hybrid 저장**: 원본/결과는 JSON, 계산은 RDB 테이블 활용

## 주제영역 분류

| No | 주제영역 | 접두사 | 테이블 수 | 정의서 파일 | 설명 |
|----|---------|--------|----------|------------|------|
| 1 | 요청 관리 | `REQ_` | 1개 | [01_REQ_요청관리.md](./01_REQ_요청관리.md) | 경정청구 요청의 생성, 상태 추적, 감사 정보 관리 |
| 2 | 입력 데이터 | `INP_` | 5개 | [02_INP_입력데이터.md](./02_INP_입력데이터.md) | 원시 JSON 보관 및 계산용 요약 데이터 |
| 3 | 검증 결과 | `CHK_` | 3개 | [03_CHK_검증결과.md](./03_CHK_검증결과.md) | 자격 진단, 점검항목 판정, 검증 규칙 실행 결과 |
| 4 | 산출 결과 | `OUT_` | 8개 | [04_OUT_산출결과.md](./04_OUT_산출결과.md) | 공제 산출, 조합 비교, 환급액 계산, 리스크 평가 |
| 5 | 감사 로그 | `LOG_` | 1개 | [05_LOG_감사로그.md](./05_LOG_감사로그.md) | 계산 단계별 실행 이력 추적 |
| 6 | 기준 정보 | `REF_` | 22개 | [06_REF_기준정보.md](./06_REF_기준정보.md) | 세율, 공제율, 지역분류, 업종 등 참조 데이터 |

## 테이블 전체 목록 (40개)

### 요청 관리 (1개)
| 테이블명 | 한글명 | PK |
|----------|--------|-----|
| REQ_REQUEST | 요청 마스터 | req_id |

### 입력 데이터 (5개)
| 테이블명 | 한글명 | PK |
|----------|--------|-----|
| INP_RAW_DATA | 원시 입력자료 보관 | raw_id (AUTO_INCREMENT) |
| INP_BASIC | 신청인 기본정보 요약 | req_id |
| INP_EMPLOYEE | 고용 정보 요약 | req_id + year_type |
| INP_DEDUCTION | 공제/감면 기초 요약 | req_id + item_category + provision + tax_year + item_seq |
| INP_FINANCIAL | 재무/세무 수치 요약 | req_id |

### 검증 결과 (3개)
| 테이블명 | 한글명 | PK |
|----------|--------|-----|
| CHK_ELIGIBILITY | 자격 진단 결과 | req_id |
| CHK_INSPECTION_LOG | 점검항목별 판정 | req_id + inspection_code |
| CHK_VALIDATION_LOG | 검증 규칙 실행 결과 | req_id + rule_code |

### 산출 결과 (8개)
| 테이블명 | 한글명 | PK |
|----------|--------|-----|
| OUT_EMPLOYEE_SUMMARY | 상시근로자 산정 | req_id + year_type |
| OUT_CREDIT_DETAIL | 개별 공제/감면 산출 | req_id + item_id |
| OUT_COMBINATION | 조합 비교/최적 선택 | req_id + combo_id |
| OUT_EXCLUSION_VERIFY | 상호배제 검증 | req_id + verify_id |
| OUT_REFUND | 최종 환급액 산출 | req_id |
| OUT_RISK | 사후관리/리스크 평가 | req_id + risk_id |
| OUT_ADDITIONAL_CHECK | 추가 확인 필요 | req_id + check_id |
| OUT_REPORT_JSON | 최종 보고서 JSON | req_id |

### 감사 로그 (1개)
| 테이블명 | 한글명 | PK |
|----------|--------|-----|
| LOG_CALCULATION | 계산 감사추적 로그 | log_id (AUTO_INCREMENT) |

### 기준 정보 (22개)
| 테이블명 | 한글명 | PK |
|----------|--------|-----|
| REF_TAX_RATE | 법인세율 | rate_id |
| REF_MIN_TAX_RATE | 최저한세율 | min_tax_id |
| REF_EMPLOYMENT_CREDIT | 고용증대 세액공제율 | credit_id |
| REF_INVESTMENT_CREDIT_RATE | 투자 세액공제율 | rate_id |
| REF_MUTUAL_EXCLUSION | 상호배제 규칙 | rule_id |
| REF_CAPITAL_ZONE | 수도권/비수도권 분류 | zone_id |
| REF_NONGTEUKSE | 농어촌특별세 면제 규칙 | provision |
| REF_SME_DEDUCTION_RATE | 중소기업 특별세액감면율 | rate_id |
| REF_RD_CREDIT_RATE | R&D 세액공제율 | rate_id |
| REF_REFUND_INTEREST_RATE | 환급가산금 이자율 | rate_id |
| REF_STARTUP_DEDUCTION_RATE | 창업중소기업 감면율 | rate_id |
| REF_DEPOPULATION_AREA | 인구감소지역 지정 | area_id |
| REF_CORP_TAX_RATE_HISTORY | 법인세율 이력 | rate_id |
| REF_LAW_VERSION | 세법 버전 관리 | version_id |
| REF_DIVIDEND_EXCLUSION | 배당소득 익금불산입율 | exclusion_id |
| REF_RD_MIN_TAX_EXEMPT | R&D 최저한세 면제율 | rd_type + corp_size |
| REF_INDUSTRY_ELIGIBILITY | 업종별 적격성 | ksic_code |
| REF_ENTERTAINMENT_LIMIT | 접대비 한도 | limit_id |
| REF_SYSTEM_PARAM | 시스템 파라미터 | param_key |
| REF_KSIC_CODE | 한국표준산업분류코드 | ksic_code |
| REF_EXCHANGE_RATE | 환율 | rate_date + currency |
| REF_DEEMED_INTEREST_RATE | 인정이자율 | year + rate_type |

### 기준 정보 - 종합소득세 (4개, REF_에 포함)
| 테이블명 | 한글명 | PK |
|----------|--------|-----|
| REF_INC_TAX_RATE | 종합소득세율 | effective_from + bracket_no |
| REF_INC_MIN_TAX | 종합소득세 최저한세 | effective_from |
| REF_INC_DEDUCTION_LIMIT | 소득공제 한도 | deduction_type + income_bracket |
| REF_INC_SINCERITY_THRESHOLD | 성실신고 확인 기준 | industry_group + effective_from |

## ER 다이어그램 (주요 관계)

```
REQ_REQUEST (1) ─── req_id ───┬── (N) INP_RAW_DATA
                               ├── (1) INP_BASIC
                               ├── (N) INP_EMPLOYEE
                               ├── (N) INP_DEDUCTION
                               ├── (1) INP_FINANCIAL
                               ├── (1) CHK_ELIGIBILITY
                               ├── (N) CHK_INSPECTION_LOG
                               ├── (N) CHK_VALIDATION_LOG
                               ├── (N) OUT_EMPLOYEE_SUMMARY
                               ├── (N) OUT_CREDIT_DETAIL
                               ├── (N) OUT_COMBINATION
                               ├── (N) OUT_EXCLUSION_VERIFY
                               ├── (1) OUT_REFUND
                               ├── (N) OUT_RISK
                               ├── (N) OUT_ADDITIONAL_CHECK
                               ├── (1) OUT_REPORT_JSON
                               └── (N) LOG_CALCULATION

REF_* 테이블들은 req_id를 갖지 않으며 계산 엔진에서 참조 전용으로 사용
```

## 데이터 타입 규칙

| 용도 | 데이터 타입 | 비고 |
|------|-----------|------|
| 금액(원) | BIGINT | 원 단위, TRUNCATE 절사 (반올림 금지) |
| 비율(%) | DECIMAL(5,2) | 소수점 2자리 |
| 정밀 비율 | DECIMAL(5,4) 또는 DECIMAL(7,5) | 접대비율, 환급가산금 이자율 등 |
| 일반 날짜 | DATE | 표준 날짜 |
| 감사 시간 | TIMESTAMP | 자동 기록 |
| 상세 데이터(JSON) | LONGTEXT 또는 TEXT | JSON 문서 저장 |
| 식별자/코드 | VARCHAR | 가변 길이 문자열 |

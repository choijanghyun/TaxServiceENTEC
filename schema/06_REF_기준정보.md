# 주제영역 6: 기준 정보 (REF)

## 개요

세율, 공제율, 지역 분류, 업종 분류, 상호배제 규칙 등 **세법 기준 데이터**를 관리하는 영역입니다.
이 영역의 테이블들은 **req_id를 갖지 않으며**, 계산 엔진에서 참조 전용으로 사용됩니다.

### 설계 원칙

- **사업연도별 법령 적용**: 세율/공제율은 대상 연도(year_from ~ year_to)에 따라 다르게 적용
- **정적 데이터**: 요청과 무관하게 사전 등재되며, 시스템 운영자가 세법 개정 시 갱신
- **3개 하위 그룹**: 법인세 기준정보, 환율/이자율, 종합소득세 기준정보

---

## 하위 분류

| 분류 | 테이블 수 | 테이블 |
|------|----------|--------|
| 법인세 기준정보 | 16개 | REF_TAX_RATE ~ REF_ENTERTAINMENT_LIMIT |
| 공통 기준정보 | 4개 | REF_SYSTEM_PARAM, REF_KSIC_CODE, REF_EXCHANGE_RATE, REF_DEEMED_INTEREST_RATE |
| 종합소득세 기준정보 | 4개 | REF_INC_TAX_RATE ~ REF_INC_SINCERITY_THRESHOLD |

---

## 법인세 기준정보

### REF_TAX_RATE - 법인세율

| 항목 | 내용 |
|------|------|
| **테이블명** | REF_TAX_RATE |
| **한글명** | 법인세율 |
| **설명** | 과세표준 구간별 법인세율 및 누진공제액. 사업연도별로 적용 세율이 다를 수 있음 |
| **PK** | rate_id |

| No | 컬럼명 | 데이터 타입 | NULL | 설명 |
|----|--------|-----------|------|------|
| 1 | rate_id | INT | NOT NULL | 레코드 식별자 |
| 2 | year_from | VARCHAR(4) | NULL | 적용 시작 연도 |
| 3 | year_to | VARCHAR(4) | NULL | 적용 종료 연도 |
| 4 | bracket_min | BIGINT | NULL | 과세표준 하한 (원) |
| 5 | bracket_max | BIGINT | NULL | 과세표준 상한 (원) |
| 6 | tax_rate | DECIMAL(5,2) | NULL | 세율 (%) |
| 7 | progressive_deduction | BIGINT | NULL | 누진공제액 (원) |

**활용**: 과세표준(INP_BASIC.taxable_income)이 속하는 구간의 세율을 적용하여 산출세액 계산. `산출세액 = 과세표준 × tax_rate - progressive_deduction`

---

### REF_MIN_TAX_RATE - 최저한세율

| 항목 | 내용 |
|------|------|
| **테이블명** | REF_MIN_TAX_RATE |
| **한글명** | 최저한세율 |
| **설명** | 기업 규모 및 과세표준 구간별 최저한세 세율. 공제/감면 적용 후에도 최소한 이 비율 이상의 세액을 부담해야 함 |
| **PK** | min_tax_id |

| No | 컬럼명 | 데이터 타입 | NULL | 설명 |
|----|--------|-----------|------|------|
| 1 | min_tax_id | INT | NOT NULL | 레코드 식별자 |
| 2 | corp_size | VARCHAR(10) | NULL | 기업 규모. `SMALL`, `MEDIUM`, `LARGE` |
| 3 | bracket_min | BIGINT | NULL | 과세표준 하한 (원) |
| 4 | bracket_max | BIGINT | NULL | 과세표준 상한 (원) |
| 5 | min_rate | DECIMAL(5,2) | NULL | 최저한세율 (%). 법인세: 7~17% |

**활용**: M5에서 공제/감면 적용 후 결정세액이 `과세표준 × min_rate`보다 낮으면, 초과분을 이월공제로 전환. 중소기업(7%), 중견기업(8~10%), 대기업(17%) 등 차등 적용.

---

### REF_EMPLOYMENT_CREDIT - 고용증대 세액공제율

| 항목 | 내용 |
|------|------|
| **테이블명** | REF_EMPLOYMENT_CREDIT |
| **한글명** | 고용증대 세액공제율 |
| **설명** | 기업 규모, 지역, 근로자 유형별 1인당 세액공제 금액. 조특법 §29의7 근거 |
| **PK** | credit_id |

| No | 컬럼명 | 데이터 타입 | NULL | 설명 |
|----|--------|-----------|------|------|
| 1 | credit_id | INT | NOT NULL | 레코드 식별자 |
| 2 | tax_year | VARCHAR(4) | NULL | 적용 사업연도 |
| 3 | corp_size | VARCHAR(10) | NULL | 기업 규모. `SMALL`, `MEDIUM` |
| 4 | region | VARCHAR(10) | NULL | 지역. `CAPITAL`(수도권), `NON_CAPITAL`(비수도권) |
| 5 | worker_type | VARCHAR(20) | NULL | 근로자 유형. `YOUTH`(청년), `DISABLED`(장애인), `AGED`(고령자), `GENERAL`(일반) |
| 6 | credit_per_person | BIGINT | NULL | 1인당 세액공제 금액 (원) |

**활용**: `고용증대 공제 = 유형별 증가 인원(OUT_EMPLOYEE_SUMMARY) × credit_per_person`. 청년은 일반보다 높은 공제액, 비수도권은 수도권보다 높은 공제액 적용.

---

### REF_INVESTMENT_CREDIT_RATE - 투자 세액공제율

| 항목 | 내용 |
|------|------|
| **테이블명** | REF_INVESTMENT_CREDIT_RATE |
| **한글명** | 투자 세액공제율 |
| **설명** | 투자 유형 및 기업 규모별 기본공제율과 추가공제율. 조특법 §25의4(통합투자), §25의5(임시투자) 등 근거 |
| **PK** | rate_id |

| No | 컬럼명 | 데이터 타입 | NULL | 설명 |
|----|--------|-----------|------|------|
| 1 | rate_id | INT | NOT NULL | 레코드 식별자 |
| 2 | tax_year_from | VARCHAR(4) | NULL | 적용 시작 연도 |
| 3 | invest_type | VARCHAR(30) | NULL | 투자 유형. `INTEGRATED`(통합투자), `TEMPORARY`(임시투자), `FACILITY`(시설투자) |
| 4 | corp_size | VARCHAR(10) | NULL | 기업 규모 |
| 5 | basic_rate | DECIMAL(5,2) | NULL | 기본 공제율 (%) |
| 6 | additional_rate | DECIMAL(5,2) | NULL | 추가 공제율 (%, 직전 3년 평균 대비 초과분) |

**활용**: `투자 공제 = 투자액 × basic_rate + 초과투자액 × additional_rate`. INP_DEDUCTION의 base_amount에 적용.

---

### REF_MUTUAL_EXCLUSION - 상호배제 규칙

| 항목 | 내용 |
|------|------|
| **테이블명** | REF_MUTUAL_EXCLUSION |
| **한글명** | 상호배제 규칙 |
| **설명** | 세액공제/감면 간 중복적용 금지 규칙. 조특법 §127④ 근거. 동시 적용 불가한 법조 쌍을 정의 |
| **PK** | rule_id |

| No | 컬럼명 | 데이터 타입 | NULL | 설명 |
|----|--------|-----------|------|------|
| 1 | rule_id | INT | NOT NULL | 규칙 식별자 |
| 2 | provision_a | VARCHAR(20) | NULL | 법조 A. 예: `조특법6` |
| 3 | provision_b | VARCHAR(20) | NULL | 법조 B. 예: `조특법7` |
| 4 | year_from | VARCHAR(4) | NULL | 적용 시작 연도 |
| 5 | year_to | VARCHAR(4) | NULL | 적용 종료 연도 |
| 6 | is_allowed | BOOLEAN | NULL | 중복 허용 여부 (true=허용, false=배제) |
| 7 | condition_note | VARCHAR(500) | NULL | 조건부 허용 시 조건 설명 |
| 8 | legal_basis | VARCHAR(100) | NULL | 근거법조 |

**활용**: OUT_COMBINATION에서 조합 생성 시, 포함된 항목 쌍이 이 테이블의 배제 규칙에 해당하면 해당 조합을 무효(is_valid=false) 처리. OUT_EXCLUSION_VERIFY에 검증 결과 기록.

---

### REF_CAPITAL_ZONE - 수도권/비수도권 분류

| 항목 | 내용 |
|------|------|
| **테이블명** | REF_CAPITAL_ZONE |
| **한글명** | 수도권/비수도권 분류 |
| **설명** | 시도/시군구별 수도권 과밀억제권역 여부 및 인구감소지역 해당 여부 |
| **PK** | zone_id |

| No | 컬럼명 | 데이터 타입 | NULL | 설명 |
|----|--------|-----------|------|------|
| 1 | zone_id | INT | NOT NULL | 지역 식별자 |
| 2 | sido | VARCHAR(20) | NULL | 시도명. 예: `서울특별시`, `경기도` |
| 3 | sigungu | VARCHAR(50) | NULL | 시군구명. 예: `강남구`, `수원시 영통구` |
| 4 | zone_type | VARCHAR(20) | NULL | 지역 유형. `CAPITAL`(수도권 과밀억제권역), `CAPITAL_OTHER`(수도권 비과밀), `NON_CAPITAL`(비수도권) |
| 5 | is_capital | BOOLEAN | NULL | 수도권 해당 여부 |
| 6 | is_depopulation | BOOLEAN | NULL | 인구감소지역 해당 여부 |

**활용**: INP_BASIC.hq_location을 기반으로 수도권/비수도권 구분. 투자 공제, 고용 공제, 창업중소기업 감면 등에서 지역별 차등 혜택 적용.

---

### REF_NONGTEUKSE - 농어촌특별세 면제 규칙

| 항목 | 내용 |
|------|------|
| **테이블명** | REF_NONGTEUKSE |
| **한글명** | 농어촌특별세 면제 규칙 |
| **설명** | 법조별 농어촌특별세 면제 여부 및 세율. 면제 대상이 아닌 공제/감면에 대해 농특세 부과 |
| **PK** | provision |

| No | 컬럼명 | 데이터 타입 | NULL | 설명 |
|----|--------|-----------|------|------|
| 1 | provision | VARCHAR(20) | NOT NULL | 세액공제/감면 근거 법조 |
| 2 | is_exempt | BOOLEAN | NULL | 농특세 면제 여부 (true=면제) |
| 3 | tax_rate | DECIMAL(5,2) | NULL | 농특세율 (%). 보통 20% |
| 4 | legal_basis | VARCHAR(100) | NULL | 근거법조 |

**활용**: OUT_CREDIT_DETAIL 산출 시, 각 항목의 provision을 이 테이블과 대조. 면제 대상이 아니면 `nongteuk_amount = gross_amount × tax_rate`로 농특세 산출.

---

### REF_SME_DEDUCTION_RATE - 중소기업 특별세액감면율

| 항목 | 내용 |
|------|------|
| **테이블명** | REF_SME_DEDUCTION_RATE |
| **한글명** | 중소기업 특별세액감면율 |
| **설명** | 기업 세부 규모, 업종, 지역별 중소기업 특별세액감면율. 조특법 §7 근거 |
| **PK** | rate_id |

| No | 컬럼명 | 데이터 타입 | NULL | 설명 |
|----|--------|-----------|------|------|
| 1 | rate_id | INT | NOT NULL | 레코드 식별자 |
| 2 | corp_size_detail | VARCHAR(10) | NULL | 세부 규모. `SMALL`(소기업), `MEDIUM_SME`(중기업 중 중소기업) |
| 3 | industry_class | VARCHAR(50) | NULL | 업종 분류. `MANUFACTURING`(제조업), `KNOWLEDGE_SERVICE`(지식기반서비스) 등 |
| 4 | zone_type | VARCHAR(20) | NULL | 수도권/비수도권 |
| 5 | deduction_rate | DECIMAL(5,2) | NULL | 감면율 (%). 5~30% |

**활용**: INP_BASIC의 corp_size, industry_code, capital_zone 조합으로 적용 감면율 결정. `감면액 = 산출세액 × deduction_rate`.

---

### REF_RD_CREDIT_RATE - R&D 세액공제율

| 항목 | 내용 |
|------|------|
| **테이블명** | REF_RD_CREDIT_RATE |
| **한글명** | R&D 세액공제율 |
| **설명** | R&D 유형(신성장/일반), 계산 방법(당기/증가분), 기업 규모별 공제율. 조특법 §10 근거 |
| **PK** | rate_id |

| No | 컬럼명 | 데이터 타입 | NULL | 설명 |
|----|--------|-----------|------|------|
| 1 | rate_id | INT | NOT NULL | 레코드 식별자 |
| 2 | rd_type | VARCHAR(20) | NULL | R&D 유형. `NEW_GROWTH`(신성장/원천기술), `BASIC`(일반), `OUTSOURCED`(위탁) |
| 3 | method | VARCHAR(10) | NULL | 계산 방법. `CURRENT`(당기분), `INCREMENT`(증가분) |
| 4 | corp_size | VARCHAR(10) | NULL | 기업 규모 |
| 5 | credit_rate | DECIMAL(5,2) | NULL | 공제율 (%) |
| 6 | min_tax_exempt | VARCHAR(20) | NULL | 최저한세 면제 여부/유형. REF_RD_MIN_TAX_EXEMPT 참조 |

**활용**: INP_DEDUCTION의 rd_type + method + INP_BASIC.corp_size 조합으로 공제율 결정. `R&D 공제 = 연구개발비 × credit_rate`. 신성장/원천기술은 일반보다 높은 공제율.

---

### REF_REFUND_INTEREST_RATE - 환급가산금 이자율

| 항목 | 내용 |
|------|------|
| **테이블명** | REF_REFUND_INTEREST_RATE |
| **한글명** | 환급가산금 이자율 |
| **설명** | 환급 시 적용되는 가산금(이자) 연이율. 기간별로 다를 수 있음 |
| **PK** | rate_id |

| No | 컬럼명 | 데이터 타입 | NULL | 설명 |
|----|--------|-----------|------|------|
| 1 | rate_id | INT | NOT NULL | 레코드 식별자 |
| 2 | effective_from | DATE | NULL | 적용 시작일 |
| 3 | effective_to | DATE | NULL | 적용 종료일 |
| 4 | annual_rate | DECIMAL(7,5) | NULL | 연이율 (%). 예: 2.10000 |
| 5 | legal_basis | VARCHAR(100) | NULL | 근거법조 |

**활용**: OUT_REFUND에서 환급가산금 산출 시 참조. `환급가산금 = 환급세액 × annual_rate × 일수/365`. 1원 미만 절사.

---

### REF_STARTUP_DEDUCTION_RATE - 창업중소기업 감면율

| 항목 | 내용 |
|------|------|
| **테이블명** | REF_STARTUP_DEDUCTION_RATE |
| **한글명** | 창업중소기업 감면율 |
| **설명** | 창업 유형, 소재지별 창업중소기업 세액감면율. 조특법 §6 근거 |
| **PK** | rate_id |

| No | 컬럼명 | 데이터 타입 | NULL | 설명 |
|----|--------|-----------|------|------|
| 1 | rate_id | INT | NOT NULL | 레코드 식별자 |
| 2 | founder_type | VARCHAR(20) | NULL | 창업 유형. `YOUTH`(청년), `GENERAL`(일반), `VENTURE`(벤처) |
| 3 | location_type | VARCHAR(30) | NULL | 소재지 유형. `DEPOPULATION`(인구감소지역), `NON_CAPITAL`(비수도권), `CAPITAL`(수도권) |
| 4 | deduction_rate | DECIMAL(5,2) | NULL | 감면율 (%). 50~100% |
| 5 | year_from | VARCHAR(4) | NULL | 적용 시작 연도 |
| 6 | year_to | VARCHAR(4) | NULL | 적용 종료 연도 |
| 7 | legal_basis | VARCHAR(100) | NULL | 근거법조 |
| 8 | remark | VARCHAR(200) | NULL | 비고 |

**활용**: INP_BASIC의 founding_date, capital_zone, depopulation_area를 기반으로 감면율 결정. 인구감소지역 청년 창업은 최대 100% 감면.

---

### REF_DEPOPULATION_AREA - 인구감소지역 지정

| 항목 | 내용 |
|------|------|
| **테이블명** | REF_DEPOPULATION_AREA |
| **한글명** | 인구감소지역 지정 |
| **설명** | 인구감소지역으로 지정된 시군구 목록 및 지정 일자 |
| **PK** | area_id |

| No | 컬럼명 | 데이터 타입 | NULL | 설명 |
|----|--------|-----------|------|------|
| 1 | area_id | INT | NOT NULL | 지역 식별자 |
| 2 | sido | VARCHAR(20) | NULL | 시도명 |
| 3 | sigungu | VARCHAR(50) | NULL | 시군구명 |
| 4 | designation_date | DATE | NULL | 지정 고시일 |
| 5 | effective_from | DATE | NULL | 효력 발생일 |
| 6 | is_active | BOOLEAN | NULL | 현재 유효 여부 |

**활용**: INP_BASIC.depopulation_area 판정의 기초 데이터. 창업중소기업 감면(조특법 §6) 우대, 고용증대 공제 추가 혜택 등에 활용.

---

### REF_CORP_TAX_RATE_HISTORY - 법인세율 이력

| 항목 | 내용 |
|------|------|
| **테이블명** | REF_CORP_TAX_RATE_HISTORY |
| **한글명** | 법인세율 이력 |
| **설명** | 과거 연도별 법인세율 이력 보관. REF_TAX_RATE의 아카이브 테이블 |
| **PK** | rate_id |

| No | 컬럼명 | 데이터 타입 | NULL | 설명 |
|----|--------|-----------|------|------|
| 1 | rate_id | INT | NOT NULL | 레코드 식별자 |
| 2 | year_from | VARCHAR(4) | NULL | 적용 시작 연도 |
| 3 | year_to | VARCHAR(4) | NULL | 적용 종료 연도 |
| 4 | bracket_min | BIGINT | NULL | 과세표준 하한 (원) |
| 5 | bracket_max | BIGINT | NULL | 과세표준 상한 (원) |
| 6 | tax_rate | DECIMAL(5,2) | NULL | 세율 (%) |
| 7 | progressive_deduction | BIGINT | NULL | 누진공제액 (원) |

**활용**: 과거 사업연도에 대한 경정청구 시, 해당 연도의 세율을 정확하게 적용하기 위한 이력 데이터. REF_TAX_RATE에는 현행 세율만 보관하고, 개정 전 세율은 이 테이블에서 조회.

---

### REF_LAW_VERSION - 세법 버전 관리

| 항목 | 내용 |
|------|------|
| **테이블명** | REF_LAW_VERSION |
| **한글명** | 세법 버전 관리 |
| **설명** | 세법 조항별 개정 이력 및 적용 기간. 법조 단위로 개정 시점을 추적 |
| **PK** | version_id |

| No | 컬럼명 | 데이터 타입 | NULL | 설명 |
|----|--------|-----------|------|------|
| 1 | version_id | INT | NOT NULL | 버전 식별자 |
| 2 | law_name | VARCHAR(100) | NULL | 법률명. 예: `조세특례제한법`, `법인세법`, `소득세법` |
| 3 | provision | VARCHAR(20) | NULL | 법조. 예: `§10`, `§25의4` |
| 4 | year_from | VARCHAR(4) | NULL | 적용 시작 연도 |
| 5 | year_to | VARCHAR(4) | NULL | 적용 종료 연도 |
| 6 | version_note | TEXT | NULL | 개정 내용 요약 |

**활용**: 경정청구 대상 연도에 적용되는 법 버전을 정확히 식별. 동일 법조라도 연도에 따라 요건/공제율이 다를 수 있으므로 버전 관리 필수.

---

### REF_DIVIDEND_EXCLUSION - 배당소득 익금불산입율

| 항목 | 내용 |
|------|------|
| **테이블명** | REF_DIVIDEND_EXCLUSION |
| **한글명** | 배당소득 익금불산입율 |
| **설명** | 법인의 수입배당금 중 익금불산입 비율. 지분율, 법인 유형(상장/비상장)에 따라 차등 적용 |
| **PK** | exclusion_id |

| No | 컬럼명 | 데이터 타입 | NULL | 설명 |
|----|--------|-----------|------|------|
| 1 | exclusion_id | INT | NOT NULL | 레코드 식별자 |
| 2 | year_from | VARCHAR(4) | NULL | 적용 시작 연도 |
| 3 | year_to | VARCHAR(4) | NULL | 적용 종료 연도 |
| 4 | corp_type | VARCHAR(20) | NULL | 법인 유형. `LISTED`(상장), `UNLISTED`(비상장) |
| 5 | share_ratio_min | DECIMAL(5,2) | NULL | 지분율 하한 (%) |
| 6 | share_ratio_max | DECIMAL(5,2) | NULL | 지분율 상한 (%) |
| 7 | exclusion_rate | DECIMAL(5,2) | NULL | 익금불산입률 (%) |
| 8 | remark | VARCHAR(200) | NULL | 비고 |

**활용**: INP_FINANCIAL.dividend_exclusion_detail의 검증 및 재계산. 법인세 전용.

---

### REF_RD_MIN_TAX_EXEMPT - R&D 최저한세 면제율

| 항목 | 내용 |
|------|------|
| **테이블명** | REF_RD_MIN_TAX_EXEMPT |
| **한글명** | R&D 최저한세 면제율 |
| **설명** | R&D 유형별, 기업 규모별 최저한세 적용 면제 비율 |
| **PK** | rd_type + corp_size (복합키) |

| No | 컬럼명 | 데이터 타입 | NULL | 설명 |
|----|--------|-----------|------|------|
| 1 | rd_type | VARCHAR(20) | NOT NULL | R&D 유형 |
| 2 | corp_size | VARCHAR(10) | NOT NULL | 기업 규모 |
| 3 | exempt_rate | DECIMAL(5,2) | NULL | 면제율 (%) |

**활용**: M5 최저한세 조정 시, 신성장/원천기술 R&D 공제 등 일부 항목은 최저한세 적용에서 일정 비율 면제. REF_RD_CREDIT_RATE.min_tax_exempt와 연계.

---

### REF_INDUSTRY_ELIGIBILITY - 업종별 적격성

| 항목 | 내용 |
|------|------|
| **테이블명** | REF_INDUSTRY_ELIGIBILITY |
| **한글명** | 업종별 적격성 |
| **설명** | 한국표준산업분류(KSIC) 코드별 세액공제/감면 적격 여부. 특정 업종은 일부 공제/감면에서 제외 |
| **PK** | ksic_code |

| No | 컬럼명 | 데이터 타입 | NULL | 설명 |
|----|--------|-----------|------|------|
| 1 | ksic_code | VARCHAR(10) | NOT NULL | 한국표준산업분류 코드 |
| 2 | industry_name | VARCHAR(100) | NULL | 업종명 |
| 3 | startup_eligible | BOOLEAN | NULL | 창업중소기업 감면 적격 여부 |
| 4 | sme_special_eligible | BOOLEAN | NULL | 중소기업 특별세액감면 적격 여부 |
| 5 | excluded_reason | VARCHAR(200) | NULL | 제외 사유 |
| 6 | effective_from | DATE | NULL | 효력 시작일 |
| 7 | effective_to | DATE | NULL | 효력 종료일 |
| 8 | is_sme_eligible | BOOLEAN | NULL | 중소기업 해당 업종 여부 |

**활용**: CHK_ELIGIBILITY에서 INP_BASIC.industry_code를 이 테이블과 대조하여 업종별 적격성 판정. 소비성 서비스업(유흥주점, 도박 등)은 대부분의 세액공제에서 제외.

---

### REF_ENTERTAINMENT_LIMIT - 접대비 한도

| 항목 | 내용 |
|------|------|
| **테이블명** | REF_ENTERTAINMENT_LIMIT |
| **한글명** | 접대비 한도 |
| **설명** | 기업 규모 및 수입금액 구간별 접대비 손금산입 한도 |
| **PK** | limit_id |

| No | 컬럼명 | 데이터 타입 | NULL | 설명 |
|----|--------|-----------|------|------|
| 1 | limit_id | INT | NOT NULL | 레코드 식별자 |
| 2 | corp_size | VARCHAR(10) | NULL | 기업 규모 |
| 3 | base_amount | BIGINT | NULL | 기본 한도 (원) |
| 4 | revenue_bracket_min | BIGINT | NULL | 수입금액 하한 (원) |
| 5 | revenue_bracket_max | BIGINT | NULL | 수입금액 상한 (원) |
| 6 | rate | DECIMAL(5,4) | NULL | 수입금액 대비 추가 한도율 |
| 7 | year_from | VARCHAR(4) | NULL | 적용 시작 연도 |
| 8 | year_to | VARCHAR(4) | NULL | 적용 종료 연도 |

**활용**: 접대비 한도 초과 여부 검증. `한도 = base_amount + 수입금액 × rate`. 한도 초과 접대비는 손금불산입 → 과세표준 증가.

---

## 공통 기준정보

### REF_SYSTEM_PARAM - 시스템 파라미터

| 항목 | 내용 |
|------|------|
| **테이블명** | REF_SYSTEM_PARAM |
| **한글명** | 시스템 파라미터 |
| **설명** | 시스템 전역 설정값. 절사 기준, 타임아웃, JSON 크기 제한 등 운영 파라미터 |
| **PK** | param_key |

| No | 컬럼명 | 데이터 타입 | NULL | 설명 |
|----|--------|-----------|------|------|
| 1 | param_key | VARCHAR(50) | NOT NULL | 파라미터 키. 예: `TRUNCATE_UNIT`, `JSON_MAX_SIZE`, `API_TIMEOUT` |
| 2 | param_value | VARCHAR(100) | NULL | 파라미터 값 |
| 3 | param_type | VARCHAR(20) | NULL | 값 타입. `INT`, `STRING`, `BOOLEAN`, `DECIMAL` |
| 4 | description | VARCHAR(200) | NULL | 파라미터 설명 |
| 5 | modifiable | BOOLEAN | NULL | 운영 중 변경 가능 여부 |
| 6 | last_updated | DATE | NULL | 최종 갱신일 |

**활용**: 시스템 전역 설정을 코드 변경 없이 DB에서 관리. 예: `TRUNCATE_UNIT=10`(10원 미만 절사), `JSON_MAX_SIZE=10485760`(10MB).

---

### REF_KSIC_CODE - 한국표준산업분류코드

| 항목 | 내용 |
|------|------|
| **테이블명** | REF_KSIC_CODE |
| **한글명** | 한국표준산업분류코드 |
| **설명** | 한국표준산업분류(KSIC) 전체 코드 체계. 대분류(section) → 중분류(division) → 소분류(group) → 세분류(class) → 세세분류(sub_class) |
| **PK** | ksic_code |

| No | 컬럼명 | 데이터 타입 | NULL | 설명 |
|----|--------|-----------|------|------|
| 1 | ksic_code | VARCHAR(10) | NOT NULL | KSIC 코드 (세세분류 단위) |
| 2 | section | VARCHAR(5) | NULL | 대분류 코드. 예: `C`(제조업) |
| 3 | division | VARCHAR(5) | NULL | 중분류 코드 |
| 4 | group_code | VARCHAR(5) | NULL | 소분류 코드 |
| 5 | class_code | VARCHAR(5) | NULL | 세분류 코드 |
| 6 | sub_class | VARCHAR(5) | NULL | 세세분류 코드 |
| 7 | industry_name | VARCHAR(200) | NULL | 업종명 |
| 8 | revision | VARCHAR(10) | NULL | KSIC 개정판 (예: `10차`) |
| 9 | effective_date | DATE | NULL | 시행일 |

**활용**: INP_BASIC.industry_code의 마스터 코드 테이블. 업종명 표시, REF_INDUSTRY_ELIGIBILITY와 연계하여 적격성 판정.

---

### REF_EXCHANGE_RATE - 환율

| 항목 | 내용 |
|------|------|
| **테이블명** | REF_EXCHANGE_RATE |
| **한글명** | 환율 |
| **설명** | 일별/통화별 환율 정보. 외국납부세액 공제, 국외원천소득 환산 시 활용 |
| **PK** | rate_date + currency (복합키) |

| No | 컬럼명 | 데이터 타입 | NULL | 설명 |
|----|--------|-----------|------|------|
| 1 | rate_date | DATE | NOT NULL | 환율 기준일 |
| 2 | currency | VARCHAR(3) | NOT NULL | ISO 4217 통화코드. 예: `USD`, `EUR`, `JPY` |
| 3 | standard_rate | DECIMAL(10,4) | NULL | 기준환율 (원/외화 단위) |
| 4 | buy_rate | DECIMAL(10,4) | NULL | 매입환율 |
| 5 | sell_rate | DECIMAL(10,4) | NULL | 매도환율 |

**활용**: INP_FINANCIAL의 foreign_tax_total, foreign_income_total이 외화인 경우 원화로 환산. 외국납부세액 공제 한도 계산 시 활용.

---

### REF_DEEMED_INTEREST_RATE - 인정이자율

| 항목 | 내용 |
|------|------|
| **테이블명** | REF_DEEMED_INTEREST_RATE |
| **한글명** | 인정이자율 |
| **설명** | 연도별/유형별 세법상 인정이자율. 특수관계자 간 거래 등에서 시가 산정 기준 |
| **PK** | year + rate_type (복합키) |

| No | 컬럼명 | 데이터 타입 | NULL | 설명 |
|----|--------|-----------|------|------|
| 1 | year | VARCHAR(4) | NOT NULL | 적용 연도 |
| 2 | rate_type | VARCHAR(20) | NOT NULL | 이자율 유형. `LOAN`(대여금), `DEPOSIT`(예금) 등 |
| 3 | rate | DECIMAL(5,2) | NULL | 인정이자율 (%) |
| 4 | legal_basis | VARCHAR(100) | NULL | 근거법조 |

**활용**: 특수관계자 간 금전 대차 거래의 부당행위계산 부인 시 적용할 이자율. 세무조정 검증에 활용.

---

## 종합소득세 기준정보

### REF_INC_TAX_RATE - 종합소득세율

| 항목 | 내용 |
|------|------|
| **테이블명** | REF_INC_TAX_RATE |
| **한글명** | 종합소득세율 |
| **설명** | 과세표준 구간별 종합소득세율 및 누진공제액. 6~45% 8단계 |
| **PK** | effective_from + bracket_no (복합키) |

| No | 컬럼명 | 데이터 타입 | NULL | 설명 |
|----|--------|-----------|------|------|
| 1 | effective_from | VARCHAR(4) | NOT NULL | 적용 시작 연도 |
| 2 | bracket_no | INT | NOT NULL | 구간 번호 (1~8) |
| 3 | lower_limit | BIGINT | NULL | 과세표준 하한 (원) |
| 4 | upper_limit | BIGINT | NULL | 과세표준 상한 (원) |
| 5 | tax_rate | DECIMAL(5,2) | NULL | 세율 (%) |
| 6 | progressive_deduction | BIGINT | NULL | 누진공제액 (원) |

**활용**: 종합소득세 산출세액 계산. INP_BASIC.tax_type = 'INC'인 경우 이 테이블 참조.

---

### REF_INC_MIN_TAX - 종합소득세 최저한세

| 항목 | 내용 |
|------|------|
| **테이블명** | REF_INC_MIN_TAX |
| **한글명** | 종합소득세 최저한세 |
| **설명** | 개인사업자의 최저한세 기준. 산출세액 기준금액 이하/이상으로 차등 적용 |
| **PK** | effective_from |

| No | 컬럼명 | 데이터 타입 | NULL | 설명 |
|----|--------|-----------|------|------|
| 1 | effective_from | VARCHAR(4) | NOT NULL | 적용 시작 연도 |
| 2 | threshold | BIGINT | NULL | 기준 금액 (원) |
| 3 | rate_below | DECIMAL(5,2) | NULL | 기준금액 이하 적용률 (%) |
| 4 | rate_above | DECIMAL(5,2) | NULL | 기준금액 초과 적용률 (%) |

**활용**: 종합소득세의 최저한세 산출. `산출세액 × rate_below/rate_above`가 최저한세. REF_MIN_TAX_RATE(법인세용)과 대응.

---

### REF_INC_DEDUCTION_LIMIT - 소득공제 한도

| 항목 | 내용 |
|------|------|
| **테이블명** | REF_INC_DEDUCTION_LIMIT |
| **한글명** | 소득공제 한도 |
| **설명** | 종합소득세 소득공제 유형별, 소득 구간별 연간 한도 |
| **PK** | deduction_type + income_bracket (복합키) |

| No | 컬럼명 | 데이터 타입 | NULL | 설명 |
|----|--------|-----------|------|------|
| 1 | deduction_type | VARCHAR(50) | NOT NULL | 공제 유형. 예: `PERSONAL`(인적공제), `PENSION`(연금보험료), `HOUSING`(주택자금) |
| 2 | income_bracket | VARCHAR(50) | NOT NULL | 소득 구간. 예: `BELOW_30M`, `30M_TO_50M`, `ABOVE_50M` |
| 3 | annual_limit | BIGINT | NULL | 연간 한도 (원) |

**활용**: INP_FINANCIAL.inc_deduction_detail의 각 소득공제 항목이 한도를 초과하지 않는지 검증. 종합소득세 전용.

---

### REF_INC_SINCERITY_THRESHOLD - 성실신고 확인 기준

| 항목 | 내용 |
|------|------|
| **테이블명** | REF_INC_SINCERITY_THRESHOLD |
| **한글명** | 성실신고 확인 기준 |
| **설명** | 업종별 성실신고확인 대상 수입금액 기준. 기준 초과 시 세무사 확인 의무 |
| **PK** | industry_group + effective_from (복합키) |

| No | 컬럼명 | 데이터 타입 | NULL | 설명 |
|----|--------|-----------|------|------|
| 1 | industry_group | VARCHAR(50) | NOT NULL | 업종 그룹. 예: `AGRICULTURE_FISHING`(농림어업), `MANUFACTURING`(제조업), `SERVICE`(서비스업) |
| 2 | effective_from | VARCHAR(4) | NOT NULL | 적용 시작 연도 |
| 3 | revenue_threshold | BIGINT | NULL | 수입금액 기준 (원) |

**활용**: INP_BASIC.revenue가 해당 업종의 revenue_threshold를 초과하면 성실신고확인 대상으로 판정 → CHK_ELIGIBILITY.sincerity_target = true. 종합소득세 전용.

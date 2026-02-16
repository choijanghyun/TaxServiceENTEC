# 주제영역 2: 입력 데이터 (INP)

## 개요

경정청구에 필요한 **입력자료를 수신, 보관, 요약**하는 영역입니다.
v3.0 아키텍처에서 **2계층 구조**로 설계되었습니다:

1. **원시 JSON 보관 계층** (INP_RAW_DATA): 클라이언트가 전송한 원본 JSON을 변환 없이 보관
2. **계산용 요약 계층** (INP_BASIC, INP_EMPLOYEE, INP_DEDUCTION, INP_FINANCIAL): 원시 JSON을 파싱하여 환급액 계산에 필요한 핵심 수치만 추출

### 설계 원칙

- **입력 원본 불변성(Immutability)**: INP_RAW_DATA는 INSERT ONLY. 수정 시 기존 req_id를 폐기하고 새 req_id를 발급
- **재현성 보장**: 원시 JSON과 요약 데이터를 모두 보관하여 계산 결과의 재현 가능
- **Hybrid 저장**: 원본은 JSON(유연성), 계산은 RDB(성능)

---

## INP_RAW_DATA - 원시 입력자료 보관

### 기본 정보

| 항목 | 내용 |
|------|------|
| **테이블명** | INP_RAW_DATA |
| **한글명** | 원시 입력자료 보관 |
| **설명** | 클라이언트로부터 수신한 원본 JSON 데이터를 카테고리별로 분리하여 원형 그대로 보관. INSERT ONLY 정책 적용 |
| **PK** | raw_id (AUTO_INCREMENT) |
| **FK** | req_id → REQ_REQUEST (ON DELETE RESTRICT) |
| **인덱스** | idx_inp_raw_data_req (req_id) |

### 컬럼 정의

| No | 컬럼명 | 데이터 타입 | NULL | 기본값 | 설명 |
|----|--------|-----------|------|--------|------|
| 1 | raw_id | BIGINT | NOT NULL | AUTO_INCREMENT | 자동 생성 레코드 식별자 |
| 2 | req_id | VARCHAR(30) | NOT NULL | - | 요청 식별자 (FK → REQ_REQUEST) |
| 3 | category | VARCHAR(30) | NOT NULL | - | 데이터 카테고리. `BASIC`, `EMPLOYEE`, `DEDUCTION`, `FINANCIAL`, `TAX_RETURN`, `ATTACHMENT` 등 |
| 4 | sub_category | VARCHAR(30) | NULL | - | 하위 분류. 예: EMPLOYEE 내 `CURRENT`, `PREV1` 등 |
| 5 | raw_json | LONGTEXT | NOT NULL | - | 원시 JSON 데이터 (변환 없이 원본 보관) |
| 6 | json_schema_version | VARCHAR(20) | NULL | - | JSON 스키마 버전 (입력 포맷 버전 관리) |
| 7 | record_count | INT | NULL | - | JSON 내 레코드 수 (배열인 경우) |
| 8 | byte_size | BIGINT | NULL | - | JSON 데이터 크기 (바이트). 성능 제한 사양 점검용 |
| 9 | checksum | VARCHAR(64) | NULL | - | SHA-256 체크섬 (데이터 무결성 검증) |
| 10 | received_at | TIMESTAMP | NULL | - | 수신 시각 |

### category 코드 값

| category | 설명 | 대응 요약 테이블 |
|----------|------|----------------|
| BASIC | 신청인 기본정보 (사업자번호, 규모, 업종 등) | INP_BASIC |
| EMPLOYEE | 고용/급여 정보 (연도별) | INP_EMPLOYEE |
| DEDUCTION | 공제/감면 신청 항목 | INP_DEDUCTION |
| FINANCIAL | 재무/세무 수치 (손익, 결손금 등) | INP_FINANCIAL |
| TAX_RETURN | 세무신고서 원본 데이터 | INP_BASIC + INP_FINANCIAL |
| ATTACHMENT | 첨부서류 메타데이터 | - |

### 활용

- **원본 보존**: 감사 시 원본 입력 데이터 확인 가능
- **재처리**: 파싱 로직 변경 시 원시 JSON으로부터 요약 테이블을 재생성 가능
- **데이터 검증**: checksum으로 전송 중 변조 여부 확인
- **용량 관리**: byte_size를 기준으로 대용량 데이터 제한(JSON 크기 제한 사양) 점검

---

## INP_BASIC - 신청인 기본정보 요약

### 기본 정보

| 항목 | 내용 |
|------|------|
| **테이블명** | INP_BASIC |
| **한글명** | 신청인 기본정보 요약 |
| **설명** | INP_RAW_DATA의 BASIC 카테고리 JSON을 파싱하여 환급액 계산에 필요한 핵심 기본정보를 정규화한 테이블 |
| **PK** | req_id |
| **FK** | 논리적으로 REQ_REQUEST.req_id 참조 |

### 컬럼 정의

| No | 컬럼명 | 데이터 타입 | NULL | 기본값 | 설명 |
|----|--------|-----------|------|--------|------|
| 1 | req_id | VARCHAR(30) | NOT NULL | - | 요청 식별자 |
| 2 | request_date | DATE | NULL | - | 요청일 (REQ_REQUEST.request_date와 동기화) |
| 3 | tax_type | VARCHAR(4) | NULL | - | 세목 코드. `CORP` 또는 `INC` |
| 4 | applicant_name | VARCHAR(100) | NULL | - | 신청인 명칭 |
| 5 | biz_reg_no | VARCHAR(15) | NULL | - | 사업자등록번호 |
| 6 | corp_size | VARCHAR(20) | NULL | - | 기업 규모. `LARGE`(대기업), `MEDIUM`(중견기업), `SMALL`(중소기업) |
| 7 | industry_code | VARCHAR(10) | NULL | - | 한국표준산업분류(KSIC) 코드. REF_KSIC_CODE 참조 |
| 8 | hq_location | VARCHAR(200) | NULL | - | 본점 소재지 (주소) |
| 9 | capital_zone | VARCHAR(20) | NULL | - | 수도권 분류. `CAPITAL`(수도권 과밀억제권역), `NON_CAPITAL`(비수도권) |
| 10 | depopulation_area | BOOLEAN | NULL | - | 인구감소지역 해당 여부 |
| 11 | tax_year | VARCHAR(4) | NULL | - | 귀속 사업연도 |
| 12 | fiscal_start | DATE | NULL | - | 사업연도 개시일 |
| 13 | fiscal_end | DATE | NULL | - | 사업연도 종료일 |
| 14 | revenue | BIGINT | NULL | - | 수입금액(매출액), 단위: 원 |
| 15 | taxable_income | BIGINT | NULL | - | 과세표준, 단위: 원 |
| 16 | computed_tax | BIGINT | NULL | - | 산출세액, 단위: 원 |
| 17 | paid_tax | BIGINT | NULL | - | 기납부세액, 단위: 원 |
| 18 | founding_date | DATE | NULL | - | 설립일(법인) 또는 개업일(개인) |
| 19 | venture_yn | BOOLEAN | NULL | - | 벤처기업 확인 여부 |
| 20 | rd_dept_yn | BOOLEAN | NULL | - | 기업부설 연구소/연구개발전담부서 보유 여부 |
| 21 | claim_reason | VARCHAR(500) | NULL | - | 경정청구 사유 |
| 22 | sincerity_target | BOOLEAN | NULL | - | 성실신고확인 대상 여부 |
| 23 | bookkeeping_type | VARCHAR(20) | NULL | - | 기장 유형. `DOUBLE`(복식부기), `SIMPLE`(간편장부), `ESTIMATE`(추계) |
| 24 | consolidated_tax | BOOLEAN | NULL | - | 연결납세 적용 여부 (법인세 전용) |
| 25 | summary_generated_at | TIMESTAMP | NULL | - | 요약 데이터 생성 시각 (M1-03 파싱 완료 시점) |

### 활용

- **자격 진단(M2)**: corp_size, capital_zone, industry_code, founding_date 등을 기반으로 중소기업 적격성, 수도권/비수도권 구분, 업종 적격성 판단
- **세율 결정**: tax_type에 따라 법인세(REF_TAX_RATE) 또는 종합소득세(REF_INC_TAX_RATE) 세율표 참조
- **공제율 결정**: corp_size + capital_zone + industry_code 조합으로 각종 세액공제/감면율 결정
- **경정청구 기한 검증**: request_date와 tax_year를 비교하여 법정 청구기한(5년) 이내 여부 확인
- **벤처/R&D 특례**: venture_yn, rd_dept_yn으로 추가 공제 적격성 판단

---

## INP_EMPLOYEE - 고용 정보 요약

### 기본 정보

| 항목 | 내용 |
|------|------|
| **테이블명** | INP_EMPLOYEE |
| **한글명** | 고용 정보 요약 |
| **설명** | 연도별 상시근로자 수, 유형별 근로자 수, 급여총액, 사회보험 납부액을 관리. 고용증대 세액공제 계산의 핵심 입력 데이터 |
| **PK** | req_id + year_type (복합키) |
| **FK** | 논리적으로 REQ_REQUEST.req_id 참조 |

### 컬럼 정의

| No | 컬럼명 | 데이터 타입 | NULL | 기본값 | 설명 |
|----|--------|-----------|------|--------|------|
| 1 | req_id | VARCHAR(30) | NOT NULL | - | 요청 식별자 |
| 2 | year_type | VARCHAR(10) | NOT NULL | - | 연도 유형. `CURRENT`(해당연도), `PREV1`(직전1년), `PREV2`(직전2년), `PREV3`(직전3년) |
| 3 | total_regular | DECIMAL(10,2) | NULL | - | 상시근로자 수 (소수점 가능: 월 단위 평균) |
| 4 | youth_count | INT | NULL | - | 청년 근로자 수 (15~34세) |
| 5 | disabled_count | INT | NULL | - | 장애인 근로자 수 |
| 6 | aged_count | INT | NULL | - | 고령자 근로자 수 (60세 이상) |
| 7 | career_break_count | INT | NULL | - | 경력단절여성 근로자 수 |
| 8 | north_defector_count | INT | NULL | - | 북한이탈주민 근로자 수 |
| 9 | general_count | INT | NULL | - | 일반 근로자 수 (위 유형에 해당하지 않는 근로자) |
| 10 | excluded_count | INT | NULL | - | 상시근로자 제외 인원 (임원, 일용직, 단기근로자 등) |
| 11 | total_salary | BIGINT | NULL | - | 급여총액, 단위: 원 |
| 12 | social_insurance_paid | BIGINT | NULL | - | 사회보험료 납부액(4대 보험 사업주 부담분), 단위: 원 |

### year_type 코드 값

| year_type | 의미 | 활용 |
|-----------|------|------|
| CURRENT | 해당 사업연도 | 현재 연도 근로자 수 기준 |
| PREV1 | 직전 1년 | 고용증대 공제 증가분 계산 기준 |
| PREV2 | 직전 2년 | 3년 이월 공제 판단 |
| PREV3 | 직전 3년 | 3년 이월 공제 판단 |

### 활용

- **상시근로자 산정(M3)**: total_regular, excluded_count를 기반으로 조특법상 상시근로자 수 산정
- **고용증대 세액공제 계산**: CURRENT - PREV1 차이(증가분)에 REF_EMPLOYMENT_CREDIT의 1인당 공제액을 곱하여 세액공제 산출
- **청년/장애인/고령자 우대**: youth_count, disabled_count, aged_count에 대해 우대 공제율 적용
- **사회보험료 세액공제**: social_insurance_paid를 기반으로 사회보험료 세액공제 산출
- **사후관리 리스크 평가**: 공제 적용 후 근로자 수 유지 의무 기간(2년) 판정 기초 데이터

---

## INP_DEDUCTION - 공제/감면 기초 요약

### 기본 정보

| 항목 | 내용 |
|------|------|
| **테이블명** | INP_DEDUCTION |
| **한글명** | 공제/감면 기초 요약 |
| **설명** | 신청인이 적용받고자 하는 세액공제/감면 항목의 기초 데이터. 항목별 근거법조, 기투자/연구개발 금액, 기적용 여부 등을 관리 |
| **PK** | req_id + item_category + provision + tax_year + item_seq (복합키) |
| **FK** | 논리적으로 REQ_REQUEST.req_id 참조 |

### 컬럼 정의

| No | 컬럼명 | 데이터 타입 | NULL | 기본값 | 설명 |
|----|--------|-----------|------|--------|------|
| 1 | req_id | VARCHAR(30) | NOT NULL | - | 요청 식별자 |
| 2 | item_category | VARCHAR(30) | NOT NULL | - | 공제/감면 대분류. `INVEST`, `RD`, `EMPLOYMENT`, `STARTUP`, `SME_SPECIAL`, `OTHER` |
| 3 | provision | VARCHAR(30) | NOT NULL | - | 근거 법조. 예: `조특법25의4`(통합투자), `조특법10`(R&D) |
| 4 | tax_year | VARCHAR(4) | NOT NULL | - | 귀속 사업연도 |
| 5 | item_seq | INT | NOT NULL | - | 동일 분류/법조/연도 내 항목 순번 |
| 6 | base_amount | BIGINT | NULL | - | 기초 금액(투자액, 연구개발비 등), 단위: 원 |
| 7 | zone_type | VARCHAR(20) | NULL | - | 수도권/비수도권 구분 (투자 소재지 기준) |
| 8 | asset_type | VARCHAR(30) | NULL | - | 자산 유형. 투자 공제 시 `MACHINERY`, `FACILITY`, `VEHICLE` 등 |
| 9 | rd_type | VARCHAR(30) | NULL | - | R&D 유형. `NEW_GROWTH`(신성장/원천기술), `BASIC`(일반 R&D), `OUTSOURCED`(위탁연구) |
| 10 | method | VARCHAR(30) | NULL | - | 공제 방법. `INCREMENT`(증가분 방식), `CURRENT`(당기분 방식) |
| 11 | sub_detail | LONGTEXT | NULL | - | 항목별 세부 정보 (JSON). 예: 자산 목록, R&D 프로젝트 내역 등 |
| 12 | existing_applied | BOOLEAN | NULL | - | 기존 신고 시 이미 적용 여부 |
| 13 | existing_amount | BIGINT | NULL | - | 기존 적용 금액, 단위: 원 |
| 14 | carryforward_balance | BIGINT | NULL | - | 이월공제 잔액 (전년도까지 미사용 공제액), 단위: 원 |

### item_category 코드 값

| item_category | 설명 | 주요 법조 |
|--------------|------|----------|
| INVEST | 투자 세액공제 | 조특법 §25의4(통합투자), §25의5(임시투자) 등 |
| RD | 연구개발 세액공제 | 조특법 §10(R&D) |
| EMPLOYMENT | 고용 관련 세액공제 | 조특법 §29의7(고용증대), §30의4(사회보험료) 등 |
| STARTUP | 창업중소기업 세액감면 | 조특법 §6 |
| SME_SPECIAL | 중소기업 특별세액감면 | 조특법 §7 |
| OTHER | 기타 공제/감면 항목 | 기타 조특법 조항 |

### 활용

- **공제 산출(M4)**: base_amount에 REF_*의 공제율/감면율을 적용하여 세액공제/감면 금액 산출
- **증가분 vs 당기분 선택**: method에 따라 R&D/투자 공제의 계산 방식 결정
- **이월공제 관리**: carryforward_balance + 당기 공제액에서 당기 사용 가능분을 계산
- **중복적용 검증**: provision 값을 REF_MUTUAL_EXCLUSION과 대조하여 상호배제 규칙 위반 점검
- **기적용 차감**: existing_applied=true인 항목은 existing_amount를 차감하여 추가 환급 가능액 산정

---

## INP_FINANCIAL - 재무/세무 수치 요약

### 기본 정보

| 항목 | 내용 |
|------|------|
| **테이블명** | INP_FINANCIAL |
| **한글명** | 재무/세무 수치 요약 |
| **설명** | 손익, 결손금, 기납부세액, 배당소득, 외국납부세액 등 환급액 산출에 필요한 재무/세무 수치를 집계한 테이블 |
| **PK** | req_id |
| **FK** | 논리적으로 REQ_REQUEST.req_id 참조 |

### 컬럼 정의

| No | 컬럼명 | 데이터 타입 | NULL | 기본값 | 설명 |
|----|--------|-----------|------|--------|------|
| 1 | req_id | VARCHAR(30) | NOT NULL | - | 요청 식별자 |
| 2 | biz_income | BIGINT | NULL | - | 사업소득금액, 단위: 원 |
| 3 | non_taxable_income | BIGINT | NULL | - | 비과세소득, 단위: 원 |
| 4 | loss_carryforward_total | BIGINT | NULL | - | 이월결손금 합계, 단위: 원 |
| 5 | loss_carryforward_detail | LONGTEXT | NULL | - | 이월결손금 연도별 상세 내역 (JSON) |
| 6 | interim_prepaid_tax | BIGINT | NULL | - | 중간예납세액, 단위: 원 |
| 7 | withholding_tax | BIGINT | NULL | - | 원천징수세액, 단위: 원 |
| 8 | determined_tax | BIGINT | NULL | - | 기신고(기결정) 세액, 단위: 원 |
| 9 | dividend_income_total | BIGINT | NULL | - | 배당소득 합계, 단위: 원 (법인세 전용) |
| 10 | dividend_exclusion_detail | LONGTEXT | NULL | - | 수입배당금 익금불산입 상세 (JSON). REF_DIVIDEND_EXCLUSION 참조 |
| 11 | foreign_tax_total | BIGINT | NULL | - | 외국납부세액 합계, 단위: 원 |
| 12 | foreign_income_total | BIGINT | NULL | - | 국외원천소득 합계, 단위: 원 |
| 13 | tax_adjustment_detail | LONGTEXT | NULL | - | 세무조정 내역 (JSON). 익금산입, 손금산입 등 |
| 14 | inc_deduction_total | BIGINT | NULL | - | 소득공제 합계, 단위: 원 (종합소득세 전용) |
| 15 | inc_deduction_detail | LONGTEXT | NULL | - | 소득공제 항목별 상세 (JSON). 인적공제, 연금보험료 등 |
| 16 | inc_comprehensive_income | BIGINT | NULL | - | 종합소득금액, 단위: 원 (종합소득세 전용) |
| 17 | current_year_loss | BIGINT | NULL | - | 당기 결손금, 단위: 원 |
| 18 | prior_year_tax_paid | BIGINT | NULL | - | 전년도 납부세액, 단위: 원 |
| 19 | amendment_history | LONGTEXT | NULL | - | 과거 수정신고/경정청구 이력 (JSON) |
| 20 | vehicle_expense_detail | LONGTEXT | NULL | - | 업무용 승용차 관련 비용 상세 (JSON) |

### 법인세(CORP) vs 종합소득세(INC) 컬럼 구분

| 컬럼 | CORP | INC | 설명 |
|------|------|-----|------|
| biz_income | O | O | 법인: 각 사업연도 소득, 개인: 사업소득금액 |
| dividend_income_total | O | - | 법인 전용: 수입배당금 |
| dividend_exclusion_detail | O | - | 법인 전용: 익금불산입 상세 |
| tax_adjustment_detail | O | - | 법인 전용: 세무조정 |
| inc_deduction_total | - | O | 개인 전용: 소득공제 합계 |
| inc_deduction_detail | - | O | 개인 전용: 소득공제 상세 |
| inc_comprehensive_income | - | O | 개인 전용: 종합소득금액 |
| 나머지 컬럼 | O | O | 공통 사용 |

### 활용

- **과세표준 검증**: biz_income에서 비과세소득, 이월결손금, 소득공제를 차감하여 과세표준 정합성 검증
- **기납부세액 집계**: interim_prepaid_tax + withholding_tax + determined_tax 합산하여 기납부세액 확인
- **환급액 산출(M5)**: 경정 후 결정세액 - 기납부세액 = 환급액 계산의 기초 데이터
- **외국납부세액 공제**: foreign_tax_total, foreign_income_total로 외국납부세액 공제 한도 산출
- **결산확정 원칙 검증(CORP)**: tax_adjustment_detail의 결산조정 항목 존재 시 경정청구 불가(Hard Fail) 판정
- **수정신고 이력 확인**: amendment_history로 과거 경정청구/수정신고 이력 파악
- **업무용 승용차 한도 검증**: vehicle_expense_detail로 업무사용비율 및 비용한도 초과 여부 확인

# 주제영역 4: 산출 결과 (OUT)

## 개요

환급액 산출 과정의 **모든 중간 결과와 최종 결과**를 저장하는 영역입니다.
v3.0 아키텍처의 **2계층 출력 구조**를 구현합니다:

1. **산출 저장 계층** (OUT_EMPLOYEE_SUMMARY ~ OUT_ADDITIONAL_CHECK): RDB 테이블에 개별 산출 결과 저장
2. **JSON 전달 계층** (OUT_REPORT_JSON): 산출 결과를 7섹션(A~G) JSON으로 조립하여 클라이언트 전달

### 처리 흐름

```
M3: 상시근로자 산정 → OUT_EMPLOYEE_SUMMARY
         ↓
M4: 개별 공제/감면 산출 → OUT_CREDIT_DETAIL
         ↓
M5: 조합 비교/최적 선택 → OUT_COMBINATION
    상호배제 검증 → OUT_EXCLUSION_VERIFY
    최종 환급액 산출 → OUT_REFUND
         ↓
    리스크 평가 → OUT_RISK
    추가 확인 → OUT_ADDITIONAL_CHECK
         ↓
M6: JSON 조립 → OUT_REPORT_JSON (7섹션 A~G)
```

---

## OUT_EMPLOYEE_SUMMARY - 상시근로자 산정

### 기본 정보

| 항목 | 내용 |
|------|------|
| **테이블명** | OUT_EMPLOYEE_SUMMARY |
| **한글명** | 상시근로자 산정 |
| **설명** | INP_EMPLOYEE 데이터를 기반으로 조세특례제한법상 상시근로자 수를 산정하고, 연도 간 증감분을 계산한 결과 |
| **PK** | req_id + year_type (복합키) |
| **FK** | 논리적으로 REQ_REQUEST.req_id 참조 |

### 컬럼 정의

| No | 컬럼명 | 데이터 타입 | NULL | 기본값 | 설명 |
|----|--------|-----------|------|--------|------|
| 1 | req_id | VARCHAR(30) | NOT NULL | - | 요청 식별자 |
| 2 | year_type | VARCHAR(20) | NOT NULL | - | 연도 유형. `CURRENT`, `PREV1`, `PREV2`, `PREV3`, `INCREASE` |
| 3 | total_regular | DECIMAL(15,2) | NULL | - | 상시근로자 수 (조특법 기준 산정) |
| 4 | youth_count | INT | NULL | - | 청년 등 우대 근로자 수 |
| 5 | general_count | INT | NULL | - | 일반 근로자 수 |
| 6 | increase_total | INT | NULL | - | 전체 증가 인원 (CURRENT - PREV1) |
| 7 | increase_youth | INT | NULL | - | 청년 등 증가 인원 |
| 8 | increase_general | INT | NULL | - | 일반 증가 인원 |
| 9 | excluded_count | INT | NULL | - | 제외 인원 (임원, 일용직 등) |
| 10 | calc_detail | TEXT | NULL | - | 산정 상세 내역 (JSON). 산정 공식, 중간 계산값 등 |

### 활용

- **고용증대 세액공제 기초**: increase_total, increase_youth, increase_general이 REF_EMPLOYMENT_CREDIT 참조 시 핵심 입력값
- **1인당 공제액 산출**: 증가 유형(청년/일반)별로 기업 규모(SMALL/MEDIUM) 및 지역(수도권/비수도권)에 따른 차등 공제액 적용
- **사후관리 기준**: CURRENT 연도의 총 근로자 수가 사후관리 의무(2년간 유지) 기준점

---

## OUT_CREDIT_DETAIL - 개별 공제/감면 산출

### 기본 정보

| 항목 | 내용 |
|------|------|
| **테이블명** | OUT_CREDIT_DETAIL |
| **한글명** | 개별 공제/감면 산출 |
| **설명** | 각 세액공제/감면 항목의 상세 산출 결과. 총공제액, 농어촌특별세, 순공제액, 최저한세 적용 여부, 이월 가능 여부 등을 기록 |
| **PK** | req_id + item_id (복합키) |
| **FK** | 논리적으로 REQ_REQUEST.req_id 참조 |
| **인덱스** | idx_out_credit_detail_provision (provision) |

### 컬럼 정의

| No | 컬럼명 | 데이터 타입 | NULL | 기본값 | 설명 |
|----|--------|-----------|------|--------|------|
| 1 | req_id | VARCHAR(30) | NOT NULL | - | 요청 식별자 |
| 2 | item_id | VARCHAR(30) | NOT NULL | - | 공제/감면 항목 ID. 예: `CREDIT_RD_10_001` |
| 3 | item_name | VARCHAR(100) | NULL | - | 항목명. 예: `연구개발비 세액공제(신성장/원천기술)` |
| 4 | provision | VARCHAR(50) | NULL | - | 근거법조. 예: `조특법10` |
| 5 | credit_type | VARCHAR(30) | NULL | - | 공제 유형. `TAX_CREDIT`(세액공제), `TAX_EXEMPTION`(세액감면) |
| 6 | item_status | VARCHAR(20) | NULL | - | 항목 상태. `CALCULATED`, `EXCLUDED`, `CARRYFORWARD_ONLY` |
| 7 | gross_amount | BIGINT | NULL | - | 총공제/감면액(농특세 차감 전), 단위: 원 |
| 8 | nongteuk_exempt | BOOLEAN | NULL | - | 농어촌특별세 면제 대상 여부 (REF_NONGTEUKSE 참조) |
| 9 | nongteuk_amount | BIGINT | NULL | - | 농어촌특별세액, 단위: 원. 면제 시 0 |
| 10 | net_amount | BIGINT | NULL | - | 순공제/감면액 (gross_amount - nongteuk_amount), 단위: 원 |
| 11 | min_tax_subject | BOOLEAN | NULL | - | 최저한세 적용 대상 여부 |
| 12 | is_carryforward | BOOLEAN | NULL | - | 이월공제 가능 여부 |
| 13 | carryforward_amount | BIGINT | NULL | - | 이월공제 잔액 (최저한세 초과분 등), 단위: 원 |
| 14 | sunset_date | VARCHAR(20) | NULL | - | 일몰(적용기한) 일자. 예: `2025-12-31` |
| 15 | deduction_rate | VARCHAR(30) | NULL | - | 적용 공제율. 예: `25%`, `40%` |
| 16 | conditions | TEXT | NULL | - | 적용 조건 설명 |
| 17 | required_documents | TEXT | NULL | - | 첨부 필요 서류 목록 |
| 18 | exclusion_items | TEXT | NULL | - | 상호배제로 제외된 항목 목록 |
| 19 | notes | TEXT | NULL | - | 비고/참고사항 |
| 20 | tax_year | VARCHAR(4) | NULL | - | 귀속 사업연도 |
| 21 | rd_type | VARCHAR(30) | NULL | - | R&D 유형 (R&D 공제 시). `NEW_GROWTH`, `BASIC`, `OUTSOURCED` |
| 22 | method | VARCHAR(50) | NULL | - | 계산 방법. `INCREMENT`(증가분), `CURRENT`(당기분) |
| 23 | calc_detail | TEXT | NULL | - | 산출 상세 내역 (JSON). 계산 공식, 중간값, 적용 세율 등 |
| 24 | legal_basis | VARCHAR(200) | NULL | - | 근거법조 전문 |
| 25 | exclusion_reasons | TEXT | NULL | - | 제외 사유 (item_status = EXCLUDED인 경우) |

### 적용 순서 (법인세법 §59 / 소득세법 §60)

```
1단계: 세액감면 (TAX_EXEMPTION)     - 예: 창업중소기업 감면, 중소기업 특별감면
2단계: 이월불가 세액공제             - 예: 고용증대 공제(당기분), 사회보험료 공제
3단계: 이월가능 세액공제             - 예: R&D 공제, 통합투자 공제
```

### 활용

- **개별 공제 산출(M4)**: INP_DEDUCTION + REF_* 기준정보를 결합하여 각 항목의 gross_amount 산출
- **농어촌특별세 계산**: nongteuk_exempt = false인 항목에 대해 REF_NONGTEUKSE의 세율 적용
- **최저한세 조정(M5)**: min_tax_subject = true인 항목 합계가 최저한세를 초과하면 초과분을 carryforward_amount로 이월
- **조합 비교**: OUT_COMBINATION에서 이 테이블의 항목들을 조합하여 최적 환급액 탐색
- **보고서 출력**: OUT_REPORT_JSON Section B의 기초 데이터
- **이월공제 추적**: is_carryforward = true인 항목의 잔액을 다음 연도로 이월 안내

---

## OUT_COMBINATION - 조합 비교/최적 선택

### 기본 정보

| 항목 | 내용 |
|------|------|
| **테이블명** | OUT_COMBINATION |
| **한글명** | 조합 비교/최적 선택 |
| **설명** | 상호배제 관계에 있는 세액공제/감면 항목들의 가능한 조합 대안을 생성하고, 순환급액 기준 최적 조합을 선정한 결과 |
| **PK** | req_id + combo_id (복합키) |
| **FK** | 논리적으로 REQ_REQUEST.req_id 참조 |
| **인덱스** | idx_out_combination_rank (req_id, combo_rank) |

### 컬럼 정의

| No | 컬럼명 | 데이터 타입 | NULL | 기본값 | 설명 |
|----|--------|-----------|------|--------|------|
| 1 | req_id | VARCHAR(30) | NOT NULL | - | 요청 식별자 |
| 2 | combo_id | VARCHAR(30) | NOT NULL | - | 조합 식별자. 예: `COMBO_001`, `COMBO_002` |
| 3 | combo_rank | INT | NULL | - | 순환급액 기준 순위 (1 = 최적) |
| 4 | group_type | VARCHAR(30) | NULL | - | 조합 그룹 유형. `FULL`, `PARTIAL`, `SINGLE` |
| 5 | combo_name | VARCHAR(200) | NULL | - | 조합 설명. 예: `R&D(신성장)+고용증대+통합투자 조합` |
| 6 | items_json | TEXT | NULL | - | 포함 항목 목록 (JSON 배열). OUT_CREDIT_DETAIL.item_id 참조 |
| 7 | exemption_total | BIGINT | NULL | - | 세액감면 합계, 단위: 원 |
| 8 | credit_total | BIGINT | NULL | - | 세액공제 합계, 단위: 원 |
| 9 | min_tax_adj | BIGINT | NULL | - | 최저한세 조정액, 단위: 원 |
| 10 | nongteuk_total | BIGINT | NULL | - | 농어촌특별세 합계, 단위: 원 |
| 11 | net_refund | BIGINT | NULL | - | 순환급액 (감면+공제-최저한세조정-농특세), 단위: 원 |
| 12 | is_valid | BOOLEAN | NULL | - | 유효 조합 여부 (상호배제 위반 없음) |
| 13 | application_order | TEXT | NULL | - | 적용 순서 (JSON). 감면→이월불가공제→이월가능공제 순서 |
| 14 | carryforward_items | TEXT | NULL | - | 이월 대상 항목 및 잔액 (JSON) |

### 활용

- **최적 조합 탐색(M5)**: 상호배제 규칙(REF_MUTUAL_EXCLUSION)을 준수하면서 net_refund가 최대인 조합 선정
- **대안 비교**: combo_rank 순으로 정렬하여 최적 대안과 차선 대안을 비교 제시
- **적용 순서 보장**: application_order에 따라 법인세법 §59 / 소득세법 §60의 적용순서 준수
- **이월공제 잔액 산출**: 최저한세 초과분을 carryforward_items에 기록하여 다음 연도 이월 안내
- **보고서 출력**: OUT_REPORT_JSON Section C의 기초 데이터

---

## OUT_EXCLUSION_VERIFY - 상호배제 검증

### 기본 정보

| 항목 | 내용 |
|------|------|
| **테이블명** | OUT_EXCLUSION_VERIFY |
| **한글명** | 상호배제 검증 |
| **설명** | 조합 내 공제/감면 항목 간 중복적용(상호배제) 규칙 위반 여부를 검증한 결과. 조특법 §127④ 근거 |
| **PK** | req_id + verify_id (복합키) |
| **FK** | 논리적으로 REQ_REQUEST.req_id 참조 |

### 컬럼 정의

| No | 컬럼명 | 데이터 타입 | NULL | 기본값 | 설명 |
|----|--------|-----------|------|--------|------|
| 1 | req_id | VARCHAR(30) | NOT NULL | - | 요청 식별자 |
| 2 | verify_id | VARCHAR(30) | NOT NULL | - | 검증 식별자. 예: `VERIFY_001` |
| 3 | combo_id | VARCHAR(30) | NULL | - | 검증 대상 조합 ID (OUT_COMBINATION.combo_id 참조) |
| 4 | provision_a | VARCHAR(50) | NULL | - | 비교 법조 A. 예: `조특법6` |
| 5 | provision_b | VARCHAR(50) | NULL | - | 비교 법조 B. 예: `조특법7` |
| 6 | overlap_allowed | VARCHAR(10) | NULL | - | 중복 허용 여부. `YES`, `NO`, `CONDITIONAL` |
| 7 | condition_note | VARCHAR(500) | NULL | - | 조건부 허용 시 조건 설명 |
| 8 | violation_detected | BOOLEAN | NULL | - | 위반 감지 여부 (true = 위반) |
| 9 | legal_basis | VARCHAR(200) | NULL | - | 근거법조. 예: `조세특례제한법 제127조 제4항` |

### 활용

- **조합 유효성 검증**: OUT_COMBINATION의 각 조합에 대해 포함된 항목 쌍을 REF_MUTUAL_EXCLUSION과 대조
- **위반 조합 필터링**: violation_detected = true인 검증이 있는 조합은 OUT_COMBINATION.is_valid = false로 설정
- **조건부 중복**: overlap_allowed = CONDITIONAL인 경우 condition_note의 조건 충족 여부를 추가 판단
- **보고서 출력**: 상호배제 검증 결과가 OUT_REPORT_JSON에 포함되어 신뢰성 확보

---

## OUT_REFUND - 최종 환급액 산출

### 기본 정보

| 항목 | 내용 |
|------|------|
| **테이블명** | OUT_REFUND |
| **한글명** | 최종 환급액 산출 |
| **설명** | 기존 신고세액과 경정 후 세액을 비교하여 최종 환급액, 환급가산금, 지방세 환급액 등을 산출한 결과. 시스템의 최종 산출물 |
| **PK** | req_id |
| **FK** | 논리적으로 REQ_REQUEST.req_id 참조 |

### 컬럼 정의

| No | 컬럼명 | 데이터 타입 | NULL | 기본값 | 설명 |
|----|--------|-----------|------|--------|------|
| 1 | req_id | VARCHAR(30) | NOT NULL | - | 요청 식별자 |
| 2 | existing_computed_tax | BIGINT | NULL | - | 기존 산출세액, 단위: 원 |
| 3 | existing_deductions | BIGINT | NULL | - | 기존 공제/감면 합계, 단위: 원 |
| 4 | existing_determined_tax | BIGINT | NULL | - | 기존 결정세액, 단위: 원 |
| 5 | existing_paid_tax | BIGINT | NULL | - | 기납부세액, 단위: 원 |
| 6 | new_computed_tax | BIGINT | NULL | - | 경정 후 산출세액, 단위: 원 |
| 7 | new_deductions | BIGINT | NULL | - | 경정 후 공제/감면 합계(최적 조합 적용), 단위: 원 |
| 8 | new_min_tax_adj | BIGINT | NULL | - | 최저한세 조정액, 단위: 원 |
| 9 | new_determined_tax | BIGINT | NULL | - | 경정 후 결정세액, 단위: 원 |
| 10 | nongteuk_total | BIGINT | NULL | - | 농어촌특별세 합계, 단위: 원 |
| 11 | refund_amount | BIGINT | NULL | - | 환급세액 (기존 결정세액 - 경정 후 결정세액), 단위: 원 |
| 12 | refund_interest_start | DATE | NULL | - | 환급가산금 기산일 |
| 13 | refund_interest_end | VARCHAR(20) | NULL | - | 환급가산금 종료일 (또는 "지급결정일") |
| 14 | refund_interest_rate | DECIMAL(10,6) | NULL | - | 환급가산금 이자율 (REF_REFUND_INTEREST_RATE 참조) |
| 15 | refund_interest_amount | BIGINT | NULL | - | 환급가산금, 단위: 원 (1원 미만 절사) |
| 16 | interim_refund_amount | BIGINT | NULL | - | 중간예납 환급액, 단위: 원 |
| 17 | interim_interest_amount | BIGINT | NULL | - | 중간예납 환급가산금, 단위: 원 |
| 18 | local_tax_refund | BIGINT | NULL | - | 지방소득세 환급 예상액 (법인세/소득세의 10%), 단위: 원 |
| 19 | total_expected | BIGINT | NULL | - | 총 예상 환급액 (환급세액+환급가산금+중간예납환급+지방세환급), 단위: 원 |
| 20 | refund_cap_detail | TEXT | NULL | - | 환급 한도 상세 (JSON). 최저한세 한도, 농특세 조정 등 |
| 21 | optimal_combo_id | VARCHAR(30) | NULL | - | 적용된 최적 조합 ID (OUT_COMBINATION.combo_id 참조) |
| 22 | carryforward_credits | BIGINT | NULL | - | 이월공제 잔액 합계, 단위: 원 |
| 23 | carryforward_detail | TEXT | NULL | - | 이월공제 항목별 상세 (JSON) |
| 24 | penalty_tax_change | BIGINT | NULL | - | 가산세 변동액, 단위: 원 (경정 시 가산세 감소분) |

### 환급액 산출 공식

```
환급세액 = 기존 결정세액 - 경정 후 결정세액
         = existing_determined_tax - new_determined_tax

경정 후 결정세액 = new_computed_tax - new_deductions + new_min_tax_adj

총 예상 환급액 = 환급세액 + 환급가산금 + 중간예납환급 + 지방세환급
              = refund_amount + refund_interest_amount
                + interim_refund_amount + local_tax_refund

※ 모든 금액은 10원 미만 절사 (TRUNCATE), 환급가산금은 1원 미만 절사
※ 반올림(ROUND) 절대 금지
```

### 활용

- **최종 산출(M5)**: 최적 조합(optimal_combo_id)의 공제/감면 합계를 적용하여 환급액 확정
- **환급가산금 산출**: refund_interest_start ~ end 기간에 REF_REFUND_INTEREST_RATE의 이자율 적용
- **지방세 환급**: 법인세/소득세 환급액의 10%를 지방소득세 환급 예상액으로 산출
- **이월공제 안내**: 최저한세 초과분 등 당기 미사용 공제액을 carryforward_detail에 기록
- **보고서 출력**: OUT_REPORT_JSON Section D의 핵심 데이터

---

## OUT_RISK - 사후관리/리스크 평가

### 기본 정보

| 항목 | 내용 |
|------|------|
| **테이블명** | OUT_RISK |
| **한글명** | 사후관리/리스크 평가 |
| **설명** | 세액공제/감면 적용 후 의무사항, 사후관리 기간, 위반 시 추징 리스크를 항목별로 평가한 결과 |
| **PK** | req_id + risk_id (복합키) |
| **FK** | 논리적으로 REQ_REQUEST.req_id 참조 |
| **인덱스** | idx_out_risk_level (risk_level) |

### 컬럼 정의

| No | 컬럼명 | 데이터 타입 | NULL | 기본값 | 설명 |
|----|--------|-----------|------|--------|------|
| 1 | req_id | VARCHAR(30) | NOT NULL | - | 요청 식별자 |
| 2 | risk_id | VARCHAR(30) | NOT NULL | - | 리스크 식별자. 예: `RISK_EMP_001` |
| 3 | provision | VARCHAR(50) | NULL | - | 관련 법조 |
| 4 | risk_type | VARCHAR(30) | NULL | - | 리스크 유형. `HEADCOUNT_MAINTAIN`(인원유지), `ASSET_HOLD`(자산보유), `PURPOSE_USE`(용도외사용) |
| 5 | obligation | VARCHAR(200) | NULL | - | 의무 사항 설명. 예: `고용증대 공제 적용 후 2년간 근로자 수 유지 의무` |
| 6 | period_start | DATE | NULL | - | 사후관리 시작일 |
| 7 | period_end | VARCHAR(20) | NULL | - | 사후관리 종료일 (또는 "영구") |
| 8 | violation_action | VARCHAR(200) | NULL | - | 위반 시 조치. 예: `공제세액 전액 추징 + 가산세` |
| 9 | potential_clawback | BIGINT | NULL | - | 추징 예상액, 단위: 원 |
| 10 | interest_surcharge | BIGINT | NULL | - | 이자 가산세 예상액, 단위: 원 |
| 11 | risk_level | VARCHAR(20) | NULL | - | 리스크 수준. `HIGH`, `MEDIUM`, `LOW` |
| 12 | description | TEXT | NULL | - | 리스크 상세 설명 |

### risk_type 코드 값

| risk_type | 설명 | 예시 |
|-----------|------|------|
| HEADCOUNT_MAINTAIN | 고용 인원 유지 의무 | 고용증대 공제 후 2년간 근로자 수 미달 시 추징 |
| ASSET_HOLD | 자산 보유 의무 | 투자 공제 후 자산 처분 시 추징 |
| PURPOSE_USE | 용도 사용 의무 | 특정 용도로 사용 약정 위반 시 추징 |
| SUNSET_RISK | 일몰 기한 리스크 | 적용기한 경과 후 이월공제 불가 |
| AUDIT_RISK | 세무조사 리스크 | 경정청구 후 세무조사 가능성 |

### 활용

- **사후관리 안내**: 각 공제/감면 항목 적용 후 발생하는 의무사항과 기간을 명확히 안내
- **추징 리스크 정량화**: potential_clawback + interest_surcharge로 위반 시 재무적 영향 추정
- **리스크 우선순위**: risk_level로 HIGH → MEDIUM → LOW 순으로 주의 항목 정렬
- **보고서 출력**: OUT_REPORT_JSON Section E의 기초 데이터

---

## OUT_ADDITIONAL_CHECK - 추가 확인 필요

### 기본 정보

| 항목 | 내용 |
|------|------|
| **테이블명** | OUT_ADDITIONAL_CHECK |
| **한글명** | 추가 확인 필요 |
| **설명** | 시스템 판단만으로 확정할 수 없어 사람의 추가 확인이 필요한 항목을 기록. 세무사/담당자가 보완 검토해야 할 사항 |
| **PK** | req_id + check_id (복합키) |
| **FK** | 논리적으로 REQ_REQUEST.req_id 참조 |

### 컬럼 정의

| No | 컬럼명 | 데이터 타입 | NULL | 기본값 | 설명 |
|----|--------|-----------|------|--------|------|
| 1 | req_id | VARCHAR(30) | NOT NULL | - | 요청 식별자 |
| 2 | check_id | VARCHAR(30) | NOT NULL | - | 확인항목 식별자. 예: `ADDCHK_001` |
| 3 | description | VARCHAR(500) | NULL | - | 확인 필요 항목 설명 |
| 4 | reason | VARCHAR(500) | NULL | - | 추가 확인이 필요한 사유 |
| 5 | related_inspection | VARCHAR(50) | NULL | - | 관련 점검항목 코드 (CHK_INSPECTION_LOG.inspection_code 참조) |
| 6 | related_module | VARCHAR(50) | NULL | - | 관련 계산 모듈 |
| 7 | priority | VARCHAR(10) | NULL | - | 우선순위. `HIGH`, `MEDIUM`, `LOW` |
| 8 | status | VARCHAR(20) | NULL | - | 확인 상태. `PENDING`(미확인), `CONFIRMED`(확인완료), `REJECTED`(불인정) |

### 활용

- **사람의 판단 필요 항목**: CHK_INSPECTION_LOG에서 REVIEW_NEEDED 판정, CHK_VALIDATION_LOG에서 WARNING 결과 발생 시 자동 등록
- **세무사 검토 대기열**: priority 순으로 정렬하여 세무사/담당자에게 검토 목록 제공
- **보완 요청**: status = PENDING인 항목에 대해 추가 서류 제출 또는 사실관계 확인 요청
- **보고서 출력**: OUT_REPORT_JSON Section F의 기초 데이터

---

## OUT_REPORT_JSON - 최종 보고서 JSON 저장

### 기본 정보

| 항목 | 내용 |
|------|------|
| **테이블명** | OUT_REPORT_JSON |
| **한글명** | 최종 보고서 JSON 저장 |
| **설명** | 모든 산출 결과를 7섹션(A~G) JSON으로 조립하여 저장. 클라이언트에게 전달되는 최종 응답 데이터의 원본 보관 |
| **PK** | req_id |
| **FK** | 논리적으로 REQ_REQUEST.req_id 참조 |

### 컬럼 정의

| No | 컬럼명 | 데이터 타입 | NULL | 기본값 | 설명 |
|----|--------|-----------|------|--------|------|
| 1 | req_id | VARCHAR(30) | NOT NULL | - | 요청 식별자 |
| 2 | report_version | VARCHAR(20) | NULL | - | 보고서 포맷 버전 |
| 3 | report_status | VARCHAR(20) | NULL | - | 보고서 상태. `DRAFT`, `FINAL`, `ERROR` |
| 4 | report_json | LONGTEXT | NULL | - | 전체 보고서 JSON (7섹션 통합) |
| 5 | section_a_json | TEXT | NULL | - | Section A: 자격 진단 결과 (CHK_ELIGIBILITY 기반) |
| 6 | section_b_json | TEXT | NULL | - | Section B: 개별 공제/감면 산출 (OUT_CREDIT_DETAIL 기반) |
| 7 | section_c_json | TEXT | NULL | - | Section C: 조합 비교/최적 선택 (OUT_COMBINATION 기반) |
| 8 | section_d_json | TEXT | NULL | - | Section D: 최종 환급액 (OUT_REFUND 기반) |
| 9 | section_e_json | TEXT | NULL | - | Section E: 사후관리/리스크 (OUT_RISK 기반) |
| 10 | section_f_json | TEXT | NULL | - | Section F: 추가 확인 필요 (OUT_ADDITIONAL_CHECK 기반) |
| 11 | section_g_meta | TEXT | NULL | - | Section G: 메타데이터 (버전, 처리시간, 설계서 버전 등) |
| 12 | json_byte_size | INT | NULL | - | 전체 JSON 크기 (바이트) |
| 13 | result_code | VARCHAR(20) | NULL | - | 처리 결과 코드. `SUCCESS`, `PARTIAL`, `FAIL` |
| 14 | checksum | VARCHAR(64) | NULL | - | SHA-256 체크섬 (무결성 검증) |
| 15 | generated_at | TIMESTAMP | NULL | - | 보고서 생성 시각 |

### 7섹션 구조

| 섹션 | 컬럼 | 내용 | 원천 테이블 |
|------|------|------|-----------|
| A | section_a_json | 자격 진단 결과 | CHK_ELIGIBILITY |
| B | section_b_json | 개별 공제/감면 산출 내역 | OUT_CREDIT_DETAIL |
| C | section_c_json | 조합 비교 및 최적 선택 | OUT_COMBINATION, OUT_EXCLUSION_VERIFY |
| D | section_d_json | 최종 환급액 산출 | OUT_REFUND |
| E | section_e_json | 사후관리 및 리스크 | OUT_RISK |
| F | section_f_json | 추가 확인 필요 항목 | OUT_ADDITIONAL_CHECK |
| G | section_g_meta | 메타데이터 | REQ_REQUEST, 시스템 정보 |

### 활용

- **API 응답(API-04)**: report_json을 클라이언트에게 전달하여 보고서 렌더링
- **섹션별 조회**: 특정 섹션만 필요한 경우 section_*_json 개별 컬럼 조회 가능
- **보관/감사**: 최종 보고서 원본을 DB에 보관하여 추후 동일한 결과 재현/확인 가능
- **무결성 검증**: checksum으로 보고서 변조 여부 확인
- **용량 모니터링**: json_byte_size로 대용량 보고서 감지 및 성능 최적화

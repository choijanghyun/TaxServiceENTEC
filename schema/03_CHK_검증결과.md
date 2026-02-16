# 주제영역 3: 검증 결과 (CHK)

## 개요

경정청구 요청에 대한 **자격 진단, 점검항목 판정, 검증 규칙 실행 결과**를 저장하는 영역입니다.
계산 엔진이 환급액을 산출하기 전에, 입력 데이터의 유효성과 신청인의 적격성을 사전 검증합니다.

### 처리 흐름

```
INP_BASIC + INP_EMPLOYEE + INP_DEDUCTION + INP_FINANCIAL
                    ↓
        M2: 자격 진단 모듈 → CHK_ELIGIBILITY
                    ↓
        M2: 점검항목 판정 → CHK_INSPECTION_LOG
                    ↓
        검증 규칙 실행 → CHK_VALIDATION_LOG
                    ↓
    overall_status = ELIGIBLE → M3~M5 계산 엔진 진입
    overall_status = INELIGIBLE → 처리 중단 (Hard Fail)
```

---

## CHK_ELIGIBILITY - 자격 진단 결과

### 기본 정보

| 항목 | 내용 |
|------|------|
| **테이블명** | CHK_ELIGIBILITY |
| **한글명** | 자격 진단 결과 |
| **설명** | 경정청구 가능 여부(기한, 중소기업 적격성, 업종, 결산확정 등)를 종합 진단한 결과. overall_status가 INELIGIBLE이면 이후 계산 불가 |
| **PK** | req_id |
| **FK** | 논리적으로 REQ_REQUEST.req_id 참조 |

### 컬럼 정의

| No | 컬럼명 | 데이터 타입 | NULL | 기본값 | 설명 |
|----|--------|-----------|------|--------|------|
| 1 | req_id | VARCHAR(30) | NOT NULL | - | 요청 식별자 |
| 2 | tax_type | VARCHAR(10) | NULL | - | 세목 코드. `CORP` 또는 `INC` |
| 3 | company_size | VARCHAR(20) | NULL | - | 판정된 기업 규모. `LARGE`, `MEDIUM`, `SMALL` |
| 4 | capital_zone | VARCHAR(20) | NULL | - | 판정된 수도권 구분. `CAPITAL`, `NON_CAPITAL` |
| 5 | filing_deadline | DATE | NULL | - | 법정 신고기한 |
| 6 | claim_deadline | DATE | NULL | - | 경정청구 법정 기한 (신고기한 + 5년) |
| 7 | deadline_eligible | VARCHAR(20) | NULL | - | 기한 적격성. `ELIGIBLE`(기한 내), `EXPIRED`(기한 초과), `WARNING`(임박) |
| 8 | sme_eligible | VARCHAR(20) | NULL | - | 중소기업 적격성. `ELIGIBLE`, `INELIGIBLE`, `GRACE_PERIOD` |
| 9 | sme_grace_end_year | VARCHAR(4) | NULL | - | 중소기업 유예기간 종료 연도 (초과한 경우) |
| 10 | small_vs_medium | VARCHAR(20) | NULL | - | 소기업/중기업 구분. `SMALL_BIZ`, `MEDIUM_BIZ` |
| 11 | venture_confirmed | BOOLEAN | NULL | - | 벤처기업 확인 결과 |
| 12 | settlement_check_result | VARCHAR(50) | NULL | - | 결산확정 검증 결과. `PASS`, `FAIL`, `NOT_APPLICABLE` |
| 13 | settlement_blocked_items | TEXT | NULL | - | 결산조정으로 인해 경정청구 불가인 항목 목록 |
| 14 | estimate_check | BOOLEAN | NULL | - | 추계신고 여부 (추계 시 일부 공제 제한) |
| 15 | sincerity_target | BOOLEAN | NULL | - | 성실신고확인 대상 여부 확인 결과 |
| 16 | overall_status | VARCHAR(30) | NULL | - | 종합 판정. `ELIGIBLE`(진행 가능), `INELIGIBLE`(진행 불가), `CONDITIONAL`(조건부 가능) |
| 17 | diagnosis_detail | TEXT | NULL | - | 진단 상세 사유 (사람이 읽을 수 있는 텍스트) |
| 18 | checked_at | TIMESTAMP | NULL | - | 진단 수행 시각 |

### overall_status 판정 기준

| 상태 | 조건 | 후속 처리 |
|------|------|----------|
| ELIGIBLE | 모든 진단 항목 통과 | M3~M5 계산 엔진 정상 진입 |
| CONDITIONAL | 일부 경고 항목 존재 (예: 기한 임박, 추계신고) | 계산은 진행하되 OUT_ADDITIONAL_CHECK에 확인 필요 항목 기록 |
| INELIGIBLE | 치명적 결격사유 존재 (기한 초과, 결산확정 실패) | 처리 중단, REQ_REQUEST.request_status = FAILED |

### 활용

- **게이트키핑**: overall_status가 ELIGIBLE이 아니면 환급액 산출을 중단하여 불필요한 계산 방지
- **기한 관리**: claim_deadline과 현재일을 비교하여 경정청구 가능 여부 즉시 판단
- **중소기업 특례 판정**: sme_eligible, small_vs_medium으로 적용 가능한 중소기업 특례 세액공제/감면 범위 결정
- **결산확정 원칙(CORP)**: settlement_check_result가 FAIL이면 해당 항목의 경정청구 불가 사유를 settlement_blocked_items에 기록
- **보고서 출력**: diagnosis_detail이 최종 보고서(OUT_REPORT_JSON) Section A에 포함

---

## CHK_INSPECTION_LOG - 점검항목별 판정

### 기본 정보

| 항목 | 내용 |
|------|------|
| **테이블명** | CHK_INSPECTION_LOG |
| **한글명** | 점검항목별 판정 |
| **설명** | 각 세액공제/감면 항목에 대한 개별 점검 결과. 근거법조별로 적용 가능 여부, 산출 금액, 판정 사유를 기록 |
| **PK** | req_id + inspection_code (복합키) |
| **FK** | 논리적으로 REQ_REQUEST.req_id 참조 |

### 컬럼 정의

| No | 컬럼명 | 데이터 타입 | NULL | 기본값 | 설명 |
|----|--------|-----------|------|--------|------|
| 1 | req_id | VARCHAR(30) | NOT NULL | - | 요청 식별자 |
| 2 | inspection_code | VARCHAR(30) | NOT NULL | - | 점검항목 코드. 예: `INS_RD_10`, `INS_EMP_29_7`, `INS_INVEST_25_4` |
| 3 | inspection_name | VARCHAR(100) | NULL | - | 점검항목 한글명. 예: `연구개발비 세액공제 점검` |
| 4 | legal_basis | VARCHAR(200) | NULL | - | 근거법조. 예: `조세특례제한법 제10조` |
| 5 | judgment | VARCHAR(30) | NULL | - | 판정 결과. `APPLICABLE`(적용가능), `NOT_APPLICABLE`(적용불가), `PARTIAL`(부분적용), `REVIEW_NEEDED`(확인필요) |
| 6 | summary | VARCHAR(500) | NULL | - | 판정 사유 요약 |
| 7 | related_module | VARCHAR(50) | NULL | - | 관련 계산 모듈. `M3`(고용), `M4`(공제산출), `M5`(조합최적화) |
| 8 | calculated_amount | BIGINT | NULL | - | 점검 시 산출된 예상 금액, 단위: 원 (참고용) |
| 9 | sort_order | INT | NULL | - | 보고서 출력 시 표시 순서 |
| 10 | checked_at | TIMESTAMP | NULL | - | 점검 수행 시각 |

### judgment 코드 값

| judgment | 설명 | 후속 처리 |
|----------|------|----------|
| APPLICABLE | 해당 공제/감면 적용 가능 | OUT_CREDIT_DETAIL에 산출 결과 생성 |
| NOT_APPLICABLE | 적용 불가 (요건 미충족) | 산출 대상에서 제외 |
| PARTIAL | 부분적으로 적용 가능 | 적용 가능 범위만 산출, 제한 사유 기록 |
| REVIEW_NEEDED | 추가 확인 필요 | OUT_ADDITIONAL_CHECK에 확인 항목 등록 |

### 활용

- **항목별 적격성 판정**: 각 세액공제/감면 항목의 적용 가능 여부를 사전에 판별
- **부분적용 사유 기록**: 공제 한도 초과, 일부 요건 미충족 시 부분 적용 범위와 사유 기록
- **보고서 출력**: 점검 결과가 OUT_REPORT_JSON Section B의 기초 데이터
- **산출 대상 필터링**: judgment = APPLICABLE 또는 PARTIAL인 항목만 M4(공제 산출) 모듈로 전달
- **우선순위 정렬**: sort_order에 따라 보고서 내 표시 순서 결정

---

## CHK_VALIDATION_LOG - 검증 규칙 실행 결과

### 기본 정보

| 항목 | 내용 |
|------|------|
| **테이블명** | CHK_VALIDATION_LOG |
| **한글명** | 검증 규칙 실행 결과 |
| **설명** | 데이터 정합성, 업무 규칙, 교차 검증 등 세부 검증 규칙의 실행 결과를 기록. PASS/FAIL/WARNING/SKIP 상태 관리 |
| **PK** | req_id + rule_code (복합키) |
| **FK** | 논리적으로 REQ_REQUEST.req_id 참조 |
| **인덱스** | idx_chk_validation_result (result) |

### 컬럼 정의

| No | 컬럼명 | 데이터 타입 | NULL | 기본값 | 설명 |
|----|--------|-----------|------|--------|------|
| 1 | req_id | VARCHAR(30) | NOT NULL | - | 요청 식별자 |
| 2 | rule_code | VARCHAR(30) | NOT NULL | - | 검증 규칙 코드. 예: `VLD_001`, `VLD_002` |
| 3 | rule_type | CHAR(1) | NULL | - | 규칙 유형. `D`=데이터 정합성, `B`=업무 규칙, `C`=교차 검증 |
| 4 | rule_description | VARCHAR(500) | NULL | - | 규칙 설명 (사람이 읽을 수 있는 텍스트) |
| 5 | result | VARCHAR(10) | NULL | - | 실행 결과. `PASS`, `FAIL`, `WARNING`, `SKIP` |
| 6 | detail | TEXT | NULL | - | 결과 상세 (실패 사유, 경고 내용 등) |
| 7 | executed_at | TIMESTAMP | NULL | - | 실행 시각 |

### rule_type 코드 값

| rule_type | 설명 | 예시 |
|-----------|------|------|
| D | 데이터 정합성 검증 | 금액 합계 불일치, 필수 항목 누락, 날짜 범위 오류 |
| B | 업무 규칙 검증 | 중소기업 매출 기준 초과, 업종 제외 업종 해당, 감면 적용기한 초과 |
| C | 교차 검증 | INP_BASIC.revenue vs INP_FINANCIAL.biz_income 정합성, 상호배제 규칙 위반 |

### result 코드 값

| result | 설명 | 후속 처리 |
|--------|------|----------|
| PASS | 검증 통과 | 정상 진행 |
| FAIL | 검증 실패 (치명적) | 해당 항목 계산 중단 또는 전체 처리 중단 |
| WARNING | 경고 (비치명적) | 계산은 진행하되 OUT_ADDITIONAL_CHECK에 확인 항목 등록 |
| SKIP | 검증 건너뜀 | 해당 세목/조건에 적용되지 않는 규칙 |

### 활용

- **데이터 품질 보증**: 입력 데이터의 정합성을 다각도로 검증하여 잘못된 계산 방지
- **업무 규칙 준수**: 세법상 요건 충족 여부를 규칙 기반으로 체계적 검증
- **교차 검증**: 서로 다른 입력 테이블 간 데이터 일관성 확인
- **규칙 실행 순서 관리**: v3.1에서 추가된 규칙 간 우선순위/의존성에 따라 순서대로 실행
- **실패 분석**: result = FAIL인 규칙을 조회하여 경정청구 불가 사유를 빠르게 파악
- **감사 대응**: 모든 검증 규칙의 실행 이력이 보관되어 판정 근거 소명 가능

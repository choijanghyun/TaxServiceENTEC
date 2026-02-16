# 주제영역 1: 요청 관리 (REQ)

## 개요

경정청구 환급액 산출 시스템의 **요청 생명주기**를 관리하는 영역입니다.
1회의 경정청구 진단 요청을 하나의 트랜잭션 단위로 관리하며, 동일 신청인이라도 요청 시점이 다르면 새로운 `req_id`가 부여되어 독립적으로 이력이 관리됩니다.

- **Request-Driven 모델**: 모든 비기준 테이블(INP_*, CHK_*, OUT_*, LOG_*)의 데이터는 이 테이블의 `req_id`를 FK로 참조합니다.
- **감사추적**: 요청원천(API/WEB/BATCH), 요청자, 클라이언트 IP를 기록하여 감사 대응력을 확보합니다.
- **낙관적 잠금**: `version` 컬럼으로 동시성 충돌을 방지합니다.

---

## REQ_REQUEST - 요청 마스터

### 기본 정보

| 항목 | 내용 |
|------|------|
| **테이블명** | REQ_REQUEST |
| **한글명** | 요청 마스터 |
| **설명** | 경정청구 진단 요청의 메타데이터를 관리하는 마스터 테이블. 시스템의 모든 처리 단위의 기준점 |
| **PK** | req_id |
| **FK** | 없음 (최상위 마스터) |
| **인덱스** | idx_req_request_applicant (applicant_id), idx_req_request_tax_year (tax_year), idx_req_request_status (request_status) |

### 컬럼 정의

| No | 컬럼명 | 데이터 타입 | NULL | 기본값 | 설명 |
|----|--------|-----------|------|--------|------|
| 1 | req_id | VARCHAR(30) | NOT NULL | - | 요청 식별자. 형식: `{applicant_id}_{YYYYMMDD}_{seq_no}` |
| 2 | applicant_type | CHAR(1) | NOT NULL | - | 신청인 유형. `C`=법인사업자, `P`=개인사업자 |
| 3 | applicant_id | VARCHAR(15) | NOT NULL | - | 납세자 식별번호. 법인=사업자등록번호, 개인=주민등록번호 |
| 4 | applicant_name | VARCHAR(100) | NOT NULL | - | 신청인 명칭 (법인명 또는 성명) |
| 5 | tax_type | VARCHAR(4) | NOT NULL | - | 세목 코드. `CORP`=법인세, `INC`=종합소득세 |
| 6 | tax_year | VARCHAR(4) | NOT NULL | - | 귀속 사업연도 (YYYY 형식) |
| 7 | request_date | DATE | NOT NULL | - | 요청 접수일 |
| 8 | seq_no | INT | NULL | - | 동일 신청인 + 동일 날짜 내 순번 (동시성 제어 시 `SELECT FOR UPDATE` 사용) |
| 9 | request_status | VARCHAR(20) | NOT NULL | - | 처리 상태. `RECEIVED`, `PARSING`, `CHECKING`, `CALCULATING`, `COMPLETED`, `FAILED` |
| 10 | prompt_version | VARCHAR(20) | NULL | - | AI 점검에 사용된 프롬프트 버전 (예: v1.3) |
| 11 | design_version | VARCHAR(20) | NULL | - | 적용된 설계서 버전 (예: v3.2) |
| 12 | created_at | TIMESTAMP | NOT NULL | - | 요청 생성 시각 (자동 감사추적) |
| 13 | completed_at | TIMESTAMP | NULL | - | 처리 완료 시각 |
| 14 | request_source | VARCHAR(30) | NULL | - | 요청 원천. `API`=REST API, `WEB`=웹 UI, `BATCH`=배치 처리 |
| 15 | requested_by | VARCHAR(50) | NULL | - | 요청자 ID (로그인 사용자 또는 시스템 계정) |
| 16 | client_ip | VARCHAR(45) | NULL | - | 요청자 클라이언트 IP (IPv4/IPv6 호환) |
| 17 | error_message | TEXT | NULL | - | 처리 실패 시 오류 상세 메시지 |
| 18 | modified_by | VARCHAR(50) | NULL | - | 최종 수정자 ID |
| 19 | modified_at | TIMESTAMP | NULL | - | 최종 수정 시각 (자동 감사추적) |
| 20 | version | INT | NOT NULL | 1 | 낙관적 잠금(Optimistic Lock) 버전. UPDATE 시 +1 증가 |

### req_id 생성 규칙

```
req_id = {applicant_id}_{YYYYMMDD}_{seq_no}

예시:
  법인: 1234567890_20260216_001
  개인: 9001011234567_20260216_001
```

- 동일 `applicant_id` + 동일 `request_date`에 대해 `seq_no`를 자동 증가
- `seq_no` 발급 시 `SELECT FOR UPDATE`로 동시성 충돌 방지

### request_status 상태 전이

```
RECEIVED → PARSING → CHECKING → CALCULATING → COMPLETED
    │          │          │            │
    └──────────┴──────────┴────────────┴──→ FAILED
```

| 상태 | 설명 | 전이 조건 |
|------|------|----------|
| RECEIVED | 요청 접수 완료 | API-01 호출 시 |
| PARSING | 입력 JSON 파싱 중 | M1-03 파싱 시작 |
| CHECKING | 자격 진단/점검 진행 중 | M2 점검 시작 |
| CALCULATING | 환급액 산출 진행 중 | M3~M5 계산 시작 |
| COMPLETED | 처리 완료 | 모든 산출 및 보고서 생성 완료 |
| FAILED | 처리 실패 | 어느 단계에서든 오류 발생 시 |

### 활용

- **API-01 POST /requests**: 신규 요청 생성 시 req_id 발급 및 INSERT
- **파이프라인 추적**: 각 모듈(M1~M6)이 처리 단계에 따라 request_status를 갱신
- **이력 조회**: applicant_id + tax_year 조합으로 동일 납세자의 과거 요청 이력 조회
- **감사 대응**: request_source, requested_by, client_ip로 요청 경위 추적
- **동시성 제어**: version 컬럼으로 낙관적 잠금 구현 (동시 수정 시 OptimisticLockException 발생)
- **오류 추적**: FAILED 상태의 요청에 대해 error_message로 원인 분석

### 관련 테이블

| 관계 | 대상 테이블 | 설명 |
|------|-----------|------|
| 1:N | INP_RAW_DATA | 하나의 요청에 여러 카테고리의 원시 JSON 데이터 |
| 1:1 | INP_BASIC | 요청당 하나의 기본정보 요약 |
| 1:N | INP_EMPLOYEE | 요청당 연도별(CURRENT, PREV1, PREV2...) 고용정보 |
| 1:N | INP_DEDUCTION | 요청당 여러 공제/감면 항목 |
| 1:1 | INP_FINANCIAL | 요청당 하나의 재무/세무 수치 요약 |
| 1:1 | CHK_ELIGIBILITY | 요청당 하나의 자격 진단 결과 |
| 1:N | CHK_INSPECTION_LOG | 요청당 여러 점검항목 판정 |
| 1:N | CHK_VALIDATION_LOG | 요청당 여러 검증 규칙 결과 |
| 1:N | OUT_* (전체) | 요청당 다수의 산출 결과 |
| 1:1 | OUT_REPORT_JSON | 요청당 하나의 최종 보고서 |
| 1:N | LOG_CALCULATION | 요청당 다수의 계산 로그 |

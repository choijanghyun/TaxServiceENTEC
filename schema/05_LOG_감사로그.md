# 주제영역 5: 감사 로그 (LOG)

## 개요

환급액 산출 과정의 **모든 계산 단계를 추적**하는 감사추적(Audit Trail) 영역입니다.
각 계산 단계의 입력값, 출력값, 적용 법조, 실행 시간 등을 기록하여 계산 결과의 **투명성과 재현성**을 보장합니다.

### 설계 원칙

- **비파괴적 기록**: INSERT ONLY, 기존 로그에 대한 UPDATE/DELETE 금지
- **추적 가능성**: trace_id로 하나의 요청 내 전체 계산 흐름을 end-to-end 추적
- **데이터 변경 감지**: prev_data_hash로 이전 단계 출력과 현재 단계 입력의 정합성 확인
- **성능 모니터링**: duration_ms로 각 단계의 처리 시간 측정

---

## LOG_CALCULATION - 계산 감사추적 로그

### 기본 정보

| 항목 | 내용 |
|------|------|
| **테이블명** | LOG_CALCULATION |
| **한글명** | 계산 감사추적 로그 |
| **설명** | M1~M6 파이프라인의 각 계산 단계별 실행 이력. 입력/출력 데이터, 적용 법조, 실행 시간, 추적 정보를 기록 |
| **PK** | log_id (AUTO_INCREMENT) |
| **FK** | 논리적으로 REQ_REQUEST.req_id 참조 (강제 FK 미설정, 감사 테이블 특성) |
| **인덱스** | idx_log_calculation_req (req_id), idx_log_calculation_trace (trace_id) |

### 컬럼 정의

| No | 컬럼명 | 데이터 타입 | NULL | 기본값 | 설명 |
|----|--------|-----------|------|--------|------|
| 1 | log_id | BIGINT | NOT NULL | AUTO_INCREMENT | 로그 레코드 식별자 |
| 2 | req_id | VARCHAR(30) | NOT NULL | - | 요청 식별자 |
| 3 | calc_step | VARCHAR(50) | NULL | - | 계산 단계명. 예: `M1_PARSE`, `M2_ELIGIBILITY`, `M3_EMPLOYEE`, `M4_CREDIT`, `M5_OPTIMIZE`, `M6_REPORT` |
| 4 | function_name | VARCHAR(100) | NULL | - | 실행 함수/메서드명. 예: `calculateRdCredit`, `buildCombination` |
| 5 | input_data | TEXT | NULL | - | 입력 데이터 (JSON). 계산에 사용된 주요 파라미터 |
| 6 | output_data | TEXT | NULL | - | 출력 데이터 (JSON). 계산 결과 |
| 7 | legal_basis | VARCHAR(200) | NULL | - | 적용 법조. 예: `조세특례제한법 제10조 제1항` |
| 8 | executed_at | TIMESTAMP | NULL | - | 실행 시각 |
| 9 | log_level | VARCHAR(10) | NULL | - | 로그 수준. `INFO`, `WARN`, `ERROR`, `DEBUG` |
| 10 | executed_by | VARCHAR(50) | NULL | - | 실행 주체 (시스템 계정 또는 배치 ID) |
| 11 | duration_ms | INT | NULL | - | 실행 소요 시간 (밀리초) |
| 12 | trace_id | VARCHAR(50) | NULL | - | 추적 ID. 하나의 요청 처리 전체를 관통하는 상관 식별자 |
| 13 | prev_data_hash | VARCHAR(64) | NULL | - | 이전 단계 출력의 해시값 (SHA-256). 단계 간 데이터 정합성 검증 |

### calc_step 코드 값

| calc_step | 모듈 | 설명 |
|-----------|------|------|
| M1_RECEIVE | M1 입력관리 | 요청 수신 및 req_id 발급 |
| M1_STORE | M1 입력관리 | 원시 JSON 저장 (INP_RAW_DATA) |
| M1_PARSE | M1 입력관리 | JSON 파싱 및 요약 테이블 생성 |
| M1_VALIDATE | M1 입력관리 | 입력 데이터 유효성 검증 |
| M2_ELIGIBILITY | M2 자격진단 | 경정청구 자격 진단 |
| M2_INSPECTION | M2 자격진단 | 개별 점검항목 판정 |
| M3_EMPLOYEE | M3 상시근로자 산정 | 상시근로자 수 산정 및 증감 계산 |
| M4_CREDIT | M4 공제산출 | 개별 세액공제/감면 산출 |
| M4_NONGTEUK | M4 공제산출 | 농어촌특별세 계산 |
| M5_COMBINATION | M5 조합최적화 | 공제 조합 생성 및 비교 |
| M5_EXCLUSION | M5 조합최적화 | 상호배제 검증 |
| M5_MIN_TAX | M5 조합최적화 | 최저한세 조정 |
| M5_REFUND | M5 조합최적화 | 최종 환급액 산출 |
| M5_RISK | M5 조합최적화 | 사후관리 리스크 평가 |
| M6_SERIALIZE | M6 보고/전달 | JSON 직렬화 및 보고서 생성 |
| M6_DELIVER | M6 보고/전달 | API 응답 전달 |

### log_level 사용 기준

| log_level | 사용 시점 | 예시 |
|-----------|----------|------|
| INFO | 정상 계산 단계 완료 | `R&D 세액공제 산출 완료: 15,000,000원` |
| WARN | 주의가 필요한 상황 | `이월공제 잔액이 최저한세 초과분보다 큼` |
| ERROR | 계산 오류 발생 | `REF_TAX_RATE 참조 실패: 해당 연도 세율 미등록` |
| DEBUG | 상세 디버깅 정보 | `투자 공제 중간값: base=100,000,000, rate=10%` |

### 활용

- **감사 대응**: 세무당국의 경정청구 심사 시, 각 산출 금액의 계산 근거를 단계별로 소명
- **계산 재현**: input_data + legal_basis를 기반으로 동일한 계산 결과 재현 가능
- **오류 추적**: log_level = ERROR인 로그를 조회하여 실패 원인 빠르게 파악
- **성능 분석**: duration_ms를 집계하여 병목 단계 식별 및 최적화
- **end-to-end 추적**: trace_id로 하나의 요청에 대한 전체 계산 흐름을 시간순 조회
- **데이터 정합성**: prev_data_hash로 단계 간 데이터 변조/불일치 감지
- **트랜잭션 경계 확인**: v3.1에서 추가된 파이프라인 단계별 트랜잭션 범위와 연계하여 롤백 범위 파악

### 조회 예시

```sql
-- 특정 요청의 전체 계산 흐름 조회 (시간순)
SELECT calc_step, function_name, legal_basis, duration_ms, log_level
FROM LOG_CALCULATION
WHERE req_id = '1234567890_20260216_001'
ORDER BY executed_at;

-- 오류 발생 로그만 조회
SELECT req_id, calc_step, input_data, output_data
FROM LOG_CALCULATION
WHERE log_level = 'ERROR'
AND executed_at >= '2026-02-16';

-- 단계별 평균 처리 시간 분석
SELECT calc_step, AVG(duration_ms) as avg_ms, MAX(duration_ms) as max_ms
FROM LOG_CALCULATION
GROUP BY calc_step
ORDER BY avg_ms DESC;
```

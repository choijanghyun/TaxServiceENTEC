-- ============================================================
-- TaxServiceENTEC DDL Script
-- 세액공제 환급 계산 시스템 테이블 정의
-- ============================================================

-- ============================================================
-- 1. 요청 (Request) 테이블
-- ============================================================

CREATE TABLE IF NOT EXISTS REQ_REQUEST (
    req_id              VARCHAR(30)     NOT NULL,
    applicant_type      CHAR(1)         NOT NULL,
    applicant_id        VARCHAR(15)     NOT NULL,
    applicant_name      VARCHAR(100)    NOT NULL,
    tax_type            VARCHAR(4)      NOT NULL,
    tax_year            VARCHAR(4)      NOT NULL,
    request_date        DATE            NOT NULL,
    seq_no              INT,
    request_status      VARCHAR(20)     NOT NULL,
    prompt_version      VARCHAR(20),
    design_version      VARCHAR(20),
    created_at          TIMESTAMP       NOT NULL,
    completed_at        TIMESTAMP,
    request_source      VARCHAR(30),
    requested_by        VARCHAR(50),
    client_ip           VARCHAR(45),
    error_message       TEXT,
    modified_by         VARCHAR(50),
    modified_at         TIMESTAMP,
    version             INT             NOT NULL DEFAULT 1,
    PRIMARY KEY (req_id)
);

-- ============================================================
-- 2. 입력 (Input) 테이블
-- ============================================================

CREATE TABLE IF NOT EXISTS INP_RAW_DATA (
    raw_id              BIGINT          NOT NULL AUTO_INCREMENT,
    req_id              VARCHAR(30)     NOT NULL,
    category            VARCHAR(30)     NOT NULL,
    sub_category        VARCHAR(30),
    raw_json            LONGTEXT        NOT NULL,
    json_schema_version VARCHAR(20),
    record_count        INT,
    byte_size           BIGINT,
    checksum            VARCHAR(64),
    received_at         TIMESTAMP,
    PRIMARY KEY (raw_id),
    CONSTRAINT fk_inp_raw_data_req FOREIGN KEY (req_id) REFERENCES REQ_REQUEST (req_id)
);

CREATE TABLE IF NOT EXISTS INP_BASIC (
    req_id              VARCHAR(30)     NOT NULL,
    request_date        DATE,
    tax_type            VARCHAR(4),
    applicant_name      VARCHAR(100),
    biz_reg_no          VARCHAR(15),
    corp_size           VARCHAR(20),
    industry_code       VARCHAR(10),
    hq_location         VARCHAR(200),
    capital_zone        VARCHAR(20),
    depopulation_area   BOOLEAN,
    tax_year            VARCHAR(4),
    fiscal_start        DATE,
    fiscal_end          DATE,
    revenue             BIGINT,
    taxable_income      BIGINT,
    computed_tax        BIGINT,
    paid_tax            BIGINT,
    founding_date       DATE,
    venture_yn          BOOLEAN,
    rd_dept_yn          BOOLEAN,
    claim_reason        VARCHAR(500),
    sincerity_target    BOOLEAN,
    bookkeeping_type    VARCHAR(20),
    consolidated_tax    BOOLEAN,
    summary_generated_at TIMESTAMP,
    PRIMARY KEY (req_id)
);

CREATE TABLE IF NOT EXISTS INP_EMPLOYEE (
    req_id              VARCHAR(30)     NOT NULL,
    year_type           VARCHAR(10)     NOT NULL,
    total_regular       DECIMAL(10,2),
    youth_count         INT,
    disabled_count      INT,
    aged_count          INT,
    career_break_count  INT,
    north_defector_count INT,
    general_count       INT,
    excluded_count      INT,
    total_salary        BIGINT,
    social_insurance_paid BIGINT,
    PRIMARY KEY (req_id, year_type)
);

CREATE TABLE IF NOT EXISTS INP_DEDUCTION (
    req_id              VARCHAR(30)     NOT NULL,
    item_category       VARCHAR(30)     NOT NULL,
    provision           VARCHAR(30)     NOT NULL,
    tax_year            VARCHAR(4)      NOT NULL,
    item_seq            INT             NOT NULL,
    base_amount         BIGINT,
    zone_type           VARCHAR(20),
    asset_type          VARCHAR(30),
    rd_type             VARCHAR(30),
    method              VARCHAR(30),
    sub_detail          LONGTEXT,
    existing_applied    BOOLEAN,
    existing_amount     BIGINT,
    carryforward_balance BIGINT,
    PRIMARY KEY (req_id, item_category, provision, tax_year, item_seq)
);

CREATE TABLE IF NOT EXISTS INP_FINANCIAL (
    req_id                  VARCHAR(30)     NOT NULL,
    biz_income              BIGINT,
    non_taxable_income      BIGINT,
    loss_carryforward_total BIGINT,
    loss_carryforward_detail LONGTEXT,
    interim_prepaid_tax     BIGINT,
    withholding_tax         BIGINT,
    determined_tax          BIGINT,
    dividend_income_total   BIGINT,
    dividend_exclusion_detail LONGTEXT,
    foreign_tax_total       BIGINT,
    foreign_income_total    BIGINT,
    tax_adjustment_detail   LONGTEXT,
    inc_deduction_total     BIGINT,
    inc_deduction_detail    LONGTEXT,
    inc_comprehensive_income BIGINT,
    current_year_loss       BIGINT,
    prior_year_tax_paid     BIGINT,
    amendment_history       LONGTEXT,
    vehicle_expense_detail  LONGTEXT,
    PRIMARY KEY (req_id)
);

-- ============================================================
-- 3. 검증 (Check) 테이블
-- ============================================================

CREATE TABLE IF NOT EXISTS CHK_ELIGIBILITY (
    req_id                  VARCHAR(30)     NOT NULL,
    tax_type                VARCHAR(10),
    company_size            VARCHAR(20),
    capital_zone            VARCHAR(20),
    filing_deadline         DATE,
    claim_deadline          DATE,
    deadline_eligible       VARCHAR(20),
    sme_eligible            VARCHAR(20),
    sme_grace_end_year      VARCHAR(4),
    small_vs_medium         VARCHAR(20),
    venture_confirmed       BOOLEAN,
    settlement_check_result VARCHAR(50),
    settlement_blocked_items TEXT,
    estimate_check          BOOLEAN,
    sincerity_target        BOOLEAN,
    overall_status          VARCHAR(30),
    diagnosis_detail        TEXT,
    checked_at              TIMESTAMP,
    PRIMARY KEY (req_id)
);

CREATE TABLE IF NOT EXISTS CHK_INSPECTION_LOG (
    req_id              VARCHAR(30)     NOT NULL,
    inspection_code     VARCHAR(30)     NOT NULL,
    inspection_name     VARCHAR(100),
    legal_basis         VARCHAR(200),
    judgment            VARCHAR(30),
    summary             VARCHAR(500),
    related_module      VARCHAR(50),
    calculated_amount   BIGINT,
    sort_order          INT,
    checked_at          TIMESTAMP,
    PRIMARY KEY (req_id, inspection_code)
);

CREATE TABLE IF NOT EXISTS CHK_VALIDATION_LOG (
    req_id              VARCHAR(30)     NOT NULL,
    rule_code           VARCHAR(30)     NOT NULL,
    rule_type           CHAR(1),
    rule_description    VARCHAR(500),
    result              VARCHAR(10),
    detail              TEXT,
    executed_at         TIMESTAMP,
    PRIMARY KEY (req_id, rule_code)
);

-- ============================================================
-- 4. 출력 (Output) 테이블
-- ============================================================

CREATE TABLE IF NOT EXISTS OUT_EMPLOYEE_SUMMARY (
    req_id              VARCHAR(30)     NOT NULL,
    year_type           VARCHAR(20)     NOT NULL,
    total_regular       DECIMAL(15,2),
    youth_count         INT,
    general_count       INT,
    increase_total      INT,
    increase_youth      INT,
    increase_general    INT,
    excluded_count      INT,
    calc_detail         TEXT,
    PRIMARY KEY (req_id, year_type)
);

CREATE TABLE IF NOT EXISTS OUT_CREDIT_DETAIL (
    req_id              VARCHAR(30)     NOT NULL,
    item_id             VARCHAR(30)     NOT NULL,
    item_name           VARCHAR(100),
    provision           VARCHAR(50),
    credit_type         VARCHAR(30),
    item_status         VARCHAR(20),
    gross_amount        BIGINT,
    nongteuk_exempt     BOOLEAN,
    nongteuk_amount     BIGINT,
    net_amount          BIGINT,
    min_tax_subject     BOOLEAN,
    is_carryforward     BOOLEAN,
    carryforward_amount BIGINT,
    sunset_date         VARCHAR(20),
    deduction_rate      VARCHAR(30),
    conditions          TEXT,
    required_documents  TEXT,
    exclusion_items     TEXT,
    notes               TEXT,
    tax_year            VARCHAR(4),
    rd_type             VARCHAR(30),
    method              VARCHAR(50),
    calc_detail         TEXT,
    legal_basis         VARCHAR(200),
    exclusion_reasons   TEXT,
    PRIMARY KEY (req_id, item_id)
);

CREATE TABLE IF NOT EXISTS OUT_COMBINATION (
    req_id              VARCHAR(30)     NOT NULL,
    combo_id            VARCHAR(30)     NOT NULL,
    combo_rank          INT,
    group_type          VARCHAR(30),
    combo_name          VARCHAR(200),
    items_json          TEXT,
    exemption_total     BIGINT,
    credit_total        BIGINT,
    min_tax_adj         BIGINT,
    nongteuk_total      BIGINT,
    net_refund          BIGINT,
    is_valid            BOOLEAN,
    application_order   TEXT,
    carryforward_items  TEXT,
    PRIMARY KEY (req_id, combo_id)
);

CREATE TABLE IF NOT EXISTS OUT_EXCLUSION_VERIFY (
    req_id              VARCHAR(30)     NOT NULL,
    verify_id           VARCHAR(30)     NOT NULL,
    combo_id            VARCHAR(30),
    provision_a         VARCHAR(50),
    provision_b         VARCHAR(50),
    overlap_allowed     VARCHAR(10),
    condition_note      VARCHAR(500),
    violation_detected  BOOLEAN,
    legal_basis         VARCHAR(200),
    PRIMARY KEY (req_id, verify_id)
);

CREATE TABLE IF NOT EXISTS OUT_REFUND (
    req_id                  VARCHAR(30)     NOT NULL,
    existing_computed_tax   BIGINT,
    existing_deductions     BIGINT,
    existing_determined_tax BIGINT,
    existing_paid_tax       BIGINT,
    new_computed_tax        BIGINT,
    new_deductions          BIGINT,
    new_min_tax_adj         BIGINT,
    new_determined_tax      BIGINT,
    nongteuk_total          BIGINT,
    refund_amount           BIGINT,
    refund_interest_start   DATE,
    refund_interest_end     VARCHAR(20),
    refund_interest_rate    DECIMAL(10,6),
    refund_interest_amount  BIGINT,
    interim_refund_amount   BIGINT,
    interim_interest_amount BIGINT,
    local_tax_refund        BIGINT,
    total_expected          BIGINT,
    refund_cap_detail       TEXT,
    optimal_combo_id        VARCHAR(30),
    carryforward_credits    BIGINT,
    carryforward_detail     TEXT,
    penalty_tax_change      BIGINT,
    PRIMARY KEY (req_id)
);

CREATE TABLE IF NOT EXISTS OUT_RISK (
    req_id              VARCHAR(30)     NOT NULL,
    risk_id             VARCHAR(30)     NOT NULL,
    provision           VARCHAR(50),
    risk_type           VARCHAR(30),
    obligation          VARCHAR(200),
    period_start        DATE,
    period_end          VARCHAR(20),
    violation_action    VARCHAR(200),
    potential_clawback  BIGINT,
    interest_surcharge  BIGINT,
    risk_level          VARCHAR(20),
    description         TEXT,
    PRIMARY KEY (req_id, risk_id)
);

CREATE TABLE IF NOT EXISTS OUT_ADDITIONAL_CHECK (
    req_id              VARCHAR(30)     NOT NULL,
    check_id            VARCHAR(30)     NOT NULL,
    description         VARCHAR(500),
    reason              VARCHAR(500),
    related_inspection  VARCHAR(50),
    related_module      VARCHAR(50),
    priority            VARCHAR(10),
    status              VARCHAR(20),
    PRIMARY KEY (req_id, check_id)
);

CREATE TABLE IF NOT EXISTS OUT_REPORT_JSON (
    req_id              VARCHAR(30)     NOT NULL,
    report_version      VARCHAR(20),
    report_status       VARCHAR(20),
    report_json         LONGTEXT,
    section_a_json      TEXT,
    section_b_json      TEXT,
    section_c_json      TEXT,
    section_d_json      TEXT,
    section_e_json      TEXT,
    section_f_json      TEXT,
    section_g_meta      TEXT,
    json_byte_size      INT,
    result_code         VARCHAR(20),
    checksum            VARCHAR(64),
    generated_at        TIMESTAMP,
    PRIMARY KEY (req_id)
);

-- ============================================================
-- 5. 로그 (Log) 테이블
-- ============================================================

CREATE TABLE IF NOT EXISTS LOG_CALCULATION (
    log_id              BIGINT          NOT NULL AUTO_INCREMENT,
    req_id              VARCHAR(30)     NOT NULL,
    calc_step           VARCHAR(50),
    function_name       VARCHAR(100),
    input_data          TEXT,
    output_data         TEXT,
    legal_basis         VARCHAR(200),
    executed_at         TIMESTAMP,
    log_level           VARCHAR(10),
    executed_by         VARCHAR(50),
    duration_ms         INT,
    trace_id            VARCHAR(50),
    prev_data_hash      VARCHAR(64),
    PRIMARY KEY (log_id)
);

-- ============================================================
-- 6. 레퍼런스 (Reference) 테이블 - 법인세
-- ============================================================

CREATE TABLE IF NOT EXISTS REF_TAX_RATE (
    rate_id             INT             NOT NULL,
    year_from           VARCHAR(4),
    year_to             VARCHAR(4),
    bracket_min         BIGINT,
    bracket_max         BIGINT,
    tax_rate            DECIMAL(5,2),
    progressive_deduction BIGINT,
    PRIMARY KEY (rate_id)
);

CREATE TABLE IF NOT EXISTS REF_MIN_TAX_RATE (
    min_tax_id          INT             NOT NULL,
    corp_size           VARCHAR(10),
    bracket_min         BIGINT,
    bracket_max         BIGINT,
    min_rate            DECIMAL(5,2),
    PRIMARY KEY (min_tax_id)
);

CREATE TABLE IF NOT EXISTS REF_EMPLOYMENT_CREDIT (
    credit_id           INT             NOT NULL,
    tax_year            VARCHAR(4),
    corp_size           VARCHAR(10),
    region              VARCHAR(10),
    worker_type         VARCHAR(20),
    credit_per_person   BIGINT,
    PRIMARY KEY (credit_id)
);

CREATE TABLE IF NOT EXISTS REF_INVESTMENT_CREDIT_RATE (
    rate_id             INT             NOT NULL,
    tax_year_from       VARCHAR(4),
    invest_type         VARCHAR(30),
    corp_size           VARCHAR(10),
    basic_rate          DECIMAL(5,2),
    additional_rate     DECIMAL(5,2),
    PRIMARY KEY (rate_id)
);

CREATE TABLE IF NOT EXISTS REF_MUTUAL_EXCLUSION (
    rule_id             INT             NOT NULL,
    provision_a         VARCHAR(20),
    provision_b         VARCHAR(20),
    year_from           VARCHAR(4),
    year_to             VARCHAR(4),
    is_allowed          BOOLEAN,
    condition_note      VARCHAR(500),
    legal_basis         VARCHAR(100),
    PRIMARY KEY (rule_id)
);

CREATE TABLE IF NOT EXISTS REF_CAPITAL_ZONE (
    zone_id             INT             NOT NULL,
    sido                VARCHAR(20),
    sigungu             VARCHAR(50),
    zone_type           VARCHAR(20),
    is_capital          BOOLEAN,
    is_depopulation     BOOLEAN,
    PRIMARY KEY (zone_id)
);

CREATE TABLE IF NOT EXISTS REF_NONGTEUKSE (
    provision           VARCHAR(20)     NOT NULL,
    is_exempt           BOOLEAN,
    tax_rate            DECIMAL(5,2),
    legal_basis         VARCHAR(100),
    PRIMARY KEY (provision)
);

CREATE TABLE IF NOT EXISTS REF_SME_DEDUCTION_RATE (
    rate_id             INT             NOT NULL,
    corp_size_detail    VARCHAR(10),
    industry_class      VARCHAR(50),
    zone_type           VARCHAR(20),
    deduction_rate      DECIMAL(5,2),
    PRIMARY KEY (rate_id)
);

CREATE TABLE IF NOT EXISTS REF_RD_CREDIT_RATE (
    rate_id             INT             NOT NULL,
    rd_type             VARCHAR(20),
    method              VARCHAR(10),
    corp_size           VARCHAR(10),
    credit_rate         DECIMAL(5,2),
    min_tax_exempt      VARCHAR(20),
    PRIMARY KEY (rate_id)
);

CREATE TABLE IF NOT EXISTS REF_REFUND_INTEREST_RATE (
    rate_id             INT             NOT NULL,
    effective_from      DATE,
    effective_to        DATE,
    annual_rate         DECIMAL(7,5),
    legal_basis         VARCHAR(100),
    PRIMARY KEY (rate_id)
);

CREATE TABLE IF NOT EXISTS REF_STARTUP_DEDUCTION_RATE (
    rate_id             INT             NOT NULL,
    founder_type        VARCHAR(20),
    location_type       VARCHAR(30),
    deduction_rate      DECIMAL(5,2),
    year_from           VARCHAR(4),
    year_to             VARCHAR(4),
    legal_basis         VARCHAR(100),
    remark              VARCHAR(200),
    PRIMARY KEY (rate_id)
);

CREATE TABLE IF NOT EXISTS REF_DEPOPULATION_AREA (
    area_id             INT             NOT NULL,
    sido                VARCHAR(20),
    sigungu             VARCHAR(50),
    designation_date    DATE,
    effective_from      DATE,
    is_active           BOOLEAN,
    PRIMARY KEY (area_id)
);

CREATE TABLE IF NOT EXISTS REF_CORP_TAX_RATE_HISTORY (
    rate_id             INT             NOT NULL,
    year_from           VARCHAR(4),
    year_to             VARCHAR(4),
    bracket_min         BIGINT,
    bracket_max         BIGINT,
    tax_rate            DECIMAL(5,2),
    progressive_deduction BIGINT,
    PRIMARY KEY (rate_id)
);

CREATE TABLE IF NOT EXISTS REF_LAW_VERSION (
    version_id          INT             NOT NULL,
    law_name            VARCHAR(100),
    provision           VARCHAR(20),
    year_from           VARCHAR(4),
    year_to             VARCHAR(4),
    version_note        TEXT,
    PRIMARY KEY (version_id)
);

CREATE TABLE IF NOT EXISTS REF_DIVIDEND_EXCLUSION (
    exclusion_id        INT             NOT NULL,
    year_from           VARCHAR(4),
    year_to             VARCHAR(4),
    corp_type           VARCHAR(20),
    share_ratio_min     DECIMAL(5,2),
    share_ratio_max     DECIMAL(5,2),
    exclusion_rate      DECIMAL(5,2),
    remark              VARCHAR(200),
    PRIMARY KEY (exclusion_id)
);

CREATE TABLE IF NOT EXISTS REF_RD_MIN_TAX_EXEMPT (
    rd_type             VARCHAR(20)     NOT NULL,
    corp_size           VARCHAR(10)     NOT NULL,
    exempt_rate         DECIMAL(5,2),
    PRIMARY KEY (rd_type, corp_size)
);

CREATE TABLE IF NOT EXISTS REF_INDUSTRY_ELIGIBILITY (
    ksic_code           VARCHAR(10)     NOT NULL,
    industry_name       VARCHAR(100),
    startup_eligible    BOOLEAN,
    sme_special_eligible BOOLEAN,
    excluded_reason     VARCHAR(200),
    effective_from      DATE,
    effective_to        DATE,
    is_sme_eligible     BOOLEAN,
    PRIMARY KEY (ksic_code)
);

CREATE TABLE IF NOT EXISTS REF_ENTERTAINMENT_LIMIT (
    limit_id            INT             NOT NULL,
    corp_size           VARCHAR(10),
    base_amount         BIGINT,
    revenue_bracket_min BIGINT,
    revenue_bracket_max BIGINT,
    rate                DECIMAL(5,4),
    year_from           VARCHAR(4),
    year_to             VARCHAR(4),
    PRIMARY KEY (limit_id)
);

CREATE TABLE IF NOT EXISTS REF_SYSTEM_PARAM (
    param_key           VARCHAR(50)     NOT NULL,
    param_value         VARCHAR(100),
    param_type          VARCHAR(20),
    description         VARCHAR(200),
    modifiable          BOOLEAN,
    last_updated        DATE,
    PRIMARY KEY (param_key)
);

CREATE TABLE IF NOT EXISTS REF_KSIC_CODE (
    ksic_code           VARCHAR(10)     NOT NULL,
    section             VARCHAR(5),
    division            VARCHAR(5),
    group_code          VARCHAR(5),
    class_code          VARCHAR(5),
    sub_class           VARCHAR(5),
    industry_name       VARCHAR(200),
    revision            VARCHAR(10),
    effective_date      DATE,
    PRIMARY KEY (ksic_code)
);

-- ============================================================
-- 7. 레퍼런스 (Reference) 테이블 - 환율/이자율
-- ============================================================

CREATE TABLE IF NOT EXISTS REF_EXCHANGE_RATE (
    rate_date           DATE            NOT NULL,
    currency            VARCHAR(3)      NOT NULL,
    standard_rate       DECIMAL(10,4),
    buy_rate            DECIMAL(10,4),
    sell_rate           DECIMAL(10,4),
    PRIMARY KEY (rate_date, currency)
);

CREATE TABLE IF NOT EXISTS REF_DEEMED_INTEREST_RATE (
    year                VARCHAR(4)      NOT NULL,
    rate_type           VARCHAR(20)     NOT NULL,
    rate                DECIMAL(5,2),
    legal_basis         VARCHAR(100),
    PRIMARY KEY (year, rate_type)
);

-- ============================================================
-- 8. 레퍼런스 (Reference) 테이블 - 종합소득세
-- ============================================================

CREATE TABLE IF NOT EXISTS REF_INC_TAX_RATE (
    effective_from      VARCHAR(4)      NOT NULL,
    bracket_no          INT             NOT NULL,
    lower_limit         BIGINT,
    upper_limit         BIGINT,
    tax_rate            DECIMAL(5,2),
    progressive_deduction BIGINT,
    PRIMARY KEY (effective_from, bracket_no)
);

CREATE TABLE IF NOT EXISTS REF_INC_MIN_TAX (
    effective_from      VARCHAR(4)      NOT NULL,
    threshold           BIGINT,
    rate_below          DECIMAL(5,2),
    rate_above          DECIMAL(5,2),
    PRIMARY KEY (effective_from)
);

CREATE TABLE IF NOT EXISTS REF_INC_DEDUCTION_LIMIT (
    deduction_type      VARCHAR(50)     NOT NULL,
    income_bracket      VARCHAR(50)     NOT NULL,
    annual_limit        BIGINT,
    PRIMARY KEY (deduction_type, income_bracket)
);

CREATE TABLE IF NOT EXISTS REF_INC_SINCERITY_THRESHOLD (
    industry_group      VARCHAR(50)     NOT NULL,
    effective_from      VARCHAR(4)      NOT NULL,
    revenue_threshold   BIGINT,
    PRIMARY KEY (industry_group, effective_from)
);

-- ============================================================
-- 9. 인덱스
-- ============================================================

CREATE INDEX IF NOT EXISTS idx_req_request_applicant      ON REQ_REQUEST (applicant_id);
CREATE INDEX IF NOT EXISTS idx_req_request_tax_year       ON REQ_REQUEST (tax_year);
CREATE INDEX IF NOT EXISTS idx_req_request_status         ON REQ_REQUEST (request_status);
CREATE INDEX IF NOT EXISTS idx_inp_raw_data_req           ON INP_RAW_DATA (req_id);
CREATE INDEX IF NOT EXISTS idx_log_calculation_req        ON LOG_CALCULATION (req_id);
CREATE INDEX IF NOT EXISTS idx_log_calculation_trace      ON LOG_CALCULATION (trace_id);
CREATE INDEX IF NOT EXISTS idx_out_credit_detail_provision ON OUT_CREDIT_DETAIL (provision);
CREATE INDEX IF NOT EXISTS idx_out_combination_rank       ON OUT_COMBINATION (req_id, combo_rank);
CREATE INDEX IF NOT EXISTS idx_out_risk_level             ON OUT_RISK (risk_level);
CREATE INDEX IF NOT EXISTS idx_chk_validation_result      ON CHK_VALIDATION_LOG (result);

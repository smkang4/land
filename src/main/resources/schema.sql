-- Add file attachment columns to contract_d table
ALTER TABLE contract_d
ADD COLUMN real_estate_files VARCHAR(1000) COMMENT '부동산정보 파일 ID 목록 (쉼표로 구분)',
ADD COLUMN credit_files VARCHAR(1000) COMMENT '채권순위 파일 ID 목록 (쉼표로 구분)';

-- Add contract period column to contract_d table
ALTER TABLE contract_d
ADD COLUMN contract_period INT COMMENT '계약기간 (개월)';
 
-- Update ContractMapper.xml to include new columns
UPDATE contract_d SET real_estate_files = NULL, credit_files = NULL, contract_period = 12;

-- Add missing columns to draft_contract_detail table
ALTER TABLE draft_contract_detail
ADD COLUMN payment_date VARCHAR(10) COMMENT '지급일',
ADD COLUMN lessor VARCHAR(100) COMMENT '임대인 사업자번호/주민번호',
ADD COLUMN lessor_name VARCHAR(200) COMMENT '임대인/임대사업자명',
ADD COLUMN lessor_account VARCHAR(50) COMMENT '임대인 계좌번호',
ADD COLUMN lessor_bank VARCHAR(100) COMMENT '임대인 은행',
ADD COLUMN cust_code VARCHAR(20) COMMENT '오라클 고객 코드';

-- Add attachment list column to draft table
ALTER TABLE draft
ADD COLUMN attachment_list TEXT COMMENT '첨부파일리스트 (줄바꿈으로 구분)';

-- Remove unused file columns from draft table (since file upload functionality is not used)
ALTER TABLE draft
DROP COLUMN contract_file_names,
DROP COLUMN interior_photo_names,
DROP COLUMN transfer_receipt_names,
DROP COLUMN brokerage_receipt_names;

-- Add appr_no column to draft_contract_detail table if not exists
ALTER TABLE draft_contract_detail
ADD COLUMN IF NOT EXISTS appr_no INT COMMENT '해당 계약의 appr_no';

-- Add post_code column to draft_contract_detail table
ALTER TABLE draft_contract_detail
ADD COLUMN IF NOT EXISTS post_code VARCHAR(10) COMMENT '우편번호';

-- Add rsrc_code column to draft_contract_detail table
ALTER TABLE draft_contract_detail
ADD COLUMN IF NOT EXISTS rsrc_code VARCHAR(20) COMMENT '자원 코드 (원룸/오피스텔/아파트 구분)'; 
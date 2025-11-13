-- draft 테이블에 거래처 등록 시도 여부 컬럼 추가
ALTER TABLE draft ADD COLUMN cust_reg_attempted VARCHAR(1) DEFAULT 'N' COMMENT '거래처 등록 시도 여부 (Y/N)';

-- 기존 데이터는 모두 'N'으로 설정
UPDATE draft SET cust_reg_attempted = 'N' WHERE cust_reg_attempted IS NULL;




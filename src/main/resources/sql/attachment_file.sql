-- 첨부파일 전용 테이블 (암호화 저장용)
-- contract_d.real_estate_files, credit_files에는 이 테이블의 id를 세미콜론으로 나열하여 저장

CREATE TABLE IF NOT EXISTS attachment_file (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    original_filename VARCHAR(500) NOT NULL COMMENT '원본 파일명 (다운로드 시 노출)',
    stored_filename VARCHAR(255) NOT NULL COMMENT '디스크 저장명 (암호화/UUID)',
    stored_path VARCHAR(500) NOT NULL COMMENT '저장 경로 (uploadDir 기준 상대경로)',
    section VARCHAR(50) NOT NULL COMMENT '구분: 부동산정보, 채권순위',
    file_size BIGINT DEFAULT 0 COMMENT '파일 크기(bytes)',
    content_type VARCHAR(100) DEFAULT NULL COMMENT 'MIME 타입',
    encrypted TINYINT(1) DEFAULT 0 COMMENT '1=암호화 저장됨, 0=평문(레거시/키 미설정 시)',
    contract_seq INT DEFAULT NULL COMMENT '연결된 contract_d.seq (저장 시점에 설정)',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_contract_section (contract_seq, section),
    INDEX idx_stored_path (stored_path(255))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='사전조사서 첨부파일 (암호화 저장)';

-- 기존 테이블이 있을 때 암호화 여부 컬럼만 추가 (한 번만 실행)
-- ALTER TABLE attachment_file ADD COLUMN encrypted TINYINT(1) DEFAULT 0 COMMENT '1=암호화 저장됨, 0=평문' AFTER content_type;

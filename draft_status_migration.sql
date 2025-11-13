-- 기안서 상태 관리 컬럼 추가
ALTER TABLE draft 
ADD COLUMN draft_status VARCHAR(20) DEFAULT 'DRAFT' COMMENT '기안서 상태: DRAFT(작성중), UPLOADING(업로드중), COMPLETED(완료), FAILED(실패)',
ADD COLUMN upload_date DATETIME NULL COMMENT '업로드 완료 일시',
ADD COLUMN upload_status VARCHAR(20) DEFAULT 'PENDING' COMMENT '업로드 상태: PENDING(대기), SUCCESS(성공), FAILED(실패)',
ADD COLUMN error_message TEXT NULL COMMENT '오류 메시지',
ADD COLUMN retry_count INT DEFAULT 0 COMMENT '재시도 횟수';

-- 인덱스 추가 (성능 최적화)
CREATE INDEX idx_draft_status ON draft(draft_status);
CREATE INDEX idx_draft_upload_status ON draft(upload_status);

-- 기존 데이터 업데이트 (이미 완료된 기안서가 있다면)
UPDATE draft SET draft_status = 'COMPLETED', upload_status = 'SUCCESS' 
WHERE mst_seq IS NOT NULL AND mst_seq != '';





















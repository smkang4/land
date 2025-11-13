-- 기안서 상태 관리 컬럼 추가 (3단계)
ALTER TABLE draft 
ADD COLUMN status VARCHAR(1) DEFAULT '1' COMMENT '기안 상태: 1(작성), 2(업로드), 3(완료)',
ADD COLUMN status_date DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '상태 변경 일시';

-- 인덱스 추가 (성능 최적화)
CREATE INDEX idx_draft_status ON draft(status);
CREATE INDEX idx_draft_status_date ON draft(status_date);

-- 기존 데이터 업데이트
-- mst_seq가 있으면 완료 상태로 설정
UPDATE draft 
SET status = '3', status_date = NOW()
WHERE mst_seq IS NOT NULL AND mst_seq != '';

-- 나머지는 기본값 '1' (작성 상태)로 유지
UPDATE draft 
SET status = '1', status_date = created_date
WHERE status IS NULL;

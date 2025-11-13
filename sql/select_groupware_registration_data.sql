-- 그룹웨어 등록 데이터 조회 (TCC_E_CONF_STATUS 테이블)
-- SPCC_E_CONF_STATUS_I 프로시저가 등록한 데이터를 조회

-- 1. 전체 데이터 조회 (최근 등록순)
SELECT 
    A.MST_SEQ,              -- 마스터 시퀀스
    A.SUB_SEQ,              -- 서브 시퀀스
    A.CRT_USER_NO,          -- 작성자 번호
    A.MAKE_PROJ,            -- 프로젝트 코드
    A.MAKE_DT,              -- 작성일
    A.MAKE_SEQ,             -- 작성 시퀀스
    A.MAKE_DOC_NO,          -- 문서번호
    A.REQUEST_ID,           -- 요청자 ID
    A.REQUEST_EMP_NO,       -- 요청자 사원번호
    A.E_DOC_CODE,           -- 전자문서 코드
    A.E_DOC_NAME,           -- 전자문서명
    A.REQUEST_NAME,         -- 요청명
    A.EXPORT_TAG,           -- 내보내기 태그
    A.REPORT_FILE_NAME,     -- 리포트 파일명
    A.PARAMETERS,           -- 파라미터
    A.E_DOC_URL,            -- 전자문서 URL
    A.E_DOC_FILE_NAME,      -- 전자문서 파일명
    A.E_CONF_STATUS,        -- 전자결재 상태 (00: 미전송, 10: 결재중, 20: 보류, 30: 완료, 40: 반려, 99: 삭제)
    A.REMARKS,              -- 비고
    A.GJ_MSBH,              -- 견적 문서번호
    A.REF_NM1,              -- 참조명1
    A.REF_NM2,              -- 참조명2
    A.REF_NM3,              -- 참조명3
    A.REF_NM4,              -- 참조명4
    A.REF_NM5,              -- 참조명5
    A.REF_URL1,             -- 참조URL1
    A.REF_URL2,             -- 참조URL2
    A.REF_URL3,             -- 참조URL3
    A.REF_URL4,             -- 참조URL4
    A.REF_URL5,             -- 참조URL5
    A.MAKE_PROJ_NAME,       -- 프로젝트명
    A.REQUEST_ID_NAME,      -- 요청자명
    A.EAID,                 -- EA ID
    A.GJ_SEQ                -- 견적 시퀀스
FROM TCC_E_CONF_STATUS A
ORDER BY A.MAKE_DT DESC, A.MAKE_SEQ DESC;


-- 2. 특정 MST_SEQ로 조회
SELECT 
    A.*
FROM TCC_E_CONF_STATUS A
WHERE A.MST_SEQ = :mstSeq  -- 예: '20250102001'
ORDER BY A.SUB_SEQ;


-- 3. 특정 프로젝트의 데이터 조회
SELECT 
    A.MST_SEQ,
    A.MAKE_DOC_NO,
    A.REQUEST_NAME,
    A.E_CONF_STATUS,
    A.MAKE_DT,
    A.MAKE_PROJ_NAME,
    A.REQUEST_ID_NAME
FROM TCC_E_CONF_STATUS A
WHERE A.MAKE_PROJ = :makeProj  -- 예: '100'
ORDER BY A.MAKE_DT DESC;


-- 4. 결재 상태별 조회
SELECT 
    A.MST_SEQ,
    A.MAKE_DOC_NO,
    A.REQUEST_NAME,
    A.E_CONF_STATUS,
    CASE A.E_CONF_STATUS
        WHEN '00' THEN '미전송'
        WHEN '10' THEN '결재중'
        WHEN '20' THEN '보류'
        WHEN '30' THEN '완료'
        WHEN '40' THEN '반려'
        WHEN '99' THEN '삭제'
        ELSE '알 수 없음'
    END AS STATUS_NAME,
    A.MAKE_DT,
    A.REQUEST_ID_NAME
FROM TCC_E_CONF_STATUS A
WHERE A.E_CONF_STATUS = :status  -- 예: '10' (결재중)
ORDER BY A.MAKE_DT DESC;


-- 5. 최근 등록된 데이터 10건 조회
SELECT 
    A.MST_SEQ,
    A.MAKE_DOC_NO,
    A.REQUEST_NAME,
    A.E_CONF_STATUS,
    A.MAKE_DT,
    A.MAKE_PROJ_NAME,
    A.REQUEST_ID_NAME
FROM TCC_E_CONF_STATUS A
ORDER BY A.MAKE_DT DESC, A.MAKE_SEQ DESC
FETCH FIRST 10 ROWS ONLY;


-- 6. 특정 작성자의 데이터 조회
SELECT 
    A.MST_SEQ,
    A.MAKE_DOC_NO,
    A.REQUEST_NAME,
    A.E_CONF_STATUS,
    A.MAKE_DT,
    A.REQUEST_ID_NAME
FROM TCC_E_CONF_STATUS A
WHERE A.REQUEST_EMP_NO = :empNo  -- 예: '1234'
ORDER BY A.MAKE_DT DESC;


-- 7. 오늘 등록된 데이터 조회
SELECT 
    A.MST_SEQ,
    A.MAKE_DOC_NO,
    A.REQUEST_NAME,
    A.E_CONF_STATUS,
    A.MAKE_DT,
    A.REQUEST_ID_NAME
FROM TCC_E_CONF_STATUS A
WHERE A.MAKE_DT = TRUNC(SYSDATE)
ORDER BY A.MAKE_SEQ DESC;


-- 8. 프로젝트별 통계 (건수, 상태별 집계)
SELECT 
    A.MAKE_PROJ,
    A.MAKE_PROJ_NAME,
    COUNT(*) AS TOTAL_COUNT,
    SUM(CASE WHEN A.E_CONF_STATUS = '00' THEN 1 ELSE 0 END) AS 미전송,
    SUM(CASE WHEN A.E_CONF_STATUS = '10' THEN 1 ELSE 0 END) AS 결재중,
    SUM(CASE WHEN A.E_CONF_STATUS = '20' THEN 1 ELSE 0 END) AS 보류,
    SUM(CASE WHEN A.E_CONF_STATUS = '30' THEN 1 ELSE 0 END) AS 완료,
    SUM(CASE WHEN A.E_CONF_STATUS = '40' THEN 1 ELSE 0 END) AS 반려,
    SUM(CASE WHEN A.E_CONF_STATUS = '99' THEN 1 ELSE 0 END) AS 삭제
FROM TCC_E_CONF_STATUS A
GROUP BY A.MAKE_PROJ, A.MAKE_PROJ_NAME
ORDER BY A.MAKE_PROJ;



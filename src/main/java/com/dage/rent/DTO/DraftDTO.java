package com.dage.rent.DTO;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Data
public class DraftDTO {
    
    // 기본 정보 (ApprovalDTO와 동일한 필드명 사용)
    private Integer id;          // 기안서 ID (PK)
    private int appr_no;
    private List<Integer> appr_nos; // 복수 appr_no (기안서 작성용)
    private int emp_no;
    private String user_nm;
    private String proj_code;
    private String proj_name;
    private String gw_code;      // 그룹웨어 프로젝트 코드
    private String mst_seq;      // 마스터 시퀀스
    
    // 계약내용 테이블 관련 필드들 (ContractDTO와 동일한 필드명 사용)
    private List<ContractDetailDTO> contractDetails;
    
    // 계약 정보
    private String rent_reason;  // 임대차 계약 사유
    private String rent_source;  // 계약상 특이사항
    
    // 실행예산 정보
    private String execution_budget; // 실행예산
    
    // 기안서 상태 관리 (3단계)
    private String status; // 기안 상태: 1(작성), 2(업로드), 3(완료)
    private String status_date; // 상태 변경 일시
    private String error_message; // 오류 메시지
    
    // ERP 등록 상태 (기존 시스템과 호환)
    private String erp_reg; // ERP 등록 여부: Y(등록완료), N(미등록), NULL(미등록)
    
    // 거래처 등록 시도 여부
    private String cust_reg_attempted; // 거래처 등록 시도 여부: Y(시도함), N(시도안함)
    
    // 상태 표시용 메서드 (JSON 직렬화에서 제외)
    @JsonIgnore
    public String getStatusDisplayName() {
        if (status == null) {
            return "작성";
        }
        switch (status) {
            case "1": return "작성";
            case "2": return "업로드";
            case "3": return "완료";
            default: return "작성";
        }
    }
    
    @JsonIgnore
    public String getStatusBadgeClass() {
        if (status == null) {
            return "badge bg-secondary";
        }
        switch (status) {
            case "1": return "badge bg-primary";
            case "2": return "badge bg-warning";
            case "3": return "badge bg-success";
            default: return "badge bg-secondary";
        }
    }
    
    // 내부 클래스: 계약내용 상세 정보
    @Data
    public static class ContractDetailDTO {
        private Integer id;          // 계약내용 상세 ID (PK)
        private Integer draftId;     // 기안서 ID (FK)
        private int appr_no;         // 해당 계약의 appr_no
        private int rowNo;           // 행 번호
        private String type;      // 용도 (숙소/사무실)
        private String address;      // 부동산 소재지
        private String addressD;     // 상세주소
        private String postCode;     // 우편번호
        private String rsrcCode;     // 자원 코드 (원룸/오피스텔/아파트 구분)
        private String area;         // 면적(㎡)
        private String contDate_s;     // 계약 시작일
        private String contDate_e;   // 계약 종료일
        private String chk_3;         // 전세권설정 (Y/N)
        private String accu;         // 사용인원
        private String depositAmt;   // 보증금
        private String rentAmt;      // 월세
        private String contAmt;      // 중개수수료
        private String paymentDate;  // 지급일
        private String lessor;
        private String lessorName; // 임대사업자
        private String lessorAccount; // 임대인 계좌번호
        private String lessorBank;   // 임대인 은행
        private String custCode;     // 거래처 코드
        private String bigo;       // 비고
    }
} 
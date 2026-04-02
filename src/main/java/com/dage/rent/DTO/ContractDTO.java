package com.dage.rent.DTO;

import lombok.Data;

import java.util.List;

@Data
public class ContractDTO {
    private String seq;
    private String emp_no;
    private String user_nm;
    private String proj_code;
    private String proj_name;
    private String appr_no;
    private String cont_date;
    private String cont_period; // DB와 통일
    private String move_date;
    private String cont_amt;
    private String deposit_amt;
    private String rent_amt;
    private String address;
    private String address_d;
    private String res_type;
    private String trans_type;
    private String area;
    private String crtdate;
    private String accu;
    private String source;
    private String chk_1;
    private String chk_2;
    private String chk_3;
    private String chk_4;
    private String chk_5;
    private String chk_reason_1;
    private String chk_reason_2;
    private String chk_reason_3;
    private String chk_reason_4;
    private String chk_reason_5;
    private String appr_stat;
    private String reject;
    private String real_estate_files;
    private String credit_files;
    /** id로 조회한 원본 파일명 목록 (화면 표시용, attachment_file 기반) */
    private List<AttachmentFileDTO> realEstateFileList;
    private List<AttachmentFileDTO> creditFileList;
    private String accu_type; // DB와 통일
    private String rewrite;
    private String post_code;
    
    // 계약 상세 정보 필드 추가
    private String lessor;        // 임대사업자
    private String lessorName;     // 임대사업자명
    private String lessorAccount;  // 임대인 계좌번호
    private String lessorBank;     // 임대인 은행
    private String existingCustCode; // 기존 거래처 코드
    private String custCode;      // 오라클 고객 코드
    private String paymentDate;   // 지급일
    private String rsrcCode;      // 자원 코드 (원룸/오피스텔/아파트 구분)

    private String d_seq;
    private String appr_num;
    private String appr_date;
    private String appr_emp_no;
    private String appr_emp_nm;
    private String appr_position;
    private String first_appr_emp_nm;
    private String first_appr_date;
    private String next_appr_emp_nm;
    private String last_appr_emp_nm;
    private String last_appr_date;
    private String temp_flag;

}

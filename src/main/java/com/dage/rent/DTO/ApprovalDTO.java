package com.dage.rent.DTO;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class ApprovalDTO {

    private int appr_no;
    private int appr_emp_no;
    private LocalDate appr_date;
    private String appr_stat;
    private String appr_emp_nm;
    private String reject;

    private int d_seq;
    private int appr_num;
    private String appr_group;
    private String appr_remarks;
    private String appr_position;
    private int appr_position_seq;
    private String last_tag;
    private String appr_tg;

    // 추가
    private int emp_no; // 작성자
    private String user_nm;// 작성자
    private String proj_code; //사용부서
    private String proj_name; //사용부서
    private String address; //물건 주소

    private int first_appr_emp_no;	//최초결재자
    private String first_appr_emp_nm;	//최초결재자
    private String first_appr_date;	//최초결재일
    private String next_appr_emp_nm; // 다음결재자
    private String last_appr_emp_nm; //최종결재자
    private String last_appr_date;//최종결재자

    private int next_emp_no;
    private String next_send_type;
    private String context;
    private String cust_code;

    
    // 기안서 작성 여부
    private String draft_exists;
    
    // mst_seq 필드 추가
    private String mst_seq;
    
    // appr_no 리스트 (콤마로 구분된 문자열, 예: "33,37")
    private String appr_no_list;
    
    // SFCC_E_CONF_STATUS 함수 결과값
    private String conf_status;
    private String conf_status_name;
    
    // draft 테이블의 ID (기안서 업로드용)
    private Integer draft_id;
    
    // 거래처 등록 시도 여부
    private String cust_reg_attempted;
    
    // 계약 건수 (ERP 등록용)
    private Integer contract_count;
} 
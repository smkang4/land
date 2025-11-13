package com.dage.rent.DTO;

import lombok.Data;

@Data
public class ApprovalDDTO {
    private int d_seq;
    private int appr_no;
    private int appr_num;
    private String appr_group;
    private int appr_emp_no;
    private String appr_emp_nm;
    private String appr_position;
    private String appr_position_seq;
    private String appr_remarks;
    private String appr_date;
    private String last_tag;
    private String appr_tg;
    private String appr_tg_nm;
    private String next_emp_no;
    private String next_send_type;
    private String context;
}

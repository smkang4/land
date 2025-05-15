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
} 
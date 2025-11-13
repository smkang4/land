package com.dage.rent.DTO;

import lombok.Data;

import java.util.Date;

@Data
public class ApprovalMDTO {

    private int appr_no;
    private int appr_emp_no;
    private String appr_emp_nm;
    private Date appr_date;
    private String appr_stat;
    private String reject;
    private String appr_admin;



}

package com.dage.rent.DTO;

import lombok.Data;

@Data
public class ApprovalCcDTO {
    private Long ccId;
    private int apprNo;
    private int empNo;
    private String empNm;
    private String positionNm;
    private Integer addedBy;
    private String addedAt;
}

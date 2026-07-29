package com.dage.rent.DTO;

import lombok.Data;

@Data
public class ApprovalTemplateStepDTO {
    private int id;
    private String templateCode;
    private int stepOrder;
    private int empNo;
    private String empNm;
    private String positionNm;
    private String isFinal;
    private String useYn;
}

package com.dage.rent.DTO;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ContractMDTO {
    private int seq;
    private int empNo;
    private String userNm;
    private int projCode;
    private String projName;
    private LocalDateTime crtdate;
    private String temp_flag;

} 
package com.dage.rent.DTO;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class ContractDDTO {
    private int seq;
    private LocalDate contDate;
    private LocalDate moveDate;
    private int contAmt;
    private int depositAmt;
    private int rentAmt;
    private String address;
    private String addressD;
    private String resType;
    private String transType;
    private int area;
    private LocalDateTime crtdate;
    private int accu;
    private String source;
    private String chkReason1;
    private String chkReason2;
    private String chkReason3;
    private String chkReason4;
    private String chkReason5;
    private String chk1;
    private String chk2;
    private String chk3;
    private String chk4;
    private String chk5;
} 
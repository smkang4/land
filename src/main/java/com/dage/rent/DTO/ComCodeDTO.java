package com.dage.rent.DTO;

import lombok.Data;

@Data
public class ComCodeDTO {
    private String id;
    private String text;
    private String custName;      // 거래처명
    private String bizNoMasked;   // 마스킹된 사업자등록번호
    private String bizNo;         // 사업자등록번호 (원본)
}

package com.dage.rent.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalLineSubmitDTO {
    private int empNo;
    /** 동일 emp_no 중복 계정 구분용 */
    private String empNm;
    /** FIELD | ADMIN */
    private String phase;
    private String isFinal;

    public ApprovalLineSubmitDTO(int empNo, String phase, String isFinal) {
        this(empNo, null, phase, isFinal);
    }
}

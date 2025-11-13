package com.dage.rent.DTO;

import lombok.Data;

@Data
public class EmpUserDTO {
    int emp_no;
    int user_no;
    String name;
    String email;
    String position_name;
    String dept_name;
    String proj_name;

    String email_chk;
}

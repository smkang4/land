package com.dage.rent.DAO.oracle;

import com.dage.rent.DTO.LoginDTO;
import com.dage.rent.DTO.ProjDTO;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Mapper
@Repository("oracleRentDAO")
public interface RentDAO {
    LoginDTO selectLogin(String userId);
    List<ProjDTO> getAllProjects();
}



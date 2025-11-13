package com.dage.rent.DAO.mysql;

import com.dage.rent.DTO.EmpUserDTO;
import com.dage.rent.DTO.LoginDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Mapper
@Repository
public interface AdminDAO {
    List<EmpUserDTO> getAllAdmins();
    List<LoginDTO> getUsers();
    boolean isAdmin(@Param("empNo") int empNo);
    void insertAdmin(EmpUserDTO adminData);
    void deleteAdmins(@Param("empNos") List<Integer> empNos);
    List<LoginDTO> searchAdmins(@Param("type") String type, @Param("keyword") String keyword);
    void updateMailReceive(String empNo, String mailReceive);
    List<Map<String, Object>> getDraftDetailsByMstSeq(@Param("mstSeq") String mstSeq);
    void updateDraftErpReg(@Param("mstSeq") String mstSeq);
    
    // 거래처 등록 후 cust_code 조회 및 업데이트
    String getCustCodeByBizNo(@Param("bizNo") String bizNo);
    void updateDraftContractDetailCustCode(@Param("detailId") int detailId, @Param("custCode") String custCode);
    
    // ERP 등록 관련
    List<Map<String, Object>> getErpRegistrationTargets();
    void updateErpRegAttempted(@Param("mstSeq") String mstSeq, @Param("attempted") String attempted);
} 
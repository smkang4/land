package com.dage.rent.DAO.oracle;

import com.dage.rent.DTO.*;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Mapper
public interface RentDAO {
    LoginDTO selectLogin(String userId);
    List<LoginDTO> getAdminList();
    List<ComCodeDTO> getAllProjects();
    List<ComCodeDTO> getSelectProjects(String user_no);
    List<ComCodeDTO> getUserList();
    List<ComCodeDTO> getProjUserList(String headCode);
    LoginDTO getUserinfo(int emp_no);
    EmpUserDTO getEmpUserInfo(int emp_no);

    String getGwPjcode(@Param("proj_code") String proj_code);
    String getMstSeq();
    String getCustCode();
    CustDTO getCustCodeForapprT(@Param("cust_code") String cust_code);

    void  callERPProcedure(Map<String, Object> erpData);

    Map<String, Object> getERPMakeSeqAndDocNo(@Param("makeProj") String makeProj, @Param("makeDt") String makeDt);

    Map<String, Object> getEDocCodeAndName(@Param("eaId") String eaId);

    List<Map<String, Object>> searchErpCustomers(@Param("custCode") String custCode);
    
    List<ComCodeDTO> getBankList();
    
    void callCustProjProcedure(Map<String, Object> custProjData);
    
    int checkDuplicateBizNo(String bizNo);
    
    List<ComCodeDTO> getExistingCustomers(String bizNo);
    
    List<ComCodeDTO> getExistingCustomersDebug(String bizNo);
    
    String getEmployeeName(String emp_no);
    
    // 임대차 계약 등록 관련 메서드들
    String getLeaseSeq();
    String getLeaseContNo();
    Integer getLeaseContSeq();
    void callLeaseProcedure(LeaseProcedureDTO leaseData);
    void callLeaseContProcedure(LeaseProcedureDTO leaseData);
    void insertLease(LeaseProcedureDTO leaseData);
    void insertLeaseCont(Map<String, Object> leaseContData);
    void insertLeaseTranDeposit(Map<String, Object> tranData);
    void insertLeaseIntr(Map<String, Object> intrData);
    
    // SFCC_E_CONF_STATUS 함수 호출
    String getConfStatus(@Param("mstSeq") String mstSeq);
    
    // 사업자등록번호로 거래처 코드 조회
    String getCustCodeByBizNo(@Param("bizNo") String bizNo);
    
    // ERP 등록 테스트 데이터 삭제 (SEQ 기준)
    void deleteLeaseIntr(@Param("seq") Integer seq);
    void deleteLeaseTran(@Param("seq") Integer seq);
    void deleteLeaseCont(@Param("seq") Integer seq);
    void deleteLease(@Param("seq") Integer seq);
}

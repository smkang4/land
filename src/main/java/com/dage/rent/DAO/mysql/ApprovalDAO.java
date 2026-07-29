package com.dage.rent.DAO.mysql;

import com.dage.rent.DTO.ApprovalCcDTO;
import com.dage.rent.DTO.ApprovalDDTO;
import com.dage.rent.DTO.ApprovalDTO;
import com.dage.rent.DTO.ApprovalMDTO;
import com.dage.rent.DTO.ApprovalTemplateStepDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;

@Mapper
@Repository("mysqlApprovaltDAO")
public interface ApprovalDAO {

    List<ApprovalDTO> getApprovalList(HashMap<String,Object> map);
    List<ApprovalDTO> getApprovalListForAdmin(HashMap<String, Object> map);
    List<ApprovalDTO> getApprovalListForDraft(HashMap<String, Object> map);
    List<ApprovalDTO> getIncompleteDraftList(HashMap<String, Object> map);
    List<ApprovalDTO> getCompletedDraftList(HashMap<String, Object> map);
    List<ApprovalDTO> getCompletedDraftListForAdmin(HashMap<String, Object> map);
    
    // 거래처 등록이 필요한 기안서 목록 조회
    List<ApprovalDTO> getPendingCustomerRegistrations(HashMap<String, Object> map);
    
    // 거래처 등록 시도 플래그 업데이트
    int updateCustomerRegistrationAttempted(@Param("draftId") Integer draftId, @Param("attempted") String attempted);

    List<ApprovalMDTO> getApprovalMaster(@Param("appr_no") int appr_no);
    List<ApprovalDDTO> getApprovalDetail(@Param("appr_no") int appr_no);

    //single
    ApprovalDTO getApprovalDetailOne(@Param("d_seq") int d_seq);

    //결재마스터
    int insertApprovalMaster(ApprovalMDTO mdto);
    void updateApprovalMaster(ApprovalMDTO mdto);

    //결재디테일
    void insertApprovalDetail(ApprovalDDTO ddto);
    void updateApprovalDetail(ApprovalDDTO ddto);

    int updateApprovalRemarksOnly(@Param("appr_no") int appr_no, @Param("d_seq") int d_seq, @Param("appr_emp_no") int appr_emp_no, @Param("appr_remarks") String appr_remarks);

    // 관리부서 접수 구분 Tag
    int updateAdminTag(@Param("appr_no") int appr_no, @Param("appr_admin") String appr_admin);

    //결재완료 확인
    String getApprovalTag(@Param("appr_no") int appr_no);

    List<ApprovalDTO> getApprovalListForUser(int empNo);

    Integer getMaxApprovalNumber(@Param("appr_no") int appr_no);

    int countLastApprovedStepByEmp(@Param("appr_no") int appr_no, @Param("appr_emp_no") int appr_emp_no);

    /** MAX(appr_num) 행이 승인(T)인지 (되돌릴 승인이 있는지) */
    int countApprovedAtMaxStep(@Param("appr_no") int appr_no);

    /** 결재 단계(appr_num)에 해당하는 결재자 사번 */
    Integer getApprEmpNoAtApprNum(@Param("appr_no") int appr_no, @Param("appr_num") int appr_num);
    int deleteHigherApprovalNumbers(@Param("appr_no") int appr_no, @Param("appr_num") int appr_num);
    int checkHigherApprovalNumbers(@Param("appr_no") int appr_no, @Param("appr_num") int appr_num);
    int deleteBGroupApproval(@Param("appr_no") int appr_no, @Param("appr_emp_no") int appr_emp_no);
    int updateApprovalMasterAdmin(@Param("appr_no") int appr_no);
    int updateApprovalDetailStatus(@Param("appr_no") int appr_no, @Param("appr_num") int appr_num, @Param("appr_tg") String appr_tg, @Param("appr_remarks") String appr_remarks, @Param("last_tag") String last_tag);

    List<ApprovalCcDTO> selectCcByApprNo(@Param("appr_no") int appr_no);

    int insertCc(ApprovalCcDTO cc);

    int countCcByApprAndEmp(@Param("appr_no") int appr_no, @Param("emp_no") int emp_no);

    int countApproverOnLine(@Param("appr_no") int appr_no, @Param("emp_no") int emp_no);

    int countCcOnlyViewer(@Param("appr_no") int appr_no, @Param("emp_no") int emp_no);

    List<ApprovalTemplateStepDTO> selectTemplateSteps(@Param("template_code") String template_code);

    int countDetailByGroup(@Param("appr_no") int appr_no, @Param("appr_group") String appr_group);
}

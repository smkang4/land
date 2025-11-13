package com.dage.rent.DAO.mysql;

import com.dage.rent.DTO.ApprovalDDTO;
import com.dage.rent.DTO.ApprovalDTO;
import com.dage.rent.DTO.ApprovalMDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    // 관리부서 접수 구분 Tag
    int updateAdminTag(@Param("appr_no") int appr_no, @Param("appr_admin") String appr_admin);

    //결재완료 확인
    String getApprovalTag(@Param("appr_no") int appr_no);

    List<ApprovalDTO> getApprovalListForUser(int empNo);

    int getMaxApprovalNumber(Map<String, Object> params);
    int deleteHigherApprovalNumbers(@Param("appr_no") int appr_no, @Param("appr_num") int appr_num);
    int checkHigherApprovalNumbers(@Param("appr_no") int appr_no, @Param("appr_num") int appr_num);
    int deleteBGroupApproval(@Param("appr_no") int appr_no, @Param("appr_emp_no") int appr_emp_no);
    int updateApprovalMasterAdmin(@Param("appr_no") int appr_no);
    int updateApprovalDetailStatus(@Param("appr_no") int appr_no, @Param("appr_num") int appr_num, @Param("appr_tg") String appr_tg, @Param("appr_remarks") String appr_remarks, @Param("last_tag") String last_tag);
}

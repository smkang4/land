package com.dage.rent.Service;

import com.dage.rent.DAO.mysql.ApprovalDAO;
import com.dage.rent.DAO.oracle.RentDAO;
import com.dage.rent.DTO.ApprovalDDTO;
import com.dage.rent.DTO.ApprovalDTO;
import com.dage.rent.DTO.ApprovalMDTO;
import org.apache.ibatis.annotations.Param;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ApprovalService {

    @Autowired
    private ApprovalDAO approvalDAO;
    
    @Autowired
    private RentDAO rentDAO;
    
    @Autowired
    private RentService rentService;

    @Transactional("mysqlTransactionManager")
    public List<ApprovalMDTO> getApprovalMaster(@Param("appr_no") int appr_no) {
        return approvalDAO.getApprovalMaster(appr_no);
    }

    @Transactional("mysqlTransactionManager")
    public List<ApprovalDDTO> getApprovalDetail(@Param("appr_no") int appr_no){
        return approvalDAO.getApprovalDetail(appr_no);
    }

    @Transactional("mysqlTransactionManager")
    public void insertApprovalMaster(ApprovalMDTO mdto){
        approvalDAO.insertApprovalMaster(mdto);
    }

    @Transactional("mysqlTransactionManager")
    public void updateApprovalMaster(ApprovalMDTO mdto){
        approvalDAO.updateApprovalMaster(mdto);
    }

    @Transactional("mysqlTransactionManager")
    public void insertApprovalDetail(ApprovalDDTO ddto){
        approvalDAO.insertApprovalDetail(ddto);
    }

    @Transactional("mysqlTransactionManager")
    public void updateApprovalDetail(ApprovalDDTO ddto){
        approvalDAO.updateApprovalDetail(ddto);
    }

    @Transactional("mysqlTransactionManager")
    public String getApprovalTag(@Param("appr_no") int appr_no){
        return approvalDAO.getApprovalTag(appr_no);
    };

    @Transactional("mysqlTransactionManager")
    public ApprovalDTO getApprovalDetailOne(@Param("d_seq") int d_seq){
        return approvalDAO.getApprovalDetailOne(d_seq);
    }

    @Transactional("mysqlTransactionManager")
    public List<ApprovalDTO> getApprovalList(HashMap<String,Object> map){
        return approvalDAO.getApprovalList(map);
    }

    @Transactional("mysqlTransactionManager")
    public List<ApprovalDTO> getApprovalListForAdmin(HashMap<String,Object> map){
        return approvalDAO.getApprovalListForAdmin(map);
    }

    @Transactional("mysqlTransactionManager")
    public List<ApprovalDTO> getApprovalListForDraft(HashMap<String,Object> map){
        List<ApprovalDTO> result = approvalDAO.getApprovalListForDraft(map);
        return result;
    }

    @Transactional("mysqlTransactionManager")
    public List<ApprovalDTO> getIncompleteDraftList(HashMap<String,Object> map){
        List<ApprovalDTO> result = approvalDAO.getIncompleteDraftList(map);
        
        // 각 직원번호에 대해 직원이름 조회 및 Oracle 상태 조회
        if (result != null && !result.isEmpty()) {
            for (ApprovalDTO dto : result) {
                Integer empNo = dto.getEmp_no();
                if (empNo != null && empNo > 0) {
                    try {
                        String employeeName = rentDAO.getEmployeeName(empNo.toString());
                        dto.setUser_nm(employeeName);
                    } catch (Exception e) {
                        System.out.println("직원이름 조회 실패 - emp_no: " + empNo + ", 오류: " + e.getMessage());
                        dto.setUser_nm(""); // 조회 실패 시 빈 문자열로 설정
                    }
                } else {
                    dto.setUser_nm(""); // emp_no가 null이거나 0인 경우 빈 문자열로 설정
                }
                
                // Oracle에서 상태 조회
                String mstSeq = dto.getMst_seq();
                if (mstSeq != null && !mstSeq.trim().isEmpty()) {
                    try {
                        String confStatus = rentService.getConfStatus(mstSeq);
                        // 공백 제거 (Oracle에서 반환되는 값에 공백이 포함될 수 있음)
                        if (confStatus != null) {
                            confStatus = confStatus.trim();
                        }
                        dto.setConf_status(confStatus);
                        
                        // 상태 코드를 한글로 변환
                        String confStatusName = getConfStatusName(confStatus);
                        dto.setConf_status_name(confStatusName);
                    } catch (Exception e) {
                        System.out.println("Oracle 상태 조회 실패 - mst_seq: " + mstSeq + ", 오류: " + e.getMessage());
                        dto.setConf_status("00");
                        dto.setConf_status_name("미전송");
                    }
                } else {
                    dto.setConf_status("00");
                    dto.setConf_status_name("미전송");
                }
            }
        }
        
        return result;
    }

    @Transactional("mysqlTransactionManager")
    public List<ApprovalDTO> getCompletedDraftList(HashMap<String,Object> map){
        List<ApprovalDTO> result = approvalDAO.getCompletedDraftList(map);
        
        // 각 직원번호에 대해 직원이름 조회 및 Oracle 상태 조회
        if (result != null && !result.isEmpty()) {
            for (ApprovalDTO dto : result) {
                Integer empNo = dto.getEmp_no();
                if (empNo != null && empNo > 0) {
                    try {
                        String employeeName = rentDAO.getEmployeeName(empNo.toString());
                        dto.setUser_nm(employeeName);
                    } catch (Exception e) {
                        System.out.println("직원이름 조회 실패 - emp_no: " + empNo + ", 오류: " + e.getMessage());
                        dto.setUser_nm(""); // 조회 실패 시 빈 문자열로 설정
                    }
                } else {
                    dto.setUser_nm(""); // emp_no가 null이거나 0인 경우 빈 문자열로 설정
                }
                
                // Oracle에서 상태 조회
                String mstSeq = dto.getMst_seq();
                if (mstSeq != null && !mstSeq.trim().isEmpty()) {
                    try {
                        String confStatus = rentService.getConfStatus(mstSeq);
                        // 공백 제거 (Oracle에서 반환되는 값에 공백이 포함될 수 있음)
                        if (confStatus != null) {
                            confStatus = confStatus.trim();
                        }
                        dto.setConf_status(confStatus);
                        
                        // 상태 코드를 한글로 변환
                        String confStatusName = getConfStatusName(confStatus);
                        dto.setConf_status_name(confStatusName);
                    } catch (Exception e) {
                        System.out.println("Oracle 상태 조회 실패 - mst_seq: " + mstSeq + ", 오류: " + e.getMessage());
                        dto.setConf_status("00");
                        dto.setConf_status_name("미전송");
                    }
                } else {
                    dto.setConf_status("00");
                    dto.setConf_status_name("미전송");
                }
            }
        }
        
        return result;
    }
    
    public List<ApprovalDTO> getCompletedDraftListForAdmin(HashMap<String,Object> map){
        List<ApprovalDTO> result = approvalDAO.getCompletedDraftListForAdmin(map);
        
        // 각 직원번호에 대해 직원이름 조회 및 Oracle 상태 조회
        if (result != null && !result.isEmpty()) {
            for (ApprovalDTO dto : result) {
                Integer empNo = dto.getEmp_no();
                if (empNo != null && empNo > 0) {
                    try {
                        String employeeName = rentDAO.getEmployeeName(empNo.toString());
                        dto.setUser_nm(employeeName);
                    } catch (Exception e) {
                        System.out.println("직원이름 조회 실패 - emp_no: " + empNo + ", 오류: " + e.getMessage());
                        dto.setUser_nm(""); // 조회 실패 시 빈 문자열로 설정
                    }
                } else {
                    dto.setUser_nm(""); // emp_no가 null이거나 0인 경우 빈 문자열로 설정
                }
                
                // Oracle에서 상태 조회
                String mstSeq = dto.getMst_seq();
                if (mstSeq != null && !mstSeq.trim().isEmpty()) {
                    try {
                        String confStatus = rentService.getConfStatus(mstSeq);
                        // 공백 제거 (Oracle에서 반환되는 값에 공백이 포함될 수 있음)
                        if (confStatus != null) {
                            confStatus = confStatus.trim();
                        }
                        dto.setConf_status(confStatus);
                        
                        // 상태 코드를 한글로 변환
                        String confStatusName = getConfStatusName(confStatus);
                        dto.setConf_status_name(confStatusName);
                    } catch (Exception e) {
                        System.out.println("Oracle 상태 조회 실패 - mst_seq: " + mstSeq + ", 오류: " + e.getMessage());
                        dto.setConf_status("00");
                        dto.setConf_status_name("미전송");
                    }
                } else {
                    dto.setConf_status("00");
                    dto.setConf_status_name("미전송");
                }
            }
        }
        
        return result;
    }
    
    /**
     * 상태 코드를 한글로 변환
     */
    private String getConfStatusName(String confStatus) {
        if (confStatus == null) return "알수없음";
        
        switch (confStatus) {
            case "00": return "미전송";
            case "40": return "반려";
            case "99": return "삭제";
            case "10": return "결재중";
            case "20": return "보류";
            case "30": return "완료";
            default: return "알수없음";
        }
    }

    @Transactional("mysqlTransactionManager")
    public int updateAdminTag(int appr_no, String appr_admin){
        return approvalDAO.updateAdminTag(appr_no,appr_admin);
    }

    @Transactional("mysqlTransactionManager")
    public List<ApprovalDTO> getApprovalListForUser(int empNo) {
        return approvalDAO.getApprovalListForUser(empNo);
    }

    @Transactional("mysqlTransactionManager")
    public Map<String, Object> cancelApprovalAndCleanup(int appr_no, int appr_emp_no) {
        Map<String, Object> result = new HashMap<>();

        try {
            // 1. 내 appr_num 확인
            Map<String, Object> params = new HashMap<>();
            params.put("appr_no", appr_no);
            params.put("appr_emp_no", appr_emp_no);

            Integer maxApprNum = approvalDAO.getMaxApprovalNumber(params);
            if (maxApprNum == null) {
                result.put("success", false);
                result.put("message", "해당 결재 데이터를 찾을 수 없습니다.");
                return result;
            }

            System.out.println("--결재 취소-- appr_num: "+maxApprNum);

            // 2. 내 appr_num보다 큰 데이터 삭제
            params.put("appr_num", maxApprNum);
            int deletedHigher = approvalDAO.deleteHigherApprovalNumbers(appr_no, maxApprNum);

            System.out.println("--결재 취소-- 이후 데이터 삭제 완료");

            // 3. 내 appr_num보다 큰 데이터가 있는지 확인
            int hasHigherNum = approvalDAO.checkHigherApprovalNumbers(appr_no, maxApprNum);

            // 4. count가 0이라면 B그룹인 경우 삭제
            int deletedBGroup = 0;
            if (hasHigherNum == 0) {
                System.out.println("--결재 취소-- 관리자 접수 삭제");
                deletedBGroup = approvalDAO.deleteBGroupApproval(appr_no, appr_emp_no);

                // 5. DELETE가 0보다 크면 approval_m에 appr_admin 업데이트
                if (deletedBGroup > 0) {
                    approvalDAO.updateApprovalMasterAdmin(appr_no);
                }
            }

            // 6. approval_d의 appr_tg와 last_tag 업데이트
            System.out.println("--결재 취소-- 업데이트 시작");
            int updatedDetail = approvalDAO.updateApprovalDetailStatus(
                appr_no, 
                maxApprNum, 
                "N", 
                "", 
                hasHigherNum == 0 ? "T" : "F"
            );


            result.put("success", true);
            result.put("message", "결재 취소가 완료되었습니다.");

        } catch (Exception e) {
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "결재 취소 중 오류가 발생했습니다: " + e.getMessage());
        }

        return result;
    }

    public void updateApprovalDetailStatus(int appr_no, int appr_num, String appr_tg, String appr_remarks, String last_tag) {
        approvalDAO.updateApprovalDetailStatus(appr_no, appr_num, appr_tg, appr_remarks, last_tag);
    }
    
    /**
     * 거래처 등록이 필요한 기안서 목록 조회
     * @param map 검색 조건
     * @return 거래처 등록이 필요한 기안서 목록
     */
    @Transactional("mysqlTransactionManager")
    public List<ApprovalDTO> getPendingCustomerRegistrations(HashMap<String,Object> map) {
        List<ApprovalDTO> result = approvalDAO.getPendingCustomerRegistrations(map);
        
        // 각 항목에 대해 직원이름과 Oracle 상태 조회
        if (result != null && !result.isEmpty()) {
            for (ApprovalDTO dto : result) {
                // 직원이름 조회
                Integer empNo = dto.getEmp_no();
                if (empNo != null && empNo > 0) {
                    try {
                        String employeeName = rentDAO.getEmployeeName(empNo.toString());
                        dto.setUser_nm(employeeName);
                    } catch (Exception e) {
                        System.out.println("직원이름 조회 실패 - emp_no: " + empNo + ", 오류: " + e.getMessage());
                        dto.setUser_nm("");
                    }
                } else {
                    dto.setUser_nm("");
                }
                
                // Oracle에서 상태 조회
                String mstSeq = dto.getMst_seq();
                if (mstSeq != null && !mstSeq.trim().isEmpty()) {
                    try {
                        String confStatus = rentService.getConfStatus(mstSeq);
                        // 공백 제거 (Oracle에서 반환되는 값에 공백이 포함될 수 있음)
                        if (confStatus != null) {
                            confStatus = confStatus.trim();
                        }
                        dto.setConf_status(confStatus);
                        
                        // 상태 코드를 한글로 변환
                        String confStatusName = getConfStatusName(confStatus);
                        dto.setConf_status_name(confStatusName);
                    } catch (Exception e) {
                        System.out.println("Oracle 상태 조회 실패 - mst_seq: " + mstSeq + ", 오류: " + e.getMessage());
                        dto.setConf_status("00");
                        dto.setConf_status_name("미전송");
                    }
                } else {
                    dto.setConf_status("00");
                    dto.setConf_status_name("미전송");
                }
            }
        }
        
        return result;
    }
    
    /**
     * 거래처 등록 시도 플래그 업데이트
     * @param draftId 기안서 ID
     * @param attempted 시도 여부 (Y/N)
     * @return 업데이트 결과
     */
    @Transactional("mysqlTransactionManager")
    public int updateCustomerRegistrationAttempted(Integer draftId, String attempted) {
        return approvalDAO.updateCustomerRegistrationAttempted(draftId, attempted);
    }
}

package com.dage.rent.Service;

import com.dage.rent.DAO.mysql.AdminDAO;
import com.dage.rent.DTO.EmpUserDTO;
import com.dage.rent.DTO.LoginDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class AdminService {

    @Value("${app.admin.erp-customer-operation-emp-nos:}")
    private String erpCustomerOperationEmpNos;

    @Autowired
    private AdminDAO adminDAO;
    
    @Autowired
    private RentService rentService;

    @Transactional("mysqlTransactionManager")
    public List<EmpUserDTO> getAllAdmins() {
        return adminDAO.getAllAdmins();
    }

    @Transactional("mysqlTransactionManager")
    public List<LoginDTO> getUsers() {
        // 임시로 빈 리스트 반환 (RentDAO 의존성 제거)
        return new ArrayList<>();
    }
    
    @Transactional("mysqlTransactionManager")
    public void updateDraftErpReg(String mstSeq) {
        adminDAO.updateDraftErpReg(mstSeq);
    }

    @Transactional("mysqlTransactionManager")
    public boolean isAdmin(int empNo) {
        return adminDAO.isAdmin(empNo);
    }

    /**
     * ERP등록·거래처등록 탭 및 관리자 결재 1단계 취소 권한.
     * 설정 app.admin.erp-customer-operation-emp-nos 가 비어 있으면 admin 테이블 등록 관리자 전원,
     * 값이 있으면 해당 사번만(반드시 admin 등록자여야 함).
     */
    public boolean canManageErpAndCustomer(int empNo) {
        if (!isAdmin(empNo)) {
            return false;
        }
        String raw = erpCustomerOperationEmpNos;
        if (raw == null || raw.trim().isEmpty()) {
            return true;
        }
        Set<Integer> allowed = new HashSet<>();
        for (String part : raw.split(",")) {
            String t = part.trim();
            if (t.isEmpty()) {
                continue;
            }
            try {
                allowed.add(Integer.parseInt(t));
            } catch (NumberFormatException ignored) {
                // skip invalid token
            }
        }
        return allowed.contains(empNo);
    }

    @Transactional("mysqlTransactionManager")
    public void addAdmin(String empNo, String userName, String userId, String mailReceive) {
        EmpUserDTO adminData = new EmpUserDTO();
        adminData.setEmp_no(Integer.parseInt(empNo));
        adminData.setName(userName);
        String mail = userId + "@dage.co.kr";
        adminData.setEmail(mail);
        adminData.setEmail_chk(mailReceive);
        
        adminDAO.insertAdmin(adminData);
    }

    @Transactional("mysqlTransactionManager")
    public void deleteAdmins(List<Integer> empNos) {
        adminDAO.deleteAdmins(empNos);
    }

    @Transactional("mysqlTransactionManager")
    public List<LoginDTO> searchAdmins(String type, String keyword) {
        return adminDAO.searchAdmins(type, keyword);
    }

    @Transactional("mysqlTransactionManager")
    public void updateMailReceive(String empNo, String mailReceive) {
        adminDAO.updateMailReceive(empNo, mailReceive);
    }

    @Transactional("mysqlTransactionManager")
    public List<Map<String, Object>> getDraftDetailsByMstSeq(String mstSeq) {
        return adminDAO.getDraftDetailsByMstSeq(mstSeq);
    }
    
    /**
     * 거래처 등록 후 cust_code 업데이트
     * @param mstSeq 마스터 시퀀스
     * @return 업데이트된 건수
     */
    @Transactional("mysqlTransactionManager")
    public int updateCustCodesAfterRegistration(String mstSeq) {
        System.out.println("=== cust_code 업데이트 시작 ===");
        System.out.println("mst_seq: " + mstSeq);
        
        // 1. draft 상세 데이터 조회
        List<Map<String, Object>> draftDetails = adminDAO.getDraftDetailsByMstSeq(mstSeq);
        
        if (draftDetails == null || draftDetails.isEmpty()) {
            System.out.println("❌ draft 상세 데이터 없음");
            return 0;
        }
        
        int updateCount = 0;
        
        // 2. 각 계약 상세에 대해 cust_code 업데이트
        for (Map<String, Object> detail : draftDetails) {
            try {
                String lessorBizNo = (String) detail.get("lessor");
                Integer detailId = (Integer) detail.get("detail_id");
                String currentCustCode = (String) detail.get("cust_code");
                
                System.out.println("=== 계약 상세 처리 ===");
                System.out.println("detail_id: " + detailId);
                System.out.println("lessor_biz_no: " + lessorBizNo);
                System.out.println("current_cust_code: " + currentCustCode);
                
                // 사업자등록번호/주민번호가 없으면 건너뛰기
                if (lessorBizNo == null || lessorBizNo.trim().isEmpty()) {
                    System.out.println("❌ 사업자등록번호 없음, 건너뛰기");
                    continue;
                }
                
                // Oracle에서 거래처 코드 조회 후 MySQL과 동기화
                String oracleCustCode = rentService.getCustCodeByBizNo(lessorBizNo);
                
                if (oracleCustCode != null && !oracleCustCode.trim().isEmpty()) {
                    if (currentCustCode == null || !oracleCustCode.equals(currentCustCode.trim())) {
                        adminDAO.updateDraftContractDetailCustCode(detailId, oracleCustCode);
                        if (currentCustCode != null && !currentCustCode.trim().isEmpty()) {
                            System.out.println("⚠️ cust_code 불일치 수정: MySQL=" + currentCustCode + " → Oracle=" + oracleCustCode);
                        } else {
                            System.out.println("✅ cust_code 업데이트 완료: " + oracleCustCode);
                        }
                        updateCount++;
                    } else {
                        System.out.println("✅ cust_code 일치: " + oracleCustCode);
                    }
                } else {
                    System.out.println("❌ Oracle 거래처 코드 조회 실패: " + lessorBizNo);
                }
                
            } catch (Exception e) {
                System.err.println("❌ cust_code 업데이트 실패: " + e.getMessage());
                e.printStackTrace();
            }
        }
        
        System.out.println("=== cust_code 업데이트 완료 ===");
        System.out.println("업데이트된 건수: " + updateCount);
        
        return updateCount;
    }

    @Transactional("mysqlTransactionManager")
    public void updateDraftContractDetailCustCode(int detailId, String custCode) {
        adminDAO.updateDraftContractDetailCustCode(detailId, custCode);
    }
    
    /**
     * ERP 등록 대상 조회 (conf_status = 30 AND cust_reg_attempted = 'Y' AND erp_reg = 'N')
     */
    @Transactional("mysqlTransactionManager")
    public List<Map<String, Object>> getErpRegistrationTargets() {
        List<Map<String, Object>> candidates = adminDAO.getErpRegistrationTargets();
        
        System.out.println("=== ERP 등록 대상 조회 결과 ===");
        System.out.println("MySQL에서 조회된 후보 개수: " + (candidates != null ? candidates.size() : 0));
        
        if (candidates != null) {
            for (Map<String, Object> candidate : candidates) {
                String mstSeq = (String) candidate.get("mst_seq");
                String createdDate = candidate.get("created_date") != null ? candidate.get("created_date").toString() : "";
                System.out.println("후보: mst_seq=" + mstSeq + ", created_date=" + createdDate);
            }
        }
        
        if (candidates == null || candidates.isEmpty()) {
            System.out.println("ERP 등록 대상 없음");
            return candidates;
        }
        
        // Oracle에서 conf_status = 30인 것만 필터링
        List<Map<String, Object>> filteredTargets = new java.util.ArrayList<>();
        
        for (Map<String, Object> candidate : candidates) {
            String mstSeq = (String) candidate.get("mst_seq");
            
            if (mstSeq != null && !mstSeq.trim().isEmpty()) {
                try {
                    // Oracle에서 conf_status 조회
                    String confStatus = rentService.getConfStatus(mstSeq);
                    
                    // conf_status가 30(완료)인 경우만 추가
                    if ("30".equals(confStatus)) {
                        filteredTargets.add(candidate);
                        System.out.println("✅ ERP 등록 대상 추가: " + mstSeq + " (conf_status: " + confStatus + ")");
                    } else {
                        System.out.println("❌ ERP 등록 대상 제외: " + mstSeq + " (conf_status: " + confStatus + ")");
                    }
                } catch (Exception e) {
                    System.err.println("❌ conf_status 조회 실패: " + mstSeq + ", 오류: " + e.getMessage());
                }
            }
        }
        
        System.out.println("ERP 등록 대상 필터링 완료: " + candidates.size() + " → " + filteredTargets.size());
        return filteredTargets;
    }
    
    /**
     * ERP 등록 시도 플래그 업데이트
     */
    @Transactional("mysqlTransactionManager")
    public void updateErpRegAttempted(String mstSeq, String attempted) {
        adminDAO.updateErpRegAttempted(mstSeq, attempted);
    }
} 
package com.dage.rent.Controller;

import com.dage.rent.Component.Mail;
import com.dage.rent.DTO.DraftDTO;
import com.dage.rent.DTO.EmpUserDTO;
import com.dage.rent.DTO.LeaseProcedureDTO;
import com.dage.rent.DTO.LoginDTO;
import com.dage.rent.Service.AdminService;
import com.dage.rent.Service.DraftService;
import com.dage.rent.Service.RentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private AdminService adminService;
    
    @Autowired
    private RentService rentService;
    
    @Autowired
    private DraftService draftService;
    
    @Autowired
    private Mail mail;
    
    @GetMapping("/test")
    @ResponseBody
    public String test() {
        return "AdminController is working!";
    }

    @GetMapping("/settings")
    public String adminSettings() {
        return "admin_settings";
    }

    @GetMapping("/list")
    @ResponseBody
    public ResponseEntity<?> getAdminList() {
        try {
            List<EmpUserDTO> admins = adminService.getAllAdmins();
            Map<String, Object> response = new HashMap<>();
            response.put("data", admins);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("error", "관리자 목록 조회 실패"));
        }
    }

    @GetMapping("/users")
    @ResponseBody
    public ResponseEntity<?> getUsers() {
        try {
            List<LoginDTO> users = adminService.getUsers();
            Map<String, Object> response = new HashMap<>();
            response.put("data", users);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("error", "사용자 목록 조회 실패"));
        }
    }

    @PostMapping("/add")
    @ResponseBody
    public ResponseEntity<?> addAdmin(@RequestBody Map<String, Object> request) {
        try {
            String empNo = request.get("empNo").toString();
            String userName = (String) request.get("userName");
            String userId = (String) request.get("userId");
            String mailReceive = (String) request.get("mailReceive");
            
            adminService.addAdmin(empNo, userName, userId, mailReceive);
            return ResponseEntity.ok(Map.of("message", "관리자 추가 성공"));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("error", "관리자 추가 실패"));
        }
    }

    @PostMapping("/mail-receive")
    @ResponseBody
    public ResponseEntity<?> updateMailReceive(@RequestBody Map<String, Object> request) {
        try {
            String empNo = request.get("empNo").toString();
            String mailReceive = (String) request.get("mailReceive");
            
            adminService.updateMailReceive(empNo, mailReceive);
            return ResponseEntity.ok(Map.of("message", "메일 수신 여부 업데이트 성공"));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("error", "메일 수신 여부 업데이트 실패"));
        }
    }

    @PostMapping("/delete")
    @ResponseBody
    public ResponseEntity<?> deleteAdmins(@RequestBody Map<String, List<Integer>> request) {
        try {
            // 현재 로그인한 사용자가 관리자인지 확인
            LoginDTO currentUser = (LoginDTO) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            if (!adminService.isAdmin(currentUser.getEmpNo())) {
                return ResponseEntity.badRequest().body(Map.of("error", "관리자만 접근 가능합니다."));
            }

            List<Integer> empNos = request.get("empNos");
            if (empNos.contains(currentUser.getEmpNo())) {
                return ResponseEntity.badRequest().body(Map.of("error", "자기 자신은 삭제할 수 없습니다."));
            }

            adminService.deleteAdmins(empNos);
            return ResponseEntity.ok(Map.of("message", "선택한 관리자가 삭제되었습니다."));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("error", "관리자 삭제 실패"));
        }
    }

    @GetMapping("/search")
    @ResponseBody
    public ResponseEntity<?> searchAdmins(
            @RequestParam String type,
            @RequestParam String keyword) {
        try {
            List<LoginDTO> admins = adminService.searchAdmins(type, keyword);
            Map<String, Object> response = new HashMap<>();
            response.put("data", admins);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("error", "관리자 검색 실패"));
        }
    }

    @PostMapping("/erp/register")
    @ResponseBody
    public ResponseEntity<?> registerToErp(@RequestBody Map<String, Object> request) {
        try {
            String mstSeq = (String) request.get("mst_seq");
            
            System.out.println("=== ERP 등록 요청 ===");
            System.out.println("mst_seq: " + mstSeq);
            
            if (mstSeq == null || mstSeq.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "마스터 시퀀스가 없습니다."));
            }
            
            // 1단계: 거래처 등록 후 cust_code 업데이트
            System.out.println("=== 1단계: cust_code 업데이트 시작 ===");
            int updateCount = adminService.updateCustCodesAfterRegistration(mstSeq);
            System.out.println("cust_code 업데이트 완료: " + updateCount + "건");
            
            // 2단계: mst_seq로 draft 테이블과 draft_contract_detail 테이블 조인해서 데이터 가져오기
            List<Map<String, Object>> draftDetails = adminService.getDraftDetailsByMstSeq(mstSeq);
            
            System.out.println("=== 조회된 draft 상세 데이터 ===");
            System.out.println("데이터 개수: " + (draftDetails != null ? draftDetails.size() : 0));
            
            int successCount = 0;
            int failCount = 0;
            
            if (draftDetails != null && !draftDetails.isEmpty()) {
                // 각 계약 상세 데이터에 대해 임대차 계약 등록 프로시저 호출
                for (Map<String, Object> detail : draftDetails) {
                    System.out.println("=== 임대차 계약 등록 시작 ===");
                    System.out.println("상세 데이터: " + detail);
                    
                    try {
                        // 시퀀스 조회
                        String leaseSeq = rentService.getLeaseSeq();
                        String contNo = rentService.getLeaseContNo();
                        
                        System.out.println("생성된 leaseSeq: " + leaseSeq);
                        System.out.println("생성된 contNo: " + contNo);
                        
                        // 임대차 계약 등록 데이터 준비
                        LeaseProcedureDTO leaseData = new LeaseProcedureDTO();
                        
                        // 고정값들
                        leaseData.setArCompanyCode("100");
                        leaseData.setArSeq(Integer.parseInt(leaseSeq));
                        // 현재 로그인한 사용자의 user_no 가져오기
                        LoginDTO loginUser = (LoginDTO) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
                        leaseData.setArCrtUserNo(loginUser.getUserNo());
                        leaseData.setArBLeaseCls("1");
                        leaseData.setArContNo(contNo);
                        leaseData.setArHCustCode(20007);
                        leaseData.setArUseType("10");
                        leaseData.setArFinTag("F");
                        leaseData.setArHCustName("1");
                        leaseData.setArDpstIntrRate(new BigDecimal("4.6"));
                        leaseData.setArLCode("LR");
                        leaseData.setArCrncCode("KRW");
                        leaseData.setArFcDpstAmt(new BigDecimal("0"));
                        leaseData.setArFcMonRent(new BigDecimal("0"));
                        leaseData.setArDaymonCls("M");
                        leaseData.setArPayCycle("1");
                        leaseData.setArPreLater("P");
                        leaseData.setArLrContNo(contNo);
                        
                        // null 값들
                        leaseData.setArRemarks(null);
                        leaseData.setArBankMainCode(null);
                        leaseData.setArBankMainName(null);
                        leaseData.setArAccno(null);
                        leaseData.setArAccOwner(null);
                        leaseData.setArCarNum(null);
                        leaseData.setArContTitle(null);
                        leaseData.setArLequType(null);
                        leaseData.setArInsuEndDt(null);
                        leaseData.setArChkEndDt(null);
                        leaseData.setArUsers(null);
                        leaseData.setArAutoExtYn(null);
                        leaseData.setArAutoExtLimit(null);
                        leaseData.setArExprLimit(null);
                        leaseData.setArExprSndDt(null);
                        
                        // draft_contract_detail에서 가져온 값들
                        leaseData.setArProjCode(detail.get("proj_code") != null ? detail.get("proj_code").toString() : null);
                        leaseData.setArContDt(detail.get("cont_date_s").toString());
                        leaseData.setArExprDt(detail.get("cont_date_e").toString());
                        leaseData.setArExprChgDt(detail.get("cont_date_e").toString());
                        leaseData.setArFinDt(detail.get("cont_date_e").toString());
                        leaseData.setArLCustCode(detail.get("cust_code") != null ? Integer.parseInt(detail.get("cust_code").toString()) : null);
                        leaseData.setArZipcode(detail.get("post_code") != null ? detail.get("post_code").toString() : null);
                        leaseData.setArAddr1(detail.get("address") != null ? detail.get("address").toString() : null);
                        leaseData.setArAddr2(detail.get("address_d") != null ? detail.get("address_d").toString() : null);
                        leaseData.setArArea(detail.get("area") != null ? detail.get("area").toString() : null);
                        leaseData.setArDpstAmt(detail.get("deposit_amt") != null ? new BigDecimal(detail.get("deposit_amt").toString()) : null);
                        leaseData.setArMonRent(detail.get("rent_amt") != null ? new BigDecimal(detail.get("rent_amt").toString()) : null);
                        
                        // payment_date가 25일 이상이면 pay_days는 0으로 설정
                        Integer payDays = null;
                        if (detail.get("payment_date") != null) {
                            int paymentDate = Integer.parseInt(detail.get("payment_date").toString());
                            payDays = (paymentDate >= 25) ? 0 : paymentDate;
                        }
                        leaseData.setArPayDays(payDays);
                        
                        leaseData.setArLCustName(detail.get("lessor_name") != null ? detail.get("lessor_name").toString() : null);
                        leaseData.setArRsrcCode(detail.get("rsrc_code") != null ? detail.get("rsrc_code").toString() : null);
                        
                        System.out.println("=== 임대차 계약 등록 데이터 ===");
                        System.out.println("leaseData: " + leaseData);

                        // 1단계: TIA_B_LEASE 테이블에 INSERT
                        rentService.insertLease(leaseData);
                        
                        // 2단계: cont_seq 생성
                        Integer contSeq = rentService.getLeaseContSeq();
                        System.out.println("=== 생성된 cont_seq: " + contSeq + " ===");
                        
                        // 3단계: TIA_B_LEASE_CONT 테이블에 INSERT
                        Map<String, Object> leaseContData = new HashMap<>();
                        leaseContData.put("companyCode", leaseData.getArCompanyCode());
                        leaseContData.put("seq", leaseData.getArSeq());
                        leaseContData.put("contSeq", contSeq);
                        leaseContData.put("crtUserNo", leaseData.getArCrtUserNo());
                        leaseContData.put("contName", 1); // 1차 계약
                        leaseContData.put("chgDt", leaseData.getArExprDt());
                        leaseContData.put("dpstAmt", leaseData.getArDpstAmt());
                        leaseContData.put("monRent", leaseData.getArMonRent());
                        leaseContData.put("remarks", "최초계약");
                        leaseContData.put("exprDt", leaseData.getArExprDt());
                        leaseContData.put("crncCode", leaseData.getArCrncCode());
                        leaseContData.put("contBaseDt", leaseData.getArContDt() != null ? 
                            leaseData.getArContDt().substring(0, 7) : null); // YYYY-MM 형식
                        
                        rentService.insertLeaseCont(leaseContData);
                        System.out.println("=== TIA_B_LEASE_CONT INSERT 완료 ===");
                        
                        // 4단계: TIA_B_LEASE_TRAN 테이블에 INSERT (보증금만 있는 경우)
                        if (leaseData.getArMonRent() == null || leaseData.getArMonRent().compareTo(BigDecimal.ZERO) == 0) {
                            Map<String, Object> tranData = new HashMap<>();
                            tranData.put("seq", leaseData.getArSeq());
                            tranData.put("contSeq", contSeq);
                            tranData.put("userNo", leaseData.getArCrtUserNo());
                            
                            rentService.insertLeaseTranDeposit(tranData);
                            System.out.println("=== TIA_B_LEASE_TRAN INSERT 완료 (보증금만 있는 경우) ===");
                        } else {
                            System.out.println("=== 월세가 있으므로 TIA_B_LEASE_TRAN INSERT 생략 (복잡한 월별 계산 필요) ===");
                        }
                        
                        // 5단계: TIA_B_LEASE_INTR 테이블에 INSERT (보증금이자율이 있는 경우)
                        if (leaseData.getArDpstIntrRate() != null && leaseData.getArDpstIntrRate().compareTo(BigDecimal.ZERO) > 0) {
                            Map<String, Object> intrData = new HashMap<>();
                            intrData.put("seq", leaseData.getArSeq());
                            intrData.put("contSeq", contSeq);
                            intrData.put("userNo", leaseData.getArCrtUserNo());
                            
                            rentService.insertLeaseIntr(intrData);
                            System.out.println("=== TIA_B_LEASE_INTR INSERT 완료 ===");
                        } else {
                            System.out.println("=== 보증금이자율이 0이므로 TIA_B_LEASE_INTR INSERT 생략 ===");
                        }
                        
                        // 6단계: draft 테이블의 erp_reg 컬럼을 'Y'로 업데이트
                        adminService.updateDraftErpReg(mstSeq);
                        System.out.println("=== draft 테이블 erp_reg 업데이트 완료 ===");
                        
                        System.out.println("✅ 임대차 계약 등록 완료: " + contNo);
                        successCount++;
                        
                    } catch (Exception e) {
                        System.err.println("❌ 임대차 계약 등록 실패: " + e.getMessage());
                        e.printStackTrace();
                        failCount++;
                    }
                }
            } else {
                System.out.println("❌ draft 상세 데이터가 없습니다.");
            }
            
            // 결과 메시지 생성
            String message;
            if (successCount > 0 && failCount == 0) {
                message = "임대차 계약 등록이 완료되었습니다. (성공: " + successCount + "건)";
                
                // 7단계: ERP 등록 완료 메일 발송 (모든 계약이 성공했을 때만)
                try {
                    System.out.println("=== ERP 등록 완료 메일 발송 시작 ===");
                    DraftDTO draft = draftService.getDraftByMstSeq(mstSeq);
                    if (draft != null) {
                        sendErpRegistrationCompleteEmail(draft);
                        System.out.println("=== ERP 등록 완료 메일 발송 성공 ===");
                    } else {
                        System.out.println("❌ draft 정보 조회 실패, 메일 발송 건너뜀");
                    }
                } catch (Exception e) {
                    System.err.println("❌ ERP 등록 완료 메일 발송 실패: " + e.getMessage());
                    e.printStackTrace();
                }
            } else if (successCount > 0 && failCount > 0) {
                message = "임대차 계약 등록이 부분적으로 완료되었습니다. (성공: " + successCount + "건, 실패: " + failCount + "건)";
            } else if (failCount > 0) {
                message = "임대차 계약 등록에 실패했습니다. (실패: " + failCount + "건)";
            } else {
                message = "등록할 계약 데이터가 없습니다.";
            }
            
            return ResponseEntity.ok(Map.of(
                "message", message,
                "success_count", successCount,
                "fail_count", failCount,
                "total_count", draftDetails != null ? draftDetails.size() : 0
            ));
            
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("error", "ERP 등록 실패: " + e.getMessage()));
        }
    }
    
    /**
     * ERP 등록 테스트 데이터 삭제 (SEQ 기준)
     */
    @PostMapping("/erp/delete")
    @ResponseBody
    public ResponseEntity<?> deleteErpData(@RequestBody Map<String, Object> request) {
        try {
            Object seqObj = request.get("seq");
            if (seqObj == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "SEQ가 없습니다."));
            }
            
            Integer seq = null;
            if (seqObj instanceof Integer) {
                seq = (Integer) seqObj;
            } else if (seqObj instanceof String) {
                try {
                    seq = Integer.parseInt((String) seqObj);
                } catch (NumberFormatException e) {
                    return ResponseEntity.badRequest().body(Map.of("error", "SEQ는 숫자여야 합니다."));
                }
            } else {
                return ResponseEntity.badRequest().body(Map.of("error", "SEQ 형식이 올바르지 않습니다."));
            }
            
            System.out.println("=== ERP 등록 데이터 삭제 요청 ===");
            System.out.println("SEQ: " + seq);
            
            // ERP 등록 데이터 삭제 (자식 테이블부터 삭제)
            rentService.deleteLeaseData(seq);
            
            System.out.println("✅ ERP 등록 데이터 삭제 완료: SEQ=" + seq);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "ERP 등록 데이터가 삭제되었습니다. (SEQ: " + seq + ")",
                "seq", seq
            ));
            
        } catch (Exception e) {
            System.err.println("❌ ERP 등록 데이터 삭제 실패: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("error", "ERP 등록 데이터 삭제 실패: " + e.getMessage()));
        }
    }
    
    @PostMapping("/erp/register/multiple")
    @ResponseBody
    public ResponseEntity<?> registerMultipleToErp(@RequestBody Map<String, Object> request) {
        try {
            @SuppressWarnings("unchecked")
            List<String> mstSeqList = (List<String>) request.get("mst_seq_list");
            
            System.out.println("=== 다중 ERP 등록 요청 ===");
            System.out.println("mst_seq 개수: " + (mstSeqList != null ? mstSeqList.size() : 0));
            
            if (mstSeqList == null || mstSeqList.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "등록할 항목이 없습니다."));
            }
            
            int totalSuccessCount = 0;
            int totalFailCount = 0;
            
            // 각 mst_seq에 대해 등록 처리
            for (String mstSeq : mstSeqList) {
                try {
                    System.out.println("=== mst_seq 처리 시작: " + mstSeq + " ===");
                    
                    if (mstSeq == null || mstSeq.trim().isEmpty()) {
                        System.err.println("❌ 빈 mst_seq 발견, 건너뜀");
                        totalFailCount++;
                        continue;
                    }
                    
                    // 1단계: 거래처 등록 후 cust_code 업데이트
                    System.out.println("=== 1단계: cust_code 업데이트 시작 (" + mstSeq + ") ===");
                    int updateCount = adminService.updateCustCodesAfterRegistration(mstSeq);
                    System.out.println("cust_code 업데이트 완료: " + updateCount + "건");
                    
                    // 2단계: mst_seq로 draft 테이블과 draft_contract_detail 테이블 조인해서 데이터 가져오기
                    List<Map<String, Object>> draftDetails = adminService.getDraftDetailsByMstSeq(mstSeq);
                    
                    if (draftDetails == null || draftDetails.isEmpty()) {
                        System.err.println("❌ draft 상세 데이터 없음: " + mstSeq);
                        totalFailCount++;
                        continue;
                    }
                    
                    // 각 계약 상세 데이터에 대해 임대차 계약 등록 프로시저 호출
                    for (Map<String, Object> detail : draftDetails) {
                        try {
                            // 시퀀스 조회
                            String leaseSeq = rentService.getLeaseSeq();
                            String contNo = rentService.getLeaseContNo();
                            
                            // 임대차 계약 등록 데이터 준비
                            LeaseProcedureDTO leaseData = new LeaseProcedureDTO();
                            
                            // 고정값들
                            leaseData.setArCompanyCode("100");
                            leaseData.setArSeq(Integer.parseInt(leaseSeq));
                            LoginDTO loginUser = (LoginDTO) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
                            leaseData.setArCrtUserNo(loginUser.getUserNo());
                            leaseData.setArBLeaseCls("1");
                            leaseData.setArContNo(contNo);
                            leaseData.setArHCustCode(20007);
                            leaseData.setArUseType("10");
                            leaseData.setArFinTag("F");
                            leaseData.setArHCustName("1");
                            leaseData.setArDpstIntrRate(new BigDecimal("4.6"));
                            leaseData.setArLCode("LR");
                            leaseData.setArCrncCode("KRW");
                            leaseData.setArFcDpstAmt(new BigDecimal("0"));
                            leaseData.setArFcMonRent(new BigDecimal("0"));
                            leaseData.setArDaymonCls("M");
                            leaseData.setArPayCycle("1");
                            leaseData.setArPreLater("P");
                            leaseData.setArLrContNo(contNo);
                            
                            // null 값들
                            leaseData.setArRemarks(null);
                            leaseData.setArBankMainCode(null);
                            leaseData.setArBankMainName(null);
                            leaseData.setArAccno(null);
                            leaseData.setArAccOwner(null);
                            leaseData.setArCarNum(null);
                            leaseData.setArContTitle(null);
                            leaseData.setArLequType(null);
                            leaseData.setArInsuEndDt(null);
                            leaseData.setArChkEndDt(null);
                            leaseData.setArUsers(null);
                            leaseData.setArAutoExtYn(null);
                            leaseData.setArAutoExtLimit(null);
                            leaseData.setArExprLimit(null);
                            leaseData.setArExprSndDt(null);
                            
                            // draft_contract_detail에서 가져온 값들
                            leaseData.setArProjCode(detail.get("proj_code") != null ? detail.get("proj_code").toString() : null);
                            leaseData.setArContDt(detail.get("cont_date_s").toString());
                            leaseData.setArExprDt(detail.get("cont_date_e").toString());
                            leaseData.setArExprChgDt(detail.get("cont_date_e").toString());
                            leaseData.setArFinDt(detail.get("cont_date_e").toString());
                            leaseData.setArLCustCode(detail.get("cust_code") != null ? Integer.parseInt(detail.get("cust_code").toString()) : null);
                            leaseData.setArZipcode(detail.get("post_code") != null ? detail.get("post_code").toString() : null);
                            leaseData.setArAddr1(detail.get("address") != null ? detail.get("address").toString() : null);
                            leaseData.setArAddr2(detail.get("address_d") != null ? detail.get("address_d").toString() : null);
                            leaseData.setArArea(detail.get("area") != null ? detail.get("area").toString() : null);
                            leaseData.setArDpstAmt(detail.get("deposit_amt") != null ? new BigDecimal(detail.get("deposit_amt").toString()) : null);
                            leaseData.setArMonRent(detail.get("rent_amt") != null ? new BigDecimal(detail.get("rent_amt").toString()) : null);
                            
                            // payment_date가 25일 이상이면 pay_days는 0으로 설정
                            Integer payDays = null;
                            if (detail.get("payment_date") != null) {
                                int paymentDate = Integer.parseInt(detail.get("payment_date").toString());
                                payDays = (paymentDate >= 25) ? 0 : paymentDate;
                            }
                            leaseData.setArPayDays(payDays);
                            
                            leaseData.setArLCustName(detail.get("lessor_name") != null ? detail.get("lessor_name").toString() : null);
                            leaseData.setArRsrcCode(detail.get("rsrc_code") != null ? detail.get("rsrc_code").toString() : null);
                            
                            // 1단계: TIA_B_LEASE 테이블에 INSERT
                            rentService.insertLease(leaseData);
                            
                            // 2단계: cont_seq 생성
                            Integer contSeq = rentService.getLeaseContSeq();
                            
                            // 3단계: TIA_B_LEASE_CONT 테이블에 INSERT
                            Map<String, Object> leaseContData = new HashMap<>();
                            leaseContData.put("companyCode", leaseData.getArCompanyCode());
                            leaseContData.put("seq", leaseData.getArSeq());
                            leaseContData.put("contSeq", contSeq);
                            leaseContData.put("crtUserNo", leaseData.getArCrtUserNo());
                            leaseContData.put("contName", 1);
                            leaseContData.put("chgDt", leaseData.getArExprDt());
                            leaseContData.put("dpstAmt", leaseData.getArDpstAmt());
                            leaseContData.put("monRent", leaseData.getArMonRent());
                            leaseContData.put("remarks", "최초계약");
                            leaseContData.put("exprDt", leaseData.getArExprDt());
                            leaseContData.put("crncCode", leaseData.getArCrncCode());
                            leaseContData.put("contBaseDt", leaseData.getArContDt() != null ? 
                                leaseData.getArContDt().substring(0, 7) : null);
                            
                            rentService.insertLeaseCont(leaseContData);
                            
                            // 4단계: TIA_B_LEASE_TRAN 테이블에 INSERT (보증금만 있는 경우)
                            if (leaseData.getArMonRent() == null || leaseData.getArMonRent().compareTo(BigDecimal.ZERO) == 0) {
                                Map<String, Object> tranData = new HashMap<>();
                                tranData.put("seq", leaseData.getArSeq());
                                tranData.put("contSeq", contSeq);
                                tranData.put("userNo", leaseData.getArCrtUserNo());
                                
                                rentService.insertLeaseTranDeposit(tranData);
                            }
                            
                            // 5단계: TIA_B_LEASE_INTR 테이블에 INSERT (보증금이자율이 있는 경우)
                            if (leaseData.getArDpstIntrRate() != null && leaseData.getArDpstIntrRate().compareTo(BigDecimal.ZERO) > 0) {
                                Map<String, Object> intrData = new HashMap<>();
                                intrData.put("seq", leaseData.getArSeq());
                                intrData.put("contSeq", contSeq);
                                intrData.put("userNo", leaseData.getArCrtUserNo());
                                
                                rentService.insertLeaseIntr(intrData);
                            }
                            
                            System.out.println("✅ 임대차 계약 등록 완료: " + contNo);
                            totalSuccessCount++;
                            
                        } catch (Exception e) {
                            System.err.println("❌ 임대차 계약 등록 실패: " + e.getMessage());
                            e.printStackTrace();
                            totalFailCount++;
                        }
                    }
                    
                    // 6단계: draft 테이블의 erp_reg 컬럼을 'Y'로 업데이트
                    if (totalSuccessCount > 0) {
                        adminService.updateDraftErpReg(mstSeq);
                        System.out.println("=== draft 테이블 erp_reg 업데이트 완료: " + mstSeq + " ===");
                    }
                    
                } catch (Exception e) {
                    System.err.println("❌ mst_seq 처리 실패: " + mstSeq + " - " + e.getMessage());
                    e.printStackTrace();
                    totalFailCount++;
                }
            }
            
            // 결과 메시지 생성
            String message;
            if (totalSuccessCount > 0 && totalFailCount == 0) {
                message = "임대차 계약 등록이 완료되었습니다.";
            } else if (totalSuccessCount > 0 && totalFailCount > 0) {
                message = "임대차 계약 등록이 부분적으로 완료되었습니다.";
            } else if (totalFailCount > 0) {
                message = "임대차 계약 등록에 실패했습니다.";
            } else {
                message = "등록할 계약 데이터가 없습니다.";
            }
            
            return ResponseEntity.ok(Map.of(
                "message", message,
                "success_count", totalSuccessCount,
                "fail_count", totalFailCount,
                "total_count", mstSeqList.size()
            ));
            
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("error", "다중 ERP 등록 실패: " + e.getMessage()));
        }
    }
    
    /**
     * 거래처 등록 API
     */
    @PostMapping("/customer/register")
    @ResponseBody
    public ResponseEntity<?> registerCustomer(@RequestBody Map<String, Object> request) {
        try {
            String mstSeq = (String) request.get("mst_seq");
            
            System.out.println("=== 거래처 등록 요청 ===");
            System.out.println("mst_seq: " + mstSeq);
            
            if (mstSeq == null || mstSeq.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "마스터 시퀀스가 없습니다."));
            }
            
            // 거래처 등록 후 cust_code 업데이트
            System.out.println("=== 거래처 등록 시작 ===");
            int updateCount = adminService.updateCustCodesAfterRegistration(mstSeq);
            System.out.println("거래처 등록 완료: " + updateCount + "건");
            
            String message = "거래처 등록이 완료되었습니다. (등록: " + updateCount + "건)";
            
            return ResponseEntity.ok(Map.of(
                "message", message,
                "update_count", updateCount
            ));
            
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("error", "거래처 등록 실패: " + e.getMessage()));
        }
    }
    
    /**
     * ERP 등록 완료 메일 발송
     */
    private void sendErpRegistrationCompleteEmail(DraftDTO draft) {
        try {
            // 수신자 설정
            String[] recipients = {
                "cwlee1@dage.co.kr",
                "jhyunlee@dage.co.kr",
                "jojang@dage.co.kr",
                "nylee@dage.co.kr",
                "yrjung@dage.co.kr"
            };
            String[] ccRecipients = null;
            String[] bccRecipients = null;
            
            // 메일 제목
            String subject = "ERP 임차계약관리(부동산) 등록 완료 - 부동산임차관리플랫폼";
            
            // 메일 내용 HTML 생성
            String htmlContent = generateErpRegistrationCompleteHtml(draft);
            
            // 메일 발송
            mail.sendEmail(recipients, ccRecipients, bccRecipients, subject, htmlContent);
            
            System.out.println("ERP 등록 완료 메일 발송 성공");
            
        } catch (Exception e) {
            System.err.println("ERP 등록 완료 메일 발송 실패: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * ERP 등록 완료 메일 HTML 생성
     */
    private String generateErpRegistrationCompleteHtml(DraftDTO draft) {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>");
        html.append("<html lang='ko'>");
        html.append("<head>");
        html.append("<meta charset='UTF-8'>");
        html.append("<title>ERP 임대차 계약 등록 완료</title>");
        html.append("</head>");
        html.append("<body style='font-family: Arial, sans-serif; line-height: 1.6; color: #333; background-color: #f5f5f5; margin: 0; padding: 20px;'>");
        html.append("<table width='800' cellpadding='0' cellspacing='0' style='background-color: white; border-radius: 10px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); margin: 0 auto;'>");
        
        // 헤더
        html.append("<tr>");
        html.append("<td style='background-color: #27ae60; color: white; padding: 20px; text-align: center; border-radius: 10px 10px 0 0;'>");
        html.append("<p style='margin: 0; font-size: 18px; font-weight: bold;'>✅ ERP 임차계약관리(부동산) 등록 완료</p>");
        html.append("<p style='margin: 10px 0 0 0; font-size: 14px;'>부동산 임차관리 플랫폼에서 ERP 등록이 완료되었습니다.</p>");
        html.append("<p style='margin: 5px 0 0 0; font-size: 14px;'>계약서와 내용이 동일한지 반드시 확인해주세요.</p>");
        html.append("</td>");
        html.append("</tr>");
        
        // 내용
        html.append("<tr>");
        html.append("<td style='padding: 30px;'>");
        html.append("<table width='100%' cellpadding='0' cellspacing='0' style='border-collapse: collapse;'>");
        html.append("<tr><td style='padding: 12px; background-color: #f8f9fa; font-weight: bold; width: 30%; border: 1px solid #ddd;'>기안자</td><td style='padding: 12px; border: 1px solid #ddd;'>").append(draft.getUser_nm() != null ? draft.getUser_nm() : "").append("</td></tr>");
        html.append("<tr><td style='padding: 12px; background-color: #f8f9fa; font-weight: bold; border: 1px solid #ddd;'>현장명</td><td style='padding: 12px; border: 1px solid #ddd;'>").append(draft.getProj_name() != null ? draft.getProj_name() : "").append("</td></tr>");
        html.append("<tr><td style='padding: 12px; background-color: #f8f9fa; font-weight: bold; border: 1px solid #ddd;'>프로젝트 코드</td><td style='padding: 12px; border: 1px solid #ddd;'>").append(draft.getProj_code() != null ? draft.getProj_code() : "").append("</td></tr>");
        html.append("<tr><td style='padding: 12px; background-color: #f8f9fa; font-weight: bold; border: 1px solid #ddd;'>등록 완료 시간</td><td style='padding: 12px; border: 1px solid #ddd;'>").append(new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date())).append("</td></tr>");
        html.append("</table>");
        
        // 계약 상세 정보
        if (draft.getContractDetails() != null && !draft.getContractDetails().isEmpty()) {
            html.append("<h3 style='color: #2c3e50; margin-top: 30px; margin-bottom: 15px;'>등록된 계약 상세 정보</h3>");
            html.append("<table width='100%' cellpadding='0' cellspacing='0' style='border-collapse: collapse; margin-top: 15px;'>");
            html.append("<tr style='background-color: #34495e; color: white;'>");
            html.append("<th style='padding: 12px; text-align: left; border: 1px solid #ddd;'>주소</th>");
            html.append("<th style='padding: 12px; text-align: left; border: 1px solid #ddd;'>보증금</th>");
            html.append("<th style='padding: 12px; text-align: left; border: 1px solid #ddd;'>월세</th>");
            html.append("<th style='padding: 12px; text-align: left; border: 1px solid #ddd;'>임대인</th>");
            html.append("</tr>");
            
            for (var detail : draft.getContractDetails()) {
                html.append("<tr>");
                html.append("<td style='padding: 12px; border: 1px solid #ddd;'>").append(detail.getAddress() != null ? detail.getAddress() : "").append("</td>");
                
                // 보증금 포맷팅 (String을 Integer로 변환)
                String depositAmtStr = "";
                if (detail.getDepositAmt() != null && !detail.getDepositAmt().trim().isEmpty()) {
                    try {
                        int depositAmt = Integer.parseInt(detail.getDepositAmt());
                        depositAmtStr = String.format("%,d", depositAmt);
                    } catch (NumberFormatException e) {
                        depositAmtStr = detail.getDepositAmt();
                    }
                }
                html.append("<td style='padding: 12px; border: 1px solid #ddd;'>").append(depositAmtStr).append("원</td>");
                
                // 월세 포맷팅 (String을 Integer로 변환)
                String rentAmtStr = "";
                if (detail.getRentAmt() != null && !detail.getRentAmt().trim().isEmpty()) {
                    try {
                        int rentAmt = Integer.parseInt(detail.getRentAmt());
                        rentAmtStr = String.format("%,d", rentAmt);
                    } catch (NumberFormatException e) {
                        rentAmtStr = detail.getRentAmt();
                    }
                }
                html.append("<td style='padding: 12px; border: 1px solid #ddd;'>").append(rentAmtStr).append("원</td>");
                
                html.append("<td style='padding: 12px; border: 1px solid #ddd;'>").append(detail.getLessorName() != null ? detail.getLessorName() : "").append("</td>");
                html.append("</tr>");
            }
            html.append("</table>");
        }
        
        html.append("</td>");
        html.append("</tr>");
        
        // 푸터
        html.append("<tr>");
        html.append("<td style='background-color: #ecf0f1; padding: 20px; text-align: center; border-radius: 0 0 10px 10px;'>");
        html.append("<p style='margin: 0; font-size: 12px; color: #7f8c8d;'>이 메일은 부동산 임차관리 플랫폼에서 자동으로 발송되었습니다.</p>");
        html.append("<p style='margin: 5px 0 0 0; font-size: 12px; color: #7f8c8d;'>문의사항은 강성민 매니저에게 연락해 주세요.</p>");
        html.append("</td>");
        html.append("</tr>");
        
        html.append("</table>");
        html.append("</body>");
        html.append("</html>");
        
        return html.toString();
    }
} 
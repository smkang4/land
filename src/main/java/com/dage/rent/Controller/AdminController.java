package com.dage.rent.Controller;

import com.dage.rent.Component.Mail;
import com.dage.rent.DTO.DraftDTO;
import com.dage.rent.DTO.EmpUserDTO;
import com.dage.rent.DTO.LoginDTO;
import com.dage.rent.Service.AdminService;
import com.dage.rent.Service.AppSettingsService;
import com.dage.rent.Service.DraftService;
import com.dage.rent.Service.ErpRegistrationService;
import com.dage.rent.Service.RentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

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
    private ErpRegistrationService erpRegistrationService;

    @Autowired
    private AppSettingsService appSettingsService;
    
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

    @GetMapping("/notice-modal")
    @ResponseBody
    public ResponseEntity<?> getNoticeModalSetting() {
        try {
            return ResponseEntity.ok(Map.of("enabled", appSettingsService.isMainNoticeModalEnabled()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("error", "공지 모달 설정 조회 실패"));
        }
    }

    @PutMapping("/notice-modal")
    @ResponseBody
    public ResponseEntity<?> updateNoticeModalSetting(@RequestBody Map<String, Object> request) {
        try {
            LoginDTO currentUser = (LoginDTO) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            if (!adminService.isAdmin(currentUser.getEmpNo())) {
                return ResponseEntity.badRequest().body(Map.of("error", "관리자만 접근 가능합니다."));
            }
            Object enabledObj = request.get("enabled");
            if (enabledObj == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "enabled 값이 필요합니다."));
            }
            boolean enabled = Boolean.parseBoolean(enabledObj.toString());
            appSettingsService.setMainNoticeModalEnabled(enabled);
            return ResponseEntity.ok(Map.of(
                    "message", "공지 모달 설정이 저장되었습니다.",
                    "enabled", enabled
            ));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("error", "공지 모달 설정 저장 실패"));
        }
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

            LoginDTO loginUser = (LoginDTO) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            int registeredCount = registerErpForMstSeq(mstSeq, loginUser.getUserNo(), ErpRegistrationService.RegisterProfile.ADMIN);

            String message = "임대차 계약 등록이 완료되었습니다. (성공: " + registeredCount + "건)";

            try {
                System.out.println("=== ERP 등록 완료 메일 발송 시작 ===");
                DraftDTO draft = draftService.getDraftByMstSeq(mstSeq);
                if (draft != null) {
                    sendErpRegistrationCompleteEmail(draft);
                    System.out.println("=== ERP 등록 완료 메일 발송 성공 ===");
                }
            } catch (Exception e) {
                System.err.println("❌ ERP 등록 완료 메일 발송 실패: " + e.getMessage());
                e.printStackTrace();
            }

            return ResponseEntity.ok(Map.of(
                    "message", message,
                    "success_count", registeredCount,
                    "fail_count", 0,
                    "total_count", registeredCount
            ));

        } catch (Exception e) {
            System.err.println("❌ ERP 등록 실패: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "ERP 등록 실패: " + e.getMessage() + " (오류 발생으로 전체 등록이 취소되었습니다.)"
            ));
        }
    }

    /**
     * mst_seq 단위 ERP 등록 — Oracle 전체 성공 또는 전체 롤백
     */
    private int registerErpForMstSeq(String mstSeq, Integer crtUserNo, ErpRegistrationService.RegisterProfile profile) {
        System.out.println("=== 1단계: cust_code 업데이트 시작 ===");
        int updateCount = adminService.updateCustCodesAfterRegistration(mstSeq);
        System.out.println("cust_code 업데이트 완료: " + updateCount + "건");

        List<Map<String, Object>> draftDetails = adminService.getDraftDetailsByMstSeq(mstSeq);
        System.out.println("=== 조회된 draft 상세 데이터 ===");
        System.out.println("데이터 개수: " + (draftDetails != null ? draftDetails.size() : 0));

        int registeredCount = erpRegistrationService.registerAllLeases(draftDetails, crtUserNo, profile);

        adminService.updateDraftErpReg(mstSeq);
        System.out.println("=== draft 테이블 erp_reg 업데이트 완료 ===");

        return registeredCount;
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

    /**
     * ERP 등록 테스트 데이터 다중 삭제 (SEQ 기준)
     */
    @PostMapping("/erp/delete/multiple")
    @ResponseBody
    public ResponseEntity<?> deleteMultipleErpData(@RequestBody Map<String, Object> request) {
        try {
            @SuppressWarnings("unchecked")
            List<Object> seqListRaw = (List<Object>) request.get("seq_list");

            if (seqListRaw == null || seqListRaw.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "삭제할 SEQ가 없습니다."));
            }

            List<Integer> seqList = new java.util.ArrayList<>();
            for (Object seqObj : seqListRaw) {
                Integer seq = parseLeaseSeq(seqObj);
                if (seq != null) {
                    seqList.add(seq);
                }
            }

            if (seqList.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "유효한 SEQ가 없습니다."));
            }

            System.out.println("=== ERP 등록 데이터 다중 삭제 요청 ===");
            System.out.println("SEQ 개수: " + seqList.size());
            System.out.println("SEQ 목록: " + seqList);

            int successCount = 0;
            int failCount = 0;
            List<Map<String, Object>> failures = new java.util.ArrayList<>();

            for (Integer seq : seqList) {
                try {
                    System.out.println("=== ERP 등록 데이터 삭제 시작: SEQ=" + seq + " ===");
                    rentService.deleteLeaseData(seq);
                    successCount++;
                    System.out.println("✅ ERP 등록 데이터 삭제 완료: SEQ=" + seq);
                } catch (Exception e) {
                    failCount++;
                    System.err.println("❌ ERP 등록 데이터 삭제 실패: SEQ=" + seq + ", " + e.getMessage());
                    failures.add(Map.of(
                            "seq", seq,
                            "error", e.getMessage() != null ? e.getMessage() : "삭제 실패"
                    ));
                }
            }

            String message;
            if (successCount > 0 && failCount == 0) {
                message = "ERP 등록 데이터 " + successCount + "건이 삭제되었습니다.";
            } else if (successCount > 0) {
                message = "ERP 등록 데이터 삭제가 부분적으로 완료되었습니다.";
            } else {
                message = "ERP 등록 데이터 삭제에 실패했습니다.";
            }

            Map<String, Object> response = new java.util.HashMap<>();
            response.put("success", failCount == 0);
            response.put("message", message);
            response.put("success_count", successCount);
            response.put("fail_count", failCount);
            response.put("total_count", seqList.size());
            if (!failures.isEmpty()) {
                response.put("failures", failures);
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("❌ ERP 등록 데이터 다중 삭제 실패: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("error", "ERP 등록 데이터 삭제 실패: " + e.getMessage()));
        }
    }

    private Integer parseLeaseSeq(Object seqObj) {
        if (seqObj instanceof Integer) {
            Integer seq = (Integer) seqObj;
            return seq > 0 ? seq : null;
        }
        if (seqObj instanceof Number) {
            int seq = ((Number) seqObj).intValue();
            return seq > 0 ? seq : null;
        }
        if (seqObj instanceof String) {
            try {
                int seq = Integer.parseInt(((String) seqObj).trim());
                return seq > 0 ? seq : null;
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
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
            
            LoginDTO loginUser = (LoginDTO) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            int totalSuccessCount = 0;
            int totalFailCount = 0;

            for (String mstSeq : mstSeqList) {
                if (mstSeq == null || mstSeq.trim().isEmpty()) {
                    totalFailCount++;
                    continue;
                }
                try {
                    System.out.println("=== mst_seq 처리 시작: " + mstSeq + " ===");
                    int registeredCount = registerErpForMstSeq(mstSeq, loginUser.getUserNo(), ErpRegistrationService.RegisterProfile.ADMIN);
                    totalSuccessCount += registeredCount;
                    System.out.println("✅ mst_seq ERP 등록 완료: " + mstSeq + " (" + registeredCount + "건)");
                } catch (Exception e) {
                    totalFailCount++;
                    System.err.println("❌ mst_seq ERP 등록 실패(전체 롤백): " + mstSeq + " - " + e.getMessage());
                    e.printStackTrace();
                }
            }

            String message;
            if (totalSuccessCount > 0 && totalFailCount == 0) {
                message = "임대차 계약 등록이 완료되었습니다.";
            } else if (totalSuccessCount > 0) {
                message = "일부 기안서 ERP 등록에 실패했습니다. (실패한 기안서는 전체 롤백됨)";
            } else if (totalFailCount > 0) {
                message = "임대차 계약 등록에 실패했습니다. (오류 발생 기안서는 전체 롤백됨)";
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
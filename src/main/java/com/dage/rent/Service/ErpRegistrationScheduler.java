package com.dage.rent.Service;

import com.dage.rent.Component.Mail;
import com.dage.rent.DTO.LoginDTO;
import com.dage.rent.DTO.DraftDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class ErpRegistrationScheduler {

    @Autowired
    private AdminService adminService;
    
    @Autowired
    private RentService rentService;
    
    @Autowired
    private DraftService draftService;

    @Autowired
    private ErpRegistrationService erpRegistrationService;

    @Autowired
    private Mail mail;

    /**
     * ERP 등록 스케줄러 - 매 30분마다 실행
     * 조건: conf_status = 30 (완료) AND cust_reg_attempted = 'Y' AND erp_reg = 'N'
     */
    @Scheduled(fixedRate = 1800000, initialDelay = 300000) // 30분마다 실행, 처음 5분 후 시작 (1800000ms = 30분, 300000ms = 5분)
    public void processErpRegistration() {
        System.out.println("=== ERP 등록 스케줄러 시작 ===");
        
        try {
            // ERP 등록 대상 조회 (conf_status = 30 AND cust_reg_attempted = 'Y' AND erp_reg = 'N')
            List<Map<String, Object>> erpTargets = adminService.getErpRegistrationTargets();
            
            if (erpTargets == null || erpTargets.isEmpty()) {
                System.out.println("ERP 등록 대상 없음");
                return;
            }
            
            System.out.println("ERP 등록 대상 개수: " + erpTargets.size());
            
            int successCount = 0;
            int failCount = 0;
            
            // 각 대상에 대해 ERP 등록 처리
            for (Map<String, Object> target : erpTargets) {
                String mstSeq = (String) target.get("mst_seq");
                
                System.out.println("=== ERP 등록 처리 시작 ===");
                System.out.println("mst_seq: " + mstSeq);
                
                try {
                    // ERP 등록 시도 플래그 업데이트 (중복 처리 방지)
                    adminService.updateErpRegAttempted(mstSeq, "Y");
                    
                    // ERP 등록 처리
                    boolean success = processErpRegistrationForMstSeq(mstSeq);
                    
                    if (success) {
                        successCount++;
                        System.out.println("✅ ERP 등록 성공: " + mstSeq);
                    } else {
                        failCount++;
                        System.out.println("❌ ERP 등록 실패: " + mstSeq);
                    }
                    
                } catch (Exception e) {
                    failCount++;
                    System.err.println("❌ ERP 등록 처리 중 오류: " + mstSeq + ", 오류: " + e.getMessage());
                    e.printStackTrace();
                }
            }
            
            System.out.println("=== ERP 등록 스케줄러 완료 ===");
            System.out.println("성공: " + successCount + "건, 실패: " + failCount + "건");
            
        } catch (Exception e) {
            System.err.println("❌ ERP 등록 스케줄러 오류: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 특정 mst_seq에 대한 ERP 등록 처리
     */
    private boolean processErpRegistrationForMstSeq(String mstSeq) {
        try {
            // 0단계: draft 정보 조회 및 emp_no + user_nm으로 user_no 조회
            DraftDTO draft = draftService.getDraftByMstSeq(mstSeq);
            Integer defaultCrtUserNo = null; // 첫 번째 성공한 계약의 crtUserNo 저장용
            if (draft != null && draft.getEmp_no() > 0) { // emp_no는 int 타입이므로 0보다 큰지 확인
                try {
                    LoginDTO loginInfo = rentService.getUserinfo(draft.getEmp_no(), draft.getUser_nm());
                    if (loginInfo != null && loginInfo.getUserNo() > 0) {
                        defaultCrtUserNo = loginInfo.getUserNo();
                        System.out.println("=== draft emp_no(" + draft.getEmp_no() + "), user_nm(" + draft.getUser_nm() + ") -> user_no(" + defaultCrtUserNo + ") 조회 완료 ===");
                    } else {
                        System.err.println("⚠️ user_no 조회 실패: emp_no=" + draft.getEmp_no() + ", user_nm=" + draft.getUser_nm());
                    }
                } catch (Exception e) {
                    System.err.println("⚠️ emp_no/user_nm으로 user_no 조회 중 오류: emp_no=" + draft.getEmp_no() + ", user_nm=" + draft.getUser_nm() + " - " + e.getMessage());
                    e.printStackTrace();
                }
            }

            if (draft != null && draft.getEmp_no() > 0 && defaultCrtUserNo == null) {
                System.err.println("❌ ERP 등록 중단: crtUserNo 조회 실패 (emp_no=" + draft.getEmp_no() + ", user_nm=" + draft.getUser_nm() + ")");
                return false;
            }
            
            // 1단계: cust_code 업데이트
            System.out.println("=== 1단계: cust_code 업데이트 시작 ===");
            int updateCount = adminService.updateCustCodesAfterRegistration(mstSeq);
            System.out.println("cust_code 업데이트 완료: " + updateCount + "건");
            
            // 2단계: draft 상세 데이터 조회
            List<Map<String, Object>> draftDetails = adminService.getDraftDetailsByMstSeq(mstSeq);
            
            if (draftDetails == null || draftDetails.isEmpty()) {
                System.out.println("❌ draft 상세 데이터 없음");
                return false;
            }
            
            System.out.println("draft 상세 데이터 개수: " + draftDetails.size());

            // 3단계: 모든 계약을 하나의 Oracle 트랜잭션으로 등록 (하나라도 실패 시 전체 롤백)
            int registeredCount = erpRegistrationService.registerAllLeases(
                    draftDetails, defaultCrtUserNo, ErpRegistrationService.RegisterProfile.SCHEDULER);
            System.out.println("=== 임대차 계약 등록 완료: " + registeredCount + "건 ===");

            // 4단계: draft 테이블 erp_reg 업데이트 (Oracle 등록 전체 성공 시에만)
            adminService.updateDraftErpReg(mstSeq);
            System.out.println("=== draft 테이블 erp_reg 업데이트 완료 ===");
            

             try {
                 System.out.println("=== ERP 등록 완료 메일 발송 시작 ===");
                 draft = draftService.getDraftByMstSeq(mstSeq);
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

            
            return true;
            
        } catch (Exception e) {
            System.err.println("❌ ERP 등록 처리 실패: " + e.getMessage());
            e.printStackTrace();
            return false;
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

                String depositAmtStr = "";
                if (detail.getDepositAmt() != null && !detail.getDepositAmt().trim().isEmpty()) {
                    try {
                        depositAmtStr = String.format("%,d", Integer.parseInt(detail.getDepositAmt().trim()));
                    } catch (NumberFormatException e) {
                        depositAmtStr = detail.getDepositAmt();
                    }
                }
                html.append("<td style='padding: 12px; border: 1px solid #ddd;'>").append(depositAmtStr).append("원</td>");

                String rentAmtStr = "";
                if (detail.getRentAmt() != null && !detail.getRentAmt().trim().isEmpty()) {
                    try {
                        rentAmtStr = String.format("%,d", Integer.parseInt(detail.getRentAmt().trim()));
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
        html.append("<p style='margin: 5px 0 0 0; font-size: 12px; color: #7f8c8d;'>문의사항이 있으시면 강성민 매니저에게 연락해 주세요.</p>");
        html.append("</td>");
        html.append("</tr>");
        
        html.append("</table>");
        html.append("</body>");
        html.append("</html>");
        
        return html.toString();
    }
}

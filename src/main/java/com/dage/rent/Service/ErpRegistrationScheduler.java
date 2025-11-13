package com.dage.rent.Service;

import com.dage.rent.Component.Mail;
import com.dage.rent.DTO.LeaseProcedureDTO;
import com.dage.rent.DTO.LoginDTO;
import com.dage.rent.DTO.DraftDTO;
import com.dage.rent.Service.DraftService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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
            // 0단계: draft 정보 조회 및 emp_no로 user_no 조회
            DraftDTO draft = draftService.getDraftByMstSeq(mstSeq);
            Integer defaultCrtUserNo = null; // 첫 번째 성공한 계약의 crtUserNo 저장용
            if (draft != null && draft.getEmp_no() > 0) { // emp_no는 int 타입이므로 0보다 큰지 확인
                try {
                    // emp_no로 user_no 조회
                    LoginDTO loginInfo = rentService.getUserinfo(draft.getEmp_no());
                    if (loginInfo != null && loginInfo.getUserNo() > 0) {
                        defaultCrtUserNo = loginInfo.getUserNo();
                        System.out.println("=== draft emp_no(" + draft.getEmp_no() + ") -> user_no(" + defaultCrtUserNo + ") 조회 완료 ===");
                    } else {
                        System.err.println("⚠️ user_no 조회 실패: emp_no=" + draft.getEmp_no());
                    }
                } catch (Exception e) {
                    System.err.println("⚠️ emp_no로 user_no 조회 중 오류: " + e.getMessage());
                    e.printStackTrace();
                }
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
            
            // 3단계: 각 계약 상세에 대해 임대차 계약 등록
            int successCount = 0;
            int failCount = 0;
            boolean hasFailure = false; // 실패 여부 플래그
            
            for (Map<String, Object> detail : draftDetails) {
                try {
                    System.out.println("=== 임대차 계약 등록 시작 ===");
                    System.out.println("상세 데이터: " + detail);
                    
                    // 시퀀스 조회
                    String leaseSeq = rentService.getLeaseSeq();
                    String contNo = rentService.getLeaseContNo();
                    
                    System.out.println("생성된 leaseSeq: " + leaseSeq);
                    System.out.println("생성된 contNo: " + contNo);
                    
                    // 임대차 계약 등록 데이터 준비
                    LeaseProcedureDTO leaseData = new LeaseProcedureDTO();
                    
                    // crtUserNo 설정: 첫 번째 성공한 계약의 값을 사용하거나, draft의 emp_no 사용
                    Integer crtUserNo = defaultCrtUserNo;
                    
                    // 고정값들
                    leaseData.setArCompanyCode("100");
                    leaseData.setArSeq(Integer.parseInt(leaseSeq));
                    leaseData.setArCrtUserNo(crtUserNo); // draft의 emp_no 사용
                    leaseData.setArBLeaseCls("1");
                    leaseData.setArContNo(contNo);
                    leaseData.setArProjCode(detail.get("proj_code") != null ? detail.get("proj_code").toString() : null);
                    leaseData.setArContDt(detail.get("cont_date_s") != null ? detail.get("cont_date_s").toString() : null);
                    leaseData.setArExprDt(detail.get("cont_date_e") != null ? detail.get("cont_date_e").toString() : null);
                    leaseData.setArExprChgDt(detail.get("cont_date_e") != null ? detail.get("cont_date_e").toString() : null);
                    leaseData.setArLCustCode(detail.get("cust_code") != null ? Integer.parseInt(detail.get("cust_code").toString()) : null);
                    leaseData.setArZipcode(detail.get("post_code") != null ? detail.get("post_code").toString() : null);
                    leaseData.setArAddr1(detail.get("address") != null ? detail.get("address").toString() : null);
                    leaseData.setArAddr2(detail.get("address_d") != null ? detail.get("address_d").toString() : null);
                    leaseData.setArArea(detail.get("area") != null ? detail.get("area").toString() : null);
                    leaseData.setArUseType("10");
                    leaseData.setArDpstAmt(detail.get("deposit_amt") != null ? new BigDecimal(detail.get("deposit_amt").toString()) : null);
                    leaseData.setArMonRent(detail.get("rent_amt") != null ? new BigDecimal(detail.get("rent_amt").toString()) : null);
                     
                    leaseData.setArFinTag("F");
                    leaseData.setArRemarks(detail.get("bigo") != null ? detail.get("bigo").toString() : null);
                    leaseData.setArLCustName(detail.get("lessor_name") != null ? detail.get("lessor_name").toString() : null);
                    leaseData.setArHCustName("1");
                    leaseData.setArBankMainCode(null);
                    leaseData.setArBankMainName(null);
                    leaseData.setArAccno(null);
                    leaseData.setArAccOwner(null);
                    leaseData.setArDpstIntrRate(new BigDecimal("4.6"));
                    leaseData.setArCarNum(null);
                    leaseData.setArLCode("LR");
                    leaseData.setArCrncCode("KRW");
                    leaseData.setArFcDpstAmt(BigDecimal.ZERO);
                    leaseData.setArFcMonRent(BigDecimal.ZERO);
                    leaseData.setArContTitle(null);
                    leaseData.setArRsrcCode(detail.get("rsrc_code") != null ? detail.get("rsrc_code").toString() : null);
                    leaseData.setArLequType(null);
                    leaseData.setArInsuEndDt(null);
                    leaseData.setArChkEndDt(null);
                    leaseData.setArUsers(null);
                    leaseData.setArAutoExtYn(null);
                    leaseData.setArAutoExtLimit(null);
                    leaseData.setArExprLimit(null);
                    leaseData.setArExprSndDt(null);
                    leaseData.setArFinDt(detail.get("cont_date_e") != null ? detail.get("cont_date_e").toString() : null);
                    leaseData.setArDaymonCls("M");
                    leaseData.setArPayCycle(String.valueOf(1));
                    leaseData.setArPreLater("P");
                    leaseData.setArLrContNo(contNo);
                    
                    System.out.println("=== 임대차 계약 등록 데이터 ===");
                    System.out.println("leaseData: " + leaseData);

                    // 1단계: TIA_B_LEASE 테이블에 INSERT
                    rentService.insertLease(leaseData);
                    
                    // 2단계: cont_seq 생성
                    Integer contSeq = rentService.getLeaseContSeq();
                    System.out.println("=== 생성된 cont_seq: " + contSeq + " ===");
                    
                    // 3단계: TIA_B_LEASE_CONT 테이블에 INSERT
                    // crtUserNo가 null이면 첫 번째 성공한 계약의 값 사용
                    crtUserNo = leaseData.getArCrtUserNo();
                    if (crtUserNo == null) {
                        crtUserNo = defaultCrtUserNo; // 첫 번째 성공한 계약의 값 또는 draft의 emp_no
                        System.out.println("⚠️ crtUserNo가 null이어서 기본값 사용: " + crtUserNo);
                    }
                    
                    Map<String, Object> leaseContData = new java.util.HashMap<>();
                    leaseContData.put("companyCode", leaseData.getArCompanyCode());
                    leaseContData.put("seq", leaseData.getArSeq());
                    leaseContData.put("contSeq", contSeq);
                    leaseContData.put("crtUserNo", crtUserNo);
                    leaseContData.put("contName", "1차 계약");
                    leaseContData.put("chgDt", leaseData.getArExprDt());
                    leaseContData.put("dpstAmt", leaseData.getArDpstAmt());
                    leaseContData.put("monRent", leaseData.getArMonRent());
                    leaseContData.put("remarks", leaseData.getArRemarks());
                    leaseContData.put("contBaseDt", leaseData.getArExprDt());
                    leaseContData.put("exprDt", leaseData.getArExprDt());
                    leaseContData.put("crncCode", leaseData.getArCrncCode());
                    
                    rentService.insertLeaseCont(leaseContData);
                    
                    // 4단계: TIA_B_LEASE_TRAN_DEPOSIT 테이블에 INSERT
                    Map<String, Object> tranData = new java.util.HashMap<>();
                    tranData.put("companyCode", leaseData.getArCompanyCode());
                    tranData.put("seq", leaseData.getArSeq());
                    tranData.put("contSeq", contSeq);
                    tranData.put("userNo", crtUserNo); // 누락된 userNo 추가
                    tranData.put("tranSeq", 1);
                    tranData.put("tranDt", leaseData.getArContDt());
                    tranData.put("tranAmt", leaseData.getArDpstAmt());
                    tranData.put("tranType", "D");
                    tranData.put("tranDesc", "보증금");
                    
                    rentService.insertLeaseTranDeposit(tranData);
                    
                    // 5단계: TIA_B_LEASE_INTR 테이블에 INSERT
                    Map<String, Object> intrData = new java.util.HashMap<>();
                    intrData.put("companyCode", leaseData.getArCompanyCode());
                    intrData.put("seq", leaseData.getArSeq());
                    intrData.put("contSeq", contSeq);
                    intrData.put("userNo", crtUserNo); // 누락된 userNo 추가
                    intrData.put("intrSeq", 1);
                    intrData.put("intrRate", leaseData.getArDpstIntrRate());
                    intrData.put("intrDt", leaseData.getArContDt());
                    intrData.put("intrDesc", "보증금 이자");
                    
                    rentService.insertLeaseIntr(intrData);
                    
                    System.out.println("✅ 임대차 계약 등록 완료: " + contNo);
                    successCount++;
                    
                    // 첫 번째 성공한 계약의 crtUserNo 저장 (나중에 실패한 계약에서 사용)
                    if (defaultCrtUserNo == null && crtUserNo != null) {
                        defaultCrtUserNo = crtUserNo;
                        System.out.println("✅ 첫 번째 성공한 계약의 crtUserNo 저장: " + defaultCrtUserNo);
                    }
                    
                } catch (Exception e) {
                    failCount++;
                    hasFailure = true;
                    System.err.println("❌ 임대차 계약 등록 실패: " + e.getMessage());
                    e.printStackTrace();
                }
            }
            
            System.out.println("=== 임대차 계약 등록 결과 ===");
            System.out.println("성공: " + successCount + "건, 실패: " + failCount + "건");
            
            // 하나라도 실패하면 전체 실패로 처리
            if (hasFailure) {
                System.err.println("❌ 일부 계약 등록 실패로 인해 ERP 등록 실패 처리");
                return false;
            }
            
            // 4단계: draft 테이블 erp_reg 업데이트 (모두 성공했을 때만)
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
                html.append("<td style='padding: 12px; border: 1px solid #ddd;'>").append(detail.getDepositAmt() != null ? String.format("%,d", detail.getDepositAmt()) : "").append("원</td>");
                html.append("<td style='padding: 12px; border: 1px solid #ddd;'>").append(detail.getRentAmt() != null ? String.format("%,d", detail.getRentAmt()) : "").append("원</td>");
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

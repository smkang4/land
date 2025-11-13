package com.dage.rent.Service;

import com.dage.rent.Component.Mail;
import com.dage.rent.DTO.ApprovalDTO;
import com.dage.rent.DTO.DraftDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CustomerRegistrationScheduler {
    
    @Autowired
    private ApprovalService approvalService;
    
    @Autowired
    private DraftService draftService;
    
    @Autowired
    private RentService rentService;

    @Autowired
    private Mail mail;
    
    /**
     * 2차: 스케줄러로 놓친 거래처 등록 처리
     * 30분마다 실행
     */
    @Scheduled(fixedDelay = 1800000, initialDelay = 300000) // 30분마다 실행, 처음 5분 후 시작 (1800000ms = 30분, 300000ms = 5분)
    public void processMissedCustomerRegistrations() {
        try {
            System.out.println("=== 스케줄러: 거래처 등록 체크 시작 ===");
            
            // 거래처 등록이 필요한 기안서 목록 조회
            HashMap<String, Object> map = new HashMap<>();
            // user_no 조건 없이 전체 조회
            List<ApprovalDTO> pendingList = approvalService.getPendingCustomerRegistrations(map);
            
            if (pendingList != null && !pendingList.isEmpty()) {
                System.out.println("거래처 등록 대상 발견: " + pendingList.size() + "건");
                
                for (ApprovalDTO dto : pendingList) {
                    try {
                        // 거래처 등록 조건 체크
                        if (shouldRegisterCustomer(dto)) {
                            System.out.println("스케줄러 거래처 등록 처리 - draftId: " + dto.getDraft_id() + 
                                             ", mstSeq: " + dto.getMst_seq() + 
                                             ", confStatus: " + dto.getConf_status());
                            
                            // 거래처 등록 처리
                            processCustomerRegistration(dto);
                        }
                    } catch (Exception e) {
                        System.err.println("스케줄러 거래처 등록 처리 중 오류 - draftId: " + dto.getDraft_id() + 
                                         ", 오류: " + e.getMessage());
                    }
                }
            } else {
                System.out.println("거래처 등록 대상 없음");
            }
            
            System.out.println("=== 스케줄러: 거래처 등록 체크 완료 ===");
            
        } catch (Exception e) {
            System.err.println("스케줄러 실행 중 오류: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 거래처 등록이 필요한지 체크
     * @param dto 기안서 정보
     * @return 거래처 등록 필요 여부
     */
    private boolean shouldRegisterCustomer(ApprovalDTO dto) {
        String confStatus = dto.getConf_status();
        String custRegAttempted = dto.getCust_reg_attempted();
        
        // 조건: 결재상태가 완료(30)이고, 거래처 등록을 아직 시도하지 않은 경우만 처리
        boolean statusOk = "30".equals(confStatus); // 결재완료만 처리
        boolean notAttempted = custRegAttempted == null || !"Y".equals(custRegAttempted);
        
        System.out.println("거래처 등록 체크 - draftId: " + dto.getDraft_id() + 
                          ", confStatus: " + confStatus + 
                          ", custRegAttempted: " + custRegAttempted + 
                          ", 등록필요: " + (statusOk && notAttempted));
        
        return statusOk && notAttempted;
    }
    
    /**
     * 거래처 등록 처리
     * @param dto 기안서 정보
     */
    private void processCustomerRegistration(ApprovalDTO dto) {
        try {
            // 거래처 등록 시도 플래그 업데이트
            approvalService.updateCustomerRegistrationAttempted(dto.getDraft_id(), "Y");
            
            System.out.println("스케줄러 거래처 등록 시도 플래그 업데이트 완료 - draftId: " + dto.getDraft_id());
            
            // 실제 거래처 등록 프로시저 호출
            callCustomerRegistrationProcedure(dto);
            
        } catch (Exception e) {
            System.err.println("스케줄러 거래처 등록 처리 중 오류 - draftId: " + dto.getDraft_id() + 
                             ", 오류: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 거래처 등록 프로시저 호출
     * @param dto 기안서 정보
     */
    private void callCustomerRegistrationProcedure(ApprovalDTO dto) {
        try {
            System.out.println("=== 스케줄러: 거래처 등록 프로시저 호출 시작 ===");
            System.out.println("draftId: " + dto.getDraft_id());
            
            // 1. draft_id로 기안서 정보 조회
            int draftId = Integer.parseInt(String.valueOf(dto.getDraft_id()));
            DraftDTO draft = draftService.getDraftById(draftId);
            
            if (draft == null) {
                System.err.println("기안서를 찾을 수 없습니다. draftId: " + draftId);
                return;
            }
            
            // 2. 계약 상세 정보 조회
            List<DraftDTO.ContractDetailDTO> contractDetails = draft.getContractDetails();
            if (contractDetails == null || contractDetails.isEmpty()) {
                System.err.println("계약 상세 정보가 없습니다. draftId: " + draftId);
                return;
            }
            
            // 3. 각 계약 상세에 대해 거래처 등록 프로시저 호출
            boolean hasNewCustomer = false; // 신규 거래처 등록 여부
            boolean hasFailure = false; // 거래처 등록 실패 여부
            
            for (DraftDTO.ContractDetailDTO detail : contractDetails) {
                try {
                    // 기존 거래처인지 체크 (custCode가 있으면 기존 거래처)
                    String custCode = detail.getCustCode();
                    
                    // custCode가 없거나 빈 값이면 신규 거래처 등록 필요
                    if (custCode == null || custCode.trim().isEmpty()) {
                        System.out.println("신규 거래처 등록 필요 - rowNo: " + detail.getRowNo());
                        
                        // ERP 데이터 준비
                        Map<String, Object> erpData = prepareErpData(draft, detail);
                        
                        if (erpData != null && erpData.get("custCode") != null) {
                            // 거래처 등록 프로시저 호출
                            System.out.println("거래처 등록 프로시저 호출 - custCode: " + erpData.get("custCode"));
                            rentService.callCustProjProcedure(erpData);
                            System.out.println("✅ 거래처 등록 프로시저 호출 성공!");
                            hasNewCustomer = true; // 신규 거래처 등록됨
                        } else {
                            System.err.println("ERP 데이터 준비 실패 - custCode가 없습니다.");
                            hasFailure = true; // 데이터 준비 실패도 실패로 간주
                        }
                    } else {
                        System.out.println("기존 거래처 사용 - custCode: " + custCode);
                    }
                    
                } catch (Exception e) {
                    System.err.println("거래처 등록 프로시저 호출 중 오류 - rowNo: " + detail.getRowNo());
                    e.printStackTrace();
                    hasFailure = true; // 예외 발생 시 실패로 표시
                }
            }
            
            System.out.println("=== 스케줄러: 거래처 등록 프로시저 호출 완료 ===");
            
            // 4. 신규 거래처가 등록되었고 실패가 없을 때만 메일 발송
            if (hasNewCustomer && !hasFailure) {
                try {
                    System.out.println("=== 거래처 등록 완료 메일 발송 시작 ===");
                    sendDraftNotificationEmail(draft);
                    System.out.println("=== 거래처 등록 완료 메일 발송 성공 ===");
                } catch (Exception e) {
                    System.err.println("메일 발송 실패: " + e.getMessage());
                    e.printStackTrace();
                }
            }
            
        } catch (Exception e) {
            System.err.println("거래처 등록 프로시저 호출 중 오류: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 거래처 등록 프로시저를 위한 ERP 데이터 준비
     */
    private Map<String, Object> prepareErpData(DraftDTO draft, DraftDTO.ContractDetailDTO detail) {
        try {
            Map<String, Object> erpData = new HashMap<>();
            
            // custCode 생성 (Oracle에서 생성된 코드)
            String custCode = rentService.getCustCode();
            
            // 기본 프로젝트 정보
            erpData.put("custCode", custCode);
            erpData.put("crtUserNo", String.valueOf(draft.getEmp_no()));
            erpData.put("reqEmpNo", String.valueOf(draft.getEmp_no()));
            erpData.put("projCode", draft.getProj_code());
            
            // 거래처 정보 (임대인 정보에서 가져오기)
            String lessorName = detail.getLessorName() != null ? detail.getLessorName() : "";
            
            // 문자열 길이 제한 (Oracle 컬럼 크기에 맞춤)
            String custName = lessorName.length() > 60 ? lessorName.substring(0, 60) : lessorName;
            String bossName = lessorName.length() > 16 ? lessorName.substring(0, 16) : lessorName;
            
            erpData.put("custName", custName);          // 최대 60자
            erpData.put("bizNo", detail.getLessor());
            erpData.put("bossName", bossName);          // 최대 16자
            erpData.put("tradeCls", "1");               // 1자 (기본값: "1")
            erpData.put("bizCond", "임대");             // 기본값
            erpData.put("bizKnd", "임대업");            // 기본값
            
            // 주소 정보 (간소화)
            erpData.put("zipCode", "");
            erpData.put("addr1", "");
            erpData.put("addr2", "");
            
            // 연락처 정보
            erpData.put("telNo", "");
            erpData.put("headFax", "");
            
            // 계좌 정보
            erpData.put("bankMainCode", "");
            erpData.put("bankCode", "");
            erpData.put("custAccNo", detail.getLessorAccount() != null ? detail.getLessorAccount() : "");
            erpData.put("elctTag", "");
            erpData.put("cOwner", bossName);  // 대표자명과 동일 (16자 제한)
            erpData.put("remark", draft.getRent_reason() != null ? draft.getRent_reason() : "");
            erpData.put("taxCls", "10");     // 2자 이하 (기본값: "10")
            erpData.put("representCustCode", "");
            erpData.put("sBankNo", "");
            erpData.put("regCls", "1");
            
            // 기존 거래처 체크를 위한 필드
            erpData.put("existing_cust_code", ""); // 빈 값으로 설정하여 신규 거래처로 처리
            
            // 프로젝트 정보 추가
            erpData.put("makeProj", draft.getProj_code());
            erpData.put("makeDt", "");
            
            System.out.println("스케줄러 ERP 데이터 준비 완료: " + erpData);
            return erpData;
            
        } catch (Exception e) {
            System.err.println("스케줄러 ERP 데이터 준비 실패: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * 거래처 등록 완료 메일 발송
     */
    private void sendDraftNotificationEmail(DraftDTO draft) {
        try {
            // 수신자 설정
//            String[] recipients = {
//                "smkang@dage.co.kr"
//            };

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
            String subject = "[긴급] 거래처 승인 요청 - 부동산임차관리플랫폼";
            
            // 메일 내용 HTML 생성
            String htmlContent = generateDraftNotificationHtml(draft);
            
            // 메일 발송
            mail.sendEmail(recipients, ccRecipients, bccRecipients, subject, htmlContent);
            
            System.out.println("거래처 승인 요청 메일 발송 성공");
            
        } catch (Exception e) {
            System.err.println("거래처 승인 요청 메일 발송 실패: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 거래처 승인 요청 메일 HTML 생성
     */
    private String generateDraftNotificationHtml(DraftDTO draft) {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>");
        html.append("<html lang='ko'>");
        html.append("<head>");
        html.append("<meta charset='UTF-8'>");
        html.append("<title>거래처 승인 요청</title>");
        html.append("</head>");
        html.append("<body style='font-family: Arial, sans-serif; line-height: 1.6; color: #333; background-color: #f5f5f5; margin: 0; padding: 20px;'>");
        html.append("<table width='800' cellpadding='0' cellspacing='0' style='background-color: white; border-radius: 10px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); margin: 0 auto;'>");
        
        // 헤더
        html.append("<tr>");
        html.append("<td style='background-color: #2c3e50; color: white; padding: 20px; text-align: center; border-radius: 10px 10px 0 0;'>");
        html.append("<p style='margin: 0; font-size: 18px; font-weight: bold;'>거래처 승인 요청 알림</p>");
        html.append("<p style='margin: 10px 0 0 0; font-size: 14px;'>부동산 임차관리 플랫폼에서 신규 거래처 등록을 요청하였습니다.</p>");
        html.append("<p style='margin: 5px 0 0 0; font-size: 14px;'>ERP 확인 후 빠른 처리 바랍니다.</p>");
        html.append("</td>");
        html.append("</tr>");
        
        // 내용
        html.append("<tr>");
        html.append("<td style='padding: 30px;'>");
        html.append("<table width='100%' cellpadding='0' cellspacing='0' style='border-collapse: collapse;'>");
        html.append("<tr><td style='padding: 12px; background-color: #f8f9fa; font-weight: bold; width: 30%; border: 1px solid #ddd;'>기안자</td><td style='padding: 12px; border: 1px solid #ddd;'>").append(draft.getUser_nm() != null ? draft.getUser_nm() : "").append("</td></tr>");
        html.append("<tr><td style='padding: 12px; background-color: #f8f9fa; font-weight: bold; border: 1px solid #ddd;'>현장명</td><td style='padding: 12px; border: 1px solid #ddd;'>").append(draft.getProj_name() != null ? draft.getProj_name() : "").append("</td></tr>");
        html.append("</table>");
        
        // 계약 상세 정보 (신규 거래처만 표시)
        if (draft.getContractDetails() != null && !draft.getContractDetails().isEmpty()) {
            // 신규 거래처만 필터링
            boolean hasNewCustomerDetail = false;
            
            html.append("<table width='100%' cellpadding='0' cellspacing='0' style='border-collapse: collapse; margin-top: 20px;'>");
            html.append("<tr>");
            html.append("<td style='background-color: #3498db; color: white; padding: 12px; text-align: center; border: 1px solid #ddd; font-weight: bold;'>유형</td>");
            html.append("<td style='background-color: #3498db; color: white; padding: 12px; text-align: center; border: 1px solid #ddd; font-weight: bold;'>주소</td>");
            html.append("<td style='background-color: #3498db; color: white; padding: 12px; text-align: center; border: 1px solid #ddd; font-weight: bold;'>임대인</td>");
            html.append("<td style='background-color: #3498db; color: white; padding: 12px; text-align: center; border: 1px solid #ddd; font-weight: bold;'>사업자번호</td>");
            html.append("</tr>");
            
            for (DraftDTO.ContractDetailDTO detail : draft.getContractDetails()) {
                // custCode가 없는 경우만 표시 (신규 거래처)
                String custCode = detail.getCustCode();
                if (custCode == null || custCode.trim().isEmpty()) {
                    html.append("<tr>");
                    html.append("<td style='padding: 12px; text-align: center; border: 1px solid #ddd;'>").append(detail.getType() != null ? detail.getType() : "").append("</td>");
                    html.append("<td style='padding: 12px; text-align: left; border: 1px solid #ddd;'>").append(detail.getAddress() != null ? detail.getAddress() : "").append("</td>");
                    html.append("<td style='padding: 12px; text-align: center; border: 1px solid #ddd;'>").append(detail.getLessorName() != null ? detail.getLessorName() : "").append("</td>");
                    html.append("<td style='padding: 12px; text-align: center; border: 1px solid #ddd;'>").append(detail.getLessor() != null ? detail.getLessor() : "").append("</td>");
                    html.append("</tr>");
                    hasNewCustomerDetail = true;
                }
            }
            
            // 신규 거래처가 없으면 안내 메시지
            if (!hasNewCustomerDetail) {
                html.append("<tr>");
                html.append("<td colspan='4' style='padding: 20px; text-align: center; color: #999; border: 1px solid #ddd;'>신규 거래처가 없습니다.</td>");
                html.append("</tr>");
            }
            
            html.append("</table>");
        }
        
        // 푸터
        html.append("<p style='margin-top: 30px; color: #666; font-size: 14px; text-align: center;'>이 메일은 부동산 임차관리 플랫폼에서 자동으로 발송되었습니다.</p>");
        html.append("</td>");
        html.append("</tr>");
        html.append("</table>");
        html.append("</body>");
        html.append("</html>");
        
        return html.toString();
    }
}




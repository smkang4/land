package com.dage.rent.Service;

import com.dage.rent.Component.Mail;
import com.dage.rent.Controller.ApprovalController;
import com.dage.rent.DAO.mysql.ApprovalDAO;
import com.dage.rent.DTO.ApprovalCcDTO;
import com.dage.rent.DTO.ApprovalDTO;
import com.dage.rent.DTO.EmpUserDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ApprovalNotificationService {

    private static final String DEFAULT_TITLE = "[동아지질] 현장숙소관리플랫폼 사전조사 확인요청의 건";

    private final Mail mail;
    private final RentService rentService;
    private final AdminService adminService;
    private final ApprovalDAO approvalDAO;

    public void sendAfterAction(ApprovalDTO nextAppr, int apprNo) {
        if (nextAppr == null) {
            return;
        }
        try {
            String sendType = nextAppr.getNext_send_type();
            String apprTg = nextAppr.getAppr_tg();
            int nextEmpNo = nextAppr.getNext_emp_no();
            int firstEmpNo = nextAppr.getFirst_appr_emp_no();
            String nextEmpNm = nextAppr.getNext_appr_emp_nm();
            String firstEmpNm = nextAppr.getFirst_appr_emp_nm();

            String title = DEFAULT_TITLE;
            List<String> toList = new ArrayList<>();

            if ("F".equals(apprTg)) {
                addEmailIfPresent(toList, firstEmpNo, firstEmpNm);
                title = "[동아지질:관리부서 반려] 현장숙소관리플랫폼 사전조사 확인반려의 건";
            } else if ("B".equals(sendType)) {
                for (EmpUserDTO admin : adminService.getAllAdmins()) {
                    if (admin.getEmail() != null && !admin.getEmail().isEmpty() && "Y".equals(admin.getEmail_chk())) {
                        toList.add(admin.getEmail());
                    }
                }
            } else if ("A".equals(sendType)) {
                addEmailIfPresent(toList, nextEmpNo, nextEmpNm);
            } else {
                addEmailIfPresent(toList, firstEmpNo, firstEmpNm);
                title = "[동아지질:관리부서 확인완료] 현장숙소관리플랫폼 사전조사 확인완료의 건";
            }

            if (toList.isEmpty()) {
                return;
            }

            String html = ApprovalController.confirmMailHtml(nextAppr);
            String[] cc = resolveCcEmails(apprNo);
            mail.sendEmail(toList.toArray(new String[0]), cc, new String[0], title, html);
        } catch (Exception e) {
            // 알림메일 발송 실패가 결재 승인/반려 트랜잭션 자체를 롤백시키면 안 됨
            System.err.println("결재 알림메일 발송 실패 - apprNo: " + apprNo + ", 오류: " + e.getMessage());
        }
    }

    public void sendOnSubmit(ApprovalDTO appr, int apprNo, int submitterEmpNo,
                             int fieldApproverEmpNo, String fieldApproverNm) {
        if (appr == null) {
            return;
        }
        try {
            String title = "[동아지질] 부동산 임차관리 플랫폼 사전조사 확인요청의 건";
            List<String> toList = new ArrayList<>();

            if (submitterEmpNo == fieldApproverEmpNo) {
                for (EmpUserDTO admin : adminService.getAllAdmins()) {
                    if (admin.getEmail() != null && !admin.getEmail().isEmpty() && "Y".equals(admin.getEmail_chk())) {
                        toList.add(admin.getEmail());
                    }
                }
            } else {
                addEmailIfPresent(toList, fieldApproverEmpNo, fieldApproverNm);
            }

            if (toList.isEmpty()) {
                return;
            }

            String html = ApprovalController.confirmMailHtml(appr);
            String[] cc = resolveCcEmails(apprNo);
            mail.sendEmail(toList.toArray(new String[0]), cc, new String[0], title, html);
        } catch (Exception e) {
            // 알림메일 발송 실패가 결재 상신 트랜잭션 자체를 롤백시키면 안 됨
            System.err.println("결재 상신 알림메일 발송 실패 - apprNo: " + apprNo + ", 오류: " + e.getMessage());
        }
    }

    private String[] resolveCcEmails(int apprNo) {
        List<ApprovalCcDTO> ccList = approvalDAO.selectCcByApprNo(apprNo);
        List<String> emails = new ArrayList<>();
        for (ApprovalCcDTO cc : ccList) {
            addEmailIfPresent(emails, cc.getEmpNo(), cc.getEmpNm());
        }
        return emails.toArray(new String[0]);
    }

    private void addEmailIfPresent(List<String> list, int empNo, String userNm) {
        EmpUserDTO emp = rentService.getEmpUserInfo(empNo, blankToNull(userNm));
        if (emp != null && emp.getEmail() != null && !emp.getEmail().trim().isEmpty()) {
            list.add(emp.getEmail().trim());
        }
    }

    private static String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}

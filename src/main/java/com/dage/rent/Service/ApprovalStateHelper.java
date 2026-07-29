package com.dage.rent.Service;

/**
 * approval_m.appr_stat / appr_admin 해석 (DB 컬럼 변경 없음)
 */
public final class ApprovalStateHelper {

    private ApprovalStateHelper() {
    }

    public static String formatStatLabel(String apprStat, String apprAdmin) {
        if (apprStat == null) {
            return "요청전";
        }
        switch (apprStat) {
            case "3":
                return "완료";
            case "4":
                return "반려";
            case "2":
                if ("T".equals(apprAdmin)) {
                    return "접수대기";
                }
                return "진행";
            case "0":
            case "1":
                return "진행";
            default:
                return "요청";
        }
    }
}

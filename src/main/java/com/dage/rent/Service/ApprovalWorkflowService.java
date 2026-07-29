package com.dage.rent.Service;

import com.dage.rent.DAO.mysql.ApprovalDAO;
import com.dage.rent.DTO.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ApprovalWorkflowService {

    private static final String TEMPLATE_ADMIN_DEFAULT = "ADMIN_DEFAULT";

    private final ApprovalDAO approvalDAO;
    private final ApprovalService approvalService;
    private final ContractService contractService;
    private final RentService rentService;
    private final ApprovalNotificationService notificationService;

    @Transactional("mysqlTransactionManager")
    public Map<String, Object> submit(int submitterEmpNo, String submitterName, String submitterPosition,
                                      List<String> contractSeqs, List<ApprovalLineSubmitDTO> approvalLines,
                                      List<ApprovalCcDTO> ccList) {
        if (approvalLines == null || approvalLines.isEmpty()) {
            throw new IllegalArgumentException("결재선을 1명 이상 지정해 주세요.");
        }

        List<ApprovalLineSubmitDTO> fieldLines = new ArrayList<>();
        for (ApprovalLineSubmitDTO line : approvalLines) {
            if (line.getEmpNo() <= 0) {
                continue;
            }
            if (!"ADMIN".equalsIgnoreCase(line.getPhase())) {
                fieldLines.add(line);
            }
        }
        if (fieldLines.isEmpty()) {
            throw new IllegalArgumentException("현장 결재자를 1명 이상 선택해 주세요.");
        }

        boolean selfFieldOnly = fieldLines.size() == 1 && fieldLines.get(0).getEmpNo() == submitterEmpNo;

        ApprovalMDTO master = new ApprovalMDTO();
        master.setAppr_emp_no(submitterEmpNo);
        master.setAppr_emp_nm(submitterName);
        if (selfFieldOnly) {
            master.setAppr_stat("2");
            master.setAppr_admin("T");
        } else {
            master.setAppr_stat("0");
        }
        approvalService.insertApprovalMaster(master);
        int apprNo = master.getAppr_no();

        LoginDTO submitter = resolveUser(submitterEmpNo, submitterName);
        ApprovalDDTO drafter = buildLine(apprNo, submitterEmpNo, submitter, submitterName, submitterPosition,
                "A", 0, "T", selfFieldOnly ? "T" : "F", null);
        approvalService.insertApprovalDetail(drafter);

        List<ApprovalLineSubmitDTO> effectiveField = new ArrayList<>();
        for (ApprovalLineSubmitDTO fl : fieldLines) {
            if (fl.getEmpNo() != submitterEmpNo) {
                effectiveField.add(fl);
            }
        }
        int apprNum = 1;
        if (!selfFieldOnly) {
            for (int i = 0; i < effectiveField.size(); i++) {
                ApprovalLineSubmitDTO fl = effectiveField.get(i);
                boolean lastField = (i == effectiveField.size() - 1);
                LoginDTO user = resolveUser(fl.getEmpNo(), fl.getEmpNm());
                ApprovalDDTO row = buildLine(apprNo, fl.getEmpNo(), user, fl.getEmpNm(), null, "A", apprNum++, "N",
                        lastField ? "T" : "F", null);
                approvalService.insertApprovalDetail(row);
            }
        }

        contractService.updateContractMasterApprNo(apprNo, contractSeqs);
        saveCcList(apprNo, ccList, submitterEmpNo);
        if (selfFieldOnly) {
            approvalService.updateAdminTag(apprNo, "T");
        }

        int mailTargetEmp = fieldLines.get(0).getEmpNo();
        String mailTargetNm = fieldLines.get(0).getEmpNm();
        for (ApprovalLineSubmitDTO fl : fieldLines) {
            if (fl.getEmpNo() != submitterEmpNo) {
                mailTargetEmp = fl.getEmpNo();
                mailTargetNm = fl.getEmpNm();
                break;
            }
        }
        int dSeq = contractService.getMaxDseq(apprNo);
        ApprovalDTO mailCtx = approvalService.getApprovalDetailOne(dSeq);
        notificationService.sendOnSubmit(mailCtx, apprNo, submitterEmpNo, mailTargetEmp, mailTargetNm);

        return Map.of("msg", "승인요청이 완료되었습니다.");
    }

    public List<ApprovalTemplateStepDTO> getTemplateSteps(String templateCode) {
        return approvalDAO.selectTemplateSteps(templateCode);
    }

    @Transactional("mysqlTransactionManager")
    public Map<String, Object> processConfirm(String gubun, int dSeq, int empNo, String empNm, int apprNo, int apprNum,
                                              String nextEmpUser, String nextEmpNm, String banRemark,
                                              List<ApprovalCcDTO> ccList) {
        ApprovalDDTO line = new ApprovalDDTO();

        if ("T".equals(gubun)) {
            line.setAppr_no(apprNo);
            line.setAppr_num(apprNum);
            line.setD_seq(dSeq);
            line.setAppr_remarks(banRemark);
            line.setAppr_tg("T");
            approvalService.updateApprovalDetail(line);
            updateMasterStatus(apprNo, "2", "");
        } else if ("A".equals(gubun)) {
            dSeq = receiveAdmin(apprNo, apprNum, empNo, empNm, nextEmpUser, nextEmpNm, banRemark);
            saveCcList(apprNo, ccList, empNo);
        } else if ("F".equals(gubun)) {
            line.setAppr_no(apprNo);
            line.setAppr_num(apprNum);
            line.setD_seq(dSeq);
            line.setAppr_remarks(banRemark);
            line.setAppr_tg("F");
            approvalService.updateApprovalDetail(line);
            updateMasterStatus(apprNo, "4", banRemark);
        } else {
            insertRejectLine(apprNo, apprNum, empNo, empNm, banRemark);
            updateMasterStatus(apprNo, "4", banRemark);
        }

        syncAdminTag(dSeq);
        finishIfComplete(apprNo);

        ApprovalDTO nextAppr = approvalService.getApprovalDetailOne(dSeq);
        notificationService.sendAfterAction(nextAppr, apprNo);

        return Map.of("msg", "승인요청이 완료되었습니다.");
    }

    public List<ApprovalCcDTO> getCcList(int apprNo) {
        return approvalDAO.selectCcByApprNo(apprNo);
    }

    public boolean isCcOnlyViewer(int apprNo, int empNo) {
        return approvalDAO.countCcOnlyViewer(apprNo, empNo) > 0;
    }

    private int receiveAdmin(int apprNo, int apprNum, int receiverEmpNo, String receiverNm,
                             String nextEmpUser, String nextEmpNm, String remarks) {
        if (approvalDAO.countDetailByGroup(apprNo, "B") > 0) {
            approvalService.updateAdminTag(apprNo, "F");
            return contractService.getMaxDseq(apprNo);
        }

        LoginDTO receiver = resolveUser(receiverEmpNo, receiverNm);
        int nextNum = apprNum + 1;

        ApprovalDDTO received = buildLine(apprNo, receiverEmpNo, receiver, receiverNm, null, "B", nextNum, "T", "F", remarks);
        approvalService.insertApprovalDetail(received);
        int dSeq = received.getD_seq();
        nextNum++;

        int nextEmp = parseIntOrZero(nextEmpUser);
        if (nextEmp > 0 && nextEmp != receiverEmpNo) {
            LoginDTO nextUser = resolveUser(nextEmp, nextEmpNm);
            ApprovalDDTO nextLine = buildLine(apprNo, nextEmp, nextUser, nextEmpNm, null, "B", nextNum, "N", "F", null);
            approvalService.insertApprovalDetail(nextLine);
            nextNum++;
        }

        List<ApprovalTemplateStepDTO> steps = approvalDAO.selectTemplateSteps(TEMPLATE_ADMIN_DEFAULT);
        for (ApprovalTemplateStepDTO step : steps) {
            ApprovalDDTO tpl = new ApprovalDDTO();
            tpl.setAppr_no(apprNo);
            tpl.setAppr_emp_no(step.getEmpNo());
            tpl.setAppr_emp_nm(step.getEmpNm());
            tpl.setAppr_position(step.getPositionNm());
            tpl.setAppr_group("B");
            tpl.setAppr_num(nextNum++);
            tpl.setAppr_tg("N");
            tpl.setLast_tag("T".equals(step.getIsFinal()) ? "T" : "F");
            approvalService.insertApprovalDetail(tpl);
        }

        approvalService.updateAdminTag(apprNo, "F");
        return dSeq;
    }

    private void insertRejectLine(int apprNo, int apprNum, int empNo, String empNm, String remarks) {
        LoginDTO emp = resolveUser(empNo, empNm);
        ApprovalDDTO line = buildLine(apprNo, empNo, emp, empNm, null, "B", apprNum + 1, "F", "F", remarks);
        approvalService.insertApprovalDetail(line);
    }

    private LoginDTO resolveUser(int empNo, String userNm) {
        LoginDTO user = rentService.getUserinfo(empNo, blankToNull(userNm));
        if (user == null && blankToNull(userNm) != null) {
            // 이름 불일치 시 사번만 재조회하지 않음(동일 emp_no 오매칭 방지). 화면에서 받은 이름으로만 사용
            return null;
        }
        return user;
    }

    private ApprovalDDTO buildLine(int apprNo, int empNo, LoginDTO user, String fallbackNm, String fallbackPos,
                                   String group, int num, String tg, String lastTag, String remarks) {
        ApprovalDDTO dto = new ApprovalDDTO();
        dto.setAppr_no(apprNo);
        dto.setAppr_emp_no(empNo);
        if (user != null) {
            dto.setAppr_emp_nm(user.getUserName());
            dto.setAppr_position(user.getPositionName());
        } else {
            dto.setAppr_emp_nm(fallbackNm != null ? fallbackNm : "");
            dto.setAppr_position(fallbackPos != null ? fallbackPos : "");
        }
        dto.setAppr_group(group);
        dto.setAppr_num(num);
        dto.setAppr_tg(tg);
        dto.setLast_tag(lastTag);
        dto.setAppr_remarks(remarks);
        return dto;
    }

    private void syncAdminTag(int dSeq) {
        ApprovalDTO detail = approvalService.getApprovalDetailOne(dSeq);
        if ("A".equals(detail.getAppr_group()) && "T".equals(detail.getLast_tag()) && "T".equals(detail.getAppr_tg())) {
            approvalService.updateAdminTag(detail.getAppr_no(), "T");
        } else {
            approvalService.updateAdminTag(detail.getAppr_no(), "F");
        }
    }

    private void finishIfComplete(int apprNo) {
        if ("T".equals(approvalService.getApprovalTag(apprNo))) {
            updateMasterStatus(apprNo, "3", "");
        }
    }

    private void updateMasterStatus(int apprNo, String stat, String rejectRemarks) {
        ApprovalMDTO mdto = new ApprovalMDTO();
        mdto.setAppr_no(apprNo);
        mdto.setAppr_stat(stat);
        mdto.setReject("4".equals(stat) ? (rejectRemarks != null ? rejectRemarks : "") : "");
        approvalService.updateApprovalMaster(mdto);
    }

    private int saveCcList(int apprNo, List<ApprovalCcDTO> ccList, int addedBy) {
        if (ccList == null || ccList.isEmpty()) {
            return 0;
        }
        int inserted = 0;
        Set<Integer> seen = new HashSet<>();
        for (ApprovalCcDTO item : ccList) {
            if (item == null) {
                continue;
            }
            int empNo = item.getEmpNo();
            if (empNo <= 0 || !seen.add(empNo)) {
                continue;
            }
            if (approvalDAO.countApproverOnLine(apprNo, empNo) > 0) {
                continue;
            }
            if (approvalDAO.countCcByApprAndEmp(apprNo, empNo) > 0) {
                continue;
            }
            String empNm = item.getEmpNm();
            LoginDTO user = resolveUser(empNo, empNm);
            ApprovalCcDTO cc = new ApprovalCcDTO();
            cc.setApprNo(apprNo);
            cc.setEmpNo(empNo);
            if (user != null) {
                cc.setEmpNm(user.getUserName());
                cc.setPositionNm(user.getPositionName());
            } else {
                cc.setEmpNm(empNm != null ? empNm : "");
                cc.setPositionNm("");
            }
            cc.setAddedBy(addedBy);
            if (approvalDAO.insertCc(cc) > 0) {
                inserted++;
            }
        }
        return inserted;
    }

    private static String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private int parseIntOrZero(String value) {
        if (value == null || value.trim().isEmpty()) {
            return 0;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * 상신·접수 JSON 배열, form-urlencoded 다중값, 콤마/JSON 문자열 모두 처리
     */
    @SuppressWarnings("unchecked")
    public static List<ApprovalLineSubmitDTO> parseApprovalLines(Object raw) {
        if (raw == null) {
            return Collections.emptyList();
        }
        List<ApprovalLineSubmitDTO> result = new ArrayList<>();
        if (raw instanceof List) {
            for (Object item : (List<?>) raw) {
                if (!(item instanceof Map)) {
                    continue;
                }
                Map<String, Object> m = (Map<String, Object>) item;
                Object emp = m.get("empNo");
                if (emp == null) {
                    emp = m.get("emp_no");
                }
                int empNo = 0;
                if (emp instanceof Number) {
                    empNo = ((Number) emp).intValue();
                } else if (emp != null) {
                    try {
                        empNo = Integer.parseInt(emp.toString());
                    } catch (NumberFormatException ignored) {
                    }
                }
                Object nmObj = m.get("empNm");
                if (nmObj == null) {
                    nmObj = m.get("emp_nm");
                }
                String empNm = nmObj != null ? nmObj.toString().trim() : null;
                if (empNm != null && empNm.isEmpty()) {
                    empNm = null;
                }
                String phase = m.get("phase") != null ? m.get("phase").toString() : "FIELD";
                String isFinal = m.get("isFinal") != null ? m.get("isFinal").toString()
                        : (m.get("is_final") != null ? m.get("is_final").toString() : "F");
                if (empNo > 0) {
                    result.add(new ApprovalLineSubmitDTO(empNo, empNm, phase, isFinal));
                }
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    public static List<ApprovalCcDTO> parseCcList(Object raw) {
        if (raw == null) {
            return Collections.emptyList();
        }
        List<ApprovalCcDTO> result = new ArrayList<>();
        if (raw instanceof List) {
            for (Object item : (List<?>) raw) {
                addCcEntry(result, item);
            }
        } else if (raw instanceof String[]) {
            for (String s : (String[]) raw) {
                addCcEntryFromString(result, s);
            }
        } else if (raw instanceof String) {
            addCcEntryFromString(result, (String) raw);
        } else if (raw instanceof Number) {
            addCcEntry(result, raw);
        }
        return result;
    }

    /** @deprecated 이름 미포함 파싱. {@link #parseCcList(Object)} 사용 */
    @Deprecated
    public static List<Integer> parseCcEmpNos(Object raw) {
        List<Integer> empNos = new ArrayList<>();
        for (ApprovalCcDTO cc : parseCcList(raw)) {
            if (cc.getEmpNo() > 0 && !empNos.contains(cc.getEmpNo())) {
                empNos.add(cc.getEmpNo());
            }
        }
        return empNos;
    }

    public static List<ApprovalCcDTO> parseCcListFromRequest(javax.servlet.http.HttpServletRequest request) {
        if (request == null) {
            return Collections.emptyList();
        }
        String json = request.getParameter("ccEmpNos");
        if (json != null && !json.trim().isEmpty()) {
            List<ApprovalCcDTO> parsed = parseCcList(json);
            if (!parsed.isEmpty()) {
                return parsed;
            }
        }
        String[] values = request.getParameterValues("ccEmpNos");
        if (values != null && values.length > 0) {
            return parseCcList(values);
        }
        return Collections.emptyList();
    }

    /** @deprecated {@link #parseCcListFromRequest} 사용 */
    @Deprecated
    public static List<Integer> parseCcEmpNosFromRequest(javax.servlet.http.HttpServletRequest request) {
        List<Integer> empNos = new ArrayList<>();
        for (ApprovalCcDTO cc : parseCcListFromRequest(request)) {
            if (cc.getEmpNo() > 0 && !empNos.contains(cc.getEmpNo())) {
                empNos.add(cc.getEmpNo());
            }
        }
        return empNos;
    }

    private static void addCcEntryFromString(List<ApprovalCcDTO> result, String s) {
        if (s == null) {
            return;
        }
        String trimmed = s.trim();
        if (trimmed.isEmpty()) {
            return;
        }
        if (trimmed.startsWith("[")) {
            // JSON 배열: 숫자 또는 객체
            try {
                // 간단 분기: 객체 배열이면 Map 파싱 대신 수동
                if (trimmed.contains("{")) {
                    parseCcJsonObjectArray(result, trimmed);
                    return;
                }
            } catch (Exception ignored) {
            }
            String inner = trimmed.replaceAll("[\\[\\]\"\\s]", "");
            if (!inner.isEmpty()) {
                for (String part : inner.split(",")) {
                    addCcEntryFromString(result, part);
                }
            }
            return;
        }
        if (trimmed.contains(",")) {
            for (String part : trimmed.split(",")) {
                addCcEntryFromString(result, part);
            }
            return;
        }
        addCcEntry(result, trimmed);
    }

    /**
     * [{"empNo":1,"empNm":"홍길동"}, ...] 또는 [1,2] 형태의 JSON 문자열 수동 파싱
     */
    @SuppressWarnings("unchecked")
    private static void parseCcJsonObjectArray(List<ApprovalCcDTO> result, String json) {
        // Jackson 없이 컨트롤러가 이미 List로 역직렬화한 경로가 주이므로,
        // form 문자열은 org.json 없이 숫자 추출 + empNm 패턴을 최소한 지원
        // 실제로는 Spring @RequestBody List가 Map으로 들어오므로 parseCcList(List)가 처리
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            Object parsed = mapper.readValue(json, Object.class);
            if (parsed instanceof List) {
                for (Object item : (List<?>) parsed) {
                    addCcEntry(result, item);
                }
            }
        } catch (Exception e) {
            String inner = json.replaceAll("[\\[\\]\"\\s]", "");
            if (!inner.isEmpty()) {
                for (String part : inner.split(",")) {
                    if (part.matches("\\d+")) {
                        addCcEntry(result, part);
                    }
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static void addCcEntry(List<ApprovalCcDTO> result, Object item) {
        if (item == null) {
            return;
        }
        if (item instanceof Map) {
            Map<String, Object> m = (Map<String, Object>) item;
            Object emp = m.get("empNo");
            if (emp == null) {
                emp = m.get("emp_no");
            }
            int empNo = 0;
            if (emp instanceof Number) {
                empNo = ((Number) emp).intValue();
            } else if (emp != null) {
                try {
                    empNo = Integer.parseInt(emp.toString());
                } catch (NumberFormatException ignored) {
                }
            }
            if (empNo <= 0) {
                return;
            }
            for (ApprovalCcDTO existing : result) {
                if (existing.getEmpNo() == empNo) {
                    return;
                }
            }
            Object nmObj = m.get("empNm");
            if (nmObj == null) {
                nmObj = m.get("emp_nm");
            }
            ApprovalCcDTO cc = new ApprovalCcDTO();
            cc.setEmpNo(empNo);
            cc.setEmpNm(nmObj != null ? nmObj.toString().trim() : null);
            result.add(cc);
            return;
        }
        if (item instanceof Number) {
            int n = ((Number) item).intValue();
            if (n > 0) {
                for (ApprovalCcDTO existing : result) {
                    if (existing.getEmpNo() == n) {
                        return;
                    }
                }
                ApprovalCcDTO cc = new ApprovalCcDTO();
                cc.setEmpNo(n);
                result.add(cc);
            }
            return;
        }
        String s = item.toString().trim();
        if (s.isEmpty()) {
            return;
        }
        try {
            int n = Integer.parseInt(s);
            if (n > 0) {
                for (ApprovalCcDTO existing : result) {
                    if (existing.getEmpNo() == n) {
                        return;
                    }
                }
                ApprovalCcDTO cc = new ApprovalCcDTO();
                cc.setEmpNo(n);
                result.add(cc);
            }
        } catch (NumberFormatException ignored) {
        }
    }
}

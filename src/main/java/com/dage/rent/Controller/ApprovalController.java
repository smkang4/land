package com.dage.rent.Controller;

import com.dage.rent.Component.Mail;
import com.dage.rent.DTO.*;
import com.dage.rent.Service.AdminService;
import com.dage.rent.Service.ApprovalService;
import com.dage.rent.Service.ApprovalWorkflowService;
import com.dage.rent.Service.AttachmentFileService;
import com.dage.rent.Service.ContractService;
import com.dage.rent.Service.RentService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.text.SimpleDateFormat;
import java.util.*;

@Controller
@RequiredArgsConstructor
@RequestMapping("/appr")
public class ApprovalController {

    private final Mail mail;

    @Autowired
    private ApprovalService approvalService;

    @Autowired
    private ContractService contractService;

    @Autowired
    private AdminService adminService;

    @Autowired
    RentService rentService;

    @Autowired
    private AttachmentFileService attachmentFileService;

    @Autowired
    private ApprovalWorkflowService approvalWorkflowService;

    @GetMapping("/list")
    @ResponseBody
    public List<ApprovalDTO> getApprovalList(HttpServletRequest request){

        HashMap<String,Object> map = new HashMap<>();
        map.put("proj_code",request.getParameter("proj_code"));
        map.put("conf_tag",request.getParameter("conf_tag"));
        LoginDTO loginDTO = (LoginDTO) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        int empno = loginDTO.getEmpNo();

        map.put("emp_no",empno);
        String tag = request.getParameter("conf_tag");

        List<ApprovalDTO> list = approvalService.getApprovalList(map);

        return list;
    }

    @GetMapping("/list/admin")
    @ResponseBody
    public List<ApprovalDTO> getContractDetailForReceipt(HttpServletRequest request){
        HashMap<String, Object> map = new HashMap<>();
        String group = request.getParameter("appr_group");
        map.put("proj_code", request.getParameter("proj_code") == null ? "" : request.getParameter("proj_code"));
        map.put("conf_tag", request.getParameter("conf_tag") == null ? "" : request.getParameter("conf_tag"));

        map.put("appr_group", group);

        List<ApprovalDTO> receipt = new ArrayList<>();
        if("B".equals(group)){
            LoginDTO loginDTO = (LoginDTO) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            int empno = loginDTO.getEmpNo();
            map.put("emp_no",empno);
            receipt = approvalService.getApprovalList(map);
            String tag = request.getParameter("conf_tag");
            if("5".equals(tag)){
                map.put("last_emp_no",empno);
            }

        }else{
            map.put("emp_no","");
            receipt =  approvalService.getApprovalListForAdmin(map);
        }
        return receipt;
    }


    @GetMapping("/reg/{appr_no}")
    public String approvalReg(Model model,@PathVariable("appr_no") int appr_no) {

        List<ApprovalDDTO> apprList = approvalService.getApprovalDetail(appr_no);

        int list_size = apprList.size();
        int cur_emp_no = 0;
        int appr_d_seq = 0;
        int appr_num = 0;

        int last_appr_d_seq = 0;
        int last_appr_num = 0;

        String request_conf = "F";
        int b_cnt = 0;
        int a_cnt = 0;
        String admin_tg = "F";
        String last_flag = "F";

        for(int i = 0 ; i <= list_size-1 ; i++){

            if(apprList.get(i).getAppr_tg().equals("N")){
                cur_emp_no =  apprList.get(i).getAppr_emp_no();
                appr_d_seq =  apprList.get(i).getD_seq();
                appr_num =  apprList.get(i).getAppr_num();
                break;
            }
        }

        for(int i = 0 ; i <= list_size-1 ; i++){

            if(apprList.get(i).getAppr_tg().equals("T") && apprList.get(i).getAppr_group().equals("A") && apprList.get(i).getLast_tag().equals("T")){
                request_conf = "T";
            }else{
                request_conf = "F";
            }

            if(apprList.get(i).getAppr_group().equals("A")){
                a_cnt++; // 현장결재 count
            }else{
                b_cnt++; // 관리부서 count
            }

            if(i==(list_size-1)){
                last_appr_d_seq = apprList.get(i).getD_seq();
                last_appr_num = apprList.get(i).getAppr_num();
            }

            if(apprList.get(i).getAppr_emp_no() == 58 && "T".equals(apprList.get(i).getAppr_tg())){
                last_flag = "T";
            }
        }

        LoginDTO loginDTO = (LoginDTO) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        int currentEmpNo = loginDTO.getEmpNo();

        if (b_cnt == 0 && request_conf.equals("T") && adminService.isAdmin(currentEmpNo)) {
            admin_tg = "T";
            appr_d_seq = last_appr_d_seq;
            appr_num = last_appr_num;
        } else {
            admin_tg = "F";
        }

        // 현재 사용자가 마지막 결재자인지 확인
        String isLastApprover = "F";
        
        // 마지막 결재자 확인
        if (list_size > 0) {
            ApprovalDDTO lastApprover = apprList.get(list_size - 1);
            if (lastApprover.getAppr_emp_no() == currentEmpNo && "T".equals(lastApprover.getAppr_tg())) {
                isLastApprover = "T";
            }
        }

        List<ContractDTO> contract = contractService.getContractDetailForList(appr_no);
        for (ContractDTO c : contract) {
            resolveFileLists(c);
        }
        model.addAttribute("contract", contract);

        model.addAttribute("appr_list",apprList);
        model.addAttribute("admin_tg",admin_tg);
        model.addAttribute("cur_emp_no",cur_emp_no);
        model.addAttribute("appr_d_seq",appr_d_seq);
        model.addAttribute("appr_num",appr_num);
        model.addAttribute("appr_no",appr_no);
        model.addAttribute("last",last_flag);
        model.addAttribute("is_last_approver",isLastApprover);
        model.addAttribute("can_admin_cancel_last_step",
                adminService.canManageErpAndCustomer(currentEmpNo) ? "T" : "F");

        boolean ccOnly = approvalWorkflowService.isCcOnlyViewer(appr_no, currentEmpNo);
        model.addAttribute("read_only", ccOnly ? "T" : "F");
        model.addAttribute("cc_list", approvalWorkflowService.getCcList(appr_no));

        return "approval/appr_reg";
    }


    @PostMapping("/update/detail")
    @ResponseBody
    public ResponseEntity<?> updateApprovalConfirm(HttpServletRequest request) {
        String gubun = request.getParameter("gubun");
        int d_seq = Integer.parseInt(request.getParameter("appr_d_seq"));
        int emp_no = Integer.parseInt(request.getParameter("appr_emp_no"));
        int appr_no = Integer.parseInt(request.getParameter("appr_no"));
        int appr_num = Integer.parseInt(request.getParameter("appr_num"));
        String next_emp_user = request.getParameter("next_emp_user");
        String next_emp_nm = request.getParameter("next_emp_nm");
        String banRemark = request.getParameter("banRemark");

        try {
            LoginDTO login = (LoginDTO) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            if (approvalWorkflowService.isCcOnlyViewer(appr_no, login.getEmpNo())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("msg", "참조 건은 승인·반려할 수 없습니다."));
            }
            if ("A".equals(gubun) && !adminService.isAdmin(login.getEmpNo())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("msg", "관리부서 접수 권한이 없습니다."));
            }
            // 요청 사번이 세션과 같으면 세션 이름으로 동일 emp_no 중복 계정 구분
            String empNm = (emp_no == login.getEmpNo()) ? login.getUserName() : null;
            List<ApprovalCcDTO> ccList = ApprovalWorkflowService.parseCcListFromRequest(request);
            Map<String, Object> result = approvalWorkflowService.processConfirm(
                    gubun, d_seq, emp_no, empNm, appr_no, appr_num,
                    next_emp_user, next_emp_nm, banRemark, ccList);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error occurred while saving data");
        }
    }

    @PostMapping("/multi/apply")
    public ResponseEntity<?> multiApplyConfirmData(@RequestBody Map<String, Object> requestData) {
        LoginDTO loginDTO = (LoginDTO) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        try {
            @SuppressWarnings("unchecked")
            List<String> arrayList = (List<String>) requestData.get("arrayList");
            List<ApprovalLineSubmitDTO> approvalLines =
                    ApprovalWorkflowService.parseApprovalLines(requestData.get("approvalLines"));
            if (approvalLines.isEmpty() && requestData.get("appr_emp_no") != null) {
                String rApprEmpNo = String.valueOf(requestData.get("appr_emp_no"));
                if ("0".equals(rApprEmpNo)) {
                    rApprEmpNo = String.valueOf(loginDTO.getEmpNo());
                }
                String fallbackNm = String.valueOf(loginDTO.getEmpNo()).equals(rApprEmpNo)
                        ? loginDTO.getUserName() : null;
                approvalLines = List.of(new ApprovalLineSubmitDTO(
                        Integer.parseInt(rApprEmpNo), fallbackNm, "FIELD", "F"));
            }
            List<ApprovalCcDTO> ccList = ApprovalWorkflowService.parseCcList(requestData.get("ccEmpNos"));

            Map<String, Object> result = approvalWorkflowService.submit(
                    loginDTO.getEmpNo(),
                    loginDTO.getUserName(),
                    loginDTO.getPositionName(),
                    arrayList,
                    approvalLines,
                    ccList);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("msg", e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error occurred while saving data");
        }
    }

    // ERP 거래처 검색 API
    @GetMapping("/api/search/erp-customers")
    public ResponseEntity<?> searchErpCustomers(@RequestParam String custCode) {
        try {
            List<Map<String, Object>> customers = rentService.searchErpCustomers(custCode);
            return ResponseEntity.ok().body(Map.of("success", true, "data", customers));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "거래처 검색 중 오류가 발생했습니다."));
        }
    }

    private final LoginDTO getUserInfo(int emp_no, String user_nm){
        return rentService.getUserinfo(emp_no, user_nm);
    }

    private final void updateApprovalStatus(int appr_no, String appr_stat, String appr_remarks){
        ApprovalMDTO mdto = new ApprovalMDTO();
        mdto.setAppr_no(appr_no);
        mdto.setAppr_stat(appr_stat);
        String reject = appr_stat == "4" ? appr_remarks : "";
        mdto.setReject(reject);
        approvalService.updateApprovalMaster(mdto);
    }

    // 최초승인자 메일 HTML
    public final static String confirmMailHtml(ApprovalDTO dto){

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy년 MM월 dd일");

        String sendType="";
        String appr_tg="";
        //vHtml+="<img src='images/logo/dongah.gif'/>";
        sendType = dto.getNext_send_type();
        appr_tg = dto.getAppr_tg();


        String htmlContent = "";
        htmlContent = "<!DOCTYPE html>";
        htmlContent += "<html lang=\"ko\">";
        htmlContent += "<head>";
        htmlContent += "    <meta charset=\"UTF-8\">";
        htmlContent += "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">";
        htmlContent += "    <title>ehddkwlwlf 부동산 임차관리 플랫폼</title>";
        htmlContent += "</head>";
        htmlContent += "<body style=\"font-family: 'Apple SD Gothic Neo', 'Malgun Gothic', '맑은 고딕', sans-serif; line-height: 1.6; color: #333333; margin: 0; padding: 0; background-color: #f5f5f5;\">";
        htmlContent += "    <table cellpadding=\"0\" cellspacing=\"0\" border=\"0\" width=\"100%\" style=\"max-width: 600px; margin: 20px auto; background-color: #ffffff; border-radius: 10px; box-shadow: 0 0 20px rgba(0, 0, 0, 0.1); overflow: hidden;\">";
        htmlContent += "        <tr>";
        htmlContent += "            <td style=\"background: #1e88e5; padding: 25px; color: white; text-align: center;\">";
        htmlContent += "                <h1 style=\"margin: 0; font-size: 24px; font-weight: 700; letter-spacing: 1px;\">[동아지질] 부동산 임차관리 플랫폼</h1>";
        htmlContent += "            </td>";
        htmlContent += "        </tr>";
        htmlContent += "        ";
        htmlContent += "        <tr>";
        htmlContent += "            <td style=\"padding: 30px;\">";
        htmlContent += "                <table cellpadding=\"0\" cellspacing=\"0\" border=\"0\" width=\"100%\" style=\"margin-bottom: 25px; border-bottom: 1px solid #eeeeee; padding-bottom: 20px;\">";
        htmlContent += "                    <tr style=\"margin-bottom: 15px; display: block;\">";
        htmlContent += "                        <td width=\"120\" style=\"font-weight: 700; color: #0d47a1; vertical-align: top; padding-bottom: 15px;\">사용현장</td>";
        htmlContent += "                        <td style=\"color: #444; vertical-align: top; padding-bottom: 15px;\">"+dto.getProj_name()+"</td>";
        htmlContent += "                    </tr>";
        htmlContent += "                    <tr style=\"margin-bottom: 15px; display: block;\">";
        htmlContent += "                        <td width=\"120\" style=\"font-weight: 700; color: #0d47a1; vertical-align: top;\">물건(주소)</td>";
        htmlContent += "                        <td style=\"color: #444; vertical-align: top;\">"+dto.getContext()+"</td>";
        htmlContent += "                    </tr>";
        if("F".equals(appr_tg)){
            String remarks = dto.getAppr_remarks() != null ? dto.getAppr_remarks().replace("\n", "<br>") : "";
            htmlContent += "                    <tr style=\"margin-bottom: 15px; display: block;\">";
            htmlContent += "                        <td width=\"120\" style=\"font-weight: 700; color: #c0392b; vertical-align: top;\">반려사유</td>";
            htmlContent += "                        <td style=\"color: #444; vertical-align: top; white-space: pre-wrap;\">"+remarks+"</td>";
            htmlContent += "                    </tr>";
        }
        htmlContent += "                </table>";
        htmlContent += "                <table cellpadding=\"0\" cellspacing=\"0\" border=\"0\" width=\"100%\" style=\"margin: 30px 0 10px; text-align: center;\">";
        htmlContent += "                    <tr>";
        htmlContent += "                        <td>";
        htmlContent += "                            <a href=\"http://rent.dage.co.kr\" style=\"display: inline-block; background: #1e88e5; color: white; padding: 12px 25px; text-decoration: none; border-radius: 5px; font-weight: 600;\">동아지질 부동산 임차관리 플랫폼 바로가기</a>";
        htmlContent += "                        </td>";
        htmlContent += "                    </tr>";
        htmlContent += "                </table>";
        htmlContent += "            </td>";
        htmlContent += "        </tr>";
        htmlContent += "        <tr>";
        htmlContent += "            <td style=\"text-align: center; padding: 20px; background-color: #f9f9f9; color: #666666; font-size: 12px;\">";
        htmlContent += "                © 2025 동아지질 부동산 임차관리 플랫폼 | 기능문의: smkang@dage.co.kr | 전화: 051-580-5534";
        htmlContent += "            </td>";
        htmlContent += "        </tr>";
        htmlContent += "    </table>";
        htmlContent += "</body>";
        htmlContent += "</html>";

        return htmlContent;
    }




    @PostMapping("/api/approve-items")
    @ResponseBody
    public ResponseEntity<?> approveItems(@RequestBody Map<String, Object> requestData) {
        try {
            List<String> seqList = (List<String>) requestData.get("seqList");
            if (seqList == null || seqList.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "승인할 항목을 선택해주세요."));
            }

            // 현재 로그인한 사용자 정보 가져오기
            LoginDTO loginDTO = (LoginDTO) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            int empNo = loginDTO.getEmpNo();

            // 승인 처리
            for (String apprNo : seqList) {
                ApprovalDDTO apprDto = new ApprovalDDTO();
                apprDto.setAppr_no(Integer.parseInt(apprNo));
                apprDto.setAppr_emp_no(empNo);
                apprDto.setAppr_tg("T"); // 승인 처리
                apprDto.setAppr_remarks("승인");
                
                approvalService.updateApprovalDetail(apprDto);
                
                // 결재 상태 업데이트
                updateApprovalStatus(Integer.parseInt(apprNo), "2", "");
                
                // 승인 완료 확인
                String finishTag = approvalService.getApprovalTag(Integer.parseInt(apprNo));
                if (finishTag.equals("T")) {
                    updateApprovalStatus(Integer.parseInt(apprNo), "3", "");
                }
            }

            return ResponseEntity.ok(Map.of("message", "선택한 항목이 승인되었습니다."));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "승인 처리 중 오류가 발생했습니다."));
        }
    }

    @PostMapping("/api/reject-items")
    @ResponseBody
    public ResponseEntity<?> rejectItems(@RequestBody Map<String, Object> requestData) {
        try {
            List<String> seqList = (List<String>) requestData.get("seqList");
            String reason = (String) requestData.get("reason");
            
            if (seqList == null || seqList.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "반려할 항목을 선택해주세요."));
            }
            if (reason == null || reason.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "반려 사유를 입력해주세요."));
            }

            // 현재 로그인한 사용자 정보 가져오기
            LoginDTO loginDTO = (LoginDTO) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            int empNo = loginDTO.getEmpNo();

            // 반려 처리
            for (String apprNo : seqList) {
                ApprovalDDTO apprDto = new ApprovalDDTO();
                apprDto.setAppr_no(Integer.parseInt(apprNo));
                apprDto.setAppr_emp_no(empNo);
                apprDto.setAppr_tg("F"); // 반려 처리
                apprDto.setAppr_remarks(reason);
                
                approvalService.updateApprovalDetail(apprDto);
                
                // 결재 상태 업데이트
                updateApprovalStatus(Integer.parseInt(apprNo), "4", reason);
            }

            return ResponseEntity.ok(Map.of("message", "선택한 항목이 반려되었습니다."));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "반려 처리 중 오류가 발생했습니다."));
        }
    }


    // 기안서 --------------------------------------------------------------------------------------------------------------

    @GetMapping("/list/draft")
    @ResponseBody
    public List<ApprovalDTO> getContractDetailForDraft(HttpServletRequest request){

        HashMap<String, Object> map = new HashMap<>();
        String proj_code = request.getParameter("proj_code") == null ? "" : request.getParameter("proj_code");
        map.put("proj_code", proj_code);

        LoginDTO loginDTO = (LoginDTO) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        int user_no = loginDTO.getUserNo();
        map.put("user_no",user_no);

        System.out.println("전달된 파라미터: " + map);
        List<ApprovalDTO> d_list = approvalService.getApprovalListForDraft(map);

        System.out.println("반환된 데이터 개수: " + (d_list != null ? d_list.size() : "null"));
        if (d_list != null && !d_list.isEmpty()) {
            System.out.println("첫 번째 데이터: " + d_list.get(0));
        }

        return d_list;
    }

    @GetMapping("/list/draft/incomplete") 
    @ResponseBody
    public List<ApprovalDTO> getIncompleteDraftList(HttpServletRequest request){
        HashMap<String, Object> map = new HashMap<>();
        
        // 현재 로그인한 사용자의 user_no 가져오기
        LoginDTO loginDTO = (LoginDTO) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        int userNo = loginDTO.getUserNo();
        map.put("user_no", userNo);
        System.out.println("기안 미완료 목록 조회 - user_no: " + userNo);
        
        List<ApprovalDTO> allList = approvalService.getIncompleteDraftList(map);
        System.out.println("기안 미완료 전체 데이터 개수: " + (allList != null ? allList.size() : "null"));
        
        // 상태값으로 필터링: 00(미전송), 40(반려), 99(삭제), null 또는 mst_seq가 없는 경우만
        List<ApprovalDTO> filteredList = new ArrayList<>();
        if (allList != null) {
            for (ApprovalDTO dto : allList) {
                String confStatus = dto.getConf_status();
                String mstSeq = dto.getMst_seq();
                
                // mst_seq가 없거나, 상태가 00, 40, 99, null인 경우만 기안 미완료 탭에 표시
                if (mstSeq == null || mstSeq.trim().isEmpty() || 
                    confStatus == null || "00".equals(confStatus) || "40".equals(confStatus) || "99".equals(confStatus)) {
                    filteredList.add(dto);
                }
            }
        }
        
        System.out.println("기안 미완료 필터링된 데이터 개수: " + filteredList.size());
        return filteredList;
    }

    @GetMapping("/list/draft/completed")
    @ResponseBody
    public List<ApprovalDTO> getCompletedDraftList(HttpServletRequest request){
        HashMap<String, Object> map = new HashMap<>();
        
        // 현재 로그인한 사용자의 user_no 가져오기
        LoginDTO loginDTO = (LoginDTO) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        int userNo = loginDTO.getUserNo();
        map.put("user_no", userNo);
        System.out.println("기안완료 목록 조회 - user_no: " + userNo);
        
        List<ApprovalDTO> allList = approvalService.getCompletedDraftList(map);
        System.out.println("기안완료 전체 데이터 개수: " + (allList != null ? allList.size() : "null"));
        
        // 상태값으로 필터링: 10(결재중), 20(보류), 30(완료)인 경우만
        List<ApprovalDTO> filteredList = new ArrayList<>();
        if (allList != null) {
            for (ApprovalDTO dto : allList) {
                String confStatus = dto.getConf_status();
                System.out.println("기안완료 필터링 - mst_seq: " + dto.getMst_seq() + ", conf_status: [" + confStatus + "], conf_status_name: " + dto.getConf_status_name());
                
                // 상태가 10, 20, 30인 경우만 기안완료 탭에 표시
                if ("10".equals(confStatus) || "20".equals(confStatus) || "30".equals(confStatus)) {
                    filteredList.add(dto);
                }
            }
        }
        
        // 1차: 기안완료 탭 접속 시 거래처 등록 체크
        processCustomerRegistrations(filteredList);
        
        System.out.println("기안완료 필터링된 데이터 개수: " + filteredList.size());
        return filteredList;
    }
    
    /**
     * 거래처 등록 처리 (1차: 기안완료 탭 접속 시)
     * @param draftList 기안완료 목록
     */
    private void processCustomerRegistrations(List<ApprovalDTO> draftList) {
        if (draftList == null || draftList.isEmpty()) {
            return;
        }
        
        System.out.println("=== 거래처 등록 체크 시작 ===");
        
        for (ApprovalDTO dto : draftList) {
            try {
                // 거래처 등록이 필요한 조건 체크
                if (shouldRegisterCustomer(dto)) {
                    System.out.println("거래처 등록 대상 발견 - draftId: " + dto.getDraft_id() + 
                                     ", mstSeq: " + dto.getMst_seq() + 
                                     ", confStatus: " + dto.getConf_status());
                    
                    // 비동기로 거래처 등록 처리
                    processCustomerRegistrationAsync(dto);
                }
            } catch (Exception e) {
                System.err.println("거래처 등록 체크 중 오류 - draftId: " + dto.getDraft_id() + 
                                 ", 오류: " + e.getMessage());
            }
        }
        
        System.out.println("=== 거래처 등록 체크 완료 ===");
    }
    
    /**
     * 거래처 등록이 필요한지 체크
     * @param dto 기안서 정보
     * @return 거래처 등록 필요 여부
     */
    private boolean shouldRegisterCustomer(ApprovalDTO dto) {
        String confStatus = dto.getConf_status();
        String custRegAttempted = dto.getCust_reg_attempted();
        
        // 조건: confStatus가 10 이상이고, 거래처 등록을 아직 시도하지 않은 경우
        boolean statusOk = "10".equals(confStatus) || "20".equals(confStatus) || "30".equals(confStatus);
        boolean notAttempted = custRegAttempted == null || !"Y".equals(custRegAttempted);
        
        return statusOk && notAttempted;
    }
    
    /**
     * 비동기 거래처 등록 처리
     * @param dto 기안서 정보
     */
    private void processCustomerRegistrationAsync(ApprovalDTO dto) {
        // TODO: 실제 거래처 등록 프로시저 호출 로직 구현
        // 현재는 플래그만 업데이트
        try {
            // 거래처 등록 시도 플래그 업데이트
            approvalService.updateCustomerRegistrationAttempted(dto.getDraft_id(), "Y");
            
            System.out.println("거래처 등록 시도 플래그 업데이트 완료 - draftId: " + dto.getDraft_id());
            
            // TODO: 실제 거래처 등록 프로시저 호출
            // callCustomerRegistrationProcedure(dto);
            
        } catch (Exception e) {
            System.err.println("거래처 등록 처리 중 오류 - draftId: " + dto.getDraft_id() + 
                             ", 오류: " + e.getMessage());
        }
    }

    @GetMapping("/list/draft/completed/admin")
    @ResponseBody
    public List<ApprovalDTO> getCompletedDraftListForAdmin(HttpServletRequest request){
        HashMap<String, Object> map = new HashMap<>();

        List<ApprovalDTO> d_list = approvalService.getCompletedDraftListForAdmin(map);
        
        if (d_list != null && !d_list.isEmpty()) {
            System.out.println("=== ERP 등록 목록 필터링 시작 ===");
            System.out.println("전체 데이터 개수: " + d_list.size());
            
            // conf_status가 30(완료)인 항목만 필터링
            List<ApprovalDTO> filteredList = new ArrayList<>();
            
            for (ApprovalDTO approvalDTO : d_list) {
                String mstSeq = approvalDTO.getMst_seq();
                
                if (mstSeq != null && !mstSeq.trim().isEmpty()) {
                    try {
                        // Oracle에서 conf_status 조회
                        String confStatus = rentService.getConfStatus(mstSeq);
                        
                        System.out.println("mst_seq: " + mstSeq + ", conf_status: " + confStatus + ", contract_count: " + approvalDTO.getContract_count());
                        
                        // conf_status가 30(완료)인 경우만 추가
                        if ("30".equals(confStatus)) {
                            approvalDTO.setConf_status(confStatus);
                            approvalDTO.setConf_status_name("완료");
                            filteredList.add(approvalDTO); // DTO 전체를 추가 (모든 필드 유지)
                            System.out.println("✅ 추가됨 - mst_seq: " + mstSeq + ", 계약건수: " + approvalDTO.getContract_count());
                        } else {
                            System.out.println("❌ 제외됨 - mst_seq: " + mstSeq + ", conf_status: " + confStatus + " (완료 아님)");
                        }
                    } catch (Exception e) {
                        System.err.println("❌ conf_status 조회 실패 - mst_seq: " + mstSeq + ", 오류: " + e.getMessage());
                        // 오류가 발생하면 해당 항목은 제외
                    }
                } else {
                    System.err.println("❌ mst_seq가 없음, 제외됨");
                }
            }
            
            System.out.println("=== 필터링 완료 ===");
            System.out.println("필터링된 데이터 개수: " + filteredList.size());
            
            return filteredList;
        }

        return d_list;
    }


    @GetMapping("/draft/{appr_no}")
    public String draft(@PathVariable("appr_no") int appr_no, 
                       @RequestParam(value = "additional_appr_nos", required = false) List<Integer> additionalApprNos,
                       Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getName().equals("anonymousUser")) {
            return "redirect:/login";
        }

        System.out.println("========================================");
        System.out.println("=== /appr/draft/{appr_no} 호출됨 ===");
        System.out.println("appr_no: " + appr_no);
        System.out.println("additional_appr_nos: " + additionalApprNos);
        System.out.println("========================================");

        // 메인 appr_no의 계약 데이터 조회 (이미 다른 기안서에 포함된 contract는 제외)
        List<ContractDTO> contract = contractService.getContractDetailForDraft(appr_no);
        
        System.out.println("=== draft 메서드 디버깅 ===");
        System.out.println("입력 appr_no: " + appr_no);
        System.out.println("조회된 contract 데이터: " + contract);
        System.out.println("contract 크기: " + (contract != null ? contract.size() : "null"));
        
        if (contract != null && !contract.isEmpty()) {
            System.out.println("첫 번째 contract 항목:");
            System.out.println("  proj_code 원본: " + contract.get(0).getProj_code());
            System.out.println("  proj_code 타입: " + (contract.get(0).getProj_code() != null ? contract.get(0).getProj_code().getClass().getSimpleName() : "null"));
            System.out.println("  proj_code 길이: " + (contract.get(0).getProj_code() != null ? contract.get(0).getProj_code().length() : "null"));
            
            // proj_code에 앞자리 0 추가 (4자리로 패딩)
            String originalProjCode = contract.get(0).getProj_code();
            if (originalProjCode != null && !originalProjCode.trim().isEmpty()) {
                String paddedProjCode = String.format("%04d", Integer.parseInt(originalProjCode));
                contract.get(0).setProj_code(paddedProjCode);
                System.out.println("  proj_code 수정 후: " + paddedProjCode);
            }
            
            System.out.println("  proj_name: " + contract.get(0).getProj_name());
        } else {
            System.out.println("⚠️ contract 데이터가 비어있습니다!");
        }
        
        // 추가 appr_no들의 계약 데이터 조회
        List<ContractDTO> allContracts = new ArrayList<>();
        if (contract != null && !contract.isEmpty()) {
            allContracts.addAll(contract);
        }
        
        if (additionalApprNos != null && !additionalApprNos.isEmpty()) {
            for (Integer additionalApprNo : additionalApprNos) {
                List<ContractDTO> additionalContract = contractService.getContractDetailForDraft(additionalApprNo);
                if (additionalContract != null && !additionalContract.isEmpty()) {
                    // 추가 계약 데이터의 proj_code도 패딩 처리
                    for (ContractDTO contractItem : additionalContract) {
                        String originalProjCode = contractItem.getProj_code();
                        if (originalProjCode != null && !originalProjCode.trim().isEmpty()) {
                            String paddedProjCode = String.format("%04d", Integer.parseInt(originalProjCode));
                            contractItem.setProj_code(paddedProjCode);
                        }
                    }
                    allContracts.addAll(additionalContract);
                }
            }
        }
        
        // 보증금과 월세를 원단위로 변환 (10000을 곱함)
        if (allContracts != null && !allContracts.isEmpty()) {
            for (ContractDTO contractItem : allContracts) {
                // deposit_amt: 1000 → 10,000,000원
                if (contractItem.getDeposit_amt() != null && !contractItem.getDeposit_amt().trim().isEmpty()) {
                    try {
                        int depositValue = Integer.parseInt(contractItem.getDeposit_amt());
                        int actualValue = depositValue * 10000;
                        contractItem.setDeposit_amt(String.valueOf(actualValue));
                    } catch (NumberFormatException e) {
                        // 변환 실패 시 원본 값 유지
                    }
                }
                
                // rent_amt: 500 → 5,000,000원
                if (contractItem.getRent_amt() != null && !contractItem.getRent_amt().trim().isEmpty()) {
                    try {
                        int rentValue = Integer.parseInt(contractItem.getRent_amt());
                        int actualValue = rentValue * 10000;
                        contractItem.setRent_amt(String.valueOf(actualValue));
                    } catch (NumberFormatException e) {
                        // 변환 실패 시 원본 값 유지
                    }
                }
            }
        }
        
        // res_type을 코드로 변환하여 ContractDTO에 rsrcCode 설정
        System.out.println("=== rsrcCode 설정 시작 ===");
        System.out.println("allContracts 크기: " + (allContracts != null ? allContracts.size() : "null"));
        
        if (allContracts != null && !allContracts.isEmpty()) {
            for (int i = 0; i < allContracts.size(); i++) {
                ContractDTO contractItem = allContracts.get(i);
                String resType = contractItem.getRes_type();
                System.out.println("Contract " + i + " res_type: '" + resType + "'");
                
                if (resType != null && !resType.trim().isEmpty()) {
                    // res_type을 코드로 변환하여 ContractDTO에 설정
                    String rsrcCode = convertResTypeToCode(resType);
                    contractItem.setRsrcCode(rsrcCode);
                    System.out.println("Contract " + i + " rsrcCode 설정: '" + rsrcCode + "'");
                } else {
                    System.out.println("Contract " + i + " res_type이 null이거나 비어있음, 빈 문자열 설정");
                    contractItem.setRsrcCode(""); // res_type이 null인 경우 빈 문자열 설정
                }
            }
        } else {
            System.out.println("allContracts가 null이거나 비어있음");
        }
        
        System.out.println("=== rsrcCode 설정 완료 ===");
        
        // mainApprNo + additionalApprNos를 합쳐서 전체 appr_no 리스트 생성
        List<Integer> allApprNos = new ArrayList<>();
        allApprNos.add(appr_no);
        if (additionalApprNos != null && !additionalApprNos.isEmpty()) {
            allApprNos.addAll(additionalApprNos);
        }
        
        System.out.println("========================================");
        System.out.println("=== Model에 전달할 데이터 ===");
        System.out.println("mainApprNo: " + appr_no);
        System.out.println("additionalApprNos: " + (additionalApprNos != null ? additionalApprNos : "null"));
        System.out.println("allApprNos: " + allApprNos);
        System.out.println("allApprNos 크기: " + allApprNos.size());
        System.out.println("전체 계약 수: " + (allContracts != null ? allContracts.size() : 0));
        System.out.println("========================================");
        if (allContracts != null) {
            for (ContractDTO c : allContracts) {
                resolveFileLists(c);
            }
        }
        model.addAttribute("contract", allContracts);
        model.addAttribute("mainApprNo", appr_no);
        model.addAttribute("additionalApprNos", additionalApprNos != null ? additionalApprNos : new ArrayList<>());
        model.addAttribute("allApprNos", allApprNos);
        
        return "draft";
    }

    private void resolveFileLists(ContractDTO contract) {
        if (contract == null) return;
        List<Long> realIds = AttachmentFileService.parseFileIds(contract.getReal_estate_files());
        List<Long> creditIds = AttachmentFileService.parseFileIds(contract.getCredit_files());
        contract.setRealEstateFileList(realIds.isEmpty() ? new ArrayList<>() : attachmentFileService.findByIds(realIds));
        contract.setCreditFileList(creditIds.isEmpty() ? new ArrayList<>() : attachmentFileService.findByIds(creditIds));
    }

    /**
     * res_type을 코드로 변환하는 메서드
     * @param resType 자원 타입
     * @return 변환된 코드
     */
    private String convertResTypeToCode(String resType) {
        if (resType == null) return "";
        
        switch (resType) {
            case "원룸":
                return "LR201020001";
            case "공동주택":
                return "LR201040001";
            case "오피스텔":
                return "LR201040001";
            case "아파트":
                return "LR201010001";
            default:
                return "";
        }
    }

    /**
     * 본인이 남긴 결재 의견(appr_remarks)만 수정
     */
    @PostMapping("/update/remarks")
    @ResponseBody
    public ResponseEntity<?> updateApprovalRemarks(HttpServletRequest request) {
        try {
            LoginDTO loginDTO = (LoginDTO) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            int appr_no = Integer.parseInt(request.getParameter("appr_no"));
            int d_seq = Integer.parseInt(request.getParameter("d_seq"));
            String appr_remarks = request.getParameter("appr_remarks");
            if (appr_remarks == null) {
                appr_remarks = "";
            }

            Map<String, Object> result = approvalService.updateOwnApprovalRemarks(
                    appr_no, d_seq, loginDTO.getEmpNo(), appr_remarks);

            if (Boolean.TRUE.equals(result.get("success"))) {
                return ResponseEntity.ok(Map.of("success", true, "message", result.get("message")));
            }
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", result.get("message")));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "의견 저장 중 오류가 발생했습니다."));
        }
    }

    /**
     * 관리자(ERP/거래처 탭 권한자) 결재 마지막 단계 1회 취소 — 요청자 사번은 세션만 사용
     */
    @PostMapping("/admin/cancel-last-step")
    @ResponseBody
    public ResponseEntity<?> adminCancelLastApprovalStep(HttpServletRequest request) {
        try {
            LoginDTO loginDTO = (LoginDTO) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            if (!adminService.canManageErpAndCustomer(loginDTO.getEmpNo())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("success", false, "message", "권한이 없습니다."));
            }
            int appr_no = Integer.parseInt(request.getParameter("appr_no"));
            Map<String, Object> result = approvalService.cancelApprovalAndCleanup(appr_no, loginDTO.getEmpNo(), true);
            if (Boolean.TRUE.equals(result.get("success"))) {
                Object msg = result.get("message");
                return ResponseEntity.ok(Map.of("success", true,
                        "message", msg != null ? msg.toString() : "결재 1단계가 취소되었습니다."));
            }
            Object failMsg = result.get("message");
            return ResponseEntity.badRequest().body(Map.of("success", false,
                    "message", failMsg != null ? failMsg.toString() : "취소할 수 없습니다."));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "처리 중 오류가 발생했습니다."));
        }
    }

    @PostMapping("/cancel")
    @ResponseBody
    public ResponseEntity<?> cancelApproval(HttpServletRequest request) {
        try {
            int appr_no = Integer.parseInt(request.getParameter("appr_no"));
            int appr_emp_no = Integer.parseInt(request.getParameter("appr_emp_no"));
            
            System.out.println("결재취소 요청: appr_no=" + appr_no + ", appr_emp_no=" + appr_emp_no);
            
            // 결재취소 서비스 호출
            Map<String, Object> result = approvalService.cancelApprovalAndCleanup(appr_no, appr_emp_no);
            
            if (result.get("success").equals(true)) {
                return ResponseEntity.ok(Map.of("success", true, "message", "결재가 취소되었습니다."));
            } else {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", (String) result.get("message")));
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "결재취소 처리 중 오류가 발생했습니다."));
        }
    }

}


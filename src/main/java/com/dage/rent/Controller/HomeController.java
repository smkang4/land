package com.dage.rent.Controller;

import com.dage.rent.DAO.mysql.ContractDAO;
import com.dage.rent.DTO.*;
import com.dage.rent.Service.AdminService;
import com.dage.rent.Service.ApprovalService;
import com.dage.rent.Service.ContractService;
import com.dage.rent.Service.DraftService;
import com.dage.rent.Service.RentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Controller
public class HomeController {

    private final RentService rentService;
    private final ContractService contractService;
    private final AdminService adminservice;
    private final ApprovalService approvalService;
    private final DraftService draftService;

    @Autowired
    public HomeController(RentService rentService, ContractDAO contractDAO, ContractService contractService, AdminService admminservice, AdminService adminservice, ApprovalService approvalService, DraftService draftService) {
        this.rentService = rentService;
        this.contractService = contractService;
        this.adminservice = adminservice;
        this.approvalService = approvalService;
        this.draftService = draftService;
    }


    @GetMapping({"/", "/login"})
    public String login(HttpServletRequest request, Model model, @RequestParam(required = false) String error) {
        System.out.println("Login page accessed. Error: " + error);
        
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !auth.getName().equals("anonymousUser")) {
            System.out.println("User already authenticated, redirecting to main");
            return "redirect:/main";
        }
        
        if (error != null) {
            System.out.println("Login error occurred");
            model.addAttribute("error", "���̵� �Ǵ� ��й�ȣ�� ��ġ���� �ʽ��ϴ�.");
        }
        
        return "login";
    }

    @GetMapping("/request")
    public String request(@RequestParam(value = "seq", required = false) Integer seq,
                          HttpServletRequest request,
                          Model model) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getName().equals("anonymousUser")) {
            return "redirect:/login";
        }

        // 현재 사용자의 임시저장 데이터 확인
        LoginDTO loginDTO = (LoginDTO) auth.getPrincipal();
        ContractDTO temp = contractService.getContractDetailForTemp(loginDTO.getUserNo());
        
        if (temp != null) {
            model.addAttribute("temp", temp);
        }

        if (seq != null) {
            ContractDTO contract = contractService.getContractDetail(seq);

            if (contract == null ) {
                return "error"; // 또는 "redirect:/error" 등 적절한 에러 페이지
            }

            model.addAttribute("contract", contract);
        }

        return "index"; // 항상 index 뷰로 이동 (seq 유무에 따라 model만 다름)
    }

    @GetMapping("/list")
    public String list(HttpServletRequest request, Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getName().equals("anonymousUser")) {
            return "redirect:/login";
        }
        return "list";
    }

    @GetMapping("/appr_list")
    public String appr_list(HttpServletRequest request, Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getName().equals("anonymousUser")) {
            return "redirect:/login";
        }
        return "approval/appr_list";
    }

    @GetMapping("/draft_list")
    public String draft_list(HttpServletRequest request, Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getName().equals("anonymousUser")) {
            return "redirect:/login";
        }

        return "list_draft";
    }




    @GetMapping("/main")
    public String main(HttpServletRequest request, Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getName().equals("anonymousUser")) {
            return "redirect:/login";
        }
        List<EmpUserDTO> adminList = adminservice.getAllAdmins();
        LoginDTO loginDTO = (LoginDTO) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String admin = "N";

        for (EmpUserDTO adminDTO : adminList) {
            if (loginDTO.getEmpNo() == adminDTO.getEmp_no()) {
                admin = "Y";
                break;
            }
        }

        model.addAttribute("admin", admin);
        return "main";
    }

    @GetMapping("/admin")
    public String admin(HttpServletRequest request, Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getName().equals("anonymousUser")) {
            return "redirect:/login";
        }
        
        // 현재 로그인한 사용자 정보 가져오기
        LoginDTO loginDTO = (LoginDTO) auth.getPrincipal();
        int userNo = loginDTO.getUserNo();
        int empNo = loginDTO.getEmpNo();
        
        // 삭제 기능 표시 여부: user_no가 26600이거나 emp_no가 35036인 경우만
        boolean showDeleteFeature = (userNo == 26600 || empNo == 35036);
        
        model.addAttribute("showDeleteFeature", showDeleteFeature);
        model.addAttribute("userNo", userNo);
        model.addAttribute("empNo", empNo);
        
        return "admin";
    }

    @GetMapping("/detail")
    public String getRequestDetail(@RequestParam int seq, Model model) {
        try {
            ContractDTO contract = contractService.getContractDetail(seq);
            model.addAttribute("contract", contract);
            model.addAttribute("flag", "Y");

            if(contract.getAppr_no() != null){
                ApprovalDTO appr = contractService.getApprM(contract.getAppr_no());
            }

            // 면적을 평으로 변환
            if (contract.getArea() != null && !contract.getArea().isEmpty()) {
                double area = Double.parseDouble(contract.getArea());
                double areaInPyeong = Math.round((area / 3.30578) * 100.0) / 100.0;
                model.addAttribute("areaInPyeong", areaInPyeong);
            }

            return "detail";
        } catch (Exception e) {
            e.printStackTrace();
            return "error";
        }
    }

    @GetMapping("/view")
    public String getRequestDetailView(@RequestParam int seq, Model model) {
        try {

            List<ApprovalDDTO> apprList = approvalService.getApprovalDetail(seq);

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

                if(apprList.get(i).getAppr_emp_no() == 58){
                    last_flag = "T";
                }
            }

            if(b_cnt == 0 && request_conf.equals("T")){
                admin_tg = "T";
                appr_d_seq = last_appr_d_seq;
                appr_num =  last_appr_num;
            }else{
                admin_tg = "F";
            }

            List<ContractDTO> contract = contractService.getContractDetailForList(seq);
            // 파일 데이터 로깅 추가
            for (ContractDTO c : contract) {
                System.out.println("Contract files for seq " + c.getSeq() + ":");
                System.out.println("Real estate files: " + c.getReal_estate_files());
                System.out.println("Credit files: " + c.getCredit_files());

                // 면적을 평으로 변환
                if (c.getArea() != null && !c.getArea().isEmpty()) {
                    double area = Double.parseDouble(c.getArea());
                    double areaInPyeong = Math.round((area / 3.30578) * 100.0) / 100.0;
                    model.addAttribute("areaInPyeong", areaInPyeong);
                }
            }
            model.addAttribute("contract", contract);

            model.addAttribute("appr_list",apprList);
            model.addAttribute("admin_tg",admin_tg);
            model.addAttribute("cur_emp_no",cur_emp_no);
            model.addAttribute("appr_d_seq",appr_d_seq);
            model.addAttribute("appr_num",appr_num);
            model.addAttribute("last",last_flag);

            return "view/view";

        } catch (Exception e) {
            e.printStackTrace();
            return "error";
        }
    }

    @GetMapping("/edit")
    public String edit(@RequestParam int seq, Model model) {
        try {

            ContractDTO contract = contractService.getContractDetail(seq);
            model.addAttribute("detail", contract);
            model.addAttribute("rewrite", "N");

            return "edit";
        } catch (Exception e) {
            e.printStackTrace();
            return "error";
        }
    }

    @GetMapping("/rewrite")
    public String rewrite(@RequestParam int seq, Model model) {
        try {

            ContractDTO contract = contractService.getContractDetail(seq);
            model.addAttribute("detail", contract);
            model.addAttribute("rewrite", "Y");

            return "edit";
        } catch (Exception e) {
            e.printStackTrace();
            return "error";
        }
    }

    @GetMapping("/receipt")
    public String accept(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getName().equals("anonymousUser")) {
            return "redirect:/login";
        }
        try {
            List<ContractDTO> contract = contractService.getContractDetailForReceipt();
            model.addAttribute("detail", contract);

             return "receipt";
        } catch (Exception e) {
            e.printStackTrace();
            return "error";
        }
    }

    @GetMapping("/apply")
    public String apply(HttpServletRequest request, Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getName().equals("anonymousUser")) {
            return "redirect:/login";
        }
        return "apply";
    }

    @GetMapping("/draft")
    public String draft(HttpServletRequest request, 
                       @RequestParam(required = false) Integer appr_no,
                       @RequestParam(value = "additional_appr_nos", required = false) List<Integer> additionalApprNos,
                       Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getName().equals("anonymousUser")) {
            return "redirect:/login";
        }

        // 기존 로직 (appr_no가 있는 경우)
        if (appr_no != null) {
            return handleNormalDraftMode(appr_no, additionalApprNos, model);
        }

        // appr_no가 없는 경우 빈 기안서 작성
        return handleEmptyDraftMode(model);
    }

    private String handleNormalDraftMode(int appr_no, List<Integer> additionalApprNos, Model model) {
        
        // 메인 appr_no의 계약 데이터 조회
        List<ContractDTO> contract = contractService.getContractDetailForList(appr_no);
        
        System.out.println("=== HomeController.draft 메서드 디버깅 ===");
        System.out.println("입력 appr_no: " + appr_no);
        System.out.println("조회된 contract 데이터: " + contract);
        System.out.println("contract 크기: " + (contract != null ? contract.size() : "null"));
        
        // 추가 appr_no들의 계약 데이터 조회
        List<ContractDTO> allContracts = new ArrayList<>();
        if (contract != null && !contract.isEmpty()) {
            allContracts.addAll(contract);
        }
        
        // 추가 appr_no들의 계약 데이터도 조회
        if (additionalApprNos != null && !additionalApprNos.isEmpty()) {
            System.out.println("=== 추가 appr_no들의 계약 데이터 조회 ===");
            for (Integer additionalApprNo : additionalApprNos) {
                System.out.println("추가 appr_no: " + additionalApprNo);
                List<ContractDTO> additionalContract = contractService.getContractDetailForList(additionalApprNo);
                if (additionalContract != null && !additionalContract.isEmpty()) {
                    allContracts.addAll(additionalContract);
                    System.out.println("추가된 계약 데이터 수: " + additionalContract.size());
                }
            }
            System.out.println("전체 계약 데이터 수: " + allContracts.size());
        }
        
        // res_type을 코드로 변환하여 ContractDTO에 rsrcCode 설정
        System.out.println("=== HomeController rsrcCode 설정 시작 ===");
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
        
        System.out.println("=== HomeController rsrcCode 설정 완료 ===");
        
        // mainApprNo + additionalApprNos를 합쳐서 전체 appr_no 리스트 생성
        List<Integer> allApprNos = new ArrayList<>();
        allApprNos.add(appr_no);
        if (additionalApprNos != null && !additionalApprNos.isEmpty()) {
            allApprNos.addAll(additionalApprNos);
        }
        
        model.addAttribute("contract", allContracts);
        model.addAttribute("appr_no", appr_no);
        model.addAttribute("mainApprNo", appr_no);
        model.addAttribute("additionalApprNos", additionalApprNos != null ? additionalApprNos : new ArrayList<>());
        model.addAttribute("allApprNos", allApprNos); // 전체 appr_no 리스트 추가!

        return "draft";
    }

    private String handleEmptyDraftMode(Model model) {
        System.out.println("=== 빈 기안서 모드 처리 ===");
        
        model.addAttribute("contract", new ArrayList<>());
        model.addAttribute("appr_no", 0);
        model.addAttribute("mainApprNo", 0);
        model.addAttribute("additionalApprNos", new ArrayList<>());
        
        return "draft";
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

    @GetMapping("/draft-document")
    public String draftDocument(HttpServletRequest request, Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getName().equals("anonymousUser")) {
            return "redirect:/login";
        }
        return "draft-document";
    }

}


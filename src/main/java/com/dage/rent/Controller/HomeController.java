package com.dage.rent.Controller;

import com.dage.rent.DAO.mysql.ContractDAO;
import com.dage.rent.DTO.*;
import com.dage.rent.Service.ContractService;
import com.dage.rent.Service.RentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@Controller
public class HomeController {

    private final RentService rentService;
    private final ContractService contractService;

    @Autowired
    public HomeController(RentService rentService, ContractDAO contractDAO, ContractService contractService) {
        this.rentService = rentService;
        this.contractService = contractService;
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

    @GetMapping("/main")
    public String main(HttpServletRequest request, Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getName().equals("anonymousUser")) {
            return "redirect:/login";
        }
        return "main";
    }

    @GetMapping("/admin")
    public String admin(HttpServletRequest request, Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getName().equals("anonymousUser")) {
            return "redirect:/login";
        }
        return "admin";
    }

    @GetMapping("/detail")
    public String getRequestDetail(@RequestParam int seq, Model model) {
        try {

            ContractDTO contract = contractService.getContractDetail(seq);
            model.addAttribute("contract", contract);

            if(contract.getAppr_no() != null){
                ApprovalDTO appr = contractService.getApprM(contract.getAppr_no());
            }

            return "detail";
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

            return "edit";
        } catch (Exception e) {
            e.printStackTrace();
            return "error";
        }
    }

}


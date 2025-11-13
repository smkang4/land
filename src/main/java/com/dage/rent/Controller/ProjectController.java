package com.dage.rent.Controller;

import com.dage.rent.DTO.ComCodeDTO;
import com.dage.rent.DTO.EmpUserDTO;
import com.dage.rent.DTO.LoginDTO;
import com.dage.rent.DTO.ProjDTO;
import com.dage.rent.Service.AdminService;
import com.dage.rent.Service.RentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class ProjectController {

    private final RentService service;
    private final AdminService adminService;


    @Autowired
    public ProjectController(RentService service, AdminService adminService) {
        this.service = service;
        this.adminService = adminService;
    }

    @GetMapping("/projects")
    public ResponseEntity<List<ComCodeDTO>> getProjects() {
        List<ComCodeDTO> projects = service.getAllProjects();
        return ResponseEntity.ok(projects);
    }

    @GetMapping("/code/proj/{user_no}")
    public ResponseEntity<List<ComCodeDTO>> getSelectProjects(@PathVariable("user_no") String user_no) {
        List<ComCodeDTO> codeDTOS = service.getSelectProjects(user_no);
        return ResponseEntity.ok(codeDTOS);
    }

    @GetMapping("/code/user")
    public ResponseEntity<List<ComCodeDTO>> getUserList() {
        List<ComCodeDTO> codeDTOS = service.getUserList();
        return ResponseEntity.ok(codeDTOS);
    }
    @GetMapping("/code/proj/user")
    public ResponseEntity<List<ComCodeDTO>> getProjUserList() {
        LoginDTO loginDTO = (LoginDTO) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        List<ComCodeDTO> codeDTOS = service.getProjUserList(loginDTO.getHeadCode());
        return ResponseEntity.ok(codeDTOS);
    }
    @GetMapping("/code/proj/admin")
    public ResponseEntity<List<EmpUserDTO>> getProjAdminList() {
        List<EmpUserDTO> codeDTOS = adminService.getAllAdmins();
        return ResponseEntity.ok(codeDTOS);
    }
} 
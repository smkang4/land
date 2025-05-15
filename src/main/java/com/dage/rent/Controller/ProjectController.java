package com.dage.rent.Controller;

import com.dage.rent.DTO.ProjDTO;
import com.dage.rent.Service.RentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class ProjectController {

    private final RentService service;

    @Autowired
    public ProjectController(RentService service) {
        this.service = service;
    }

    @GetMapping("/projects")
    public ResponseEntity<List<ProjDTO>> getProjects() {
        List<ProjDTO> projects = service.getAllProjects();
        return ResponseEntity.ok(projects);
    }
} 
package com.dage.rent.Controller;

import com.dage.rent.Service.RentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/erp")
public class ErpController {

    @Autowired
    private RentService rentService;

    /**
     * ERP 거래처 검색 API
     * @param custCode 검색할 거래처코드 또는 거래처명
     * @return 거래처 정보 목록
     */
    @GetMapping("/search/customers")
    public ResponseEntity<?> searchErpCustomers(@RequestParam String custCode) {
        try {
            List<Map<String, Object>> customers = rentService.searchErpCustomers(custCode);
            return ResponseEntity.ok().body(Map.of("success", true, "data", customers));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "거래처 검색 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }
}


package com.dage.rent.Controller;

import com.dage.rent.Component.Mail;
import com.dage.rent.DTO.ComCodeDTO;
import com.dage.rent.DTO.DraftDTO;
import com.dage.rent.DTO.LoginDTO;
import com.dage.rent.Service.DraftService;
import com.dage.rent.Service.DraftStatusService;
import com.dage.rent.Service.RentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/draft")
public class DraftController {

    @Autowired
    private DraftService draftService;
    
    @Autowired
    private DraftStatusService draftStatusService;
    
    @Autowired
    private RentService rentService;
    
    @Autowired
    private Mail mail;

    @GetMapping("/{draftId}")
    @ResponseBody
    public ResponseEntity<DraftDTO> getDraftById(@PathVariable Long draftId) {
        try {
            System.out.println("기안서 정보 조회 요청 - draftId: " + draftId);
            
            DraftDTO draft = draftService.getDraftByIdLong(draftId);
            
            if (draft == null) {
                return ResponseEntity.notFound().build();
            }
            
            System.out.println("기안서 정보 조회 성공 - draftId: " + draftId);
            return ResponseEntity.ok(draft);
            
        } catch (Exception e) {
            System.err.println("기안서 정보 조회 오류: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        }
    }
    
    @GetMapping("/get-by-id")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getDraftByIdParam(@RequestParam Long draftId) {
        try {
            System.out.println("기안서 정보 조회 요청 - draftId: " + draftId);
            
            DraftDTO draft = draftService.getDraftByIdLong(draftId);
            
            if (draft == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "기안서를 찾을 수 없습니다.");
                return ResponseEntity.ok(response);
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("draft", draft);
            
            System.out.println("기안서 정보 조회 성공 - draftId: " + draftId);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.err.println("기안서 정보 조회 오류: " + e.getMessage());
            e.printStackTrace();
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "기안서 조회 중 오류가 발생했습니다: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @PostMapping("/save")
    public ResponseEntity<?> saveDraft(@RequestBody DraftDTO draftDTO) {
        try {
            // 현재 로그인한 사용자 확인
            Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            if (principal == null || "anonymousUser".equals(principal)) {
                return ResponseEntity.status(401).body(Map.of(
                    "success", false,
                    "message", "로그인이 필요합니다."
                ));
            }

            System.out.println("=== Draft Save Request ===");
            System.out.println("appr_no: " + draftDTO.getAppr_no());
            System.out.println("emp_no: " + draftDTO.getEmp_no());
            System.out.println("user_nm: " + draftDTO.getUser_nm());
            System.out.println("proj_code: " + draftDTO.getProj_code());
            System.out.println("proj_name: " + draftDTO.getProj_name());
            System.out.println("gw_code: " + draftDTO.getGw_code());
            System.out.println("mst_seq: " + draftDTO.getMst_seq());
            System.out.println("rent_reason: " + draftDTO.getRent_reason());
            System.out.println("rent_source: " + draftDTO.getRent_source());
            
            if (draftDTO.getContractDetails() != null) {
                System.out.println("Contract Details Count: " + draftDTO.getContractDetails().size());
                draftDTO.getContractDetails().forEach(detail -> {
                    System.out.println("Row " + detail.getRowNo() + 
                                     ": appr_no=" + detail.getAppr_no() + 
                                     ", type=" + detail.getType() + 
                                     ", address=" + detail.getAddress());
                });
            }

            // 실제 DB 저장 로직 실행
            int result = draftService.saveDraft(draftDTO);

            Map<String, Object> response = new HashMap<>();
            if (result > 0) {
                // 기안서 작성 단계에서는 메일 발송하지 않음 (기안서 업로드 시에만 발송)
                System.out.println("기안서 작성 완료 - 메일은 업로드 시 발송됨");
                
                response.put("success", true);
                response.put("message", "기안서가 성공적으로 저장되었습니다.");
                response.put("draftId", draftDTO.getId()); // 저장된 기안서 ID 반환
            } else {
                response.put("success", false);
                response.put("message", "기안서 저장에 실패했습니다.");
            }
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "기안서 저장 중 오류가 발생했습니다: " + e.getMessage()
            ));
        }
    }

    @PostMapping("/update")
    public ResponseEntity<?> updateDraft(@RequestBody DraftDTO draftDTO) {
        try {
            // 현재 로그인한 사용자 정보 가져오기
            LoginDTO loginDTO = (LoginDTO) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            if (loginDTO == null || "anonymousUser".equals(loginDTO)) {
                return ResponseEntity.status(401).body(Map.of(
                    "success", false,
                    "message", "로그인이 필요합니다."
                ));
            }

            System.out.println("=== Draft Update Request ===");
            System.out.println("draftId: " + draftDTO.getId());
            System.out.println("rent_reason: " + draftDTO.getRent_reason());
            System.out.println("rent_source: " + draftDTO.getRent_source());
            System.out.println("execution_budget: " + draftDTO.getExecution_budget());
            
            if (draftDTO.getContractDetails() != null) {
                System.out.println("Contract Details Count: " + draftDTO.getContractDetails().size());
                draftDTO.getContractDetails().forEach(detail -> {
                    System.out.println("Row " + detail.getRowNo() + ": " + detail.getType() + ", " + detail.getAddress());
                });
            }

            // 실제 DB 업데이트 로직 실행
            int result = draftService.updateDraft(draftDTO);

            Map<String, Object> response = new HashMap<>();
            if (result > 0) {
                
                response.put("success", true);
                response.put("message", "기안서가 성공적으로 수정되었습니다.");
                response.put("draftId", draftDTO.getId());
            } else {
                response.put("success", false);
                response.put("message", "기안서 수정에 실패했습니다.");
            }
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "기안서 수정 중 오류가 발생했습니다: " + e.getMessage()
            ));
        }
    }
    


    @GetMapping("/getGwPjcode")
    public ResponseEntity<Map<String, Object>> getGwPjcode(@RequestParam("proj_code") String proj_code) {
        try {
            System.out.println("=== getGwPjcode API 호출 ===");
            System.out.println("입력 proj_code: " + proj_code);
            
            String code = draftService.getGwPjcode(proj_code);
            
            System.out.println("조회된 code: " + code);
            System.out.println("code 타입: " + (code != null ? code.getClass().getSimpleName() : "null"));
            System.out.println("code 길이: " + (code != null ? code.length() : "null"));
            
            Map<String, Object> response = new HashMap<>();
            response.put("code", code != null ? code : "0");
            
            System.out.println("응답 데이터: " + response);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> response = new HashMap<>();
            response.put("code", "0");
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/getMstSeq")
    public ResponseEntity<Map<String, Object>> getMstSeq() {
        try {
            String seq = draftService.getMstSeq();
            Map<String, Object> response = new HashMap<>();
            response.put("seq", seq != null ? seq : "0");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> response = new HashMap<>();
            response.put("seq", "0");
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/getCustCode")
    public ResponseEntity<Map<String, Object>> getCustCode() {
        try {
            String custCode = rentService.getCustCode();
            Map<String, Object> response = new HashMap<>();
            response.put("custCode", custCode != null ? custCode : "0");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> response = new HashMap<>();
            response.put("custCode", "0");
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/get-by-mstseq")
    public ResponseEntity<Map<String, Object>> getDraftByMstSeq(@RequestParam("mstSeq") String mstSeq) {
        try {
            System.out.println("=== getDraftByMstSeq API 호출 ===");
            System.out.println("입력 mstSeq: " + mstSeq);
            
            DraftDTO draft = draftService.getDraftByMstSeq(mstSeq);
            
            Map<String, Object> response = new HashMap<>();
            if (draft != null) {
                response.put("success", true);
                response.put("draft", draft);
                System.out.println("기안서 조회 성공: " + draft.getId());
            } else {
                response.put("success", false);
                response.put("message", "해당 mstSeq에 대한 기안서를 찾을 수 없습니다.");
                System.out.println("기안서 조회 실패: mstSeq = " + mstSeq);
            }
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "기안서 조회 중 오류가 발생했습니다: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/checkDuplicateBizNo")
    public ResponseEntity<Map<String, Object>> checkDuplicateBizNo(@RequestParam String bizNo) {
        try {
            int count = rentService.checkDuplicateBizNo(bizNo);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("isDuplicate", count > 0);
            response.put("count", count);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "사업자등록번호 중복 체크 실패: " + e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @GetMapping("/getExistingCustomers")
    public ResponseEntity<Map<String, Object>> getExistingCustomers(@RequestParam String bizNo) {
        try {
            System.out.println("=== 기존 거래처 조회 시작 ===");
            System.out.println("입력된 bizNo: " + bizNo);
            
            List<ComCodeDTO> customers = rentService.getExistingCustomers(bizNo);
            
            System.out.println("조회된 거래처 개수: " + (customers != null ? customers.size() : "null"));
            if (customers != null) {
                for (int i = 0; i < customers.size(); i++) {
                    ComCodeDTO customer = customers.get(i);
                    System.out.println("거래처 " + i + ": " + customer);
                }
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("customers", customers);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "기존 거래처 조회 실패: " + e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @GetMapping("/banks")
    public ResponseEntity<List<ComCodeDTO>> getBankList() {
        try {
            List<ComCodeDTO> banks = rentService.getBankList();
            return ResponseEntity.ok(banks);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/callCustProjProcedure")
    public ResponseEntity<Map<String, Object>> callCustProjProcedure(@RequestBody Map<String, Object> custProjData) {
        try {
            System.out.println("=== 고객 프로젝트 프로시저 호출 ===");
            System.out.println("전달된 데이터: " + custProjData);
            
            rentService.callCustProjProcedure(custProjData);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "고객 프로젝트 등록이 완료되었습니다.");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "고객 프로젝트 등록 중 오류가 발생했습니다: " + e.getMessage());
            
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    @GetMapping("/getExistingCustomersDebug")
    public ResponseEntity<Map<String, Object>> getExistingCustomersDebug(@RequestParam String bizNo) {
        try {
            System.out.println("=== 기존 거래처 디버그 조회 시작 ===");
            System.out.println("입력된 bizNo: " + bizNo);
            
            List<ComCodeDTO> customers = rentService.getExistingCustomersDebug(bizNo);
            
            System.out.println("디버그 조회된 거래처 개수: " + (customers != null ? customers.size() : "null"));
            if (customers != null) {
                for (int i = 0; i < customers.size(); i++) {
                    ComCodeDTO customer = customers.get(i);
                    System.out.println("디버그 거래처 " + i + ": " + customer);
                }
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("customers", customers);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "기존 거래처 디버그 조회 실패: " + e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }


    // 프로시저 실행 (오라클 프로시저 호출)
    @PostMapping("/process")
    public ResponseEntity<Map<String, Object>> processDrafts(@RequestBody Map<String, Object> request) {
        try {
            System.out.println("=== 프로시저 실행 요청 받음 ===");
            System.out.println("요청 데이터: " + request);
            
            // draftIds를 안전하게 변환
            List<Long> draftIds = new java.util.ArrayList<>();
            Object draftIdsObj = request.get("draftIds");
            
            if (draftIdsObj instanceof List) {
                @SuppressWarnings("unchecked")
                List<?> rawList = (List<?>) draftIdsObj;
                for (Object obj : rawList) {
                    if (obj instanceof Number) {
                        draftIds.add(((Number) obj).longValue());
                    } else if (obj instanceof String) {
                        try {
                            draftIds.add(Long.parseLong((String) obj));
                        } catch (NumberFormatException e) {
                            System.err.println("잘못된 draft ID 형식: " + obj);
                        }
                    }
                }
            }
            
            System.out.println("변환된 draft ID 목록: " + draftIds);
            System.out.println("처리할 draft ID 개수: " + draftIds.size());
            
            if (draftIds.isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "처리할 기안서가 없습니다.");
                return ResponseEntity.badRequest().body(response);
            }
            
            int processedCount = draftService.processDraftProcedures(draftIds);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("processedCount", processedCount);
            response.put("message", processedCount + "개의 기안서 프로시저 처리 완료");
            
            System.out.println("프로시저 실행 완료: " + processedCount + "개 처리");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "프로시저 실행 중 오류가 발생했습니다: " + e.getMessage());
            
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    // 결재문서 생성 (그룹웨어 연동)
    @PostMapping("/generate-approval")
    public ResponseEntity<Map<String, Object>> generateApprovalDocuments(@RequestBody Map<String, Object> request) {
        try {
            @SuppressWarnings("unchecked")
            List<Long> draftIds = (List<Long>) request.get("draftIds");
            
            System.out.println("=== 결재문서 생성 시작 ===");
            System.out.println("처리할 draft ID 개수: " + (draftIds != null ? draftIds.size() : 0));
            
            if (draftIds == null || draftIds.isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "처리할 기안서가 없습니다.");
                return ResponseEntity.badRequest().body(response);
            }
            
            int generatedCount = draftService.generateApprovalDocuments(draftIds);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("generatedCount", generatedCount);
            response.put("message", generatedCount + "개의 결재문서 생성 완료");
            
            System.out.println("결재문서 생성 완료: " + generatedCount + "개 처리");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "결재문서 생성 중 오류가 발생했습니다: " + e.getMessage());
            
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }


 
}
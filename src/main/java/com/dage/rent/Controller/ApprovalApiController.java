package com.dage.rent.Controller;

import com.dage.rent.DTO.ComCodeDTO;
import com.dage.rent.DTO.DraftDTO;
import com.dage.rent.DTO.LoginDTO;
import com.dage.rent.Service.DraftService;
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
@RequestMapping("/api/approval")
@CrossOrigin(origins = "*")
public class ApprovalApiController {

    @Autowired
    private DraftService draftService;
    
    @Autowired
    private RentService rentService;

    @GetMapping("/generate-document")
    public ResponseEntity<Map<String, Object>> generateDocument(
            @RequestParam("draftId") Long draftId,
            @RequestParam("projName") String projName,
            @RequestParam("gwCode") String gwCode,
            @RequestParam("mstSeq") String mstSeq,
            @RequestParam("empNo") String empNo,
            @RequestParam("userNo") String userNo,
            @RequestParam("projCode") String projCode,
            @RequestParam("eaId") String eaId,
            @RequestParam(value = "attachmentList", required = false, defaultValue = "") String attachmentList
    ) {
        try {
            DraftDTO draft = draftService.getDraftById(draftId.intValue());
            
            System.out.println("첨부파일 리스트 파라미터: " + attachmentList);
            
            // 첨부파일 리스트를 HTML 생성 시 전달
            String htmlContent = draftService.generateApprovalDocumentHTML(draft, attachmentList);
            
            // Oracle에서 E_DOC_CODE와 E_DOC_NAME 조회 (eaId는 클라이언트에서 전달받음)
            Map<String, Object> eDocInfo = draftService.getEDocCodeAndName(eaId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("html", htmlContent);
            response.put("eDocCode", eDocInfo.get("E_DOC_CODE"));
            response.put("eDocName", eDocInfo.get("E_DOC_NAME"));
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "결재문서 HTML 생성 중 오류가 발생했습니다: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    // mstSeq를 사용해서 기안서 문서 HTML 생성
    @GetMapping("/generate-document-by-mstseq")
    public ResponseEntity<Map<String, Object>> generateDocumentByMstSeq(
            @RequestParam("mstSeq") String mstSeq
    ) {
        try {
            // mstSeq를 사용해서 draft 정보 조회
            DraftDTO draft = draftService.getDraftByMstSeq(mstSeq);
            
            if (draft == null) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "해당 mstSeq에 대한 기안서를 찾을 수 없습니다.");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
            }
            
            String htmlContent = draftService.generateApprovalDocumentHTML(draft);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("html", htmlContent);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "기안서 문서 HTML 생성 중 오류가 발생했습니다: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @PostMapping("/send-to-erp")
    public ResponseEntity<Map<String, Object>> sendToERP(@RequestBody Map<String, Object> erpData) {
        try {
            System.out.println("=== ERP 전송 요청 데이터 ===");
            System.out.println("전체 데이터: " + erpData);
            System.out.println("erpData.custCode: " + erpData.get("custCode"));
            System.out.println("erpData.custCode 타입: " + (erpData.get("custCode") != null ? erpData.get("custCode").getClass().getSimpleName() : "null"));
            
            // draftId가 없으면 추가 (MySQL 조회용)
            if (!erpData.containsKey("draftId")) {
                // mstSeq를 draftId로 사용 (임시 해결책)
                Object mstSeq = erpData.get("mstSeq");
                if (mstSeq != null) {
                    erpData.put("draftId", mstSeq);
                    System.out.println("draftId 추가됨: " + mstSeq);
                }
            }
            
            // HTML 내용을 erpData에 추가 (Windows 네트워크 공유 업로드용)
            String htmlContent = (String) erpData.get("html");
            System.out.println("HTML 내용 존재 여부: " + (htmlContent != null));
            
            if (htmlContent == null) {
                // HTML이 없으면 draftId로 조회
                Long draftId = Long.valueOf(erpData.get("draftId").toString());
                System.out.println("Draft ID: " + draftId);
                DraftDTO draft = draftService.getDraftById(draftId.intValue());
                htmlContent = draftService.generateApprovalDocumentHTML(draft);
                erpData.put("html", htmlContent);
                System.out.println("HTML 생성 완료, 길이: " + htmlContent.length());
            } else {
                // Base64로 인코딩된 HTML을 디코딩
                try {
                    // Base64 패턴 확인 (일반적으로 Base64는 A-Z, a-z, 0-9, +, /, = 문자만 포함)
                    if (htmlContent.matches("^[A-Za-z0-9+/]*={0,2}$")) {
                        byte[] decodedBytes = java.util.Base64.getDecoder().decode(htmlContent);
                        String decodedHtml = new String(decodedBytes, "UTF-8");
                        erpData.put("html", decodedHtml);
                        System.out.println("Base64 HTML 디코딩 완료, 길이: " + decodedHtml.length());
                    } else {
                        System.out.println("HTML이 이미 디코딩된 상태입니다.");
                    }
                } catch (Exception e) {
                    System.err.println("Base64 디코딩 실패, 원본 HTML 사용: " + e.getMessage());
                    // 디코딩 실패 시 원본 사용 (이미 디코딩된 상태일 수 있음)
                }
            }
            
            // 거래처 등록 로직 제거 - 1차+2차 방안으로 대체
            System.out.println("=== 거래처 등록 로직 제거됨 ===");
            System.out.println("거래처 등록은 1차(기안완료 탭 접속 시) 또는 2차(스케줄러)에서 처리됩니다.");
            
            System.out.println("ERP 프로시저 호출 시작...");
            // ERP 프로시저 호출
            boolean result = draftService.callERPProcedure(erpData);
            
            Map<String, Object> response = new HashMap<>();
            if (result) {
                System.out.println("✅ ERP 프로시저 호출 성공!");
                
                // ERP 프로시저 성공
                response.put("success", true);
                response.put("message", "ERP 프로시저가 성공적으로 완료되었습니다.");
            } else {
                response.put("success", false);
                response.put("message", "ERP 프로시저 호출에 실패했습니다.");
                System.out.println("❌ ERP 프로시저 호출 실패!");
            }
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "ERP 프로시저 호출 중 오류가 발생했습니다: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * MySQL에서 custCode 조회
     */
    private String getCustCodeFromMySQL(Map<String, Object> erpData) {
        try {
            // draftId로 MySQL에서 custCode 조회
            Object draftIdObj = erpData.get("draftId");
            System.out.println("draftIdObj: " + draftIdObj + " (타입: " + (draftIdObj != null ? draftIdObj.getClass().getSimpleName() : "null") + ")");
            
            if (draftIdObj == null) {
                System.out.println("❌ draftId가 null입니다!");
                return null;
            }
            
            String draftId = draftIdObj.toString();
            System.out.println("변환된 draftId: " + draftId);
            
            String custCode = draftService.getCustCodeByDraftId(draftId);
            System.out.println("MySQL 조회 결과 custCode: " + custCode);
            return custCode;
        } catch (Exception e) {
            System.out.println("MySQL에서 custCode 조회 실패: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

} 
package com.dage.rent.Controller;

import com.dage.rent.DTO.*;
import com.dage.rent.Service.ContractService;
import com.dage.rent.Service.EmailService;
import com.dage.rent.Service.RentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/api")
public class RequestController {

    @Autowired
    private  ContractService contractService;
    private final RentService rentService;
    private final EmailService emailService;
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Autowired
    public RequestController(RentService rentService, EmailService emailService) {
        this.rentService = rentService;
        this.emailService = emailService;
    }

    @GetMapping("/cont_list/{proj_code}")
    @ResponseBody
    public List<ContractDTO> getContractList(@PathVariable("proj_code") String proj_code){
        List<ContractDTO> contList = contractService.getContractList(proj_code);
        return contList;
    }


    @GetMapping("/cont_list_all")
    @ResponseBody
    public List<ContractDTO> getContractListForAppr(){
        List<ContractDTO> contList = contractService.getContractListForAppr();
        return contList;
    }

    @GetMapping("/contracts")
    @ResponseBody
    public ResponseEntity<?> getContracts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "6") int size,
            @RequestParam(required = false) String search) {
        try {
            LoginDTO loginDTO = (LoginDTO) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            Pageable pageable = PageRequest.of(page, size);
            

            Integer empNo = loginDTO.getUserNo() == 26601 ? null : loginDTO.getUserNo();
            
            Page<ContractMDTO> pageResult = rentService.getContracts(empNo, search, pageable);
            
          
            Map<String, Object> response = new HashMap<>();
            response.put("content", pageResult.getContent());
            response.put("currentPage", pageResult.getNumber());
            response.put("totalItems", pageResult.getTotalElements());
            response.put("totalPages", pageResult.getTotalPages());
            response.put("size", pageResult.getSize());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "오류가 발생했습니다."
            ));
        }
    }

    @GetMapping("/admin/contracts")
    @ResponseBody
    public ResponseEntity<?> getAdminContracts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "6") int size,
            @RequestParam(required = false) String search) {
        try {
            LoginDTO loginDTO = (LoginDTO) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            
            // 관리자페이지는 관리자만 접속 USER_NO
           /*  if (loginDTO.getUserNo() != 26601) {
                return ResponseEntity.status(403).body(Map.of(
                    "success", false,
                    "message", "관리자만 접속할 수 있습니다."
                ));
            }*/

            Pageable pageable = PageRequest.of(page, size);
            Page<ContractMDTO> pageResult = rentService.getContracts(null, search, pageable);
            
            Map<String, Object> response = new HashMap<>();
            response.put("content", pageResult.getContent());
            response.put("currentPage", pageResult.getNumber());
            response.put("totalItems", pageResult.getTotalElements());
            response.put("totalPages", pageResult.getTotalPages());
            response.put("size", pageResult.getSize());
            
            return ResponseEntity.ok()
                .header("Content-Type", "application/json;charset=UTF-8")
                .body(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest()
                .header("Content-Type", "application/json;charset=UTF-8")
                .body(Map.of(
                    "success", false,
                    "message", "오류가 발생했습니다."
                ));
        }
    }

    @PostMapping("/submit-request")
    @ResponseBody
    public ResponseEntity<?> submitRequest(@RequestBody Map<String, Object> requestData) {
        try {

            LoginDTO loginDTO = (LoginDTO) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

            Integer empNo = loginDTO.getUserNo();
            String userNm = loginDTO.getUserName();
            Integer projCode = requestData.get("projCode") != null ? Integer.parseInt(requestData.get("projCode").toString()) : null;
            String projName = (String) requestData.get("projName");
            String contDateStr = (String) requestData.get("contDate");
            String moveDateStr = (String) requestData.get("moveDate");

            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            LocalDate contDate = contDateStr != null ? LocalDate.parse(contDateStr, dateFormatter) : null;
            LocalDate moveDate = moveDateStr != null ? LocalDate.parse(moveDateStr, dateFormatter) : null;

            Integer contAmt = requestData.get("contAmt") != null ? Integer.parseInt(requestData.get("contAmt").toString()) : null;
            Integer depositAmt = requestData.get("depositAmt") != null ? Integer.parseInt(requestData.get("depositAmt").toString()) : null;
            Integer rentAmt = requestData.get("rentAmt") != null ? Integer.parseInt(requestData.get("rentAmt").toString()) : null;
            String resType = (String) requestData.get("resType");
            String transType = (String) requestData.get("transType");
            Integer area = requestData.get("area") != null ? Integer.parseInt(requestData.get("area").toString()) : null;
            Integer accu = requestData.get("accu") != null ? Integer.parseInt(requestData.get("accu").toString()) : null;
            String source = (String) requestData.get("source");

            String address = (String) requestData.get("address");
            String addressD = (String) requestData.get("addressD");

            String fullAddress = address.replaceAll("\\s+", "") + addressD.replaceAll("\\s+", "");

            //주소 중복값 확인
            List<ContractDDTO> dtoList = rentService.getContractAddress();
            for (ContractDDTO dto : dtoList) {
                String existingAddress = (dto.getAddress() + dto.getAddressD()).replaceAll("\\s+", "");
                if (fullAddress.equalsIgnoreCase(existingAddress)) {
                    // 중복일 경우 바로 리턴
                    throw new RuntimeException();
                }
            }

            ContractMDTO contractM = new ContractMDTO();
            contractM.setEmpNo(empNo);
            contractM.setUserNm(userNm);
            contractM.setProjCode(projCode);
            contractM.setProjName(projName);
            contractM.setCrtdate(LocalDateTime.now());


            ContractDDTO contractD = new ContractDDTO();
            contractD.setContDate(contDate);
            contractD.setMoveDate(moveDate);
            contractD.setContAmt(contAmt);
            contractD.setDepositAmt(depositAmt);
            contractD.setRentAmt(rentAmt);
            contractD.setAddress(address);
            contractD.setAddressD(addressD);
            contractD.setResType(resType);
            contractD.setTransType(transType);
            contractD.setArea(area);
            contractD.setCrtdate(LocalDateTime.now());
            contractD.setAccu(accu);
            contractD.setSource(source);

            // checklist 처리
            List<Map<String, Object>> checklist = (List<Map<String, Object>>) requestData.get("checklist");


            for (int i = 0; i < checklist.size(); i++) {
                Map<String, Object> item = checklist.get(i);
                boolean isYes = Boolean.TRUE.equals(item.get("isYes"));
                String value = isYes ? "Y" : "N";

                String reasonValue = (String) item.get("reason");

                switch (i) {
                    case 0:
                        contractD.setChk1(value);
                        if ("N".equals(value)) contractD.setChkReason1(reasonValue);
                        break;
                    case 1:
                        contractD.setChk2(value);
                        if ("N".equals(value)) contractD.setChkReason2(reasonValue);
                        break;
                    case 2:
                        contractD.setChk3(value);
                        if ("N".equals(value)) contractD.setChkReason3(reasonValue);
                        break;
                    case 3:
                        contractD.setChk4(value);
                        if ("N".equals(value)) contractD.setChkReason4(reasonValue);
                        break;
                    case 4:
                        contractD.setChk5(value);
                        if ("N".equals(value)) contractD.setChkReason5(reasonValue);
                        break;
                }
            }

            int seq = 0;

            Object seqObj = requestData.get("seq");

            if (seqObj == null || seqObj.toString().trim().isEmpty()) {
                seq = rentService.getNextSeq();
            } else {
                try {
                    seq = Integer.parseInt(seqObj.toString().trim());
                    seq += 1;
                } catch (NumberFormatException e) {
                    return ResponseEntity.badRequest().body(Map.of(
                            "success", false,
                            "message", "SEQ 값이 올바른 숫자가 아닙니다."
                    ));
                }
            }

            contractM.setSeq(seq);
            contractD.setSeq(seq);

            rentService.saveContract(contractM, contractD);

            return ResponseEntity.ok().body(Map.of(
                    "success", true,
                    "message", "요청이 정상적으로 처리되었습니다.",
                    "seq", seq
            ));
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "잘못된 숫자 형식입니다."
            ));
        }catch (RuntimeException e){
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "동일한 주소의 사전조사서가 존재합니다."
            ));
        }catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "예기치 않은 오류가 발생했습니다."
            ));
        }
    }

    @GetMapping("/request-detail")
    public String getRequestDetail(@RequestParam Integer seq, Model model) {
        try {

            ContractDTO contract = contractService.getContractDetail(seq);
            if (contract == null) {
                return "error";
            }
            model.addAttribute("contract", contract);
            
            return "detail";
        } catch (Exception e) {
            e.printStackTrace();
            return "error";
        }
    }

    @GetMapping("/detail-admin")
    public String getRequestDetailForAdmin(@RequestParam Integer seq, Model model) {
        try {

            List<ContractDTO> contract = contractService.getContractDetailForAdmin(seq);
            if (contract == null) {
                return "error";
            }
            System.out.println("contract size = " + contract.size());
            model.addAttribute("contract", contract);

            return "detail";
        } catch (Exception e) {
            e.printStackTrace();
            return "error";
        }
    }

    @PostMapping("/approve-contract")
    @ResponseBody
    public ResponseEntity<?> approveContract(@RequestBody Map<String, Object> requestData) {
        try {
            Integer seq = Integer.parseInt(requestData.get("seq").toString());
            
            ContractDTO contract = contractService.getContractDetail(seq);
            if (contract != null) {
                rentService.updateContract(contract);
                // 승인 테이블에 상태 넣기

               /* 메일기능(미완성)
               try {
                    LoginDTO loginDTO = (LoginDTO) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
                    emailService.sendApprovalEmail(
                        loginDTO.getUserId(),
                        contractM.getUserNm(),
                        String.valueOf(contractM.getSeq())
                    );
                } catch (MessagingException e) {
                    e.printStackTrace();

                } */
                
                return ResponseEntity.ok(Map.of("success", true));
            }
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "��༭�� ã�� �� �����ϴ�."));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "ó�� �� ������ �߻��߽��ϴ�."));
        }
    }

    @PostMapping("/reject-contract")
    @ResponseBody
    public ResponseEntity<?> rejectContract(@RequestBody Map<String, Object> requestData) {
        try {
            Integer seq = Integer.parseInt(requestData.get("seq").toString());
            String reason = (String) requestData.get("reason");
            
            ContractDTO contract = contractService.getContractDetail(seq);
            if (contract != null) {
                rentService.updateContract(contract);
                // 승인 테이블에 상태 및 사유넣기
                
               /* 메일기능(미완성)
                try {
                    LoginDTO loginDTO = (LoginDTO) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
                    emailService.sendRejectionEmail(
                        loginDTO.getUserId(),  // ����� ID (�̸��� �ּ� ������)
                        contractM.getUserNm(), // ����� �̸�
                        String.valueOf(contractM.getSeq()), // ����ȣ
                        reason // �ݷ�����
                    );
                } catch (MessagingException e) {
                    e.printStackTrace();
                } */
                
                return ResponseEntity.ok(Map.of("success", true));
            }
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "��༭�� ã�� �� �����ϴ�."));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "ó�� �� ������ �߻��߽��ϴ�."));
        }
    }

    @GetMapping("/getApprAllM")
    @ResponseBody
    public List<ApprovalDTO>getApprM() {
        List<ApprovalDTO> ApprovalDTO = contractService.getApprAllM();
        return ApprovalDTO;
    }

    @PostMapping("/update")
    public String updateContract(@ModelAttribute ContractDTO contract,
                                 RedirectAttributes redirectAttributes) {

        
        try {

            rentService.updateContract(contract);
            redirectAttributes.addFlashAttribute("message", "수정이 완료되었습니다.");
            System.out.println("수정됨");
            return "redirect:/detail?seq=" + contract.getSeq();

        } catch (Exception e) {

            redirectAttributes.addFlashAttribute("error", "수정 중 오류가 발생했습니다.");
            System.out.println("오류남ㅠ 이유는..?"+e.getMessage());
            return "redirect:/edit?seq=" + contract.getSeq();
        }
    }

} 
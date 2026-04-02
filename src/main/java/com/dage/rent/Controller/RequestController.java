package com.dage.rent.Controller;

import com.dage.rent.DTO.*;
import com.dage.rent.Service.AttachmentFileService;
import com.dage.rent.Service.ContractService;
import com.dage.rent.Service.RentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Controller
@RequestMapping("/api")
public class RequestController {

    private static final Logger log = LoggerFactory.getLogger(RequestController.class);

    @Autowired
    private ContractService contractService;
    @Autowired
    private RentService rentService;
    @Autowired
    private AttachmentFileService attachmentFileService;
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Value("${file.upload-dir}")
    private String uploadDir;

    @PostConstruct
    public void init() {
        try {
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
                System.out.println("Upload directory created: " + uploadPath);
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Failed to create upload directory: " + uploadDir);
        }
    }


    @GetMapping("/cont_list")
    @ResponseBody
    public List<ContractDTO> getContractList(HttpServletRequest request){
        HashMap<String, Object> map = new HashMap<>();
        map.put("proj_code", request.getParameter("proj_code"));
        map.put("conf_tag", request.getParameter("conf_tag"));
        System.out.println("conf_tag: "+request.getParameter("conf_tag"));
        LoginDTO loginDTO = (LoginDTO) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        map.put("user_no", loginDTO.getUserNo());

        List<ContractDTO> contList = contractService.getContractList(map);
        return contList;
    }


    @GetMapping("/cont_list_all")
    @ResponseBody
    public List<ContractDTO> getContractListForAppr(HttpServletRequest request){
        HashMap<String, Object> map = new HashMap<>();
        map.put("proj_code", request.getParameter("proj_code"));
        List<ContractDTO> contList = contractService.getContractListForAppr(map);
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
            Integer empNo = loginDTO.getUserNo() == 26601 ? null : loginDTO.getUserNo();
            
            Pageable pageable = PageRequest.of(page, size);
            Page<ContractMDTO> pageResult = rentService.getAdminContracts(empNo, search, pageable);
            
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

    /**
     * 파일 업로드 API
     */
    @PostMapping("/upload-files")
    @ResponseBody
    public ResponseEntity<?> uploadFiles(
            @RequestParam("files") MultipartFile[] files,
            @RequestParam("section") String section) {
        try {
            System.out.println("=== 파일 업로드 시작 (attachment_file + 암호화) ===");
            System.out.println("섹션: " + section);
            System.out.println("파일 개수: " + files.length);

            List<Map<String, Object>> uploadedFiles = new java.util.ArrayList<>();
            for (MultipartFile file : files) {
                if (file.isEmpty()) {
                    System.out.println("빈 파일 건너뜀: " + file.getOriginalFilename());
                    continue;
                }
                System.out.println("파일 처리 중: " + file.getOriginalFilename() + " (크기: " + file.getSize() + " bytes)");
                com.dage.rent.DTO.AttachmentFileDTO dto = attachmentFileService.saveFile(file, section);
                Map<String, Object> fileInfo = new HashMap<>();
                fileInfo.put("id", dto.getId());
                fileInfo.put("originalName", dto.getOriginalFilename());
                fileInfo.put("size", dto.getFileSize());
                fileInfo.put("section", dto.getSection());
                uploadedFiles.add(fileInfo);
                System.out.println("파일 저장 완료: id=" + dto.getId() + ", original=" + dto.getOriginalFilename());
            }
            System.out.println("업로드 완료된 파일 수: " + uploadedFiles.size());
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "파일 업로드가 완료되었습니다.");
            response.put("files", uploadedFiles);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("파일 업로드 실패 section={}, 파일수={}, 원인: {}", section, files != null ? files.length : 0, e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "파일 업로드 중 오류가 발생했습니다: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * 파일 다운로드 API
     */
    @GetMapping("/download-file/{fileIdOrName}")
    @ResponseBody
    public ResponseEntity<Resource> downloadFile(@PathVariable String fileIdOrName) {
        try {
            // 숫자 id면 attachment_file 테이블에서 조회 후 암호화 파일 복호화 반환
            if (fileIdOrName != null && fileIdOrName.matches("\\d+")) {
                Long id = Long.parseLong(fileIdOrName);
                com.dage.rent.DTO.AttachmentFileDTO dto = attachmentFileService.findById(id);
                if (dto == null) return ResponseEntity.notFound().build();
                byte[] bytes = attachmentFileService.loadFileBytes(id);
                if (bytes == null) return ResponseEntity.notFound().build();
                String originalFilename = dto.getOriginalFilename() != null ? dto.getOriginalFilename() : "download";
                return ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_OCTET_STREAM)
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + originalFilename + "\"")
                        .body(new org.springframework.core.io.ByteArrayResource(bytes));
            }
            // 레거시: 원본 파일명으로 검색 (영문 폴더 먼저, 한글 폴더는 구 폴더명 호환)
            Path filePath = null;
            String[] legacyFolders = {"real_estate", "credit", "부동산정보", "채권순위"};
            for (String folder : legacyFolders) {
                Path searchPath = Paths.get(uploadDir, folder, fileIdOrName);
                if (Files.exists(searchPath)) {
                    filePath = searchPath;
                    break;
                }
            }
            if (filePath == null || !Files.exists(filePath)) return ResponseEntity.notFound().build();
            Resource resource = new UrlResource(filePath.toUri());
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileIdOrName + "\"")
                    .body(resource);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 파일 삭제 API
     */
    @DeleteMapping("/delete-file/{fileIdOrName}")
    @ResponseBody
    public ResponseEntity<?> deleteFile(@PathVariable String fileIdOrName) {
        try {
            if (fileIdOrName != null && fileIdOrName.matches("\\d+")) {
                Long id = Long.parseLong(fileIdOrName);
                boolean deleted = attachmentFileService.deleteById(id);
                if (!deleted) {
                    Map<String, Object> err = new HashMap<>();
                    err.put("success", false);
                    err.put("message", "파일을 찾을 수 없습니다.");
                    return ResponseEntity.badRequest().body(err);
                }
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "파일이 삭제되었습니다.");
                return ResponseEntity.ok(response);
            }
            // 레거시: 디스크에서 파일명으로 삭제 (영문 폴더 먼저, 한글 폴더는 구 폴더명 호환)
            Path filePath = null;
            String[] legacyFolders = {"real_estate", "credit", "부동산정보", "채권순위"};
            for (String folder : legacyFolders) {
                Path searchPath = Paths.get(uploadDir, folder, fileIdOrName);
                if (Files.exists(searchPath)) {
                    filePath = searchPath;
                    break;
                }
            }
            if (filePath == null || !Files.exists(filePath)) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "파일을 찾을 수 없습니다.");
                return ResponseEntity.badRequest().body(errorResponse);
            }
            Files.delete(filePath);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "파일이 삭제되었습니다.");
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            e.printStackTrace();
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "파일 삭제 중 오류가 발생했습니다: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @PostMapping("/submit-request")
    @ResponseBody
    public ResponseEntity<?> submitRequest(@RequestBody Map<String, Object> requestData) {

        System.out.println("Request Data: " + requestData);

        try {
            LoginDTO loginDTO = (LoginDTO) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            System.out.println("Login Info - UserNo: " + loginDTO.getUserNo() + ", UserName: " + loginDTO.getUserName());

            Integer empNo = loginDTO.getUserNo();
            String userNm = loginDTO.getUserName();
            Integer projCode = requestData.get("projCode") != null ? Integer.parseInt(requestData.get("projCode").toString()) : null;
            String projName = (String) requestData.get("projName");
            System.out.println("Project Info - Code: " + projCode + ", Name: " + projName);
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
            String contPeriod = (String) requestData.get("contPeriod");
            String accuType = (String) requestData.get("accuType");

            String address = (String) requestData.get("address");
            String addressD = (String) requestData.get("addressD");

            String fullAddress = address.replaceAll("\\s+", "") + addressD.replaceAll("\\s+", "");

            //주소 중복값 확인 - 임시저장의 경우 제외시켜야함
            List<ContractDTO> dtoList = rentService.getContractAddress();
            ContractDTO chk = contractService.getContractDetailForTemp(loginDTO.getUserNo());
            if(chk == null) {
                for (ContractDTO dto : dtoList) {
                    String existingAddress = (dto.getAddress() + dto.getAddress_d()).replaceAll("\\s+", "");
                    if (fullAddress.equalsIgnoreCase(existingAddress)) {
                        if (dto.getTemp_flag() != null && dto.getTemp_flag().equals("Y")) {
                            throw new RuntimeException("동일한 주소의 사전조사서가 존재합니다.");
                        }
                    }
                }
            }

            ContractMDTO contractM = new ContractMDTO();
            contractM.setEmpNo(empNo);
            contractM.setUserNm(userNm);
            contractM.setProjCode(projCode);
            contractM.setProjName(projName);
            contractM.setCrtdate(LocalDateTime.now());
            contractM.setTemp_flag("N");

            ContractDDTO contractD = new ContractDDTO();
            contractD.setContDate(contDate);
            contractD.setMoveDate(moveDate);
            contractD.setDepositAmt(depositAmt);
            contractD.setRentAmt(rentAmt);
            contractD.setAddress(address);
            contractD.setAddressD(addressD);
            contractD.setResType(resType);
            contractD.setTransType(transType);
            contractD.setArea(area);
            contractD.setCrtdate(LocalDateTime.now());
            contractD.setAccu(accu);
            contractD.setCont_period(contPeriod);
            contractD.setAccu_type(accuType);

            // checklist 처리 전 로깅
            System.out.println("Processing checklist...");
            List<Map<String, Object>> checklist = (List<Map<String, Object>>) requestData.get("checklist");
            System.out.println("Checklist size: " + (checklist != null ? checklist.size() : "null"));

            // 파일 첨부 정보 처리 전 로깅
            System.out.println("Processing attachments...");
            Map<String, Object> attachments = (Map<String, Object>) requestData.get("attachments");
            System.out.println("Attachments: " + attachments);

            // checklist 처리
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

            // 파일 첨부 정보 처리
            if (attachments != null) {
                // 부동산정보 파일 처리
                List<Map<String, Object>> realEstateFiles = (List<Map<String, Object>>) attachments.get("realEstate");
                if (realEstateFiles != null && !realEstateFiles.isEmpty()) {
                    StringBuilder fileIds = new StringBuilder();
                    for (Map<String, Object> file : realEstateFiles) {
                        if (fileIds.length() > 0) fileIds.append(";");
                        fileIds.append(file.get("id"));
                    }
                    contractD.setRealEstateFiles(fileIds.toString());
                }

                // 채권순위 파일 처리
                List<Map<String, Object>> creditFiles = (List<Map<String, Object>>) attachments.get("credit");
                if (creditFiles != null && !creditFiles.isEmpty()) {
                    StringBuilder fileIds = new StringBuilder();
                    for (Map<String, Object> file : creditFiles) {
                        if (fileIds.length() > 0) fileIds.append(";");
                        fileIds.append(file.get("id"));
                    }
                    contractD.setCreditFiles(fileIds.toString());
                }
            }

            // 임시저장 불러오기
            ContractDTO isDTO = contractService.getContractDetailForTemp(loginDTO.getUserNo());

            int seq = 0;
            Object seqObj = requestData.get("seq");
            System.out.println("seq 확인: "+seqObj);
            if (seqObj == null || seqObj.toString().trim().isEmpty()) {
                if(isDTO != null) {
                    seq = Integer.parseInt(isDTO.getSeq());
                    System.out.println("동일 사용자 임시저장 있음: "+seq);
                }else{
                    seq = rentService.getNextSeq();
                    System.out.println("SEQ 없어서 새로 생성: "+seq);
                }
            } else {
                try {
                    if(isDTO.getSeq().equals(seqObj.toString())){
                        seq = Integer.parseInt(seqObj.toString().trim());
                    }else{
                        seq += 1;
                    }
                } catch (NumberFormatException e) {
                    return ResponseEntity.badRequest().body(Map.of(
                            "success", false,
                            "message", "SEQ 값이 올바른 숫자가 아닙니다."
                    ));
                }
            }

            contractM.setSeq(seq);
            contractD.setSeq(seq);

            //이미 seq이 존재한다면 update, 없다면 insert
            if(isDTO != null){
                System.out.println("update 시작");
                isDTO.setAddress(contractD.getAddress());
                isDTO.setAddress_d(contractD.getAddressD());
                isDTO.setAccu(String.valueOf(contractD.getAccu()));
                isDTO.setArea(String.valueOf(contractD.getArea()));
                isDTO.setCont_period(contractD.getCont_period());
                isDTO.setAccu_type(contractD.getAccu_type());
                isDTO.setChk_1(contractD.getChk1());
                isDTO.setChk_2(contractD.getChk2());
                isDTO.setChk_3(contractD.getChk3());
                isDTO.setChk_4(contractD.getChk4());
                isDTO.setChk_5(contractD.getChk5());
                isDTO.setChk_reason_1(contractD.getChkReason1());
                isDTO.setChk_reason_2(contractD.getChkReason2());
                isDTO.setChk_reason_3(contractD.getChkReason3());
                isDTO.setChk_reason_4(contractD.getChkReason4());
                isDTO.setChk_reason_5(contractD.getChkReason5());
                isDTO.setCont_date(contractD.getContDate() != null ? contractD.getContDate().toString() : null);
                isDTO.setMove_date(contractD.getMoveDate() != null ? contractD.getMoveDate().toString() : null);
                isDTO.setDeposit_amt(String.valueOf(contractD.getDepositAmt()));
                isDTO.setRent_amt(String.valueOf(contractD.getRentAmt()));
                isDTO.setRes_type(contractD.getResType());
                isDTO.setTrans_type(contractD.getTransType());
                isDTO.setCrtdate(String.valueOf(contractD.getCrtdate()));
                isDTO.setReal_estate_files(contractD.getRealEstateFiles());
                isDTO.setCredit_files(contractD.getCreditFiles());

                System.out.println("updating contract data...");
                rentService.updateContract(isDTO);
                HashMap<String, Object> updateM = new HashMap<>();
                updateM.put("proj_code", contractM.getProjCode());
                updateM.put("proj_name", contractM.getProjName());
                updateM.put("temp_flag", contractM.getTemp_flag());
                updateM.put("seq",seq);
                rentService.updateContractForM(updateM);
                System.out.println("Contract updated successfully!");
                // 첨부파일 contract_seq 연결
                java.util.List<Long> realIds = AttachmentFileService.parseFileIds(contractD.getRealEstateFiles());
                java.util.List<Long> creditIds = AttachmentFileService.parseFileIds(contractD.getCreditFiles());
                attachmentFileService.linkToContract(realIds, seq);
                attachmentFileService.linkToContract(creditIds, seq);
            }else{
                System.out.println("Saving contract data...");
                rentService.saveContract(contractM, contractD);
                System.out.println("Contract saved successfully!");
                java.util.List<Long> realIds = AttachmentFileService.parseFileIds(contractD.getRealEstateFiles());
                java.util.List<Long> creditIds = AttachmentFileService.parseFileIds(contractD.getCreditFiles());
                attachmentFileService.linkToContract(realIds, seq);
                attachmentFileService.linkToContract(creditIds, seq);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "요청이 정상적으로 처리되었습니다.");
            response.put("seq", seq);

            return ResponseEntity.ok().body(response);
        } catch (NumberFormatException e) {
            System.out.println("NumberFormatException: " + e.getMessage());
            e.printStackTrace();
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "잘못된 숫자 형식입니다: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        } catch (RuntimeException e) {
            System.out.println("RuntimeException: " + e.getMessage());
            e.printStackTrace();
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        } catch (Exception e) {
            System.out.println("Unexpected Exception: " + e.getMessage());
            e.printStackTrace();
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "예기치 않은 오류가 발생했습니다: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @GetMapping("/request-detail")
    public String getRequestDetail(@RequestParam Integer seq, Model model) {
        try {

            ContractDTO contract = contractService.getContractDetail(seq);
            if (contract == null) {
                return "error";
            }
            resolveFileLists(contract);
            model.addAttribute("contract", java.util.Collections.singletonList(contract));
            
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
            for (ContractDTO c : contract) {
                resolveFileLists(c);
            }
            System.out.println("contract size = " + contract.size());
            model.addAttribute("contract", contract);
            model.addAttribute("flag", "N");

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

                return ResponseEntity.ok(Map.of("success", true));
            }
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "처리할 계약이 없습니다."));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "오류가 발생했습니다."));
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

                return ResponseEntity.ok(Map.of("success", true));
            }
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "처리할 계약이 없습니다."));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "오류가 발생했습니다."));
        }
    }

    @GetMapping("/getApprAllM")
    @ResponseBody
    public List<ApprovalDTO>getApprM() {
        List<ApprovalDTO> ApprovalDTO = contractService.getApprAllM();
        return ApprovalDTO;
    }

    @PostMapping("/update")
    @ResponseBody
    public ResponseEntity<?> updateContract(@RequestBody Map<String, Object> requestData) {
        System.out.println("=== 계약 수정 시작 ===");
        System.out.println("수정 동작");
        System.out.println("전체 요청 데이터: " + requestData);
        try {
            System.out.println("seq : " + requestData.get("seq"));
            ContractDTO contract = new ContractDTO();
            contract.setSeq((String) requestData.get("seq"));
            contract.setProj_code((String) requestData.get("proj_code"));
            contract.setProj_name((String) requestData.get("proj_name"));
            contract.setEmp_no((String) requestData.get("emp_no"));
            contract.setUser_nm((String) requestData.get("user_nm"));
            contract.setAddress((String) requestData.get("address"));
            contract.setAddress_d((String) requestData.get("address_d"));
            contract.setCont_period((String) requestData.get("cont_period"));
            contract.setAccu((String) requestData.get("accu"));
            contract.setAccu_type((String) requestData.get("accu_type"));
            contract.setArea((String) requestData.get("area"));
            contract.setChk_1((String) requestData.get("chk_1"));
            contract.setChk_2((String) requestData.get("chk_2"));
            contract.setChk_3((String) requestData.get("chk_3"));
            contract.setChk_4((String) requestData.get("chk_4"));
            contract.setChk_5((String) requestData.get("chk_5"));
            contract.setChk_reason_1((String) requestData.get("chk_reason_1"));
            contract.setChk_reason_2((String) requestData.get("chk_reason_2"));
            contract.setChk_reason_3((String) requestData.get("chk_reason_3"));
            contract.setChk_reason_4((String) requestData.get("chk_reason_4"));
            contract.setChk_reason_5((String) requestData.get("chk_reason_5"));
            contract.setCont_date((String) requestData.get("cont_date"));
            contract.setMove_date((String) requestData.get("move_date"));
            contract.setDeposit_amt((String) requestData.get("deposit_amt"));
            contract.setRent_amt((String) requestData.get("rent_amt"));
            contract.setRes_type((String) requestData.get("res_type"));
            contract.setTrans_type((String) requestData.get("trans_type"));

            // 파일 첨부 정보 처리
            System.out.println("=== 파일 첨부 정보 처리 시작 ===");
            Map<String, Object> attachments = (Map<String, Object>) requestData.get("attachments");
            System.out.println("받은 attachments: " + attachments);
            System.out.println("attachments 타입: " + (attachments != null ? attachments.getClass().getName() : "null"));
            if (attachments != null) {
                // 부동산정보 파일 처리
                List<Map<String, Object>> realEstateFiles = (List<Map<String, Object>>) attachments.get("realEstate");
                System.out.println("부동산정보 파일 목록: " + realEstateFiles);
                System.out.println("부동산정보 파일 개수: " + (realEstateFiles != null ? realEstateFiles.size() : "null"));
                
                if (realEstateFiles != null && !realEstateFiles.isEmpty()) {
                    StringBuilder fileIds = new StringBuilder();
                    System.out.println("부동산정보 파일 처리 시작:");
                    for (Map<String, Object> file : realEstateFiles) {
                        System.out.println("  - 파일 정보: " + file);
                        if (fileIds.length() > 0) fileIds.append(";");
                        fileIds.append(file.get("id"));
                    }
                    String finalFileIds = fileIds.toString();
                    System.out.println("부동산정보 최종 파일 ID 문자열: " + finalFileIds);
                    contract.setReal_estate_files(finalFileIds);
                } else {
                    // 파일이 없으면 빈 문자열로 설정
                    System.out.println("부동산정보 파일이 없어서 빈 문자열로 설정");
                    contract.setReal_estate_files("");
                }

                // 채권순위 파일 처리
                List<Map<String, Object>> creditFiles = (List<Map<String, Object>>) attachments.get("credit");
                System.out.println("채권순위 파일 목록: " + creditFiles);
                System.out.println("채권순위 파일 개수: " + (creditFiles != null ? creditFiles.size() : "null"));
                
                if (creditFiles != null && !creditFiles.isEmpty()) {
                    StringBuilder fileIds = new StringBuilder();
                    System.out.println("채권순위 파일 처리 시작:");
                    for (Map<String, Object> file : creditFiles) {
                        System.out.println("  - 파일 정보: " + file);
                        if (fileIds.length() > 0) fileIds.append(";");
                        fileIds.append(file.get("id"));
                    }
                    String finalFileIds = fileIds.toString();
                    System.out.println("채권순위 최종 파일 ID 문자열: " + finalFileIds);
                    contract.setCredit_files(finalFileIds);
                } else {
                    // 파일이 없으면 빈 문자열로 설정
                    System.out.println("채권순위 파일이 없어서 빈 문자열로 설정");
                    contract.setCredit_files("");
                }
            } else {
                // attachments가 null이면 모든 파일 정보를 빈 문자열로 설정
                System.out.println("attachments가 null이어서 모든 파일 정보를 빈 문자열로 설정");
                contract.setReal_estate_files("");
                contract.setCredit_files("");
            }

            String rewrite = (String) requestData.get("rewrite");

            System.out.println("=== DB 업데이트 시작 ===");
            System.out.println(" 재작성 여부 :"+rewrite);

            if("Y".equals(rewrite)){
                System.out.println(" 재작성, insert 진행 ");
                int newSeq = contractService.insertRewrite(contract);
                System.out.println(" 재작성, rewrite 업데이트 ");
                contractService.updateContractRewrite(Integer.parseInt((String) requestData.get("seq")));
                java.util.List<Long> realIds = AttachmentFileService.parseFileIds(contract.getReal_estate_files());
                java.util.List<Long> creditIds = AttachmentFileService.parseFileIds(contract.getCredit_files());
                attachmentFileService.linkToContract(realIds, newSeq);
                attachmentFileService.linkToContract(creditIds, newSeq);
            }else{
                rentService.updateContract(contract);
                int seqNum = Integer.parseInt(contract.getSeq());
                java.util.List<Long> realIds = AttachmentFileService.parseFileIds(contract.getReal_estate_files());
                java.util.List<Long> creditIds = AttachmentFileService.parseFileIds(contract.getCredit_files());
                attachmentFileService.linkToContract(realIds, seqNum);
                attachmentFileService.linkToContract(creditIds, seqNum);
            }

            System.out.println("=== DB 업데이트 완료 ===");
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "완료되었습니다.");
            response.put("seq", contract.getSeq());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.out.println("수정 중 오류 발생: " + e.getMessage());
            e.printStackTrace();
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "오류가 발생했습니다: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }


    @PostMapping("/cont/delete")
    @ResponseBody
    public ResponseEntity<?> deleteContracts(@RequestBody Map<String, List<Integer>> request) {
        try {
            List<Integer> seqList = request.get("seqList");
            if (seqList == null || seqList.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "삭제할 항목을 선택해주세요."
                ));
            }

            // 현재 로그인한 사용자 확인
            LoginDTO loginDTO = (LoginDTO) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            if (loginDTO == null) {
                return ResponseEntity.status(401).body(Map.of(
                        "success", false,
                        "message", "로그인이 필요합니다."
                ));
            }

            // 계약 삭제 처리
            contractService.deleteContracts(seqList);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "선택한 계약이 삭제되었습니다."
            ));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "계약 삭제 중 오류가 발생했습니다: " + e.getMessage()
            ));
        }
    }

    private Integer parseInteger(Object obj) {
        if (obj == null) return null;
        String str = obj.toString().trim();
        return str.isEmpty() ? null : Integer.parseInt(str);
    }

    @PostMapping("/temp")
    @ResponseBody
    public ResponseEntity<?> temp(@RequestBody Map<String, Object> requestData) {
        System.out.println("임시저장시작");
        try {
            LoginDTO loginDTO = (LoginDTO) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

            Integer empNo = loginDTO.getUserNo();
            String userNm = loginDTO.getUserName();
            System.out.println("login Info - empNo: " + empNo + ", Name: " + userNm);

            Integer projCode = parseInteger(requestData.get("projCode"));
            String projName = (String) requestData.get("projName");
            System.out.println("Project Info - Code: " + projCode + ", Name: " + projName);
            String contDateStr = (String) requestData.get("contDate");
            String moveDateStr = (String) requestData.get("moveDate");

            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            LocalDate contDate = (contDateStr != null && !contDateStr.trim().isEmpty())
                    ? LocalDate.parse(contDateStr.trim(), dateFormatter)
                    : null;

            LocalDate moveDate = (moveDateStr != null && !moveDateStr.trim().isEmpty())
                    ? LocalDate.parse(moveDateStr.trim(), dateFormatter)
                    : null;

            Integer depositAmt = parseInteger(requestData.get("depositAmt"));
            Integer rentAmt = parseInteger(requestData.get("rentAmt"));
            Integer area = parseInteger(requestData.get("area"));
            Integer accu = parseInteger(requestData.get("accu"));
            String contPeriod = (String) requestData.get("contPeriod");
            String accuType = (String) requestData.get("accuType");
            String resType = (String) requestData.get("resType");
            String transType = (String) requestData.get("transType");

            String address = (String) requestData.get("address");
            String addressD = (String) requestData.get("addressD");
            String postCode = (String) requestData.get("postCode");

            String fullAddress = address.replaceAll("\\s+", "") + addressD.replaceAll("\\s+", "");


            ContractMDTO contractM = new ContractMDTO();
            if (empNo != null) contractM.setEmpNo(empNo);
            if (userNm != null) contractM.setUserNm(userNm);
            if (projCode != null) contractM.setProjCode(projCode);
            if (projName != null) contractM.setProjName(projName);
            contractM.setTemp_flag("Y");
            contractM.setCrtdate(LocalDateTime.now());

            ContractDDTO contractD = new ContractDDTO();
            if (contDate != null) contractD.setContDate(contDate);
            if (moveDate != null) contractD.setMoveDate(moveDate);
            if (depositAmt != null) contractD.setDepositAmt(depositAmt);
            if (rentAmt != null) contractD.setRentAmt(rentAmt);
            if (address != null) contractD.setAddress(address);
            if (addressD != null) contractD.setAddressD(addressD);
            if (postCode != null) contractD.setPost_code(postCode);
            if (resType != null) contractD.setResType(resType);
            if (transType != null) contractD.setTransType(transType);
            if (area != null) contractD.setArea(area);
            if (accu != null) contractD.setAccu(accu);
            if (contPeriod != null) contractD.setCont_period(contPeriod);
            if (accuType != null) contractD.setAccu_type(accuType);

            contractD.setCrtdate(LocalDateTime.now());

            // checklist 처리 전 로깅
            System.out.println("Processing checklist...");
            List<Map<String, Object>> checklist = (List<Map<String, Object>>) requestData.get("checklist");
            System.out.println("Checklist size: " + (checklist != null ? checklist.size() : "null"));

            // 파일 첨부 정보 처리 전 로깅
            System.out.println("Processing attachments...");
            Map<String, Object> attachments = (Map<String, Object>) requestData.get("attachments");
            System.out.println("Attachments: " + attachments);

            // checklist 처리
            if (checklist != null) {
                for (int i = 0; i < checklist.size(); i++) {
                    Map<String, Object> item = checklist.get(i);
                    if (item == null) continue;

                    boolean isYes = Boolean.TRUE.equals(item.get("isYes"));
                    String value = isYes ? "Y" : "N";
                    String reasonValue = (String) item.get("reason");

                    switch (i) {
                        case 0:
                            contractD.setChk1(value);
                            if ("N".equals(value) && reasonValue != null) contractD.setChkReason1(reasonValue);
                            break;
                        case 1:
                            contractD.setChk2(value);
                            if ("N".equals(value) && reasonValue != null) contractD.setChkReason2(reasonValue);
                            break;
                        case 2:
                            contractD.setChk3(value);
                            if ("N".equals(value) && reasonValue != null) contractD.setChkReason3(reasonValue);
                            break;
                        case 3:
                            contractD.setChk4(value);
                            if ("N".equals(value) && reasonValue != null) contractD.setChkReason4(reasonValue);
                            break;
                        case 4:
                            contractD.setChk5(value);
                            if ("N".equals(value) && reasonValue != null) contractD.setChkReason5(reasonValue);
                            break;
                    }
                }
            }


            // 파일 첨부 정보 처리
            if (attachments != null) {
                // 부동산정보 파일 처리
                List<Map<String, Object>> realEstateFiles = (List<Map<String, Object>>) attachments.get("realEstate");
                if (realEstateFiles != null && !realEstateFiles.isEmpty()) {
                    StringBuilder fileIds = new StringBuilder();
                    for (Map<String, Object> file : realEstateFiles) {
                        if (fileIds.length() > 0) fileIds.append(";");
                        fileIds.append(file.get("id"));
                    }
                    contractD.setRealEstateFiles(fileIds.toString());
                }

                // 채권순위 파일 처리
                List<Map<String, Object>> creditFiles = (List<Map<String, Object>>) attachments.get("credit");
                if (creditFiles != null && !creditFiles.isEmpty()) {
                    StringBuilder fileIds = new StringBuilder();
                    for (Map<String, Object> file : creditFiles) {
                        if (fileIds.length() > 0) fileIds.append(";");
                        fileIds.append(file.get("id"));
                    }
                    contractD.setCreditFiles(fileIds.toString());
                }
            }

            //SEQ 정리 - 기존 임시저장이 있으면 해당 SEQ 사용, 없으면 +1
            System.out.println("SEQ 정리");
            int seq = 0;
            Object seqObj = requestData.get("seq");
            if (seqObj == null || seqObj.toString().trim().isEmpty()) {
                System.out.println("request SEQ 없음!");
                ContractDTO dto = contractService.getContractDetailForTemp(loginDTO.getUserNo());
                if(dto != null) {
                    seq = Integer.parseInt(dto.getSeq());
                    System.out.println("동일 사용자 임시저장 있음: "+seq);
                }else{
                    seq = rentService.getNextSeq();
                    System.out.println("SEQ 없어서 새로 생성: "+seq);
                }
            } else {
                System.out.println("request SEQ 있음! "+seqObj);
                try {
                    seq = Integer.parseInt(seqObj.toString());
                } catch (NumberFormatException e) {
                    return ResponseEntity.badRequest().body(Map.of(
                            "success", false,
                            "message", "SEQ 값이 올바른 숫자가 아닙니다."
                    ));
                }
            }


            contractM.setSeq(seq);
            contractD.setSeq(seq);

            //이미 seq이 존재한다면 update, 없다면 insert
            ContractDTO isDTO = contractService.getContractDetailForTemp(loginDTO.getUserNo());
            if(isDTO != null){
                System.out.println("update 시작");
                isDTO.setAddress(contractD.getAddress());
                isDTO.setAddress_d(contractD.getAddressD());
                isDTO.setAccu(String.valueOf(contractD.getAccu()));
                isDTO.setArea(String.valueOf(contractD.getArea()));
                isDTO.setChk_1(contractD.getChk1());
                isDTO.setChk_2(contractD.getChk2());
                isDTO.setChk_3(contractD.getChk3());
                isDTO.setChk_4(contractD.getChk4());
                isDTO.setChk_5(contractD.getChk5());
                isDTO.setChk_reason_1(contractD.getChkReason1());
                isDTO.setChk_reason_2(contractD.getChkReason2());
                isDTO.setChk_reason_3(contractD.getChkReason3());
                isDTO.setChk_reason_4(contractD.getChkReason4());
                isDTO.setChk_reason_5(contractD.getChkReason5());
                isDTO.setCont_date(contractD.getContDate() != null ? contractD.getContDate().toString() : null);
                isDTO.setMove_date(contractD.getMoveDate() != null ? contractD.getMoveDate().toString() : null);
                isDTO.setDeposit_amt(String.valueOf(contractD.getDepositAmt()));
                isDTO.setRent_amt(String.valueOf(contractD.getRentAmt()));
                isDTO.setRes_type(contractD.getResType());
                isDTO.setTrans_type(contractD.getTransType());
                isDTO.setCrtdate(String.valueOf(contractD.getCrtdate()));
                isDTO.setReal_estate_files(contractD.getRealEstateFiles());
                isDTO.setCredit_files(contractD.getCreditFiles());

                System.out.println("updating contract data...");
                rentService.updateContract(isDTO);
                HashMap<String, Object> updateM = new HashMap<>();
                updateM.put("proj_code", contractM.getProjCode());
                updateM.put("proj_name", contractM.getProjName());
                updateM.put("seq",seq);
                rentService.updateContractForM(updateM);

                System.out.println("Contract updated successfully!");
            }else{
                System.out.println("신규저장시작");
                System.out.println("Saving contract data...");
                rentService.saveContract(contractM, contractD);
                System.out.println("Contract saved successfully!");
            }


            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "요청이 정상적으로 처리되었습니다.");
            response.put("seq", seq);

            return ResponseEntity.ok().body(response);
        } catch (NumberFormatException e) {
            System.out.println("NumberFormatException: " + e.getMessage());
            e.printStackTrace();
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "잘못된 숫자 형식입니다: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        } catch (RuntimeException e) {
            System.out.println("RuntimeException: " + e.getMessage());
            e.printStackTrace();
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        } catch (Exception e) {
            System.out.println("Unexpected Exception: " + e.getMessage());
            e.printStackTrace();
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "예기치 않은 오류가 발생했습니다: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * contract_d의 real_estate_files, credit_files(id 목록)를 attachment_file에서 조회해 원본 파일명 목록 세팅
     */
    private void resolveFileLists(ContractDTO contract) {
        if (contract == null) return;
        List<Long> realIds = AttachmentFileService.parseFileIds(contract.getReal_estate_files());
        List<Long> creditIds = AttachmentFileService.parseFileIds(contract.getCredit_files());
        contract.setRealEstateFileList(realIds.isEmpty() ? new java.util.ArrayList<>() : attachmentFileService.findByIds(realIds));
        contract.setCreditFileList(creditIds.isEmpty() ? new java.util.ArrayList<>() : attachmentFileService.findByIds(creditIds));
    }
} 
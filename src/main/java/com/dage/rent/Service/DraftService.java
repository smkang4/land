package com.dage.rent.Service;

import com.dage.rent.DAO.mysql.DraftDAO;
import com.dage.rent.DAO.oracle.RentDAO;
import com.dage.rent.DTO.ApprovalDDTO;
import com.dage.rent.DTO.ApprovalMDTO;
import com.dage.rent.DTO.ContractDTO;
import com.dage.rent.DTO.DraftDTO;
import com.dage.rent.DTO.LoginDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class DraftService {

    @Autowired
    private DraftDAO draftDAO;
    @Autowired
    private RentDAO rentDAO;
    @Autowired
    private FtpService ftpService;
    @Autowired
    private RentService rentService;
    @Autowired
    private ApprovalService approvalService;
    @Autowired
    private ContractService contractService;

    /**
     * contract_seq(contract_m.seq) 기준으로 정확한 appr_no를 설정한다.
     */
    private void resolveApprNoFromContractSeq(DraftDTO.ContractDetailDTO detail) {
        if (detail.getContractSeq() == null || detail.getContractSeq() <= 0) {
            return;
        }
        ContractDTO contract = contractService.getContractDetail(detail.getContractSeq());
        if (contract == null || contract.getAppr_no() == null || contract.getAppr_no().trim().isEmpty()) {
            return;
        }
        try {
            detail.setAppr_no(Integer.parseInt(contract.getAppr_no().trim()));
        } catch (NumberFormatException ignored) {
            // contract_m.appr_no 파싱 실패 시 클라이언트 값 유지
        }
    }

    private void resolveAllContractDetailApprNos(DraftDTO draft) {
        if (draft.getContractDetails() == null) {
            return;
        }
        for (DraftDTO.ContractDetailDTO detail : draft.getContractDetails()) {
            resolveApprNoFromContractSeq(detail);
        }
    }

    /**
     * draftId로 기안서 상세 정보 조회 (Long 타입)
     */
    @Transactional("mysqlTransactionManager")
    public DraftDTO getDraftByIdLong(Long draftId) {
        DraftDTO draft = draftDAO.getDraftByIdLong(draftId);
        if (draft != null) {
            // 계약내용 상세 조회
            draft.setContractDetails(draftDAO.getContractDetailsByDraftId(draft.getId()));
        }
        return draft;
    }

    /**
     * 기안서 저장 (트랜잭션 처리)
     */
    @Transactional("mysqlTransactionManager")
    public int saveDraft(DraftDTO draftDTO) {
        // 1. 기안서 기본 정보 저장
        int result = draftDAO.saveDraft(draftDTO);
        int draftId = draftDTO.getId();

        if (result > 0 && draftId > 0) {
            // 2. 계약내용 상세 저장
            if (draftDTO.getContractDetails() != null) {
                System.out.println("=== 계약내용 상세 저장 시작 ===");
                System.out.println("contractDetails 개수: " + draftDTO.getContractDetails().size());
                
                for (DraftDTO.ContractDetailDTO detail : draftDTO.getContractDetails()) {
                    detail.setDraftId(draftId);
                    resolveApprNoFromContractSeq(detail);
                    
                    System.out.println("=== ContractDetail 저장 ===");
                    System.out.println("rowNo: " + detail.getRowNo());
                    System.out.println("appr_no: " + detail.getAppr_no() + " (contract_seq: " + detail.getContractSeq() + ")");
                    System.out.println("type: " + detail.getType());
                    System.out.println("address: " + detail.getAddress());
                    System.out.println("rsrcCode: " + detail.getRsrcCode());
                    System.out.println("custCode: " + detail.getCustCode());
                    
                    // 신규 거래처: custCode가 null이거나 빈 문자열 -> DB에 cust_code 컬럼 제외하고 저장
                    // 기존 거래처: custCode가 존재 -> DB에 cust_code 컬럼 포함하여 저장
                    if (detail.getCustCode() == null || detail.getCustCode().trim().isEmpty()) {
                        System.out.println("신규 거래처 - cust_code 없이 저장 (ERP 등록 시 생성됨)");
                    } else {
                        System.out.println("기존 거래처 - cust_code: " + detail.getCustCode());
                    }
                    
                    draftDAO.saveContractDetail(detail);
                }
            }
            
            // 3. mst_seq 생성 및 Oracle 프로시저 호출은 기안서 업로드 시점에 수행됨
            System.out.println("=== 기안서 저장 완료 (MySQL만 저장) ===");
            System.out.println("※ mst_seq 생성 및 Oracle 프로시저 호출은 기안서 업로드 시점에 수행됩니다.");

        }

        return result;
    }



    /**
     * 기안서 조회 (ID로)
     */
    @Transactional("mysqlTransactionManager")
    public DraftDTO getDraftById(int id) {
        DraftDTO draft = draftDAO.getDraftById(id);
        if (draft != null) {
            // 계약내용 상세 조회
            draft.setContractDetails(draftDAO.getContractDetailsByDraftId(id));
        }
        return draft;
    }

    /**
     * 기안서 조회 (승인번호로)
     */
    @Transactional("mysqlTransactionManager")
    public DraftDTO getDraftByApprNo(int apprNo) {
        DraftDTO draft = draftDAO.getDraftByApprNo(apprNo);
        if (draft != null) {
            // 계약내용 상세 조회
            draft.setContractDetails(draftDAO.getContractDetailsByDraftId(draft.getId()));
        }
        return draft;
    }

    /**
     * 기안서 조회 (mstSeq로)
     */
    @Transactional("mysqlTransactionManager")
    public DraftDTO getDraftByMstSeq(String mstSeq) {
        DraftDTO draft = draftDAO.getDraftByMstSeq(mstSeq);
        if (draft != null) {
            // 계약내용 상세 조회
            draft.setContractDetails(draftDAO.getContractDetailsByDraftId(draft.getId()));
        }
        return draft;
    }

    /**
     * 복수 승인번호로 기안서 조회
     */
    @Transactional("mysqlTransactionManager")
    public DraftDTO getDraftByApprNos(List<Integer> apprNos) {
        if (apprNos == null || apprNos.isEmpty()) {
            return null;
        }
        
        // 첫 번째 appr_no로 기본 기안서 조회
        DraftDTO draft = getDraftByApprNo(apprNos.get(0));
        if (draft != null) {
            // 복수 appr_no 설정
            draft.setAppr_nos(apprNos);
            
            // 추가 계약내용 조회 및 병합
            List<DraftDTO.ContractDetailDTO> allContractDetails = new ArrayList<>();
            
            // 첫 번째 appr_no의 계약내용 추가
            if (draft.getContractDetails() != null) {
                allContractDetails.addAll(draft.getContractDetails());
            }
            
            // 추가 appr_no들의 계약내용 조회 및 추가
            for (int i = 1; i < apprNos.size(); i++) {
                DraftDTO additionalDraft = getDraftByApprNo(apprNos.get(i));
                if (additionalDraft != null && additionalDraft.getContractDetails() != null) {
                    allContractDetails.addAll(additionalDraft.getContractDetails());
                }
            }
            
            draft.setContractDetails(allContractDetails);
        }
        
        return draft;
    }

    /**
     * 기안서 목록 조회
     */
    @Transactional("mysqlTransactionManager")
    public List<DraftDTO> getDraftList(Map<String, Object> params) {
        return draftDAO.getDraftList(params);
    }

    /**
     * 기안서 수정
     */
    @Transactional("mysqlTransactionManager")
    public int updateDraft(DraftDTO draftDTO) {
        int draftId = draftDTO.getId();
        
        // 1. 기안서 기본 정보 수정
        int result = draftDAO.updateDraft(draftDTO);

        if (result > 0) {
            // 2. 기존 계약내용 상세 삭제
            draftDAO.deleteContractDetailsByDraftId(draftId);
            
            // 3. 새로운 계약내용 상세 저장
            if (draftDTO.getContractDetails() != null) {
                for (DraftDTO.ContractDetailDTO detail : draftDTO.getContractDetails()) {
                    detail.setDraftId(draftId);
                    resolveApprNoFromContractSeq(detail);
                    draftDAO.saveContractDetail(detail);
                }
            }


        }

        return result;
    }

    /**
     * 기안서 삭제
     */
    @Transactional("mysqlTransactionManager")
    public int deleteDraft(int id) {
        // 1. 계약내용 상세 삭제
        draftDAO.deleteContractDetailsByDraftId(id);
        
        // 2. 기안서 삭제
        return draftDAO.deleteDraft(id);
    }

    @Transactional("oracleTransactionManager")
    public String getGwPjcode(String proj_code) {
        return rentDAO.getGwPjcode(proj_code);
    }

    @Transactional("oracleTransactionManager")
    public String getMstSeq() {
        return rentDAO.getMstSeq();
    }

    /**
     * 결재문서 HTML 생성 (첨부파일 리스트 포함)
     */
    public String generateApprovalDocumentHTML(DraftDTO draft, String attachmentList) {
        resolveAllContractDetailApprNos(draft);

        StringBuilder html = new StringBuilder();
        
        // HTML 문서 시작
        html.append("<!DOCTYPE html>");
        html.append("<html lang='ko'>");
        html.append("<head>");
        html.append("<meta charset='UTF-8'>");
        html.append("<meta name='viewport' content='width=device-width, initial-scale=1.0'>");
        html.append("<title>기안서</title>");
        html.append("<style>");
        html.append("body { font-family: 'Malgun Gothic', sans-serif; font-size: 10px; line-height: 1.2; margin: 0; padding: 15px; }");
        html.append(".section { margin-top: 1rem; margin-bottom: 15px; }");
        html.append(".section-title { font-family: 'Malgun Gothic', sans-serif; font-size: 12px; font-weight: bold; margin-bottom: 8px; }");
        html.append(".info-table { width: 100%; border-collapse: collapse; margin: 8px 0; }");
        html.append(".info-table th, .info-table td { border: 1px solid #000; padding: 6px; text-align: left; font-family: 'Malgun Gothic', sans-serif; }");
        html.append(".info-table th { background-color: #f8f9fa; font-weight: bold; width: 120px; }");
        html.append(".contract-table { width: 100%; border-collapse: collapse; margin: 8px 0; }");
        html.append(".contract-table th, .contract-table td { border: 1px solid #000; padding: 6px; text-align: center; font-family: 'Malgun Gothic', sans-serif; }");
        html.append(".contract-table th { background-color: #f8f9fa; font-weight: bold; }");
        html.append("@media print {");
        html.append("  @page { size: A4 landscape; margin: 15mm; }");
        html.append("  body { font-size: 9px; padding: 10px; }");
        html.append("  .section { margin-top: 0.8rem; margin-bottom: 12px; }");
        html.append("  .section-title { font-size: 11px; margin-bottom: 6px; }");
        html.append("  .info-table th, .info-table td { padding: 4px; }");
        html.append("  .contract-table th, .contract-table td { padding: 4px; }");
        html.append("}");
        html.append("</style>");
        html.append("</head>");
        html.append("<body>");

        // 현장명과 숙소 임대차 계약 문구 추가
        html.append("<div style='text-align: center; margin: 20px 0; font-size: 12px; font-weight: bold; line-height: 1.6; font-family: \'Malgun Gothic\', sans-serif;'>");
        html.append("( ").append(draft.getProj_name() != null ? draft.getProj_name() : "현장명").append(" ) 임대차 계약을 아래와 같이 진행하고자 하오니 검토 후 재가하여 주시기 바랍니다.");
        html.append("</div>");
        html.append("<div style='text-align: center; margin: 15px 0; font-size: 14px; font-weight: bold; color: #333; font-family: \'Malgun Gothic\', sans-serif;'>");
        html.append("- 아 래 -");
        html.append("</div>");

        // 1. 기안 정보
        html.append("<div class='section'>");
        html.append("<div class='section-title'>1. 임대차 계약 사유</div>");
        html.append("<table class='info-table'>");
        html.append("<tr><th>사유</th><td>").append(convertLineBreaks(draft.getRent_reason())).append("</td></tr>");
        html.append("</table>");
        html.append("</div>");

        // 2. 계약내용
        if (draft.getContractDetails() != null && !draft.getContractDetails().isEmpty()) {
            html.append("<div class='section'>");
            html.append("<div class='section-title'>2. 계약내용</div>");
            html.append("<table class='contract-table'>");
            html.append("<thead><tr>");
            html.append("<th style='width: 2%;'></th>");
            html.append("<th style='width: 5%;'>용도</th>");
            html.append("<th style='width: 25%;'>부동산 소재지</th>");
            html.append("<th style='width: 4%;'>면적(평)</th>");
            html.append("<th style='width: 7%;'>계약시작일</th>");
            html.append("<th style='width: 7%;'>계약종료일</th>");
            html.append("<th style='width: 4%;'>전세권</th>");
            html.append("<th style='width: 5%;'>사용인원</th>");
            html.append("<th style='width: 7%;'>보증금</th>");
            html.append("<th style='width: 6%;'>월세</th>");
            html.append("<th style='width: 5%;'>지급일</th>");
            html.append("<th style='width: 7%;'>임대사업자</th>");
            html.append("<th style='width: 8%;'>사업자등록번호</th>");
            html.append("<th style='width: 5%;'>승인번호</th>");
            html.append("</tr></thead><tbody>");

            for (int i = 0; i < draft.getContractDetails().size(); i++) {
                DraftDTO.ContractDetailDTO detail = draft.getContractDetails().get(i);
                html.append("<tr>");
                html.append("<td style='text-align: center; font-weight: bold;' rowspan='2'>").append(i + 1).append("</td>");
                html.append("<td style='text-align: center;'>").append(detail.getType() != null ? detail.getType() : "").append("</td>");
                html.append("<td style='text-align: left;'>").append(convertLineBreaks(detail.getAddress())).append("</td>");
                html.append("<td style='text-align: center;'>").append(convertAreaToPyeong(detail.getArea())).append("</td>");
                html.append("<td style='text-align: center;'>").append(detail.getContDate_s() != null ? detail.getContDate_s() : "").append("</td>");
                html.append("<td style='text-align: center;'>").append(detail.getContDate_e() != null ? detail.getContDate_e() : "").append("</td>");
                html.append("<td style='text-align: center;'>").append("Y".equals(detail.getChk_3()) ? "있음" : "N".equals(detail.getChk_3()) ? "없음" : "").append("</td>");
                html.append("<td style='text-align: center;'>").append(detail.getAccu() != null ? detail.getAccu() : "").append("</td>");
                html.append("<td style='text-align: center; font-weight: bold;'>").append(formatNumber(detail.getDepositAmt())).append("</td>");
                html.append("<td style='text-align: center; font-weight: bold;'>").append(formatNumber(detail.getRentAmt())).append("</td>");
                html.append("<td style='text-align: center;'>").append(detail.getPaymentDate() != null ? detail.getPaymentDate() : "").append("</td>");
                html.append("<td style='text-align: center;'>").append(detail.getLessorName() != null ? detail.getLessorName() : "").append("</td>");
                html.append("<td style='text-align: center;'>").append(formatLessorNumber(detail.getLessor())).append("</td>");
                html.append("<td style='text-align: center;'>").append(detail.getAppr_no()).append("</td>");
                html.append("</tr>");
                
                // 비고 행 추가
                html.append("<tr>");
                html.append("<td style='text-align: center; font-weight: bold; background-color: #f8f9fa;'>비고</td>");
                html.append("<td colspan='13' style='text-align: left;'>").append(detail.getBigo() != null && !detail.getBigo().trim().isEmpty() ? detail.getBigo() : "없음").append("</td>");
                html.append("</tr>");
            }
            html.append("</tbody></table>");
            html.append("</div>");


        }

        // 3. 실행예산 / 견적실행 (필수 표시)
        html.append("<div class='section'>");
        html.append("<div class='section-title'>3. 실행예산 / 견적실행</div>");
        html.append("<table class='info-table'>");
        html.append("<tr><th>실행예산 / 견적실행</th><td>").append(draft.getExecution_budget() != null ? draft.getExecution_budget() : "").append("</td></tr>");
        html.append("</table>");
        html.append("</div>");

        // 4. 계약상 특이사항
        String contractSectionNumber = "4";
        html.append("<div class='section'>");
        html.append("<div class='section-title'>").append(contractSectionNumber).append(". 계약상 특이사항</div>");
        html.append("<table class='info-table'>");
        html.append("<tr><th>계약상 특이사항</th><td>").append(draft.getRent_source() != null && !draft.getRent_source().trim().isEmpty() ? draft.getRent_source() : "없음").append("</td></tr>");
        html.append("</table>");
        html.append("</div>");

        // 5. 부동산 물건 사전조사서
        html.append("<div class='section'>");
        html.append("<div class='section-title'>5. 부동산 물건 사전조사서</div>");
        html.append("<div class='content-text'>");
        
        // 모든 contractDetail의 appr_no에 대한 링크 생성 (복수 appr_no 지원)
        if (draft.getContractDetails() != null && !draft.getContractDetails().isEmpty()) {
            for (int i = 0; i < draft.getContractDetails().size(); i++) {
                DraftDTO.ContractDetailDTO detail = draft.getContractDetails().get(i);
                int apprNo = detail.getAppr_no();
                
                if (i > 0) {
                    html.append("<br>");
                }
                
                html.append((i + 1)).append(". <a href='http://rent.dage.co.kr/view?seq=").append(apprNo).append("' target='_blank'>");
                html.append("http://rent.dage.co.kr/view?seq=").append(apprNo).append("</a>");
            }
        } else if (draft.getAppr_no() != 0) {
            int apprNo = draft.getAppr_no();
            html.append("<a href='http://rent.dage.co.kr/view?seq=").append(apprNo).append("' target='_blank'>");
            html.append("http://rent.dage.co.kr/view?seq=").append(apprNo).append("</a>");
        }
        
        html.append("</div>");
        html.append("</div>");

        // 6. 첨부파일리스트 (attachmentList가 있을 때만 표시)
        if (attachmentList != null && !attachmentList.trim().isEmpty()) {
            html.append("<div class='section'>");
            html.append("<div class='section-title'>6. 첨부파일리스트</div>");
            html.append("<div class='content-text'>");
            html.append(convertLineBreaks(attachmentList));
            html.append("</div>");
            html.append("</div>");
        }

        html.append("</body></html>");
        
        return html.toString();
    }
    
    /**
     * 결재문서 HTML 생성 (오버로드 - 첨부파일 리스트 없는 버전)
     */
    public String generateApprovalDocumentHTML(DraftDTO draft) {
        return generateApprovalDocumentHTML(draft, "");
    }

    /**
     * 텍스트의 줄바꿈을 HTML <br> 태그로 변환
     */
    private String convertLineBreaks(String text) {
        if (text == null || text.trim().isEmpty()) {
            return "";
        }
        // 특수문자 처리 및 \n을 <br>로 변환하고, 연속된 공백을 보존
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#39;")
                   .replace("\n", "<br>")
                   .replace("\r\n", "<br>")
                   .replace("\r", "<br>");
    }

    /**
     * 면적을 평수로 환산
     */
    private String convertAreaToPyeong(String area) {
        if (area == null || area.trim().isEmpty()) {
            return "";
        }
        try {
            double areaNum = Double.parseDouble(area);
            double pyeong = areaNum * 0.3025;
            return String.format("%.2f", pyeong);
        } catch (NumberFormatException e) {
            // 숫자 변환 실패 시 원본 값 반환
            return area;
        }
    }

    /**
     * 숫자 천단위 구분 포맷팅
     */
    private String formatNumber(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "";
        }
        try {
            // 특수문자 제거 및 숫자만 추출
            String cleanValue = value.replaceAll("[^0-9]", "");
            if (cleanValue.isEmpty()) {
                return "";
            }
            int num = Integer.parseInt(cleanValue);
            return String.format("%,d", num);
        } catch (NumberFormatException e) {
            // 숫자 변환 실패 시 원본 값 반환 (특수문자 처리됨)
            return value.replace("&", "&amp;")
                       .replace("<", "&lt;")
                       .replace(">", "&gt;")
                       .replace("\"", "&quot;")
                       .replace("'", "&#39;");
        }
    }

    /**
     * 사업자등록번호 포맷팅 (000000-0000000 형태일 때만 앞 6자리만 표시)
     * @param lessor 사업자등록번호
     * @return 포맷팅된 사업자등록번호
     */
    private String formatLessorNumber(String lessor) {
        if (lessor == null || lessor.trim().isEmpty() || "번호 미지정".equals(lessor)) {
            return "번호 미지정";
        }
        
        String lessorStr = lessor.toString();
        // 000000-0000000 형태일 때만 앞의 6자리만 표시
        if (lessorStr.contains("-") && lessorStr.length() > 6 && lessorStr.indexOf("-") == 6) {
            return lessorStr.substring(0, 6);
        }
        
        return lessorStr;
    }

    /**
     * EAID로 E_DOC_CODE와 E_DOC_NAME 조회
     * @param eaId EA ID
     * @return E_DOC_CODE와 E_DOC_NAME을 포함한 Map
     */
    @Transactional("oracleTransactionManager")
    public Map<String, Object> getEDocCodeAndName(String eaId) {
        try {
            return rentDAO.getEDocCodeAndName(eaId);
        } catch (Exception e) {
            e.printStackTrace();
            return new HashMap<>();
        }
    }

    /**
     * ERP 프로시저 호출
     * @param erpData ERP 프로시저 파라미터
     * @return 성공 여부
     */
    @Transactional("oracleTransactionManager")
    public boolean callERPProcedure(Map<String, Object> erpData) {
        try {
            System.out.println("=== DraftService.callERPProcedure 시작 ===");
            System.out.println("===  그룹웨어 기안서 업로드  ===");
            System.out.println("입력 데이터: " + erpData);
            
            // 기존 거래처인지 확인 (existing_cust_code가 있으면 기존 거래처)
            String existingCustCode = (String) erpData.get("existing_cust_code");
            String custCode = (String) erpData.get("custCode");

            // 1. MAKE_SEQ와 MAKE_DOC_NO 조회
            String makeProj = (String) erpData.get("makeProj");
            String makeDt = (String) erpData.get("makeDt");
            
            System.out.println("makeProj: " + makeProj);
            System.out.println("makeDt: " + makeDt);

            Map<String, Object> seqData = rentDAO.getERPMakeSeqAndDocNo(makeProj, makeDt);

            // 2. E_DOC_FILE_NAME 생성
            LocalDate currentDate = LocalDate.now();
            String yearMonth = String.format("%d%02d", currentDate.getYear(), currentDate.getMonthValue());
            String fileName = yearMonth+"/"+seqData.get("MAKE_DOC_NO")+".htm";

            // 3. custCode 확인 (ApprovalApiController에서 이미 설정됨)
            System.out.println("=== callERPProcedure custCode 확인 ===");
            System.out.println("erpData 전체: " + erpData);
            System.out.println("erpData.custCode: " + erpData.get("custCode"));
            System.out.println("erpData.custCode 타입: " + (erpData.get("custCode") != null ? erpData.get("custCode").getClass().getSimpleName() : "null"));
            System.out.println("사용할 custCode: " + custCode);
            
            // 4. 조회된 값으로 업데이트
            erpData.put("makeSeq", seqData.get("MAKE_SEQ"));
            erpData.put("makeDocNo", seqData.get("MAKE_DOC_NO"));
            erpData.put("eDocUrl","http://derp.dage.co.kr/DAGE/unicon_gw/gw_doc_file/"+fileName);
            erpData.put("eDocFileName",fileName);
            erpData.put("reportFileName", "RentContRequest.jsp");
            erpData.put("parameters", "MST_SEQ=" + erpData.get("mstSeq"));
            
            // 5. HTML 파일을 Windows 네트워크 공유로 업로드
            String htmlContent = (String) erpData.get("html");
            System.out.println("FTP 업로드 시작 - HTML 길이: " + (htmlContent != null ? htmlContent.length() : "null"));
            
            if (htmlContent != null) {
                // Base64로 인코딩된 HTML인지 확인하고 디코딩
                String decodedHtml = htmlContent;
                try {
                    // Base64 패턴 확인 (일반적으로 Base64는 A-Z, a-z, 0-9, +, /, = 문자만 포함)
                    if (htmlContent.matches("^[A-Za-z0-9+/]*={0,2}$")) {
                        byte[] decodedBytes = java.util.Base64.getDecoder().decode(htmlContent);
                        decodedHtml = new String(decodedBytes, "UTF-8");
                        System.out.println("Base64 HTML 디코딩 완료, 길이: " + decodedHtml.length());
                    } else {
                        System.out.println("HTML이 이미 디코딩된 상태입니다.");
                    }
                } catch (Exception e) {
                    System.err.println("Base64 디코딩 실패, 원본 HTML 사용: " + e.getMessage());
                    // 디코딩 실패 시 원본 사용
                }
                
                // Windows 네트워크 공유로 파일 업로드
                System.out.println("=== Windows 네트워크 공유 업로드 시작 ===");
                System.out.println("파일 업로드 시도: " + fileName);
                boolean uploadSuccess = ftpService.uploadHtmlFileToWindowsShare(decodedHtml, fileName);
                if (!uploadSuccess) {
                    throw new RuntimeException("HTML 파일 Windows 네트워크 공유 업로드 실패: " + fileName);
                }
                System.out.println("HTML 파일 Windows 네트워크 공유 업로드 완료: " + fileName);
            } else {
                System.err.println("HTML 내용이 없어서 파일 업로드 건너뜀");
            }

            // 6. Oracle DAO를 통해 프로시저 호출
            System.out.println("=== ERP 프로시저 호출 시작 ===");
            try {
                rentDAO.callERPProcedure(erpData);
                System.out.println("=== ERP 프로시저 호출 완료 ===");
            } catch (org.springframework.dao.DuplicateKeyException e) {
                // 중복 키 오류: 이미 업로드된 기안서 (무결성 제약 조건 위배)
                System.out.println("⚠️ 이미 업로드된 기안서입니다. 프로시저 실행을 건너뜁니다.");
                System.out.println("에러 메시지: " + e.getMessage());
                // 중복인 경우에도 성공으로 처리 (이미 데이터가 존재하므로)
                return true;
            }

            return true; // 예외가 발생하지 않으면 성공
            
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 거래처 등록 프로시저 호출
     * @param custProjData 거래처 프로젝트 데이터
     */
    @Transactional("oracleTransactionManager")
    public void callCustProjProcedure(Map<String, Object> custProjData) {
        try {
            System.out.println("=== DraftService.callCustProjProcedure 시작 ===");
            System.out.println("입력 데이터: " + custProjData);
            
            // RentService를 통해 거래처 등록 프로시저 호출
            rentService.callCustProjProcedure(custProjData);
            
            System.out.println("=== DraftService.callCustProjProcedure 완료 ===");
            
        } catch (Exception e) {
            System.err.println("=== DraftService.callCustProjProcedure 실패 ===");
            e.printStackTrace();
            throw e; // 예외를 다시 던져서 상위에서 처리하도록 함
        }
    }
    
    /**
     * MySQL에서 draftId로 custCode 조회
     */
    @Transactional("mysqlTransactionManager")
    public String getCustCodeByDraftId(String draftId) {
        return draftDAO.getCustCodeByDraftId(draftId);
    }

    /**
     * mst_seq 생성 및 업데이트 (MySQL)
     */
    @Transactional("mysqlTransactionManager")
    public String generateAndUpdateMstSeq(Long draftId) {
        // Oracle에서 mst_seq 생성
        String newMstSeq = getMstSeq();
        System.out.println("생성된 mst_seq: " + newMstSeq);
        
        if (newMstSeq != null && !newMstSeq.equals("0")) {
            // draft 테이블에 mst_seq 업데이트
            Map<String, Object> updateParams = new HashMap<>();
            updateParams.put("id", draftId);
            updateParams.put("mst_seq", newMstSeq);
            draftDAO.updateDraftMstSeq(updateParams);
            
            System.out.println("mst_seq 업데이트 완료 - draftId: " + draftId + ", mst_seq: " + newMstSeq);
            return newMstSeq;
        } else {
            System.err.println("mst_seq 생성 실패 - draftId: " + draftId);
            return null;
        }
    }
    
    /**
     * 프로시저 실행 (오라클 프로시저 호출) - 기안서 업로드용 (mst_seq만 생성)
     */
    public int processDraftProcedures(List<Long> draftIds) {
        int processedCount = 0;
        
        for (Long draftId : draftIds) {
            try {
                System.out.println("=== 기안서 업로드: mst_seq 생성 시작 - draftId: " + draftId + " ===");
                
                // 드래프트 정보 조회 (계약 상세 포함)
                DraftDTO draft = getDraftByIdLong(draftId);
                if (draft == null) {
                    System.out.println("드래프트를 찾을 수 없음 - draftId: " + draftId);
                    continue;
                }
                
                System.out.println("드래프트 조회 완료 - draftId: " + draftId);
                
                // mst_seq 생성 또는 재생성 (기안서 업로드 시에는 mst_seq만 생성)
                String newMstSeq = null;
                if (draft.getMst_seq() == null || draft.getMst_seq().trim().isEmpty()) {
                    // 신규: mst_seq가 없으면 생성
                    System.out.println("=== 신규 mst_seq 생성 시작 ===");
                    newMstSeq = generateAndUpdateMstSeq(draftId);
                    
                    if (newMstSeq != null) {
                        draft.setMst_seq(newMstSeq);
                        System.out.println("신규 mst_seq 생성 완료: " + newMstSeq);
                        processedCount++;
                    } else {
                        System.err.println("mst_seq 생성 실패 - draftId: " + draftId);
                        continue;
                    }
                } else {
                    // 재기안: mst_seq가 이미 존재하면 새로 생성하여 업데이트
                    System.out.println("=== 재기안 mst_seq 재생성 시작 (기존: " + draft.getMst_seq() + ") ===");
                    newMstSeq = generateAndUpdateMstSeq(draftId);
                    
                    if (newMstSeq != null) {
                        draft.setMst_seq(newMstSeq);
                        System.out.println("재기안 mst_seq 재생성 완료: " + newMstSeq);
                        processedCount++;
                    } else {
                        System.err.println("재기안 mst_seq 재생성 실패 - draftId: " + draftId);
                        continue;
                    }
                }
                
                // mst_seq 생성 완료 후 Oracle 프로시저 호출
                if (newMstSeq != null) {
                    System.out.println("✅ mst_seq 생성 완료: " + newMstSeq);
                    
                    // Oracle 프로시저 호출 (SPCC_E_CONF_STATUS_I)
                    try {
                        System.out.println("=== 오라클 결재문서 저장 시작 ===");
                        System.out.println("mst_seq: " + newMstSeq);
                        
                        // 기안서 정보 다시 조회 (최신 정보)
                        DraftDTO savedDraft = getDraftByIdLong(draftId);
                        if (savedDraft != null) {
                            // mst_seq 업데이트
                            savedDraft.setMst_seq(newMstSeq);
                            
                            // 결재문서 HTML 생성
                            String htmlContent = generateApprovalDocumentHTML(savedDraft, "");
                            
                            // ERP 데이터 준비
                            Map<String, Object> erpData = prepareErpDataForOracle(savedDraft, 0, htmlContent);
                            
                            if (erpData != null) {
                                // 오라클 프로시저 호출 (SPCC_E_CONF_STATUS_I)
                                boolean oracleResult = callERPProcedure(erpData);
                                if (oracleResult) {
                                    System.out.println("=== 오라클 결재문서 저장 완료 ===");
                                } else {
                                    System.err.println("❌ 오라클 결재문서 저장 실패");
                                }
                            } else {
                                System.err.println("❌ ERP 데이터 준비 실패");
                            }
                        } else {
                            System.err.println("❌ 기안서 정보 조회 실패");
                        }
                    } catch (Exception e) {
                        System.err.println("오라클 결재문서 저장 실패: " + e.getMessage());
                        e.printStackTrace();
                    }
                }
                
                System.out.println("=== 기안서 업로드: mst_seq 생성 및 Oracle 저장 완료 - draftId: " + draftId + " ===");
                
            } catch (Exception e) {
                System.out.println("mst_seq 생성 실패 - draftId: " + draftId + ", 오류: " + e.getMessage());
                e.printStackTrace();
            }
        }
        
        return processedCount;
    }

    /**
     * 결재문서 생성 (그룹웨어 연동)
     */
    @Transactional("mysqlTransactionManager")
    public int generateApprovalDocuments(List<Long> draftIds) {
        int generatedCount = 0;
        
        for (Long draftId : draftIds) {
            try {
                System.out.println("결재문서 생성 중 - draftId: " + draftId);
                
                // 기안서 정보 조회 (계약 상세 포함)
                DraftDTO draft = getDraftByIdLong(draftId);
                if (draft == null) {
                    System.out.println("기안서를 찾을 수 없음 - draftId: " + draftId);
                    continue;
                }
                
                System.out.println("기안서 조회 완료 - draftId: " + draftId);
                System.out.println("계약 상세 개수: " + (draft.getContractDetails() != null ? draft.getContractDetails().size() : 0));
                
                // 결재문서 생성 로직 (기존의 generateApprovalDocumentAfterSave와 유사)
                generateApprovalDocumentForDraft(draft);
                generatedCount++;
                
                System.out.println("결재문서 생성 완료 - draftId: " + draftId);
                
            } catch (Exception e) {
                System.out.println("결재문서 생성 실패 - draftId: " + draftId + ", 오류: " + e.getMessage());
                e.printStackTrace();
            }
        }
        
        return generatedCount;
    }

    /**
     * lessor 값의 형식에 따라 regCls 결정
     * @param lessor 임대인 사업자번호/주민번호
     * @return "1" (사업자등록번호 10자리) 또는 "2" (주민번호 13자리)
     */
    private String determineRegCls(String lessor) {
        if (lessor == null || lessor.trim().isEmpty()) {
            return "1"; // 기본값: 사업자등록번호
        }
        
        // 하이픈 제거하고 숫자만 추출
        String numbersOnly = lessor.replaceAll("[^0-9]", "");
        
        // 길이에 따라 구분
        if (numbersOnly.length() == 13) {
            // 주민번호 형식 (13자리)
            return "2";
        } else if (numbersOnly.length() == 10) {
            // 사업자등록번호 형식 (10자리)
            return "1";
        } else {
            // 기본값: 사업자등록번호로 간주
            System.out.println("⚠️ lessor 형식이 명확하지 않음 (길이: " + numbersOnly.length() + "), 기본값 '1' 사용: " + lessor);
            return "1";
        }
    }
    
    /**
     * 거래처 등록 프로시저를 위한 ERP 데이터 준비
     */
    private Map<String, Object> prepareErpData(DraftDTO draft, DraftDTO.ContractDetailDTO detail) {
        try {
            Map<String, Object> erpData = new HashMap<>();
            
            // 기본 프로젝트 정보
            erpData.put("custCode", detail.getCustCode());
            erpData.put("crtUserNo", String.valueOf(draft.getEmp_no()));
            erpData.put("reqEmpNo", String.valueOf(draft.getEmp_no()));
            erpData.put("projCode", draft.getProj_code());
            
            // 거래처 정보 (임대인 정보에서 가져오기)
            erpData.put("custName", detail.getLessorName());
            erpData.put("bizNo", detail.getLessor());
            erpData.put("bossName", detail.getLessorName());
            erpData.put("tradeCls", "01"); // 기본값
            erpData.put("bizCond", "임대"); // 기본값
            erpData.put("bizKnd", "임대업"); // 기본값
            
            // 주소 정보 (간소화)
            erpData.put("zipCode", "");
            erpData.put("addr1", "");
            erpData.put("addr2", "");
            
            // 연락처 정보
            erpData.put("telNo", "");
            erpData.put("headFax", "");
            
            // 계좌 정보
            erpData.put("bankMainCode", "");
            erpData.put("bankCode", "");
            erpData.put("custAccNo", detail.getLessorAccount());
            erpData.put("elctTag", "");
            erpData.put("cOwner", detail.getLessorName());
            erpData.put("remark", draft.getRent_reason());
            erpData.put("taxCls", "VAT");
            erpData.put("representCustCode", "");
            erpData.put("sBankNo", "");
            
            // regCls 결정: lessor 값의 형식에 따라 구분
            // 주민번호 형식 (13자리): "2", 사업자등록번호 형식 (10자리): "1"
            String regCls = determineRegCls(detail.getLessor());
            erpData.put("regCls", regCls);
            
            // 기존 거래처 체크를 위한 필드
            erpData.put("existing_cust_code", ""); // 빈 값으로 설정하여 신규 거래처로 처리
            
            // 프로젝트 정보 추가
            erpData.put("makeProj", draft.getProj_code());
            erpData.put("makeDt", "");
            
            System.out.println("ERP 데이터 준비 완료: " + erpData);
            return erpData;
            
        } catch (Exception e) {
            System.out.println("ERP 데이터 준비 실패: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 단일 드래프트의 결재문서 생성
     */
    private void generateApprovalDocumentForDraft(DraftDTO draft) {
        // 기존의 generateApprovalDocumentAfterSave 로직을 재사용
        try {
            System.out.println("=== 결재문서 생성 시작 ===");
            System.out.println("드래프트 ID: " + draft.getId());
            
            // TODO: 실제 결재문서 생성 로직 구현
            // 현재는 로그만 출력
            System.out.println("결재문서 생성 로직 실행됨 - draftId: " + draft.getId());
            
        } catch (Exception e) {
            System.out.println("결재문서 생성 중 오류: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    /**
     * 기안서 업로드 시 mst_seq 저장을 위한 ERP 데이터 준비
     */
    private Map<String, Object> prepareErpDataForMstSeq(DraftDTO draft, String mstSeq) {
        try {
            System.out.println("=== prepareErpDataForMstSeq 시작 ===");
            System.out.println("mstSeq: " + mstSeq);
            System.out.println("draftId: " + draft.getId());
            
            Map<String, Object> erpData = new HashMap<>();
            
            // 현재 로그인한 사용자 정보 가져오기
            try {
                LoginDTO loginUser = (LoginDTO) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
                erpData.put("crtUserNo", String.valueOf(loginUser.getUserNo()));
                erpData.put("requestEmpNo", String.valueOf(draft.getEmp_no()));
                erpData.put("requestId", loginUser.getUserId());
                erpData.put("requestIdName", loginUser.getUserName());
            } catch (Exception e) {
                // 로그인 정보가 없으면 draft에서 가져오기
                System.out.println("로그인 정보 조회 실패, draft 정보 사용: " + e.getMessage());
                erpData.put("crtUserNo", String.valueOf(draft.getEmp_no()));
                erpData.put("requestEmpNo", String.valueOf(draft.getEmp_no()));
                erpData.put("requestId", "");
                erpData.put("requestIdName", draft.getUser_nm());
            }
            
            // 기본 정보
            erpData.put("mstSeq", mstSeq);
            erpData.put("subSeq", "1"); // 기본값
            erpData.put("makeProj", draft.getProj_code() != null ? draft.getProj_code() : "100");
            erpData.put("makeProjName", draft.getProj_name() != null ? draft.getProj_name() : "");
            
            // 날짜 정보
            LocalDate currentDate = LocalDate.now();
            String makeDt = String.format("%d-%02d-%02d", currentDate.getYear(), currentDate.getMonthValue(), currentDate.getDayOfMonth());
            erpData.put("makeDt", makeDt);
            
            // MAKE_SEQ와 MAKE_DOC_NO 조회
            Map<String, Object> seqData = rentDAO.getERPMakeSeqAndDocNo(draft.getProj_code() != null ? draft.getProj_code() : "100", makeDt);
            erpData.put("makeSeq", seqData.get("MAKE_SEQ"));
            erpData.put("makeDocNo", seqData.get("MAKE_DOC_NO"));
            
            // E_DOC_CODE와 E_DOC_NAME (기본값)
            erpData.put("eDocCode", "");
            erpData.put("eDocName", "");
            erpData.put("eaId", "");
            
            // 요청명
            String requestName = "임대차계약 (" + (draft.getProj_name() != null ? draft.getProj_name() : "") + ")";
            erpData.put("requestName", requestName);
            
            // 파일 정보 (기본값 - 실제 파일은 없음)
            erpData.put("exportTag", "H");
            erpData.put("reportFileName", "");
            erpData.put("parameters", "MST_SEQ=" + mstSeq);
            erpData.put("eDocUrl", "");
            erpData.put("eDocFileName", "");
            
            // 상태 정보
            erpData.put("eConfStatus", "00"); // 미전송
            erpData.put("remarks", "");
            erpData.put("gjMsbh", "");
            erpData.put("gjSeq", "");
            
            // 참조 정보 (기본값)
            erpData.put("refNm1", "");
            erpData.put("refNm2", "");
            erpData.put("refNm3", "");
            erpData.put("refNm4", "");
            erpData.put("refNm5", "");
            erpData.put("refUrl1", "");
            erpData.put("refUrl2", "");
            erpData.put("refUrl3", "");
            erpData.put("refUrl4", "");
            erpData.put("refUrl5", "");
            
            // HTML 파일은 없음 (기안서 업로드 시에는 생성되지 않음)
            erpData.put("html", "");
            
            System.out.println("ERP 데이터 준비 완료: " + erpData);
            return erpData;
            
        } catch (Exception e) {
            System.err.println("ERP 데이터 준비 실패: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * 오라클 저장을 위한 ERP 데이터 준비 (기안서 작성 시)
     */
    private Map<String, Object> prepareErpDataForOracle(DraftDTO draft, int appr_no, String htmlContent) {
        try {
            System.out.println("=== prepareErpDataForOracle 시작 ===");
            System.out.println("draftId: " + draft.getId());
            System.out.println("appr_no: " + appr_no);
            System.out.println("mst_seq: " + draft.getMst_seq());
            
            Map<String, Object> erpData = new HashMap<>();
            
            // 현재 로그인한 사용자 정보 가져오기
            try {
                LoginDTO loginUser = (LoginDTO) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
                erpData.put("crtUserNo", String.valueOf(loginUser.getUserNo()));
                erpData.put("requestEmpNo", String.valueOf(draft.getEmp_no()));
                erpData.put("requestId", loginUser.getUserId());
                erpData.put("requestIdName", loginUser.getUserName());
            } catch (Exception e) {
                // 로그인 정보가 없으면 draft에서 가져오기
                System.out.println("로그인 정보 조회 실패, draft 정보 사용: " + e.getMessage());
                erpData.put("crtUserNo", String.valueOf(draft.getEmp_no()));
                erpData.put("requestEmpNo", String.valueOf(draft.getEmp_no()));
                erpData.put("requestId", "");
                erpData.put("requestIdName", draft.getUser_nm());
            }
            
            // 기본 정보
            erpData.put("mstSeq", draft.getMst_seq());
            erpData.put("subSeq", "1"); // 기본값
            erpData.put("makeProj", draft.getProj_code() != null ? draft.getProj_code() : "100");
            erpData.put("makeProjName", draft.getProj_name() != null ? draft.getProj_name() : "");
            
            // 날짜 정보
            LocalDate currentDate = LocalDate.now();
            String makeDt = String.format("%d-%02d-%02d", currentDate.getYear(), currentDate.getMonthValue(), currentDate.getDayOfMonth());
            erpData.put("makeDt", makeDt);
            
            // MAKE_SEQ와 MAKE_DOC_NO 조회
            Map<String, Object> seqData = rentDAO.getERPMakeSeqAndDocNo(draft.getProj_code() != null ? draft.getProj_code() : "100", makeDt);
            erpData.put("makeSeq", seqData.get("MAKE_SEQ"));
            erpData.put("makeDocNo", seqData.get("MAKE_DOC_NO"));
            
            // E_DOC_CODE와 E_DOC_NAME (기본값 - 실제로는 조회 필요)
            erpData.put("eDocCode", "");
            erpData.put("eDocName", "");
            erpData.put("eaId", "");
            
            // E_DOC_FILE_NAME 생성
            String yearMonth = String.format("%d%02d", currentDate.getYear(), currentDate.getMonthValue());
            String fileName = yearMonth+"/"+seqData.get("MAKE_DOC_NO")+".htm";
            erpData.put("eDocUrl", "http://derp.dage.co.kr/DAGE/unicon_gw/gw_doc_file/"+fileName);
            erpData.put("eDocFileName", fileName);
            
            // 요청명
            String requestName = "임대차계약 (" + (draft.getProj_name() != null ? draft.getProj_name() : "") + ")";
            erpData.put("requestName", requestName);
            
            // HTML 내용
            erpData.put("html", htmlContent);
            
            // 상태 정보
            erpData.put("eConfStatus", "00"); // 미전송
            erpData.put("remarks", "");
            erpData.put("gjMsbh", "");
            erpData.put("gjSeq", "");
            
            // 참조 정보 (기본값)
            erpData.put("refNm1", "");
            erpData.put("refNm2", "");
            erpData.put("refNm3", "");
            erpData.put("refNm4", "");
            erpData.put("refNm5", "");
            erpData.put("refUrl1", "");
            erpData.put("refUrl2", "");
            erpData.put("refUrl3", "");
            erpData.put("refUrl4", "");
            erpData.put("refUrl5", "");
            
            // 파일 정보
            erpData.put("exportTag", "H");
            erpData.put("reportFileName", "RentContRequest.jsp");
            erpData.put("parameters", "MST_SEQ=" + draft.getMst_seq());
            
            System.out.println("오라클 ERP 데이터 준비 완료: " + erpData);
            return erpData;
            
        } catch (Exception e) {
            System.err.println("오라클 ERP 데이터 준비 실패: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

}
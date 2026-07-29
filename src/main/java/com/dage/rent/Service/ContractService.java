package com.dage.rent.Service;

import com.dage.rent.DAO.mysql.ContractDAO;
import com.dage.rent.DTO.ApprovalDTO;
import com.dage.rent.DTO.ContractDDTO;
import com.dage.rent.DTO.ContractDTO;
import com.dage.rent.DTO.ContractMDTO;
import org.apache.ibatis.annotations.Param;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ContractService {

    private final ContractDAO contractDAO;

    @Autowired
    public ContractService(@Qualifier("mysqlContractDAO") ContractDAO contractDAO) {
        this.contractDAO = contractDAO;
    }

    @Transactional("mysqlTransactionManager")
    public int submitContract(Map<String, Object> requestData) {

        int seq = contractDAO.getNextSeq();
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");


        ContractMDTO contractM = new ContractMDTO();
        contractM.setSeq(seq);
        contractM.setEmpNo((Integer) requestData.get("empNo"));
        contractM.setUserNm((String) requestData.get("userNm"));
        contractM.setProjCode((Integer) requestData.get("projCode"));
        contractM.setProjName((String) requestData.get("projectName"));
        contractM.setCrtdate(now);
        contractDAO.insertContractM(contractM);


        ContractDDTO contractD = new ContractDDTO();
        contractD.setSeq(seq);
        contractD.setContDate(LocalDate.parse((String) requestData.get("contractDate"), dateFormatter));
        contractD.setMoveDate(LocalDate.parse((String) requestData.get("moveInDate"), dateFormatter));
        contractD.setContAmt((Integer) requestData.get("contAmt"));
        contractD.setDepositAmt((Integer) requestData.get("depositAmt"));
        contractD.setRentAmt((Integer) requestData.get("rentAmt"));
        contractD.setAddress((String) requestData.get("address"));
        contractD.setAddressD((String) requestData.get("addressD"));
        contractD.setResType((String) requestData.get("resType"));
        contractD.setTransType((String) requestData.get("transType"));
        contractD.setArea((Integer) requestData.get("area"));
        contractD.setCrtdate(now);
        contractD.setAccu((Integer) requestData.get("accu"));
        contractD.setSource((String) requestData.get("source"));
        contractD.setChkReason1((String) requestData.get("chkReason1"));
        contractD.setChkReason2((String) requestData.get("chkReason2"));
        contractD.setChkReason3((String) requestData.get("chkReason3"));
        contractD.setChkReason4((String) requestData.get("chkReason4"));
        contractD.setChkReason5((String) requestData.get("chkReason5"));
        contractDAO.insertContractD(contractD);

        return seq;
    }

    @Transactional("mysqlTransactionManager")
    public int insertRewrite(ContractDTO requestData) {

        System.out.println(" insertRewrite ");
        int seq = contractDAO.getNextSeq();
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        ContractMDTO contractM = new ContractMDTO();
        contractM.setSeq(seq);
        contractM.setEmpNo(Integer.parseInt(requestData.getEmp_no()));
        contractM.setUserNm((String) requestData.getUser_nm());
        contractM.setProjCode(Integer.parseInt(requestData.getProj_code()));
        contractM.setProjName((String) requestData.getProj_name());
        contractM.setCrtdate(now);
        contractM.setTemp_flag("N");
        contractDAO.insertContractM(contractM);

        ContractDDTO contractD = new ContractDDTO();
        contractD.setSeq(seq);
        contractD.setContDate(LocalDate.parse((String) requestData.getCont_date(), dateFormatter));
        contractD.setMoveDate(LocalDate.parse((String) requestData.getMove_date(), dateFormatter));
        contractD.setDepositAmt(Integer.parseInt(requestData.getDeposit_amt()));
        contractD.setRentAmt(Integer.parseInt(requestData.getRent_amt()));
        contractD.setCont_period((String) requestData.getCont_period());
        contractD.setAddress((String) requestData.getAddress());
        contractD.setAddressD((String) requestData.getAddress_d());
        contractD.setResType((String) requestData.getRes_type());
        contractD.setTransType((String) requestData.getTrans_type());
        contractD.setArea(Integer.parseInt(requestData.getArea()));
        contractD.setCrtdate(now);
        contractD.setAccu(Integer.parseInt(requestData.getAccu()));
        contractD.setAccu_type(requestData.getAccu_type());
        contractD.setChk1((String) requestData.getChk_1());
        contractD.setChk2((String) requestData.getChk_2());
        contractD.setChk3((String) requestData.getChk_3());
        contractD.setChk4((String) requestData.getChk_4());
        contractD.setChk5((String) requestData.getChk_5());
        contractD.setChkReason1((String) requestData.getChk_reason_1());
        contractD.setChkReason2((String) requestData.getChk_reason_2());
        contractD.setChkReason3((String) requestData.getChk_reason_3());
        contractD.setChkReason4((String) requestData.getChk_reason_4());
        contractD.setChkReason5((String) requestData.getChk_reason_5());
        contractD.setRealEstateFiles((String) requestData.getReal_estate_files());
        contractD.setCreditFiles((String) requestData.getCredit_files());
        contractDAO.insertContractD(contractD);

        return seq;
    }


    @Transactional("mysqlTransactionManager")
    public List<ContractDTO> getContractList(HashMap<String, Object> map) {
        return contractDAO.getContractList(map);
    }

    @Transactional("mysqlTransactionManager")
    public List<ContractDTO> getContractListForAppr(HashMap<String,Object> map) {
        return contractDAO.getContractListForAppr(map);
    }

    @Transactional("mysqlTransactionManager")
    public List<ApprovalDTO> getApprAllM() {
        return contractDAO.getApprAllM();
    }

    @Transactional("mysqlTransactionManager")
    public ApprovalDTO getApprM(String appr_no) {
        return contractDAO.getApprM(appr_no);
    }

    @Transactional("mysqlTransactionManager")
    public List<ContractDTO> getContractDetailForAdmin(Integer seq) {
        return contractDAO.getContractDetailForAdmin(seq);
    }
    @Transactional("mysqlTransactionManager")
    public List<ContractDTO> getContractDetailForList(Integer appr_no) {
        return contractDAO.getContractDetailForList(appr_no);
    }

    /** 기안서 작성 화면 전용: 이미 다른 기안서에 포함된 contract는 제외 */
    @Transactional("mysqlTransactionManager")
    public List<ContractDTO> getContractDetailForDraft(Integer appr_no) {
        return contractDAO.getContractDetailForDraft(appr_no);
    }

    @Transactional("mysqlTransactionManager")
    public List<ContractDTO> getContractDetailForReceipt() {
        return contractDAO.getContractDetailForReceipt();
    }

    @Transactional("mysqlTransactionManager")
    public ContractDTO getContractDetail(Integer seq) {
        return contractDAO.getContractDetail(seq);
    }

    @Transactional("mysqlTransactionManager")
    public ContractDTO getContractDetailForTemp(Integer user_no) {
        return contractDAO.getContractDetailForTemp(user_no);
    }

    public void updateContractMasterApprNo(@Param("appr_no") int appr_no , @Param("seqList") List<String> sqlList){
        contractDAO.updateContractMasterApprNo(appr_no, sqlList);
    }

    @Transactional("mysqlTransactionManager")
    public void updateContractRewrite(Integer seq) {
        contractDAO.updateContractRewrite(seq);
    }

    @Transactional("mysqlTransactionManager")
    public void deleteContracts(List<Integer> seqList) {
        // contract_d 테이블에서 삭제
        contractDAO.deleteContractDetails(seqList);
        // contract_m 테이블에서 삭제
        contractDAO.deleteContractMasters(seqList);
    }

    @Transactional("mysqlTransactionManager")
    public int getMaxDseq(int appr_no) {
        return contractDAO.getMaxDseq(appr_no);
    }

}
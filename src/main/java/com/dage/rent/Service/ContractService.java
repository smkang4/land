package com.dage.rent.Service;

import com.dage.rent.DAO.mysql.ContractDAO;
import com.dage.rent.DTO.ApprovalDTO;
import com.dage.rent.DTO.ContractDDTO;
import com.dage.rent.DTO.ContractDTO;
import com.dage.rent.DTO.ContractMDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
    public List<ContractDTO> getContractList(String proj_code) {
        return contractDAO.getContractList(proj_code);
    }

    @Transactional("mysqlTransactionManager")
    public List<ContractDTO> getContractListForAppr() {
        return contractDAO.getContractListForAppr();
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
    public ContractDTO getContractDetail(Integer seq) {
        return contractDAO.getContractDetail(seq);
    }


}
package com.dage.rent.DAO.mysql;

import com.dage.rent.DTO.ApprovalDTO;
import com.dage.rent.DTO.ContractDDTO;
import com.dage.rent.DTO.ContractDTO;
import com.dage.rent.DTO.ContractMDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;

@Mapper
@Repository("mysqlContractDAO")
public interface ContractDAO {
    int getNextSeq();
    void insertContractM(ContractMDTO contractM);
    void insertContractD(ContractDDTO contractD);
    ContractMDTO getContractM(int seq);
    ContractDDTO getContractD(int seq);
    List<ContractMDTO> getContracts(Integer empNo, String search, Pageable pageable);
    long countContracts(Integer empNo, String search);
    ContractDTO getContractDetail(Integer seq);
    void updateContract(ContractDTO contract);
    List<ContractDTO> getContractList(@Param("proj_code") String proj_code);
    List<ContractDDTO> getContractAddress();
    List<ContractDTO> getContractListForAppr();
    ApprovalDTO getApprM(String appr_no);
    List<ApprovalDTO> getApprAllM();
    List<ContractDTO> getContractDetailForAdmin(Integer seq);
} 
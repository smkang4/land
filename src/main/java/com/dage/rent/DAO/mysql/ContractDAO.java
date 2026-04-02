package com.dage.rent.DAO.mysql;

import com.dage.rent.DTO.ApprovalDTO;
import com.dage.rent.DTO.ContractDDTO;
import com.dage.rent.DTO.ContractDTO;
import com.dage.rent.DTO.ContractMDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;

@Mapper
@Repository("mysqlContractDAO")
public interface ContractDAO {
    int getNextSeq();
    int getMaxDseq(int appr_no);
    void insertContractM(ContractMDTO contractM);
    void insertContractD(ContractDDTO contractD);
    ContractMDTO getContractM(int seq);
    ContractDDTO getContractD(int seq);
    List<ContractMDTO> getContracts(Integer empNo, String search, Pageable pageable);
    long countContracts(Integer empNo, String search);
    ContractDTO getContractDetail(Integer seq);
    ContractDTO getContractDetailForTemp(Integer user_no);
    void updateContract(ContractDTO contract);
    void updateContractForM(HashMap<String,Object> map);
    List<ContractDTO> getContractList(HashMap<String,Object> map);
    List<ContractDTO> getContractAddress();
    List<ContractDTO> getContractListForAppr(HashMap<String,Object> map);
    ApprovalDTO getApprM(String appr_no);
    List<ApprovalDTO> getApprAllM();
    List<ContractDTO> getContractDetailForAdmin(Integer seq);
    List<ContractDTO> getContractDetailForList(Integer appr_no);
    List<ContractDTO> getContractDetailForReceipt();
    void updateContractMasterApprNo(
            @Param("appr_no") int appr_no
            ,@Param("seqList") List<String> sqlList
    );
    void updateContractRewrite(Integer seq);
    void deleteContractDetails(@Param("seqList") List<Integer> seqList);
    void deleteContractMasters(@Param("seqList") List<Integer> seqList);
    /** 마이그레이션: 파일 참조가 있는 contract_d 목록 (seq, real_estate_files, credit_files) */
    List<ContractDTO> selectContractDWithFileReferences();
    /** 마이그레이션: contract_d의 파일 컬럼만 업데이트 */
    void updateContractFileReferences(@Param("seq") int seq, @Param("realEstateFiles") String realEstateFiles, @Param("creditFiles") String creditFiles);
}
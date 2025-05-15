package com.dage.rent.Service;

import com.dage.rent.DAO.mysql.ContractDAO;
import com.dage.rent.DAO.oracle.RentDAO;
import com.dage.rent.DTO.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class RentService {

    private final RentDAO rentDAO;
    private final ContractDAO contractDAO;

    @Autowired
    public RentService(@Qualifier("oracleRentDAO") RentDAO rentDAO,
                      @Qualifier("mysqlContractDAO") ContractDAO contractDAO) {
        this.rentDAO = rentDAO;
        this.contractDAO = contractDAO;
    }

    @Transactional("oracleTransactionManager")
    public LoginDTO login(String userId) {
        return rentDAO.selectLogin(userId);
    }

    @Transactional("oracleTransactionManager")
    public List<ProjDTO> getAllProjects() {
        return rentDAO.getAllProjects();
    }

    @Transactional("mysqlTransactionManager")
    public void saveContract(ContractMDTO contractM, ContractDDTO contractD) {
        contractDAO.insertContractM(contractM);
        contractDAO.insertContractD(contractD);
    }

    @Transactional("mysqlTransactionManager")
    public int getNextSeq() {
        int seq = contractDAO.getNextSeq();
        return seq;
    }

    @Transactional("mysqlTransactionManager")
    public Page<ContractMDTO> getContracts(Integer empNo, String search, Pageable pageable) {
        List<ContractMDTO> contracts = contractDAO.getContracts(empNo, search, pageable);
        long total = contractDAO.countContracts(empNo, search);
        return new PageImpl<>(contracts, pageable, total);
    }

    @Transactional("mysqlTransactionManager")
    public ContractMDTO getContractM(Integer seq) {
        return contractDAO.getContractM(seq);
    }

    @Transactional("mysqlTransactionManager")
    public ContractDDTO getContractD(Integer seq) {
        return contractDAO.getContractD(seq);
    }

    @Transactional("mysqlTransactionManager")
    public void updateContract(ContractDTO contract) {
        contractDAO.updateContract(contract);
    }

    @Transactional("mysqlTransactionManager")
    public List<ContractDDTO> getContractAddress() {
        return contractDAO.getContractAddress();
    }
}

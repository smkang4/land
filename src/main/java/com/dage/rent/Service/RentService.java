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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class RentService {

    private final RentDAO rentDAO;
    private final ContractDAO contractDAO;

    @Autowired
    public RentService(@Qualifier("rentDAO") RentDAO rentDAO,
                      @Qualifier("mysqlContractDAO") ContractDAO contractDAO) {
        this.rentDAO = rentDAO;
        this.contractDAO = contractDAO;
    }

    @Transactional("oracleTransactionManager")
    public LoginDTO login(String userId) {
        return rentDAO.selectLogin(userId);
    }

    @Transactional("oracleTransactionManager")
    public List<ComCodeDTO> getAllProjects() {
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
    public Page<ContractMDTO> getAdminContracts(Integer empNo, String search, Pageable pageable) {
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
    public void updateContractForM(HashMap<String,Object> map) {
        contractDAO.updateContractForM(map);
    }

    @Transactional("mysqlTransactionManager")
    public List<ContractDTO> getContractAddress() {
        return contractDAO.getContractAddress();
    }
    @Transactional("mysqlTransactionManager")

    public List<ContractDTO> getContractList(HashMap<String,Object> map){
        return contractDAO.getContractList(map);
    }

    @Transactional("oracleTransactionManager")
    public LoginDTO getUserinfo(int emp_no){
        return getUserinfo(emp_no, null);
    }

    @Transactional("oracleTransactionManager")
    public LoginDTO getUserinfo(int emp_no, String user_nm){
        return rentDAO.getUserinfo(emp_no, user_nm);
    }

    @Transactional("oracleTransactionManager")
    public String getEmployeeName(String emp_no){
        return getEmployeeName(emp_no, null);
    }

    @Transactional("oracleTransactionManager")
    public String getEmployeeName(String emp_no, String user_nm){
        return rentDAO.getEmployeeName(emp_no, user_nm);
    }

    @Transactional("oracleTransactionManager")
    public List<Map<String, Object>> searchErpCustomers(String custCode) {
        return rentDAO.searchErpCustomers(custCode);
    }

    @Transactional("oracleTransactionManager")
    public List<ComCodeDTO> getSelectProjects(String user_no){
        return rentDAO.getSelectProjects(user_no);
    }

    @Transactional("oracleTransactionManager")
    public List<ComCodeDTO> getUserList(){
        return rentDAO.getUserList();
    }

    @Transactional("oracleTransactionManager")
    public List<ComCodeDTO> getProjUserList(String headCode){
        return rentDAO.getProjUserList(headCode);
    }

    public EmpUserDTO getEmpUserInfo(int emp_no){
        return getEmpUserInfo(emp_no, null);
    }

    public EmpUserDTO getEmpUserInfo(int emp_no, String user_nm){
        return rentDAO.getEmpUserInfo(emp_no, user_nm);
    }

    @Transactional("oracleTransactionManager")
    public String getCustCode(){
        return rentDAO.getCustCode();
    }

    @Transactional("oracleTransactionManager")
    public int checkDuplicateBizNo(String bizNo){
        return rentDAO.checkDuplicateBizNo(bizNo);
    }

    @Transactional("oracleTransactionManager")
    public List<ComCodeDTO> getExistingCustomers(String bizNo){
        return rentDAO.getExistingCustomers(bizNo);
    }

    @Transactional("oracleTransactionManager")
    public List<ComCodeDTO> getBankList(){
        return rentDAO.getBankList();
    }

    @Transactional("oracleTransactionManager")
    public void callCustProjProcedure(Map<String, Object> custProjData){
        rentDAO.callCustProjProcedure(custProjData);
    }

    @Transactional("oracleTransactionManager")
    public List<ComCodeDTO> getExistingCustomersDebug(String bizNo){
        return rentDAO.getExistingCustomersDebug(bizNo);
    }

    @Transactional("oracleTransactionManager")
    public CustDTO getCustCodeForapprT(String cust_code){
        return rentDAO.getCustCodeForapprT(cust_code);
    }

    @Transactional("oracleTransactionManager")
    public String getLeaseSeq() {
        return rentDAO.getLeaseSeq();
    }

    @Transactional("oracleTransactionManager")
    public String getLeaseContNo() {
        return rentDAO.getLeaseContNo();
    }

    @Transactional("oracleTransactionManager")
    public void callLeaseProcedure(LeaseProcedureDTO leaseData) {
        rentDAO.callLeaseProcedure(leaseData);
    }
    
    @Transactional("oracleTransactionManager")
    public void callLeaseContProcedure(LeaseProcedureDTO leaseData) {
        rentDAO.callLeaseContProcedure(leaseData);
    }
    
    @Transactional("oracleTransactionManager")
    public void insertLease(LeaseProcedureDTO leaseData) {
        rentDAO.insertLease(leaseData);
    }
    
    @Transactional("oracleTransactionManager")
    public Integer getLeaseContSeq() {
        return rentDAO.getLeaseContSeq();
    }
    
    @Transactional("oracleTransactionManager")
    public void insertLeaseCont(Map<String, Object> leaseContData) {
        rentDAO.insertLeaseCont(leaseContData);
    }
    
    @Transactional("oracleTransactionManager")
    public void insertLeaseTranDeposit(Map<String, Object> tranData) {
        rentDAO.insertLeaseTranDeposit(tranData);
    }
    
    @Transactional("oracleTransactionManager")
    public void insertLeaseIntr(Map<String, Object> intrData) {
        rentDAO.insertLeaseIntr(intrData);
    }
    
    @Transactional("oracleTransactionManager")
    public String getConfStatus(String mstSeq) {
        return rentDAO.getConfStatus(mstSeq);
    }
    
    @Transactional("oracleTransactionManager")
    public String getCustCodeByBizNo(String bizNo) {
        return rentDAO.getCustCodeByBizNo(bizNo);
    }
    
    /**
     * ERP 등록 테스트 데이터 삭제 (SEQ 기준)
     * 자식 테이블부터 삭제하여 외래키 제약조건 문제 방지
     */
    @Transactional("oracleTransactionManager")
    public void deleteLeaseData(Integer seq) {
        // 1. TIA_B_LEASE_INTR 삭제
        rentDAO.deleteLeaseIntr(seq);
        // 2. TIA_B_LEASE_TRAN 삭제
        rentDAO.deleteLeaseTran(seq);
        // 3. TIA_B_LEASE_CONT 삭제
        rentDAO.deleteLeaseCont(seq);
        // 4. TIA_B_LEASE 삭제
        rentDAO.deleteLease(seq);
    }

}

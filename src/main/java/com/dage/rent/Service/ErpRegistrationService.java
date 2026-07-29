package com.dage.rent.Service;

import com.dage.rent.DTO.LeaseProcedureDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ERP 임대차 계약 등록 — mst_seq 단위 전체 성공 또는 전체 롤백
 */
@Service
public class ErpRegistrationService {

    public enum RegisterProfile {
        ADMIN,
        SCHEDULER
    }

    @Autowired
    private RentService rentService;

    /**
     * 기안(mst_seq)의 모든 계약을 하나의 Oracle 트랜잭션으로 등록한다.
     * 하나라도 실패하면 전체 롤백된다.
     */
    @Transactional(value = "oracleTransactionManager", rollbackFor = Exception.class)
    public int registerAllLeases(List<Map<String, Object>> draftDetails, Integer crtUserNo, RegisterProfile profile) {
        if (draftDetails == null || draftDetails.isEmpty()) {
            throw new IllegalArgumentException("등록할 계약 데이터가 없습니다.");
        }
        if (crtUserNo == null) {
            throw new IllegalArgumentException("등록 사용자(crtUserNo) 정보가 없습니다.");
        }

        int count = 0;
        for (Map<String, Object> detail : draftDetails) {
            registerOneLease(detail, crtUserNo, profile);
            count++;
        }
        return count;
    }

    private void registerOneLease(Map<String, Object> detail, Integer crtUserNo, RegisterProfile profile) {
        System.out.println("=== 임대차 계약 등록 시작 ===");
        System.out.println("상세 데이터: " + detail);

        String leaseSeq = rentService.getLeaseSeq();
        String contNo = rentService.getLeaseContNo();
        System.out.println("생성된 leaseSeq: " + leaseSeq);
        System.out.println("생성된 contNo: " + contNo);

        LeaseProcedureDTO leaseData = buildLeaseData(detail, crtUserNo, profile, leaseSeq, contNo);
        System.out.println("=== 임대차 계약 등록 데이터 ===");
        System.out.println("leaseData: " + leaseData);

        rentService.insertLease(leaseData);

        Integer contSeq = rentService.getLeaseContSeq();
        System.out.println("=== 생성된 cont_seq: " + contSeq + " ===");

        Map<String, Object> leaseContData = new HashMap<>();
        leaseContData.put("companyCode", leaseData.getArCompanyCode());
        leaseContData.put("seq", leaseData.getArSeq());
        leaseContData.put("contSeq", contSeq);
        leaseContData.put("crtUserNo", crtUserNo);
        leaseContData.put("contName", 1);
        leaseContData.put("chgDt", leaseData.getArExprDt());
        leaseContData.put("dpstAmt", leaseData.getArDpstAmt());
        leaseContData.put("monRent", leaseData.getArMonRent());
        if (profile == RegisterProfile.ADMIN) {
            leaseContData.put("remarks", "최초계약");
        } else {
            leaseContData.put("remarks", leaseData.getArRemarks());
        }
        leaseContData.put("exprDt", leaseData.getArExprDt());
        leaseContData.put("crncCode", leaseData.getArCrncCode());
        leaseContData.put("contBaseDt", leaseData.getArContDt() != null && leaseData.getArContDt().length() >= 7
                ? leaseData.getArContDt().substring(0, 7) : null);

        rentService.insertLeaseCont(leaseContData);
        System.out.println("=== TIA_B_LEASE_CONT INSERT 완료 ===");

        boolean insertTran = profile == RegisterProfile.SCHEDULER
                || leaseData.getArMonRent() == null
                || leaseData.getArMonRent().compareTo(BigDecimal.ZERO) == 0;

        if (insertTran) {
            Map<String, Object> tranData = new HashMap<>();
            tranData.put("companyCode", leaseData.getArCompanyCode());
            tranData.put("seq", leaseData.getArSeq());
            tranData.put("contSeq", contSeq);
            tranData.put("userNo", crtUserNo);
            if (profile == RegisterProfile.SCHEDULER) {
                tranData.put("tranSeq", 1);
                tranData.put("tranDt", leaseData.getArContDt());
                tranData.put("tranAmt", leaseData.getArDpstAmt());
                tranData.put("tranType", "D");
                tranData.put("tranDesc", "보증금");
            }
            rentService.insertLeaseTranDeposit(tranData);
            System.out.println("=== TIA_B_LEASE_TRAN INSERT 완료 ===");
        } else {
            System.out.println("=== 월세가 있으므로 TIA_B_LEASE_TRAN INSERT 생략 (복잡한 월별 계산 필요) ===");
        }

        if (leaseData.getArDpstIntrRate() != null && leaseData.getArDpstIntrRate().compareTo(BigDecimal.ZERO) > 0) {
            Map<String, Object> intrData = new HashMap<>();
            intrData.put("companyCode", leaseData.getArCompanyCode());
            intrData.put("seq", leaseData.getArSeq());
            intrData.put("contSeq", contSeq);
            intrData.put("userNo", crtUserNo);
            if (profile == RegisterProfile.SCHEDULER) {
                intrData.put("intrSeq", 1);
                intrData.put("intrRate", leaseData.getArDpstIntrRate());
                intrData.put("intrDt", leaseData.getArContDt());
                intrData.put("intrDesc", "보증금 이자");
            }
            rentService.insertLeaseIntr(intrData);
            System.out.println("=== TIA_B_LEASE_INTR INSERT 완료 ===");
        }

        System.out.println("✅ 임대차 계약 등록 완료: " + contNo);
    }

    private LeaseProcedureDTO buildLeaseData(
            Map<String, Object> detail,
            Integer crtUserNo,
            RegisterProfile profile,
            String leaseSeq,
            String contNo
    ) {
        LeaseProcedureDTO leaseData = new LeaseProcedureDTO();

        leaseData.setArCompanyCode("100");
        leaseData.setArSeq(Integer.parseInt(leaseSeq));
        leaseData.setArCrtUserNo(crtUserNo);
        leaseData.setArBLeaseCls("1");
        leaseData.setArContNo(contNo);
        leaseData.setArUseType("10");
        leaseData.setArFinTag("F");
        leaseData.setArHCustName("1");
        leaseData.setArDpstIntrRate(new BigDecimal("4.6"));
        leaseData.setArLCode("LR");
        leaseData.setArCrncCode("KRW");
        leaseData.setArFcDpstAmt(BigDecimal.ZERO);
        leaseData.setArFcMonRent(BigDecimal.ZERO);
        leaseData.setArDaymonCls("M");
        leaseData.setArPayCycle("1");
        leaseData.setArPreLater("P");
        leaseData.setArLrContNo(contNo);

        if (profile == RegisterProfile.ADMIN) {
            leaseData.setArHCustCode(20007);
        }

        leaseData.setArRemarks(profile == RegisterProfile.ADMIN ? null
                : (detail.get("bigo") != null ? detail.get("bigo").toString() : null));
        leaseData.setArBankMainCode(null);
        leaseData.setArBankMainName(null);
        leaseData.setArAccno(null);
        leaseData.setArAccOwner(null);
        leaseData.setArCarNum(null);
        leaseData.setArContTitle(null);
        leaseData.setArLequType(null);
        leaseData.setArInsuEndDt(null);
        leaseData.setArChkEndDt(null);
        leaseData.setArUsers(null);
        leaseData.setArAutoExtYn(null);
        leaseData.setArAutoExtLimit(null);
        leaseData.setArExprLimit(null);
        leaseData.setArExprSndDt(null);

        leaseData.setArProjCode(detail.get("proj_code") != null ? detail.get("proj_code").toString() : null);
        leaseData.setArContDt(detail.get("cont_date_s") != null ? detail.get("cont_date_s").toString() : null);
        leaseData.setArExprDt(detail.get("cont_date_e") != null ? detail.get("cont_date_e").toString() : null);
        leaseData.setArExprChgDt(detail.get("cont_date_e") != null ? detail.get("cont_date_e").toString() : null);
        leaseData.setArFinDt(detail.get("cont_date_e") != null ? detail.get("cont_date_e").toString() : null);
        leaseData.setArLCustCode(detail.get("cust_code") != null ? Integer.parseInt(detail.get("cust_code").toString()) : null);
        leaseData.setArZipcode(detail.get("post_code") != null ? detail.get("post_code").toString() : null);
        leaseData.setArAddr1(detail.get("address") != null ? detail.get("address").toString() : null);
        leaseData.setArAddr2(detail.get("address_d") != null ? detail.get("address_d").toString() : null);
        leaseData.setArArea(detail.get("area") != null ? detail.get("area").toString() : null);
        leaseData.setArDpstAmt(detail.get("deposit_amt") != null ? new BigDecimal(detail.get("deposit_amt").toString()) : null);
        leaseData.setArMonRent(detail.get("rent_amt") != null ? new BigDecimal(detail.get("rent_amt").toString()) : null);
        leaseData.setArLCustName(detail.get("lessor_name") != null ? detail.get("lessor_name").toString() : null);
        leaseData.setArRsrcCode(detail.get("rsrc_code") != null ? detail.get("rsrc_code").toString() : null);

        if (profile == RegisterProfile.ADMIN && detail.get("payment_date") != null) {
            int paymentDate = Integer.parseInt(detail.get("payment_date").toString());
            leaseData.setArPayDays(paymentDate >= 25 ? 0 : paymentDate);
        }

        return leaseData;
    }
}

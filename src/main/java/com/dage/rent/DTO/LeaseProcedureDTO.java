package com.dage.rent.DTO;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class LeaseProcedureDTO {
    private String arCompanyCode;
    private Integer arSeq;
    private Integer arCrtUserNo;
    private String arBLeaseCls;
    private String arContNo;
    private String arProjCode;
    private String arContDt;
    private String arExprDt;
    private String arExprChgDt;
    private Integer arLCustCode;
    private Integer arHCustCode;
    private String arZipcode;
    private String arAddr1;
    private String arAddr2;
    private String arArea;
    private String arUseType;
    private BigDecimal arDpstAmt;
    private BigDecimal arMonRent;
    private Integer arPayDays;
    private String arFinTag;
    private String arRemarks;
    private String arLCustName;
    private String arHCustName;
    private String arBankMainCode;
    private String arBankMainName;
    private String arAccno;
    private String arAccOwner;
    private BigDecimal arDpstIntrRate;
    private String arCarNum;
    private String arLCode;
    private String arCrncCode;
    private BigDecimal arFcDpstAmt;
    private BigDecimal arFcMonRent;
    private String arContTitle;
    private String arRsrcCode;
    private String arLequType;
    private String arInsuEndDt;
    private String arChkEndDt;
    private String arUsers;
    private String arAutoExtYn;
    private String arAutoExtLimit;
    private String arExprLimit;
    private String arExprSndDt;
    private String arFinDt;
    private String arDaymonCls;
    private String arPayCycle;
    private String arPreLater;
    private String arLrContNo;
}
package io.github.vikaschaudharyarit_max.smartdbf;

import java.math.BigDecimal;
import java.time.LocalDate;
import io.github.vikaschaudharyarit_max.smartdbf.annotation.DbfColumn;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class CAMs {

    @DbfColumn("AMC_CODE")
    private String amcCode;
    @DbfColumn("FOLIO_NO")
    private String folioNo;
    @DbfColumn("PROD_CODE")
    private String prodCode;
    @DbfColumn("SCHEME")
    private String scheme;
    @DbfColumn("INV_NAME")
    private String invName;
    @DbfColumn("TRXN_TYPE")
    private String trxntype;
    @DbfColumn("TRXN_NO")
    private Double trxnno;
    @DbfColumn("TRXN_MODE")
    private String trxnmode;
    @DbfColumn("TRXN_STAT")
    private String trxnstat;
    @DbfColumn("USERCODE")
    private String usercode;
    @DbfColumn("USRTRXNO")
    private Long usrtrxno;
    @DbfColumn("TRADDATE")
    private String traddate;
    @DbfColumn("POSTDATE")
    private String postdate;
    @DbfColumn("PURPRICE")
    private BigDecimal purprice;
    @DbfColumn("UNITS")
    private BigDecimal units;
    @DbfColumn("AMOUNT")
    private Double amount;
    @DbfColumn("BROKCODE")
    private String brokcode;
    @DbfColumn("SUBBROK")
    private String subbrok;
    @DbfColumn("BROKPERC")
    private Double brokperc;
    @DbfColumn("BROKCOMM")
    private BigDecimal brokcomm;
    @DbfColumn("ALTFOLIO")
    private Long altfolio;
    @DbfColumn("REP_DATE")
    private String time1;
    @DbfColumn("TRXNSUBTYP")
    private String trxnsubtyp;
    @DbfColumn("APPLICATIO")
    private String applicatio;
    @DbfColumn("TRXN_NATURE")
    private String trxnNatur;
    @DbfColumn("TAX")
    private BigDecimal tax;
    @DbfColumn("TOTAL_TAX")
    private Double totalTax;
    @DbfColumn("TE15H")
    private String te15h;
    @DbfColumn("MICR_NO")
    private String micrNo;
    @DbfColumn("REMARKS")
    private String remarks;
    @DbfColumn("SWFLAG")
    private String swflag;
    @DbfColumn("OLD_FOLIO")
    private String oldFolio;
    @DbfColumn("SEQ_NO")
    private Long seqNo;
    @DbfColumn("REINVEST_F")
    private String reinvestF;
    @DbfColumn("MULT_BROK")
    private String multBrok;
    @DbfColumn("STT")
    private BigDecimal stt;
    @DbfColumn("LOCATION")
    private String location;
    @DbfColumn("SCHEME_TYP")
    private String schemeTyp;
    @DbfColumn("TAX_STATUS")
    private String taxStatus;
    @DbfColumn("LOAD")
    private Double load;
    @DbfColumn("SCANREFNO")
    private String scanrefno;
    @DbfColumn("PAN")
    private String pan;
    @DbfColumn("INV_IIN")
    private String invIin;  
    @DbfColumn("TARG_SRC_S")
    private String targSrcS;
    @DbfColumn("TRXN_TYPE")
    private String trxnType;
    @DbfColumn("TICOB_TRTY")
    private String ticobTrty;
    @DbfColumn("TICOB_TRNO")
    private String ticobTrno;
    @DbfColumn("TICOB_POST")
    private LocalDate ticobPost;
    @DbfColumn("DP_ID")
    private String dpId;
    @DbfColumn("TRXN_CHARG")
    private Double trxnCharg;
    @DbfColumn("ELIGIB_AMT")
    private BigDecimal eligibAmt;
    @DbfColumn("SRC_OF_TXN")
    private String srcOfTxn;
    @DbfColumn("TRXN_SUFFI")
    private String trxnSuffi;
    @DbfColumn("SIPTRXNNO")
    private Long siptrxnno;
    @DbfColumn("TER_LOCATION")
    private String terLocati;
    @DbfColumn("EUIN")
    private String euin;
    @DbfColumn("EUIN_VALID")
    private String euinValid;
    @DbfColumn("EUIN_OPTED")
    private String euinOpted;
    @DbfColumn("SUB_BRK_AR")
    private String subBrkAr;
    @DbfColumn("EXCH_DC_FL")
    private String exchDcFl;
    @DbfColumn("SRC_BRK_CO")
    private String srcBrkCo;
    @DbfColumn("SYS_REGN_D")
    private LocalDate sysRegnD;
    @DbfColumn("AC_NO")
    private String acNo;
    @DbfColumn("BANK_NAME")
    private String bankName;
    @DbfColumn("REVERSAL_C")
    private Double reversalC;
    @DbfColumn("EXCHANGE_F")
    private String exchangeF;
    @DbfColumn("CA_INITIAT")
    private LocalDate caInitiat;
    @DbfColumn("GST_STATE")
    private String gstState;
    @DbfColumn("IGST_AMOUN")
    private Double igstAmoun;
    @DbfColumn("CGST_AMOUN")
    private Double cgstAmoun;
    @DbfColumn("SGST_AMOUN")
    private Double sgstAmoun;
    @DbfColumn("REV_REMARK")
    private String revRemark;
    @DbfColumn("ORIGINAL_T")
    private String originalT;
    @DbfColumn("STAMP_DUTY")
    private BigDecimal stampDuty;
    @DbfColumn("FOLIO_OLD")
    private String folioOld;
    @DbfColumn("SCHEME_FOL")
    private String schemeFol;
    @DbfColumn("AMC_REF_NO")
    private String amcRefNo;
    @DbfColumn("REQUEST_RE")
    private String requestRe;
    @DbfColumn("TRANSMISSION")
    private String transmissi;

}

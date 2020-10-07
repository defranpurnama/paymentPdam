package id.co.vsi.switcher.bjb.pdam.mitrateknis;

import id.co.vsi.common.ISO8583.ISO8583Message;
import id.co.vsi.common.bp.ibank.BJBJsonHandler;
import static id.co.vsi.common.bp.ibank.BJBJsonHandler.cLoadedKey;
import static id.co.vsi.common.bp.ibank.BJBJsonHandler.cQueryTimeOutKey;
import static id.co.vsi.common.bp.ibank.BJBJsonHandler.cRCDKey;
import static id.co.vsi.common.bp.ibank.BJBJsonHandler.cRCKey;
import id.co.vsi.common.bp.ibank.BPConUtil;
import id.co.vsi.common.bp.ibank.CommonMail;
import id.co.vsi.common.bp.ibank.DBUtil;
import id.co.vsi.common.constants.ResponseCode;
import id.co.vsi.common.database.DB;
import id.co.vsi.common.database.DBConnectionPools;
import id.co.vsi.common.database.DBFieldEntry;
import id.co.vsi.common.settings.SystemConfig;
import static id.co.vsi.switcher.bjb.pdam.mitrateknis.Common.cModuleNameSpace;
import static id.co.vsi.switcher.bjb.pdam.mitrateknis.Common.cResponseCodeTable;
import static id.co.vsi.switcher.bjb.pdam.mitrateknis.ReceiptFormat.buildReceiptFormat;
import id.co.vsi.systemcore.isocore.SystemException;
import id.co.vsi.systemcore.jasoncore.JSONPlugin;
import java.math.BigInteger;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import org.json.JSONObject;

public abstract class BjbPdamMessageHandler extends BJBJsonHandler {

    public static String cProcCodeInquiry = "321066";
    public static String cProcCodePayment = "521066";

    public BjbPdamMessageHandler(JSONPlugin pParentPlugin) {
        super(pParentPlugin);
    }

    protected JSONObject performPdamMessage(final JSONObject pRequestMessage, final String pRF, final String pProcessingCode) {
        JSONObject tResponseMessage = new JSONObject(pRequestMessage.toString());
        JSONObject tMP = pRequestMessage.getJSONObject(cModuleParamKey);
        final Map<String, String> tAccountData = getAccountData(cModuleNameSpace, cLoadedKey, pRequestMessage);
        final ISO8583Message tRequestInquiryPayment = buildISO8583Message(pProcessingCode, pRequestMessage, tAccountData);
        final String tStan = pRequestMessage.getString(cSTKey);
        final Map<String, String> tChannelData = getChannelData(cModuleNameSpace, cLoadedKey, pRequestMessage.getString(cChannelCodeKey));
        final String tMailSubject = tChannelData.get("EBC_C_LABEL") + " — " + SystemConfig.getNameSpace(cModuleNameSpace).getStringParameter(cFeatureName, "Internet Banking");

        Common.logToTransferTranTable(tRequestInquiryPayment, pRF, tStan, pProcessingCode);

        final ISO8583Message tResponseInquiryPayment;
        final boolean isSimulateISO = SystemConfig.getNameSpace(cModuleNameSpace).getBooleanParameter("simulate-iso", false);
        if (isSimulateISO) {
            tResponseInquiryPayment = new SimulateResponse().getIsoResponse(tRequestInquiryPayment);
        } else {
            tResponseInquiryPayment = new BPConUtil().sendISOMessage(SystemConfig.getNameSpace(Common.cModuleNameSpace), tRequestInquiryPayment);
        }

        Common.logToTransferTranTable(tResponseInquiryPayment, pRF, tStan, pProcessingCode);

        final String tResponseMT = convertResponseMessage(tResponseMessage.getString(cMTIKey));

        tResponseMessage.put(cMTIKey, tResponseMT);
        tResponseMessage.put(cRFKey, pRF);

        // Main business process on specific plugin
        if (tResponseInquiryPayment.getDataElements().containsKey(39) && tResponseInquiryPayment.getValueForDataElement(39).equals("00")) {
            tResponseMessage.put(cRCKey, "0000");
            tResponseMessage.put(cRCDKey, DBUtil.responseCodeDesc("0000", cResponseCodeTable, SystemConfig.getNameSpace(cModuleNameSpace).getStringParameter(cLoadedKey)));

            JSONObject tJsonPrivateData = new JSONObject(tMP.toString());

            // 1. Inquiry Success will be executed on this block of codes
            if (pRequestMessage.getString(cMTIKey).equals(c2100) && tResponseInquiryPayment.getDataElements().containsKey(61)) {
                final String tDataBit61 = tResponseInquiryPayment.getValueForDataElement(61);
                tJsonPrivateData = getParsePrivateDataBit61(tDataBit61, tJsonPrivateData);

                final String tDataBit62 = tResponseInquiryPayment.getValueForDataElement(62);
                tJsonPrivateData = getParsePrivateDataBit62(tDataBit62, tJsonPrivateData);

                tJsonPrivateData.put("BIT61", tDataBit61);
                tJsonPrivateData.put("BIT62", tDataBit62);

                tJsonPrivateData.put("NO_REKENING", tRequestInquiryPayment.getValueForDataElement(102).trim());
                tJsonPrivateData.put("BILLID", tJsonPrivateData.getString("BILLID").trim());
                tJsonPrivateData.put("CUSTOMER_NAME", tJsonPrivateData.getString("CUSTOMER_NAME").trim());
                tJsonPrivateData.put("ADDRESS", tJsonPrivateData.getString("ADDRESS").trim());
                tJsonPrivateData.put("ACCSRC", tJsonPrivateData.getString("NO_REKENING").trim());
                tJsonPrivateData.put("FOOTER", tJsonPrivateData.getString("FOOTER").trim());
                tJsonPrivateData.put("PERIODE_TAGIHAN", tJsonPrivateData.getString("PERIODE_TAGIHAN").trim());

//                BigInteger admin = new BigInteger(tJsonPrivateData.getString("BIAYA_ADMIN61").replaceAll("[^\\d]", ""));
                BigInteger admin = new BigInteger("0");
                if (!tResponseInquiryPayment.hasDataElement(57) || tResponseInquiryPayment.getValueForDataElement(57).isEmpty()) {
                    admin = new BigInteger("0");
                } else {
                    admin = new BigInteger(tResponseInquiryPayment.getValueForDataElement(57));
                }

//                if (admin.toString().equals("0")) {
//                    admin = new BigInteger(tJsonPrivateData.getString("BIAYA_ADMIN61").replaceAll("[^\\d]", ""));
//                }

                BigInteger biayaAir = new BigInteger(tJsonPrivateData.getString("BIAYA_AIR"));
                BigInteger biayaMeter = new BigInteger(tJsonPrivateData.getString("BIAYA_METER"));
                BigInteger biayaAngsuran = new BigInteger(tJsonPrivateData.getString("BIAYA_ANGSURAN"));
                BigInteger denda = new BigInteger(tJsonPrivateData.getString("DENDA"));
                BigInteger biayaLain = new BigInteger(tJsonPrivateData.getString("BIAYA_LAIN"));

                tJsonPrivateData.put("BIAYA_AIR", biayaAir.toString());
                tJsonPrivateData.put("BIAYA_METER", biayaMeter.toString());
                tJsonPrivateData.put("BIAYA_ANGSURAN", biayaAngsuran.toString());
                tJsonPrivateData.put("DENDA", denda.toString());
                tJsonPrivateData.put("BIAYA_LAIN", biayaLain.toString());

                BigInteger amount = biayaAir.add(biayaMeter).add(biayaAngsuran).add(denda).add(biayaLain);
                BigInteger total_amount = amount.add(admin);

                tJsonPrivateData.put("AMOUNT", amount.toString());
                tJsonPrivateData.put("ADMIN", admin.toString());
                tJsonPrivateData.put("TOTAL_AMOUNT", total_amount.toString());

                tJsonPrivateData.put("BILLER_NAME", getBillerName(tResponseInquiryPayment.getValueForDataElement(60).trim(), tResponseMessage.getString(cModuleCodeKey)));
                tJsonPrivateData.put("REFFNUM", pRF);
            }

            // 2. Payment Success will be executed on this block of codes
            if (tResponseMT.equals(c2210)) {
                Common.updateStatusLogMainTable(pRF, tStan, "1", pRequestMessage);

                /*TRACE NUMBER from BIT37*/
                tJsonPrivateData.put(cTRACE_NUMBERKey, tResponseInquiryPayment.getDataElements().containsKey(37)
                        ? tResponseInquiryPayment.getValueForDataElement(37).substring(6, 12)
                        : tResponseInquiryPayment.getDataElements().containsKey(11)
                        ? leftZeroPadding(String.valueOf(Integer.valueOf(tResponseInquiryPayment.getValueForDataElement(11).trim())), 6)
                        : "000000");

                /*BUILD MAIL NOTIFICATION*/
                final String tReceiptFormat = buildReceiptFormat(tJsonPrivateData, tResponseMessage, tMailSubject);
                Common.updateAccountActity(tResponseMessage, pRF, "1", tReceiptFormat);

                /*SEND EMAIL*/
                Executors.newSingleThreadExecutor().execute(new Runnable() {
                    @Override
                    public void run() {
                        new CommonMail().sendMessageToSMTPServer(tReceiptFormat, tMailSubject, tAccountData.get("EBC_CD_MAIL"));
                    }
                });
            }

            tResponseMessage.put(cModuleParamKey, tJsonPrivateData);
        } else if (tResponseInquiryPayment.getValueForDataElement(3).equals(cProcCodePayment)
                && tResponseInquiryPayment.getDataElements().containsKey(39)
                && tResponseInquiryPayment.getValueForDataElement(39).equals("68")) {
            final String tRC = leftZeroPadding(tResponseInquiryPayment.getValueForDataElement(39), 4);

            tResponseMessage.put(cRCKey, tRC);
            tResponseMessage.put(cRCDKey, DBUtil.responseCodeDesc(tRC, cResponseCodeTable, SystemConfig.getNameSpace(cModuleNameSpace).getStringParameter(cLoadedKey)));

            Common.updateStatusLogMainTable(pRF, tStan, "2", pRequestMessage);
            Common.updateAccountActity(tResponseMessage, pRF, "2", "");
        } else {
            final String tRC = tResponseInquiryPayment.hasDataElement(39)
                    ? leftZeroPadding(tResponseInquiryPayment.getValueForDataElement(39), 4)
                    : ResponseCode.ERROR_OTHER.getResponseCodeString();

            if (tResponseMT.equals(c2210)) {
                Common.updateStatusLogMainTable(pRF, tStan, "2", pRequestMessage);
                Common.updateAccountActity(tResponseMessage, pRF, "2", "");
            }

            tResponseMessage.put(cRCKey, tRC);
            tResponseMessage.put(cRCDKey, DBUtil.responseCodeDesc(tRC, cResponseCodeTable, SystemConfig.getNameSpace(cModuleNameSpace).getStringParameter(cLoadedKey)));
        }

        return tResponseMessage;
    }

    private ISO8583Message buildISO8583Message(final String pProcessingCode, final JSONObject pRequestMessage, Map<String, String> pAccountData) {
        final JSONObject tMP = pRequestMessage.getJSONObject(cModuleParamKey);
        final ISO8583Message tPaymentRequestMessage = new ISO8583Message();
        final String tTransmissionDateTime = new SimpleDateFormat("MMddHHmmss").format(new Date());
        final String tTimelocalTransaction = new SimpleDateFormat("HHmmss").format(new Date());
        final String tDateSettlement = new SimpleDateFormat("MMdd").format(new Date());
        final String tStan = generateSTAN();
        final String tChannelCode = pRequestMessage.getString(cChannelCodeKey);
        final String tTrack_2_Data = pAccountData.get("EBC_A_TRACK_2_DATA");
        final String tCardNumber = pAccountData.get("EBC_A_CARD_NUMBER");
        final String tSourceAccountNumber = tMP.getString(cACCSRCKey);
        final String tAmount = pRequestMessage.getString(cMTIKey).equals(c2100) ? "000000000000" : leftZeroPadding(tMP.getString("AMOUNT").trim(), 12);
        final String tSequenceNumber = leftZeroPadding(tStan, 12);
        final String tCardAcceptorNameLocation = tChannelCode.equalsIgnoreCase(c0001)
                ? cCardAcceptorName_SMS : (tChannelCode.equalsIgnoreCase(c0002)
                ? cCardAcceptorName_NET : (tChannelCode.equalsIgnoreCase(c7777)
                ? cCardAcceptorName_MBL : ""));

        if (tCardAcceptorNameLocation == null) {
            throw new SystemException(ResponseCode.ERROR_INVALID_MESSAGE, "Failed build bjb request message : " + pRequestMessage);
        }

        String tTerminalIdentification = "";
        String tMerchantType = "";
        if (tChannelCode.equalsIgnoreCase(c0001)) {
            tTerminalIdentification = "02A001";
            tMerchantType = "6017";
        } else if (tChannelCode.equalsIgnoreCase(c0002)) {
            tTerminalIdentification = "02A002";
            tMerchantType = "6014";
        } else if (tChannelCode.equalsIgnoreCase(c7777)) {
            tTerminalIdentification = "02" + c7777;
            tMerchantType = "6017";
        }

        // Setting value for Bit 41, Bit 42, and Bit 43
        final boolean tUseRightPaddingSpace = SystemConfig.getNameSpace(cModuleNameSpace).getBooleanParameter(cUseRightPaddingSpace, false);

        final String tBit41 = tUseRightPaddingSpace ? rightSpacePadding(tTerminalIdentification, 8) : tTerminalIdentification;
        final String tBit42 = tUseRightPaddingSpace ? rightSpacePadding(tTerminalIdentification, 15) : tTerminalIdentification;
        final String tBit43 = tUseRightPaddingSpace ? rightSpacePadding(tCardAcceptorNameLocation, 40) : tCardAcceptorNameLocation;

        tPaymentRequestMessage.setValueForDataElement(0, "0200");
        tPaymentRequestMessage.setValueForDataElement(2, tCardNumber);
        tPaymentRequestMessage.setValueForDataElement(3, pProcessingCode);
        tPaymentRequestMessage.setValueForDataElement(4, tAmount);
        tPaymentRequestMessage.setValueForDataElement(7, tTransmissionDateTime);
        tPaymentRequestMessage.setValueForDataElement(11, tStan);
        tPaymentRequestMessage.setValueForDataElement(12, tTimelocalTransaction);
        tPaymentRequestMessage.setValueForDataElement(13, tDateSettlement);
        tPaymentRequestMessage.setValueForDataElement(18, tMerchantType);
        tPaymentRequestMessage.setValueForDataElement(32, "000110" + tChannelCode);
        tPaymentRequestMessage.setValueForDataElement(35, tTrack_2_Data);
        tPaymentRequestMessage.setValueForDataElement(37, tSequenceNumber);
        tPaymentRequestMessage.setValueForDataElement(41, tBit41);
        tPaymentRequestMessage.setValueForDataElement(42, tBit42);
        tPaymentRequestMessage.setValueForDataElement(43, tBit43);

//        if (pProcessingCode.equals(cProcCodeInquiry)) {
//            String tBit48 = tMP.getString("BILLID");
//            tPaymentRequestMessage.setValueForDataElement(48, leftSpacePadding(tBit48, 20));
//        } else if (pProcessingCode.equals(cProcCodePayment)) {
//            String tBit48 = tMP.getString("BIT48");
//            tPaymentRequestMessage.setValueForDataElement(48, tBit48);
//        }
        tPaymentRequestMessage.setValueForDataElement(49, "360");

//        if (pProcessingCode.equals(cProcCodePayment)) {
//            String tBit58 = new SimpleDateFormat("yyyy").format(new Date());
//            tPaymentRequestMessage.setValueForDataElement(58, tBit58);
//        }
        tPaymentRequestMessage.setValueForDataElement(59, "PAY");
        String tRoutingToBiler = tMP.getString("BILLER");
        tPaymentRequestMessage.setValueForDataElement(60, tRoutingToBiler);

        if (pProcessingCode.equals(cProcCodeInquiry)) {
            String tRoutingToBilerID = tMP.getString("BILLID");
            tPaymentRequestMessage.setValueForDataElement(61, rightSpacePadding(tRoutingToBilerID.trim(), 18));
        }

        if (pProcessingCode.equals(cProcCodePayment)) {
            String tBit61 = tMP.getString("BIT61");
            String tBit62 = tMP.getString("BIT62");

            tPaymentRequestMessage.setValueForDataElement(61, tBit61);
            tPaymentRequestMessage.setValueForDataElement(62, tBit62);
        }

        tPaymentRequestMessage.setValueForDataElement(63, "214");

        tPaymentRequestMessage.setValueForDataElement(102, tSourceAccountNumber);

        if (tChannelCode.equalsIgnoreCase(c0001)) {
            tPaymentRequestMessage.setValueForDataElement(107, "A001");
        } else if (tChannelCode.equalsIgnoreCase(c0002)) {
            tPaymentRequestMessage.setValueForDataElement(107, "A002");
        } else if (tChannelCode.equalsIgnoreCase(c7777)) {
            tPaymentRequestMessage.setValueForDataElement(107, c7777);
        }

        return tPaymentRequestMessage;
    }

    public static JSONObject getParsePrivateDataBit61(final String tPrivateData, final JSONObject pModulParameter) {
        final JSONObject tJSONMessage = new JSONObject(pModulParameter.toString());

        for (BjbPdamPrivateDataBit61 tBjbPdamPrivateDataBit61 : BjbPdamPrivateDataBit61.values()) {
            final String bit61DataValue = getBit61DataValue(tBjbPdamPrivateDataBit61, tPrivateData);

            tJSONMessage.put(tBjbPdamPrivateDataBit61.toString(), bit61DataValue);
        }

        return tJSONMessage;
    }

    public static String getBit61DataValue(final BjbPdamPrivateDataBit61 pBit61n, final String pBit61) {
        final int length = pBit61n.getLength();
        String tReturn = "";
        int start = 0;

        for (BjbPdamPrivateDataBit61 tPdamRule : BjbPdamPrivateDataBit61.values()) {
            final int end = start + length;

            if (tPdamRule == pBit61n && end <= pBit61.length()) {
                tReturn = pBit61.substring(start, end);

                break;
            }

            start += tPdamRule.getLength();
        }

        return tReturn.trim();
    }

    protected JSONObject getParsePrivateDataBit62(final String tPrivateData, final JSONObject pModulParameter) {
        final JSONObject tJSONMessage = new JSONObject(pModulParameter.toString());

        for (BjbPdamPrivateDataBit62 tBjbPdamPrivateDataBit62 : BjbPdamPrivateDataBit62.values()) {
            final String bit62DataValue = getBit62DataValue(tBjbPdamPrivateDataBit62, tPrivateData);

            tJSONMessage.put(tBjbPdamPrivateDataBit62.toString(), bit62DataValue);
        }

        return tJSONMessage;
    }

    public static String getBit62DataValue(final BjbPdamPrivateDataBit62 pBit62n, final String pBit62) {
        final int length = pBit62n.getLength();
        String tReturn = "";
        int start = 0;

        for (BjbPdamPrivateDataBit62 tPdamRule : BjbPdamPrivateDataBit62.values()) {
            final int end = start + length;

            if (tPdamRule == pBit62n && end <= pBit62.length()) {
                tReturn = pBit62.substring(start, end);

                break;
            }

            start += tPdamRule.getLength();
        }

        return tReturn.trim();
    }

    public String getBillerName(final String pBillerCode, final String pModuleCode) {
        String tResult = "";
        final Map<String, String> tResultMap = new HashMap<>();

        try {
            final String name = (String) SystemConfig.getNameSpace(cModuleNameSpace).getArrayParameter(cLoadedKey).get(0);
            final DB tUsedDB = DBConnectionPools.getInstance().getDB(name);
            final String t_Query = "SELECT EBC_IPP_NAME FROM EBANKCORE_IB_PAY_PRODUCT WHERE EBC_IPP_KEY = '" + pBillerCode + "' AND EBC_IPP_MODULE_CODE = '" + pModuleCode + "'";
            HashMap<Integer, DBFieldEntry> tRecordDataHashMap = new HashMap<>();

            Statement tStatement = tUsedDB.executeCallableQuery(t_Query, tRecordDataHashMap, SystemConfig.getNameSpace(cModuleNameSpace).getIntParameter(cQueryTimeOutKey, 5));

            if (tStatement != null) {
                ResultSet tResultSet = tStatement.getResultSet();

                if (tResultSet != null) {
                    if (tResultSet.next()) {
                        tResultMap.put("EBC_IPP_NAME", tResultSet.getString("EBC_IPP_NAME"));
                    }
                    tResultSet.close();
                }
                tStatement.close();
            }
            tResult = tResultMap.get("EBC_IPP_NAME");

        } catch (SQLException e) {
        }

        return tResult;
    }
}

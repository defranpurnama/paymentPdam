package id.co.vsi.switcher.bjb.pdam.mitrateknis;

import id.co.vsi.common.ISO8583.ISO8583Message;
import static id.co.vsi.common.bp.ibank.BJBJsonHandler.cExpireOTPInMinute;
import static id.co.vsi.common.bp.ibank.BJBJsonHandler.cFeatureName;
import static id.co.vsi.common.bp.ibank.BJBJsonHandler.cLoadedKey;
import static id.co.vsi.common.bp.ibank.BJBJsonHandler.cModuleParamKey;
import static id.co.vsi.common.bp.ibank.BJBJsonHandler.cQueryTimeOutKey;
import static id.co.vsi.common.bp.ibank.BJBJsonHandler.cRCKey;
import static id.co.vsi.common.bp.ibank.BJBJsonHandler.cSSKey;
import static id.co.vsi.common.bp.ibank.BJBJsonHandler.jsonToMap;
import id.co.vsi.common.constants.ResponseCode;
import id.co.vsi.common.database.DB;
import id.co.vsi.common.database.DBConnectionPools;
import id.co.vsi.common.database.DBFieldEntry;
import id.co.vsi.common.database.DBFieldType;
import id.co.vsi.common.log.LogType;
import id.co.vsi.common.log.SystemLog;
import id.co.vsi.common.settings.SystemConfig;
import id.co.vsi.systemcore.isocore.SystemException;
import java.math.BigInteger;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.json.JSONObject;

public class Common {

    public static Main cLoggingStaticHookObject = new Main();
    public static Object[] cValidMCAndIP = new Object[2];
    public static final String cInquiryProcessingCode = "321066";
    public static final String cPaymentProcessingCode = "521066";
    public static final String cLogTranMain = "EBANKMOD_PAM_TRAN_MAIN";
    public static final String cModuleNameSpace = "bjb-payment-pdam";
    public static final String cResponseCodeTable = "EBANKMOD_RC_PDAM";
    public static final String[] cValidInquiryModulParameter = new String[]{"ACCSRC", "BILLER", "BILLID"};
    public static final String[] cValidPaymentModulParameter = new String[]{"ACCSRC", "BILLER", "BILLER_NAME", "BILLID", "CUSTOMER_NAME", "ADDRESS", "PERIODE_TAGIHAN", "BIAYA_AIR", "BIAYA_METER", "BIAYA_ANGSURAN", "DENDA", "BIAYA_LAIN", "AMOUNT", "ADMIN", "TOTAL_AMOUNT", "GOLONGAN", "FOOTER", "BIT61", "BIT62"};

    public static void logToAccActivityTable(final JSONObject pRequestMessage, final String pRF) {
        if (pRequestMessage.getString("MT").equals("2100")) {
            final String tTableName = "EBANKCORE_ACC_ACTIVITY";
            final HashMap<String, String> tTableRecordDataHashMap = new HashMap<>();
            final String tLoggedTime = "'" + new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()) + "'";
            final String tINFO = pRequestMessage.has("INFO") ? pRequestMessage.getString("INFO") : "";
            if (pRequestMessage.getJSONObject("MP") != null) {
                final JSONObject tMP = pRequestMessage.getJSONObject("MP");

                tTableRecordDataHashMap.put("EBC_AA_ACCOUNT", "'" + tMP.getString("ACCSRC") + "'");
            }

            tTableRecordDataHashMap.put("EBC_AA_ID", "'" + pRF + "'");
            tTableRecordDataHashMap.put("EBC_AA_MID", "'" + pRequestMessage.getString("MC") + "'");
            tTableRecordDataHashMap.put("EBC_AA_CID", "'" + pRequestMessage.getString("CC") + "'");
            tTableRecordDataHashMap.put("EBC_AA_INFO", "'" + tINFO + "'");
            tTableRecordDataHashMap.put("EBC_AA_SESSION", "'" + pRequestMessage.getString("SS") + "'");
            tTableRecordDataHashMap.put("EBC_AA_DATE", tLoggedTime);
            tTableRecordDataHashMap.put("EBC_AA_STATUS", "'0'");    // 0= berhasil, 1 payment, 2 gagal
            executeLogToTranDownTable(tTableRecordDataHashMap, tTableName);
        }
    }

    public static void logToTransferTranDownTable(final JSONObject pRequestMessage, final String pRF, final String pGatewayStan) {
        final String tTableName = "EBANKMOD_PAM_LOG_TRAN_DOWN";
        final HashMap<String, String> tTableRecordDataHashMap = new HashMap<>();
        final String tLoggedTime = "'" + new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()) + "'";
        final String tRawStream = pRequestMessage.toString().replace("'", "`");
        final String tUUID = "'" + UUID.randomUUID().toString() + "'";
        final String tGatewayStan = pGatewayStan == null ? " " : pGatewayStan;
        final String tRC = pRequestMessage.has(cRCKey) ? pRequestMessage.getString(cRCKey) : "";
        final Map<String, String> tMP = jsonToMap(pRequestMessage.getJSONObject("MP").toString());

        tTableRecordDataHashMap.put("EBM_LT_BILL_ID", "'" + (tMP.containsKey("BILLID") ? tMP.get("BILLID") : "NULL") + "'");
        tTableRecordDataHashMap.put("EBM_LT_ACCOUNT", "'" + (tMP.containsKey("ACCSRC") ? tMP.get("ACCSRC") : "NULL") + "'");
        tTableRecordDataHashMap.put("EBM_LT_ID", tUUID);
        tTableRecordDataHashMap.put("EBM_LT_REFNUM", "'" + pRF + "'");
        tTableRecordDataHashMap.put("EBM_LT_CMD", "'" + pRequestMessage.getString("MT") + "'");
        tTableRecordDataHashMap.put("EBM_LT_RC", "'" + tRC + "'");
        tTableRecordDataHashMap.put("EBM_LT_LOGGED_DT", tLoggedTime);
        tTableRecordDataHashMap.put("EBM_LT_STAN", "'" + pRequestMessage.getString("ST") + "'");
        tTableRecordDataHashMap.put("EBM_LT_RAW_STREAM", "'" + tRawStream + "'");
        tTableRecordDataHashMap.put("EBM_LT_GW_STAN", "'" + tGatewayStan + "'");
        tTableRecordDataHashMap.put("EBM_LT_WID", "'" + String.valueOf(Thread.currentThread().getId()) + "'");
        executeLogToTranDownTable(tTableRecordDataHashMap, tTableName);
    }

    public static void logToTransferTranDownTable(JSONObject pRequestMessage) {
        final String tREFFNUM = pRequestMessage.getString("RF");
        final String tGatewayStan = null;

        logToTransferTranDownTable(pRequestMessage, tREFFNUM, tGatewayStan);
    }

    public static void logToTransferTranMainTable(final JSONObject pRequestMessage, final String pRF, final String pStan) {
        if (pRequestMessage.getString("MT").equals("2100") && pStan != null) {
            final String tTableName = cLogTranMain;
            final HashMap<String, String> tTableRecordDataHashMap = new HashMap<>();
            final String tLoggedTime = "'" + new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()) + "'";
            final String tUUID = "'" + UUID.randomUUID().toString() + "'";
            final JSONObject tMP = pRequestMessage.getJSONObject("MP");
            final String tGWStan = pStan;
            String tStatus = "'0'";
            final Map<String, String> tMPMap = jsonToMap(tMP.toString());
            final String tINFO = pRequestMessage.has("INFO") ? pRequestMessage.getString("INFO") : "";
            String tAmount = tMPMap.containsKey("AMOUNT") ? tMPMap.get("AMOUNT") : "NULL";
            String tName = tMPMap.containsKey("CUSTOMER_NAME") ? tMPMap.get("CUSTOMER_NAME").trim() : "NULL";
            String tAdmin = tMPMap.containsKey("ADMIN") ? String.valueOf(Integer.valueOf(tMPMap.get("ADMIN"))) : "NULL";

            tTableRecordDataHashMap.put("EBM_TM_ID", tUUID);
            tTableRecordDataHashMap.put("EBM_TM_REFNUM", "'" + pRF + "'");
            tTableRecordDataHashMap.put("EBM_TM_STAN", "'" + pRequestMessage.getString("ST") + "'");
            tTableRecordDataHashMap.put("EBM_TM_ACCOUNT", "'" + tMP.getString("ACCSRC") + "'");
            tTableRecordDataHashMap.put("EBM_TM_BILL_ID", "'" + tMP.getString("BILLID") + "'");
            tTableRecordDataHashMap.put("EBM_TM_CURRENCY", "'" + tMP.getString("CR") + "'");
            tTableRecordDataHashMap.put("EBM_TM_AMOUNT", "'" + tAmount + "'");
            tTableRecordDataHashMap.put("EBM_TM_SAVED_DT", tLoggedTime);
            tTableRecordDataHashMap.put("EBM_TM_TRAN_DT", tLoggedTime);
            tTableRecordDataHashMap.put("EBM_TM_MSG_DT", tLoggedTime);
            tTableRecordDataHashMap.put("EBM_TM_STATUS", tStatus);    // 00 inquiry, 01 payment sukses, 02 gagal
            tTableRecordDataHashMap.put("EBM_TM_OTP", "'" + "'");
            tTableRecordDataHashMap.put("EBM_TM_OTP_EXPIRE", "'" + "'");
            tTableRecordDataHashMap.put("EBM_TM_GW_STAN", "'" + tGWStan + "'");
            tTableRecordDataHashMap.put("EBM_TM_INFO", "'" + tINFO + "'");
            tTableRecordDataHashMap.put("EBM_TM_CC", "'" + pRequestMessage.getString("CC") + "'");
            tTableRecordDataHashMap.put("EBM_TM_NAME", "'" + tName + "'");
            tTableRecordDataHashMap.put("EBM_TM_ADMIN", "'" + tAdmin + "'");
            executeLogToTranDownTable(tTableRecordDataHashMap, tTableName);
        }
    }

    public static void logToTransferTranTable(final ISO8583Message pRequestMessage, final String pRF, final String pStan) {
        final String tTableName = "EBANKMOD_PAM_LOG_TRAN";
        final HashMap<String, String> tTableRecordDataHashMap = new HashMap<>();
        final String tLoggedTime = "'" + new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()) + "'";
        final String tUUID = "'" + UUID.randomUUID().toString() + "'";
        final String tRC = pRequestMessage.getDataElements().containsKey(39) ? "'" + pRequestMessage.getValueForDataElement(39) + "'" : "NULL";

        tTableRecordDataHashMap.put("EBM_LT_ID", tUUID);
        tTableRecordDataHashMap.put("EBM_LT_ACCOUNT", "'" + pRequestMessage.getValueForDataElement(102) + "'");
        tTableRecordDataHashMap.put("EBM_LT_PC", "'" + pRequestMessage.getValueForDataElement(3) + "'");
        tTableRecordDataHashMap.put("EBM_LT_REFNUM", "'" + pRF + "'");
        tTableRecordDataHashMap.put("EBM_LT_BILL_ID", "'" + pRequestMessage.getValueForDataElement(61).trim() + "'");
        tTableRecordDataHashMap.put("EBM_LT_CMD", "'" + pRequestMessage.getValueForDataElement(0) + "'");
        tTableRecordDataHashMap.put("EBM_LT_LOGGED_DT", tLoggedTime);
        tTableRecordDataHashMap.put("EBM_LT_RC", tRC);
        tTableRecordDataHashMap.put("EBM_LT_CC", "'" + pRequestMessage.getValueForDataElement(32) + "'");
        tTableRecordDataHashMap.put("EBM_LT_STAN", "'" + pStan + "'");
        tTableRecordDataHashMap.put("EBM_LT_WID", "'" + String.valueOf(Thread.currentThread().getId()) + "'");
        tTableRecordDataHashMap.put("EBM_LT_RAW_STREAM", "'" + pRequestMessage.getMessageStream().replace("'", "`") + "'");
        tTableRecordDataHashMap.put("EBM_LT_GW_STAN", "'" + pRequestMessage.getValueForDataElement(11) + "'");
        executeLogToTranDownTable(tTableRecordDataHashMap, tTableName);
    }

    public static void requestMessageToLogTable(final JSONObject pRequestMessage, final String pGatewayStan) {
        String tREFNUM = null;

        if (pRequestMessage.getString("MT").equals("2200")) {
            tREFNUM = pRequestMessage.getString("RF");
        }

        logToTransferTranDownTable(pRequestMessage, tREFNUM, pGatewayStan);
        logToTransferTranMainTable(pRequestMessage, tREFNUM, pGatewayStan);
    }

    public static void requestMessageToLogTable(final JSONObject pRequestMessage, final String pGatewayStan, final String tRF) {
        logToTransferTranDownTable(pRequestMessage, tRF, pGatewayStan);
        logToTransferTranMainTable(pRequestMessage, tRF, pGatewayStan);
        logToAccActivityTable(pRequestMessage, tRF);
    }

    public static void responseMessageToTable(final JSONObject pRequestMessage, final String pGatewayStan, final String pRF) {
        logToTransferTranDownTable(pRequestMessage, pRF, pGatewayStan);
        logToTransferTranMainTable(pRequestMessage, pRF, pGatewayStan);
        logToAccActivityTable(pRequestMessage, pRF);
    }

    public static String statusPaymentMainTable(final String pRF) {
        try {
            final Map<String, String> tDefineReturnFieldNameDatabase = new HashMap<>();

            tDefineReturnFieldNameDatabase.put("FieldNameReturn", "EBM_TM_STATUS");
            tDefineReturnFieldNameDatabase.put("FieldNameValue", pRF);
            tDefineReturnFieldNameDatabase.put("FieldNameKey", "EBM_TM_REFNUM");
            tDefineReturnFieldNameDatabase.put("TableName", cLogTranMain);

            final String tStatus = getValueByFieldNameDatabase(tDefineReturnFieldNameDatabase).get(0);

            return tStatus;
        } catch (Exception ex) {
        }

        return null;
    }

    public static void updateAccountActity(final JSONObject pResponseMessage, final String pRF, final String pStatus, final String pReceipt) {
        try {
            if (pResponseMessage.getString(cSSKey) != null) {
                final String tLoggedTime = "'" + new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()) + "'";
                final String tStatus = "'" + pStatus + "'";
                final String tSessionId = "'" + pResponseMessage.getString(cSSKey) + "'";
                final String tRefNum = "'" + pRF + "'";
                final String tReceipt = "'" + pReceipt.replace("'", "`") + "'";
                final String tAmount = "'" + (pResponseMessage.getJSONObject(cModuleParamKey).has("AMOUNT") ? pResponseMessage.getJSONObject(cModuleParamKey).getString("AMOUNT") : "0") + "'";
                final String name = (String) SystemConfig.getNameSpace(cModuleNameSpace).getArrayParameter(cLoadedKey).get(0);
                final DB tUsedDB = DBConnectionPools.getInstance().getDB(name);
                final String t_Query = "UPDATE EBANKCORE_ACC_ACTIVITY "
                        + "SET EBC_AA_STATUS = " + tStatus
                        + ", EBC_AA_DATE = " + tLoggedTime
                        + ", EBC_AA_INFO = '" + SystemConfig.getNameSpace(cModuleNameSpace).getStringParameter(cFeatureName, "Internet Banking") + "'"
                        + ", EBC_AA_RECEIPT = " + tReceipt
                        + ", EBC_AA_AMOUNT = " + tAmount
                        + " WHERE EBC_AA_SESSION = " + tSessionId
                        + " AND EBC_AA_ID = " + tRefNum;

                HashMap<Integer, DBFieldEntry> tRecordDataHashMap = new HashMap<>();
                Statement tStatement = tUsedDB.executeCallableQuery(t_Query, tRecordDataHashMap, 5);

                if (tStatement != null) {
                    tStatement.close();
                }
            }
        } catch (SQLException ex) {
            throw new SystemException(ResponseCode.ERROR_DATABASE, "Error Update database for log EBANKCORE_ACC_ACTIVITY");
        }
    }

    public static void updateOTPLogMainTable(final String pRF, final String pOTP) {
        try {
            if (pRF != null) {
                final String tOTP = "'" + pOTP + "'";
                final String tRefNum = "'" + pRF + "'";
                final Calendar tCalendar = Calendar.getInstance();
                final int tDayDueDate = SystemConfig.getNameSpace(cModuleNameSpace).getIntParameter(cExpireOTPInMinute, 15);

                tCalendar.add(Calendar.MINUTE, tDayDueDate);

                final String tOtpExpire = "'" + new SimpleDateFormat("yyyyMMddHHmmss").format(tCalendar.getTime()) + "'";
                final String name = (String) SystemConfig.getNameSpace(cModuleNameSpace).getArrayParameter(cLoadedKey).get(0);
                final DB tUsedDB = DBConnectionPools.getInstance().getDB(name);
                final String t_Query = "UPDATE " + cLogTranMain + " SET EBM_TM_OTP = " + tOTP + ", EBM_TM_OTP_EXPIRE = " + tOtpExpire
                        + " WHERE EBM_TM_REFNUM = " + tRefNum;
                HashMap<Integer, DBFieldEntry> tRecordDataHashMap = new HashMap<>();
                Statement tStatement = tUsedDB.executeCallableQuery(t_Query, tRecordDataHashMap, 5);

                if (tStatement != null) {
                    tStatement.close();
                }
            }
        } catch (SQLException ex) {
        }
    }

    public static void updateStatusLogMainTable(final String pRF, final String pStan, final String pStatus, final JSONObject pRequestMessage) {
        try {
            if (pRF != null) {
                final JSONObject tMP = pRequestMessage.getJSONObject("MP");
                final String tStatus = "'" + pStatus + "'";
                final String tStan = "'" + pStan + "'";
                final String tRefNum = "'" + pRF + "'";
                final String name = (String) SystemConfig.getNameSpace(cModuleNameSpace).getArrayParameter(cLoadedKey).get(0);
                final DB tUsedDB = DBConnectionPools.getInstance().getDB(name);

                final Map<String, String> tMPMap = jsonToMap(tMP.toString());
                String tNamaPel = tMPMap.containsKey("CUSTOMER_NAME") ? tMPMap.get("CUSTOMER_NAME").replace("'", "`").trim() : "NULL";

                final String t_Query = "UPDATE " + cLogTranMain + " SET EBM_TM_STATUS = " + tStatus + ", EBM_TM_GW_STAN =" + tStan
                        + ", EBM_TM_AMOUNT =" + "'"
                        + String.valueOf(new BigInteger(tMP.getString("TOTAL_AMOUNT")))
                        + "'" + ", EBM_TM_NAME =" + "'" + tNamaPel + "'" + ", EBM_TM_ADMIN =" + "'"
                        + tMP.getString("ADMIN") + "'" + " WHERE EBM_TM_REFNUM = " + tRefNum;
                HashMap<Integer, DBFieldEntry> tRecordDataHashMap = new HashMap<>();
                Statement tStatement = tUsedDB.executeCallableQuery(t_Query, tRecordDataHashMap, 5);

                if (tStatement != null) {
                    tStatement.close();
                }
            }
        } catch (SQLException ex) {
            throw new SystemException(ResponseCode.ERROR_DATABASE, "Error Update database for log main table " + cLogTranMain);
        }
    }

    public static boolean isExitsRefnum(final String pRF) {
        try {
            final Map<String, String> tDefineReturnFieldNameDatabase = new HashMap<>();

            tDefineReturnFieldNameDatabase.put("FieldNameReturn", "EBM_TM_REFNUM");
            tDefineReturnFieldNameDatabase.put("FieldNameValue", pRF);
            tDefineReturnFieldNameDatabase.put("FieldNameKey", "EBM_TM_REFNUM");
            tDefineReturnFieldNameDatabase.put("TableName", cLogTranMain);

            final String tRefNum = getValueByFieldNameDatabase(tDefineReturnFieldNameDatabase).get(0);

            return tRefNum != null;
        } catch (Exception ex) {
        }

        return false;
    }

    public static boolean isExitsSessionID(final String pSessionId, final String pRF) {
        boolean tResult = false;

        try {
            final String tREFFNUM = "'" + pRF + "'";
            final String tSessionId = "'" + pSessionId + "'";
            final String name = (String) SystemConfig.getNameSpace(cModuleNameSpace).getArrayParameter(cLoadedKey).get(0);
            final DB tUsedDB = DBConnectionPools.getInstance().getDB(name);
            final String t_Query = "SELECT EBC_AA_ID, EBC_AA_SESSION FROM EBANKCORE_ACC_ACTIVITY WHERE EBC_AA_ID = " + tREFFNUM
                    + " AND EBC_AA_SESSION = " + tSessionId;
            HashMap<Integer, DBFieldEntry> tRecordDataHashMap = new HashMap<>();
            Statement tStatement = tUsedDB.executeCallableQuery(t_Query, tRecordDataHashMap, 5);

            if (tStatement != null) {
                ResultSet tResultSet = tStatement.getResultSet();

                if (tResultSet != null) {
                    if (tResultSet.next()) {
                        if (tResultSet.getString("EBC_AA_ID").equals(pRF) && tResultSet.getString("EBC_AA_SESSION").equals(pSessionId)) {
                            tResult = true;
                        }
                    }

                    tResultSet.close();
                }

                tStatement.close();
            }
        } catch (SQLException ex) {
        }

        return tResult;
    }

    public static boolean isNeedOtp(final String pOtp) {
        try {
            final Map<String, String> tDefineReturnFieldNameDatabase = new HashMap<>();

            tDefineReturnFieldNameDatabase.put("FieldNameReturn", "EBM_TM_OTP, EBM_TM_OTP_EXPIRE");
            tDefineReturnFieldNameDatabase.put("FieldNameValue", pOtp);
            tDefineReturnFieldNameDatabase.put("FieldNameKey", "EBM_TM_REFNUM");
            tDefineReturnFieldNameDatabase.put("TableName", cLogTranMain);

            final String tOtpData = getValueByFieldNameDatabase(tDefineReturnFieldNameDatabase).get(0);

            if (tOtpData == null) {
                return true;
            }
        } catch (Exception ex) {
        }

        return false;
    }

    public static boolean isValidOtp(final String pRF, final String pOtp) {
        try {
            final Map<String, String> tDefineReturnFieldNameDatabase = new HashMap<>();

            tDefineReturnFieldNameDatabase.put("FieldNameReturn", "EBM_TM_OTP, EBM_TM_OTP_EXPIRE");
            tDefineReturnFieldNameDatabase.put("FieldNameValue", pRF);
            tDefineReturnFieldNameDatabase.put("FieldNameKey", "EBM_TM_REFNUM");
            tDefineReturnFieldNameDatabase.put("TableName", cLogTranMain);

            final List<String> tOtpData = getValueByFieldNameDatabase(tDefineReturnFieldNameDatabase);
            final String tOtp = tOtpData.get(0);
            final String tOtpExpire = tOtpData.get(1);
            final long start = new SimpleDateFormat("yyyyMMddHHmmss").parse(tOtpExpire).getTime();
            final long end = System.currentTimeMillis();

            if (pOtp.equals(tOtp) && (end < start)) {
                return true;
            }
        } catch (Exception ex) {
        }

        return false;
    }

    public static boolean isOtpStillValid(final String pRF) {
        try {
            final Map<String, String> tDefineReturnFieldNameDatabase = new HashMap<>();

            tDefineReturnFieldNameDatabase.put("FieldNameReturn", "EBM_TM_REFNUM, EBM_TM_OTP_EXPIRE");
            tDefineReturnFieldNameDatabase.put("FieldNameValue", pRF);
            tDefineReturnFieldNameDatabase.put("FieldNameKey", "EBM_TM_REFNUM");
            tDefineReturnFieldNameDatabase.put("TableName", cLogTranMain);

            final List<String> tOtpData = getValueByFieldNameDatabase(tDefineReturnFieldNameDatabase);
            final String tRefnumExist = tOtpData.get(0);
            final String tOtpExpire = tOtpData.get(1);
            final long start = new SimpleDateFormat("yyyyMMddHHmmss").parse(tOtpExpire).getTime();
            final long end = System.currentTimeMillis();

            if (pRF.equals(tRefnumExist) && (end < start)) {
                return true;
            }
        } catch (Exception ex) {
        }

        return false;
    }

    private static void executeLogToTranDownTable(final HashMap<String, String> tTableRecordDataHashMap, final String pTablenName) {
        Statement tStatement = null;

        try {
            DBConnectionPools.getInstance().getDB(
                    (String) SystemConfig.getNameSpace(cModuleNameSpace).getArrayParameter(cLoadedKey).get(0)).insertToTable(
                    tTableRecordDataHashMap, pTablenName);
        } catch (SQLException ex) {
            SystemLog.getSingleton().log(cLoggingStaticHookObject, LogType.DB_ERROR, "Error when inserting to table [" + pTablenName + "]. Record data hash map : " + tTableRecordDataHashMap, ex);

            throw new SystemException(ResponseCode.ERROR_TIMEOUT, "Error when inserting to table [" + pTablenName + "]. Record data hash map : " + tTableRecordDataHashMap, ex, cLoggingStaticHookObject);
        } finally {
            if (tStatement != null) {
                try {
                    tStatement.close();
                } catch (SQLException ex) {
                }
            }
        }
    }

    private static List<String> getValueByFieldNameDatabase(final Map<String, String> pDatabaseFieldMap) throws SQLException, Exception {
        List<String> tResult = new ArrayList<>();
        final String name = (String) SystemConfig.getNameSpace(cModuleNameSpace).getArrayParameter(cLoadedKey).get(0);
        final String tFieldNameValue = pDatabaseFieldMap.get("FieldNameValue");
        final String tFieldNameReturn = pDatabaseFieldMap.get("FieldNameReturn");
        final String tFieldNameKey = pDatabaseFieldMap.get("FieldNameKey");
        final String tTableName = pDatabaseFieldMap.get("TableName");
        final String t_Query = "SELECT " + tFieldNameReturn + " FROM " + tTableName + " WHERE " + tFieldNameKey + " =?";
        HashMap<Integer, DBFieldEntry> tRecordDataHashMap = new HashMap<>();

        tRecordDataHashMap.put(1, DBFieldEntry.createEntry(tFieldNameValue, DBFieldType.VARCHAR));

        final DB tUsedDB = DBConnectionPools.getInstance().getDB(name);
        Statement tStatement = tUsedDB.executeCallableQuery(t_Query, tRecordDataHashMap, SystemConfig.getNameSpace(cModuleNameSpace).getIntParameter(cQueryTimeOutKey, 5));

        if (tStatement != null) {
            ResultSet tResultSet = tStatement.getResultSet();

            if (tResultSet != null) {
                if (tResultSet.next()) {
                    final String[] tFieldNames = tFieldNameReturn.replace(",", "").split("\\s");

                    for (String tFieldName : tFieldNames) {
                        tResult.add(tResultSet.getString(tFieldName));
                    }
                }

                tResultSet.close();
            }

            tStatement.close();
        }

        return tResult;
    }
}

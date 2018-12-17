package id.co.vsi.switcher.bjb.pdam.mitrateknis;

import static id.co.vsi.common.bp.ibank.BJBJsonHandler.cLoadedKey;
import static id.co.vsi.common.bp.ibank.BJBJsonHandler.cQueryTimeOutKey;
import id.co.vsi.common.constants.ResponseCode;
import id.co.vsi.common.database.DB;
import id.co.vsi.common.database.DBConnectionPools;
import id.co.vsi.common.database.DBFieldEntry;
import id.co.vsi.common.log.LogType;
import id.co.vsi.common.log.SystemLog;
import id.co.vsi.common.settings.SystemConfig;
import static id.co.vsi.switcher.bjb.pdam.mitrateknis.Common.cModuleNameSpace;
import id.co.vsi.systemcore.isocore.SystemException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.json.JSONObject;

public class SMSTransactions {
    
    public void logToModuleTransactionTable(final JSONObject pMessage, final String pRF) {
        final String                  tTableName              = "EBANKMOD_MODULE_TRANSACTION";
        final HashMap<String, String> tTableRecordDataHashMap = new HashMap<>();
        final String                  tUUID                   = "'" + UUID.randomUUID().toString() + "'";
        final JSONObject              tMP                     = pMessage.getJSONObject("MP");
        final String                  tAccountSource          = tMP.getString("ACCSRC");
        final String                  tAmount                 = "000000000000000";
        final String                  tPhoneNumber            = tMP.getString("SMS_PHONE_NUMBER");
        
        tTableRecordDataHashMap.put("EBM_MT_ID", tUUID);
        tTableRecordDataHashMap.put("EBM_MT_REFNUM", "'" + pRF + "'");
        tTableRecordDataHashMap.put("EBM_MT_RAW_STREAM", "'" + pMessage.toString().replace("'", "`") + "'");
        tTableRecordDataHashMap.put("EBM_MT_ACCOUNT_NUMBER", "'" + tAccountSource + "'");
        tTableRecordDataHashMap.put("EBM_MT_PHONE_NUMBER", "'" + tPhoneNumber + "'");
        tTableRecordDataHashMap.put("EBM_MT_MODULE_CODE", "'" + pMessage.getString("MC") + "'");
        tTableRecordDataHashMap.put("EBM_MT_AMOUNT", "'" + tAmount + "'");
        tTableRecordDataHashMap.put("EBM_MT_DT", "'" + pMessage.getString("DT") + "'");
        tTableRecordDataHashMap.put("EBM_MT_STATUS", "'0'");
        executeQuery(tTableRecordDataHashMap, tTableName); 
    }
    
    private void executeQuery(final HashMap<String, String> tTableRecordDataHashMap, final String pTableName) {
        Statement tStatement = null;
        
        try {
            DBConnectionPools.getInstance().getDB((String) SystemConfig.getNameSpace(cModuleNameSpace).getArrayParameter(cLoadedKey).get(0)).insertToTable(tTableRecordDataHashMap, pTableName);
        } catch (SQLException ex) {
            SystemLog.getSingleton().log(Common.cLoggingStaticHookObject, LogType.DB_ERROR, "Error when inserting to table [" + pTableName + "]. Record data hash map : " + tTableRecordDataHashMap,ex);

            throw new SystemException(ResponseCode.ERROR_TIMEOUT, "Error when inserting to table [" + pTableName + "]. Record data hash map : " + tTableRecordDataHashMap, ex, Common.cLoggingStaticHookObject);
        } finally {
            if (tStatement != null) {
                try {
                    tStatement.close();
                } catch (SQLException ex) {}
            }
        }
    }
    
    public JSONObject getRawStream(final JSONObject pMessage) {
        JSONObject tResultJson = new JSONObject();
        final Map<String, String> tResultMap = new HashMap<>();
        final JSONObject          tMP        = pMessage.getJSONObject("MP");
        final String              tMC        = pMessage.getString("MC");
        final String              tAccountSource = tMP.getString("ACCSRC");
        final String              tAmount        = "000000000000000";
        final String              tPhoneNumber   = tMP.getString("SMS_PHONE_NUMBER");        

        try {
            final String name    = (String) SystemConfig.getNameSpace(cModuleNameSpace).getArrayParameter(cLoadedKey).get(0);
            final DB     tUsedDB = DBConnectionPools.getInstance().getDB(name);
            final String t_Query = "SELECT EBM_MT_RAW_STREAM FROM EBANKMOD_MODULE_TRANSACTION "
                    + "WHERE EBM_MT_MODULE_CODE = '" + tMC + "' "
                    + "AND EBM_MT_PHONE_NUMBER = '" + tPhoneNumber + "' "
                    + "AND EBM_MT_ACCOUNT_NUMBER = '" + tAccountSource + "' "
                    + "AND EBM_MT_AMOUNT = '" + tAmount + "' "
                    + "AND EBM_MT_STATUS = '0' "
                    + "ORDER BY EBM_MT_DT DESC LIMIT 1 ";
            HashMap<Integer, DBFieldEntry> tRecordDataHashMap = new HashMap<>();

            Statement tStatement = tUsedDB.executeCallableQuery(t_Query, tRecordDataHashMap, SystemConfig.getNameSpace(cModuleNameSpace).getIntParameter(cQueryTimeOutKey, 5));

            if (tStatement != null) {
                ResultSet tResultSet = tStatement.getResultSet();

                if (tResultSet != null) {
                    if (tResultSet.next()) {
                        tResultMap.put("EBM_MT_RAW_STREAM", tResultSet.getString("EBM_MT_RAW_STREAM"));
                    }
                    tResultSet.close();
                }
                tStatement.close();
            }
            tResultJson = new JSONObject(tResultMap.get("EBM_MT_RAW_STREAM"));
            
        } catch (SQLException e) {}

        return tResultJson;     
    }
    
    public void updateStatusSMSPayment(final String pRF, final JSONObject pResponseMessage) {
        try {
            final JSONObject tMP     = pResponseMessage.getJSONObject("MP");
            final String     name    = (String) SystemConfig.getNameSpace(cModuleNameSpace).getArrayParameter(cLoadedKey).get(0);
            final DB         tUsedDB = DBConnectionPools.getInstance().getDB(name);
            final String     t_Query = "UPDATE EBANKMOD_MODULE_TRANSACTION "
                    + "SET EBM_MT_STATUS = '1', EBM_MT_AMOUNT = '" + tMP.getString("AMOUNT") + "' "
                    + "WHERE EBM_MT_REFNUM = '" + pRF + "'";
            HashMap<Integer, DBFieldEntry> tRecordDataHashMap = new HashMap<>();
            Statement                      tStatement         = tUsedDB.executeCallableQuery(t_Query, tRecordDataHashMap, 5);

            if (tStatement != null) {
                tStatement.close();
            }
        } catch (SQLException ex) {
            throw new SystemException(ResponseCode.ERROR_DATABASE, "Error Update database for EBANKMOD_MODULE_TRANSACTIONS table : " + ex);
        }
    } 
}

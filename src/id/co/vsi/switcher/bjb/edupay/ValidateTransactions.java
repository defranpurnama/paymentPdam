package id.co.vsi.switcher.bjb.edupay;

import static id.co.vsi.common.bp.ibank.BJBJsonHandler.cLoadedKey;
import static id.co.vsi.common.bp.ibank.BJBJsonHandler.cQueryTimeOutKey;
import id.co.vsi.common.constants.ResponseCode;
import id.co.vsi.common.crypto.SymetricCryptoHandler;
import id.co.vsi.common.database.DB;
import id.co.vsi.common.database.DBConnectionPools;
import id.co.vsi.common.database.DBFieldEntry;
import id.co.vsi.common.log.LogType;
import id.co.vsi.common.log.SystemLog;
import id.co.vsi.common.settings.SystemConfig;
import static id.co.vsi.switcher.bjb.edupay.Common.cModuleNameSpace;
import id.co.vsi.systemcore.isocore.SystemException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import org.json.JSONObject;

public class ValidateTransactions {
    private static final String cAESEncryptionKey   = "encryption-key";
    private static final String cAESEncryptionType  = "encryption-type";
    
    private String getCheckSum(final String pMessageStream){
        /*GET ENCRYPTION TYPE*/
        final Map<String, Object> tParams = new HashMap<>();
        final String tAESEncryptionType   = SystemConfig.getNameSpace(cModuleNameSpace).getStringParameter(cAESEncryptionType, "AES/ECB/PKCS5Padding");

        if (tAESEncryptionType != null) {
            final String[] tAESConfig = tAESEncryptionType.split("/");

            tParams.put("AESType", tAESConfig[0]);
            tParams.put("AESMode", tAESConfig[1]);
            tParams.put("AESPadding", tAESConfig[2]);
            tParams.put("AESKey", SystemConfig.getNameSpace(cModuleNameSpace).getStringParameter(cAESEncryptionKey));
        }
        tParams.put("requestMessage", pMessageStream);
        
        /*ENCRYPT*/
        final String            tAESType              = (String) tParams.get("AESType");
        final String            tAESMode              = (String) tParams.get("AESMode");
        final String            tAESPadding           = (String) tParams.get("AESPadding");
        final String            tAESKey               = (String) tParams.get("AESKey");
        String                  tRequestStream        = (String) tParams.get("requestMessage");
        SymetricCryptoHandler   tCryptoHandler        = null;

        try {
            tCryptoHandler = new SymetricCryptoHandler(tAESType, tAESMode, tAESPadding, tAESKey, null);
        } catch (InvalidAlgorithmParameterException | NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException ex) {
            throw new SystemException(ResponseCode.ERROR_OTHER, "Error when encrypting message " + tRequestStream + " with key " + tAESKey, ex,
                                      this);
        }

        try {
            tRequestStream = tCryptoHandler.getCryptoMessage(SymetricCryptoHandler.ACTION_ENCRYPT, tRequestStream);
        } catch (InvalidAlgorithmParameterException | NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException ex) {
            throw new SystemException(ResponseCode.ERROR_OTHER, "Error when encrypting message " + tRequestStream + " with key " + tAESKey, ex,
                                      this);
        }
            
        /*CHECKSUM*/   
        StringBuilder sb = new StringBuilder();

        try {
            final String tFullString = tRequestStream;

            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(tFullString.getBytes());

            byte byteData[] = md.digest();

            /*Convert the byte to hex*/
            for (int i = 0; i < byteData.length; i++) {
                sb.append(Integer.toString((byteData[i] & 0xff) + 0x100, 16).substring(1));
            }
        } catch (NoSuchAlgorithmException e) {
        }
        
        return sb.toString();
    }
    
    public void logToValidateTransactionTable(final JSONObject pMessage, final String pRF) {
        final String                  tTableName              = "EBANKMOD_VALIDATE_TRANSACTION";
        final HashMap<String, String> tTableRecordDataHashMap = new HashMap<>();
        final String                  tUUID                   = "'" + UUID.randomUUID().toString() + "'";
        final JSONObject              tMP                     = pMessage.getJSONObject("MP");
        final String                  tAccountSource          = tMP.getString("ACCSRC");
        final String                  tBillID                 = tMP.getString("BILLID");
        final String                  tCheckSum               = new ValidateTransactions().getCheckSum(pRF+tAccountSource+tBillID);

        tTableRecordDataHashMap.put("EBM_VT_ID", tUUID);
        tTableRecordDataHashMap.put("EBM_VT_REFNUM", "'" + pRF + "'");
        tTableRecordDataHashMap.put("EBM_VT_CHECKSUM", "'" + tCheckSum + "'");
        tTableRecordDataHashMap.put("EBM_VT_COUNTER_OTP", "'0'");
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
    
    private Map<String, String> getCheckSumData(final String pCheckSum) {
        final Map<String, String> tResult = new HashMap<>();

        try {
            final String name           = (String) SystemConfig.getNameSpace(cModuleNameSpace).getArrayParameter(cLoadedKey).get(0);
            final DB     tUsedDB        = DBConnectionPools.getInstance().getDB(name);
            final String t_Query        = "SELECT EBM_VT_CHECKSUM FROM EBANKMOD_VALIDATE_TRANSACTION WHERE EBM_VT_CHECKSUM = '" + pCheckSum + "'";
            HashMap<Integer, DBFieldEntry> tRecordDataHashMap = new HashMap<>();

            Statement tStatement = tUsedDB.executeCallableQuery(t_Query, tRecordDataHashMap, SystemConfig.getNameSpace(cModuleNameSpace).getIntParameter(cQueryTimeOutKey, 5));

            if (tStatement != null) {
                ResultSet tResultSet = tStatement.getResultSet();

                if (tResultSet != null) {
                    if (tResultSet.next()) {
                        tResult.put("EBM_VT_CHECKSUM", tResultSet.getString("EBM_VT_CHECKSUM"));
                    }
                    tResultSet.close();
                }
                tStatement.close();
            }
        } catch (SQLException e) {}

        return tResult;
    }
    
    private Map<String, String> getCounterOTPData(final String pRF) {
        final Map<String, String> tResult = new HashMap<>();

        try {
            final String name           = (String) SystemConfig.getNameSpace(cModuleNameSpace).getArrayParameter(cLoadedKey).get(0);
            final DB     tUsedDB        = DBConnectionPools.getInstance().getDB(name);
            final String t_Query        = "SELECT EBM_VT_COUNTER_OTP FROM EBANKMOD_VALIDATE_TRANSACTION WHERE EBM_VT_REFNUM = '" + pRF + "'";
            HashMap<Integer, DBFieldEntry> tRecordDataHashMap = new HashMap<>();

            Statement tStatement = tUsedDB.executeCallableQuery(t_Query, tRecordDataHashMap, SystemConfig.getNameSpace(cModuleNameSpace).getIntParameter(cQueryTimeOutKey, 5));

            if (tStatement != null) {
                ResultSet tResultSet = tStatement.getResultSet();

                if (tResultSet != null) {
                    if (tResultSet.next()) {
                        tResult.put("EBM_VT_COUNTER_OTP", tResultSet.getString("EBM_VT_COUNTER_OTP"));
                    }

                    tResultSet.close();
                }

                tStatement.close();
            }
        } catch (SQLException e) {}

        return tResult;
    }
    
    private void updateCounterOTP(final String pRF, final int pCounterOTP) {
        try {
            final String      name    = (String) SystemConfig.getNameSpace(cModuleNameSpace).getArrayParameter(cLoadedKey).get(0);
            final DB          tUsedDB = DBConnectionPools.getInstance().getDB(name);
            final String      t_Query = "UPDATE EBANKMOD_VALIDATE_TRANSACTION SET EBM_VT_COUNTER_OTP = '"+ pCounterOTP +"' WHERE EBM_VT_REFNUM = '"+ pRF +"'";
            HashMap<Integer, DBFieldEntry> tRecordDataHashMap = new HashMap<>();
            Statement                      tStatement         = tUsedDB.executeCallableQuery(t_Query, tRecordDataHashMap, 5);

            if (tStatement != null) {
                tStatement.close();
            }
        } catch (SQLException ex) {
            throw new SystemException(ResponseCode.ERROR_DATABASE, "Error Update database for EBANKMOD_VALIDATE_TRANSACTION table : " + ex);
        }
    }
    
    public void validCheckSum(final JSONObject pMessage){
        final JSONObject tMP            = pMessage.getJSONObject("MP");
        final String     tRF            = pMessage.getString("RF");
        final String     tAccountSource = tMP.getString("ACCSRC");
        final String     tBillID        = tMP.getString("BILLID");
        final String     tCheckSum      = new ValidateTransactions().getCheckSum(tRF+tAccountSource+tBillID);
        
        Map<String, String> tGetCheckSumData = getCheckSumData(tCheckSum);
        
        if (tGetCheckSumData.isEmpty()) {
            throw new SystemException(ResponseCode.ERROR_TRANSACTION_NOT_FOUND, "Illegal Transaction for : " + tAccountSource + " RF : " + tRF + ", cause CheckSum not found");
        }
    }
    
    public void checkCounterOTP(final JSONObject pMessage){
        final JSONObject tMP = pMessage.getJSONObject("MP");
        final String     tRF = pMessage.getString("RF");
        final String     tAccountSource = tMP.getString("ACCSRC");
        
        Map<String, String> tGetCounterOTPData = getCounterOTPData(tRF);
        
        if (tGetCounterOTPData.isEmpty()) {
            throw new SystemException(ResponseCode.ERROR_TRANSACTION_NOT_FOUND, "Illegal OTP for : " + tAccountSource + " RF : " + tRF + ", cause data not found");
        }
        
        int tCounterOTP = Integer.valueOf(tGetCounterOTPData.get("EBM_VT_COUNTER_OTP")) + 1;
        
        if (tCounterOTP >= 3) {
            updateCounterOTP(tRF, tCounterOTP);
            throw new SystemException(ResponseCode.ERROR_INVALID_HASHCODE, "COUNTER_OTP:" + String.valueOf(tCounterOTP));
        }        
        
        if (tCounterOTP < 3) {
            updateCounterOTP(tRF, tCounterOTP);
            throw new SystemException(ResponseCode.ERROR_INVALID_HASHCODE, "COUNTER_OTP:" + String.valueOf(tCounterOTP));
        }
    }
}

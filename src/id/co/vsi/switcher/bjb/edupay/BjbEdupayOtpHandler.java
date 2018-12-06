package id.co.vsi.switcher.bjb.edupay;

import static id.co.vsi.common.bp.ibank.BJBJsonHandler.c0001;
import static id.co.vsi.common.bp.ibank.BJBJsonHandler.c0002;
import static id.co.vsi.common.bp.ibank.BJBJsonHandler.c9100;
import static id.co.vsi.common.bp.ibank.BJBJsonHandler.c9110;
import static id.co.vsi.common.bp.ibank.BJBJsonHandler.cChannelCodeKey;
import static id.co.vsi.common.bp.ibank.BJBJsonHandler.cLoadedKey;
import static id.co.vsi.common.bp.ibank.BJBJsonHandler.cMTIKey;
import static id.co.vsi.common.bp.ibank.BJBJsonHandler.cModuleCodeKey;
import static id.co.vsi.common.bp.ibank.BJBJsonHandler.cOTPKey;
import static id.co.vsi.common.bp.ibank.BJBJsonHandler.cRCDKey;
import static id.co.vsi.common.bp.ibank.BJBJsonHandler.cRCKey;
import static id.co.vsi.common.bp.ibank.BJBJsonHandler.cRFKey;
import static id.co.vsi.common.bp.ibank.BJBJsonHandler.cSSKey;
import id.co.vsi.common.bp.ibank.SMSUtil;
import id.co.vsi.common.constants.ResponseCode;
import id.co.vsi.common.settings.SystemConfig;
import static id.co.vsi.switcher.bjb.edupay.Common.cModuleNameSpace;
import static id.co.vsi.switcher.bjb.edupay.Common.cValidMCAndIP;
import id.co.vsi.systemcore.isocore.SystemException;
import id.co.vsi.systemcore.jasoncore.JSONMessage;
import id.co.vsi.systemcore.jasoncore.JSONPlugin;
import org.json.JSONObject;

public class BjbEdupayOtpHandler extends BjbEdupayMessageHandler {

    public BjbEdupayOtpHandler(JSONPlugin pParentPlugin) {
        super(pParentPlugin);
    }
    
    @Override
    public boolean canHandleMessage(JSONMessage pRequestmessage) {
        return pRequestmessage.getString(cMTIKey).equals(c9100);
    }

    @Override
    public JSONMessage handleMessage(JSONMessage pRequestMessage) {
        final JSONObject tResponseMessage = new JSONObject(pRequestMessage.toString());
        isValidSession(tResponseMessage, SystemConfig.getNameSpace(cModuleNameSpace).getStringParameter(cLoadedKey));
        
        try {
            Common.logToTransferTranDownTable(tResponseMessage);
            validOtpHandler(tResponseMessage);

            final String tNewOTP = generateNewOTP(8, 0);
            final String tRF     = tResponseMessage.getString(cRFKey);
            final String tCC     = tResponseMessage.getString(cChannelCodeKey);
            final String tHashPasswordMD5 = generateHashMD5(tNewOTP);
            String tPhoneNumber = null;

            if (tCC.equals(c0002)) {
                tPhoneNumber = SMSUtil.indonesianFormatPhoneNumber(getPhoneNumberNET(pRequestMessage.getString(cUIDKey), cModuleNameSpace));
                final boolean isOtpStillValid = Common.isOtpStillValid(tRF);
                if (isOtpStillValid) {
                    throw new SystemException(ResponseCode.ERROR_INVALID_ACCESS_TIME, "[" + tPhoneNumber + "] OTP Not Sent cause browser refresh, " + " OTP = " + tNewOTP);
                }
            }
            
            Common.updateOTPLogMainTable(tRF, tHashPasswordMD5);
            tResponseMessage.put(cMTIKey, c9110);

            final String tMC = pRequestMessage.getString(cModuleCodeKey);

            if (tCC.equals(c0001)) {
                tResponseMessage.put(cOTPKey, tHashPasswordMD5);
            } else if (tCC.equals(c0002)) {
                SMSUtil.sendOTPViaSMS(SystemConfig.getNameSpace(cModuleNameSpace), tPhoneNumber, tNewOTP, tMC);
            }

            tResponseMessage.put(cRCKey, "0000");
            tResponseMessage.put(cRCDKey, "Sukses");
        } catch (SystemException exception) {
            tResponseMessage.put(cMTIKey, c9110);
            tResponseMessage.put(cRCKey, exception.getResponseCode().getResponseCodeString());
            tResponseMessage.put(cRCDKey, "Transaksi Tidak Dapat Diproses (" + exception.getResponseCode().getResponseCodeString() + ")");
        }

        Common.logToTransferTranDownTable(tResponseMessage);

        return new JSONMessage(tResponseMessage.toString());
    }

    public void validOtpHandler(JSONObject pRequestMessage) throws SystemException {
        final String tSessionId = pRequestMessage.getString(cSSKey);
        final boolean isExitsSessionID = Common.isExitsSessionID(tSessionId, pRequestMessage.getString(cRFKey));

        validIPandMC(cValidMCAndIP, pRequestMessage.getString(cModuleCodeKey));

        if (isExitsSessionID == false) {
            throw new SystemException(ResponseCode.ERROR_NEED_TO_SIGN_ON, "OTP request no sessionid  for [" + tSessionId + "]  - " + pRequestMessage, null, this);
        }
    }
}

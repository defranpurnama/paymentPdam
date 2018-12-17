package id.co.vsi.switcher.bjb.pdam.mitrateknis;

import static id.co.vsi.common.bp.ibank.BJBJsonHandler.c0002;
import static id.co.vsi.common.bp.ibank.BJBJsonHandler.cACCSRCKey;
import static id.co.vsi.common.bp.ibank.BJBJsonHandler.cChannelCodeKey;
import static id.co.vsi.common.bp.ibank.BJBJsonHandler.cLoadedKey;
import static id.co.vsi.common.bp.ibank.BJBJsonHandler.cMobile_PINKey;
import static id.co.vsi.common.bp.ibank.BJBJsonHandler.cModuleParamKey;
import static id.co.vsi.common.bp.ibank.BJBJsonHandler.cOTPKey;
import static id.co.vsi.common.bp.ibank.BJBJsonHandler.cRFKey;
import static id.co.vsi.common.bp.ibank.BJBJsonHandler.cUIDKey;
import static id.co.vsi.common.bp.ibank.BJBJsonHandler.cValidPaymentField;
import static id.co.vsi.common.bp.ibank.BJBJsonHandler.isMultiAccountRegistered;
import static id.co.vsi.common.bp.ibank.BJBJsonHandler.isValidFieldElements;
import static id.co.vsi.common.bp.ibank.BJBJsonHandler.jsonToMap;
import id.co.vsi.common.bp.ibank.DBUtil;
import id.co.vsi.common.constants.ResponseCode;
import id.co.vsi.common.settings.SystemConfig;
import static id.co.vsi.switcher.bjb.pdam.mitrateknis.Common.cModuleNameSpace;
import static id.co.vsi.switcher.bjb.pdam.mitrateknis.Common.cPaymentProcessingCode;
import static id.co.vsi.switcher.bjb.pdam.mitrateknis.Common.cResponseCodeTable;
import static id.co.vsi.switcher.bjb.pdam.mitrateknis.Common.cValidPaymentModulParameter;
import id.co.vsi.systemcore.isocore.SystemException;
import id.co.vsi.systemcore.jasoncore.JSONMessage;
import id.co.vsi.systemcore.jasoncore.JSONPlugin;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.json.JSONObject;
import static id.co.vsi.common.bp.ibank.BJBJsonHandler.c7777;

public class BjbPdamPaymentHandler extends BjbPdamMessageHandler {

    public BjbPdamPaymentHandler(JSONPlugin pParentPlugin) {
        super(pParentPlugin);
    }

    @Override
    public boolean canHandleMessage(JSONMessage pRequestMessage) {
        return pRequestMessage.getString(cMTIKey).equals(c2200);
    }

    @Override
    public JSONMessage handleMessage(JSONMessage pRequestMessage) {
        JSONObject tResponseMessage = new JSONObject(pRequestMessage.toString());
        final String tGatewayStan = generateSTAN();

        try {
            /*CHECK CHECKSUM--if not equal throw exception, send RC ERROR_TRANSACTION_NOT_FOUND*/
            if (tResponseMessage.getString(cChannelCodeKey).equals(c0002) || tResponseMessage.getString(cChannelCodeKey).equals(c7777)) {
                new ValidateTransactions().validCheckSum(tResponseMessage);
            }

            Common.requestMessageToLogTable(tResponseMessage, tGatewayStan);
            validPayment(tResponseMessage);

            tResponseMessage = performPdamPayment(tResponseMessage);

            if (tResponseMessage.getString(cChannelCodeKey).equals(c0001)) {
                new SMSTransactions().updateStatusSMSPayment(tResponseMessage.getString(cRFKey), tResponseMessage);
            }
        } catch (SystemException e) {
            tResponseMessage = constructErrorMessage(tResponseMessage, e, cResponseCodeTable, cModuleNameSpace);
        }

        Common.responseMessageToTable(tResponseMessage, tGatewayStan, tResponseMessage.getString(cRFKey));

        return new JSONMessage(tResponseMessage.toString());
    }

    private JSONObject performPdamPayment(final JSONObject pRequestMessage) {
        return performPdamMessage(pRequestMessage, pRequestMessage.getString(cRFKey), cPaymentProcessingCode);
    }

    private void validPayment(JSONObject pRequestMessage) throws SystemException {
        isValidSession(pRequestMessage, SystemConfig.getNameSpace(cModuleNameSpace).getStringParameter(cLoadedKey));
        final Map<String, String> tJsonRequestMap = jsonToMap(pRequestMessage.toString());
        final List<String> tValidPaymentField = Arrays.asList(cValidPaymentField);
        final boolean isValidPaymentFieldElements = isValidFieldElements(tValidPaymentField, tJsonRequestMap);

        if (!isValidPaymentFieldElements) {
            throw new SystemException(ResponseCode.ERROR_INVALID_MESSAGE, "Invalid JSON message format message : " + pRequestMessage);
        }

        final List<String> tValidModulParameterField = Arrays.asList(cValidPaymentModulParameter);
        final Map<String, String> tModulParameterFieldMap = jsonToMap(pRequestMessage.getJSONObject(cModuleParamKey).toString());
        final boolean isValidModulParameter = isValidFieldElements(tValidModulParameterField, tModulParameterFieldMap);

        if (!isValidModulParameter) {
            throw new SystemException(ResponseCode.ERROR_INVALID_MESSAGE, "Invalid JSON format message : " + pRequestMessage);
        }

        final String tRF = pRequestMessage.getString(cRFKey);

        final JSONObject tMP = pRequestMessage.getJSONObject(cModuleParamKey);
        final String tAccountNumberSource = tMP.getString(cACCSRCKey);
        final boolean isMultiAccountRegistered = isMultiAccountRegistered(cModuleNameSpace, cLoadedKey, pRequestMessage);

        if (!isMultiAccountRegistered) {
            throw new SystemException(ResponseCode.ERROR_UNREGISTERED_ACCOUNT, "Account not registered[" + tAccountNumberSource + "]");
        }

        final String tActivityPaymentMain = Common.statusPaymentMainTable(tRF);

        if (tActivityPaymentMain == null) {
            throw new SystemException(ResponseCode.ERROR_DATABASE, "Fail retrieve data from database ", null, this);
        }

        if (tActivityPaymentMain.equals("1")) {
            throw new SystemException(ResponseCode.ERROR_BILL_ALREADY_PAID, "Already payment success from [" + tAccountNumberSource + "] bill id [" + tMP.getString("BILLID") + "]", null, this);
        } else if (tActivityPaymentMain.equals("2")) {
            throw new SystemException(ResponseCode.ERROR_NO_PAYMENT, "Fail payment", null, this);
        }

        final boolean isExitsRefnum = Common.isExitsRefnum(tRF);

        if (isExitsRefnum == false) {
            throw new SystemException(ResponseCode.ERROR_INVALID_SWITCHER_REF_NUMBER, "Payment Different Refnum for [" + tAccountNumberSource + "] ", null, this);
        }

        /*Validasi bjb NET atau bjb Mobile*/
        if (pRequestMessage.getString(cChannelCodeKey).equals(c7777)) {
            final boolean isValidMobilePIN = DBUtil.isValidMobilePIN(pRequestMessage.getString(cMobile_PINKey), pRequestMessage.getString(cUIDKey), SystemConfig.getNameSpace(cModuleNameSpace).getStringParameter(cLoadedKey));
            if (isValidMobilePIN == false) {
                /*VALID SMS_PIN COUNTER, jika <=3 (UPDATE db+1) jika >3 throw exception RC ERROR_REQUEST_EXPIRED*/
                new ValidateTransactions().checkCounterOTP(pRequestMessage);
                throw new SystemException(ResponseCode.ERROR_INVALID_HASHCODE, "Invalid Mobile PIN for [" + tAccountNumberSource + "] ", null, this);
            }
        } else if (pRequestMessage.getString(cChannelCodeKey).equals(c0002)) {
            final boolean isValidOtp = Common.isValidOtp(tRF, pRequestMessage.getString(cOTPKey));
            if (isValidOtp == false) {
                /*VALID OTP COUNTER, jika <=3 (UPDATE db+1) jika >3 throw exception RC ERROR_REQUEST_EXPIRED*/
                new ValidateTransactions().checkCounterOTP(pRequestMessage);
                throw new SystemException(ResponseCode.ERROR_INVALID_HASHCODE, "Invalid OTP for [" + tAccountNumberSource + "] ", null, this);
            }
        }
    }
}

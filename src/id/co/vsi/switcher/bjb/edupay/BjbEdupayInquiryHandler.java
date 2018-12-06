package id.co.vsi.switcher.bjb.edupay;

import id.co.vsi.common.constants.ResponseCode;
import id.co.vsi.common.settings.SystemConfig;
import static id.co.vsi.switcher.bjb.edupay.Common.cModuleNameSpace;
import id.co.vsi.systemcore.isocore.SystemException;
import id.co.vsi.systemcore.jasoncore.JSONMessage;
import id.co.vsi.systemcore.jasoncore.JSONPlugin;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.json.JSONObject;
import static id.co.vsi.switcher.bjb.edupay.Common.cInquiryProcessingCode;
import static id.co.vsi.switcher.bjb.edupay.Common.cResponseCodeTable;
import static id.co.vsi.switcher.bjb.edupay.Common.cValidInquiryModulParameter;

public class BjbEdupayInquiryHandler extends BjbEdupayMessageHandler {

    public BjbEdupayInquiryHandler(JSONPlugin pParentPlugin) {
        super(pParentPlugin);
    }

    @Override
    public boolean canHandleMessage(JSONMessage pRequestMessage) {
        return pRequestMessage.getString(cMTIKey).equals(c2100);
    }

    @Override
    public JSONMessage handleMessage(JSONMessage pRequestMessage) {
        JSONObject tResponseMessage = new JSONObject(pRequestMessage.toString());
        final String tGatewayStan = generateSTAN();
        final String tRF = UUID.randomUUID().toString().replace("-", "").toUpperCase();
        final JSONObject tDefaultAmount = setDefaultAmount(tResponseMessage);

        try {
            Common.requestMessageToLogTable(tDefaultAmount, tGatewayStan, tRF);
            validInquiry(tDefaultAmount);

            final Map<String, String> tJsonRequestMessageMap = jsonToMap(tResponseMessage.toString());

            if (tJsonRequestMessageMap.containsKey(cSMSKEYKey) && tJsonRequestMessageMap.get(cSMSKEYKey).equalsIgnoreCase(cPAYKey)) {
                tResponseMessage = new SMSTransactions().getRawStream(tResponseMessage);

                String tReplaceResponseMessage = tResponseMessage.toString().replace("`", "'");

                tResponseMessage = new JSONObject(tReplaceResponseMessage);
            } else {
                tResponseMessage = performInquiry(tDefaultAmount, tRF);

                if (tResponseMessage.getString(cChannelCodeKey).equals(c0001)) {
                    new SMSTransactions().logToModuleTransactionTable(tResponseMessage, tRF);
                }
            }

            /*INSERT CHECKSUM*/
            new ValidateTransactions().logToValidateTransactionTable(tResponseMessage, tRF);
        } catch (SystemException e) {
            tResponseMessage = constructErrorMessage(tDefaultAmount, e, cResponseCodeTable, cModuleNameSpace);
        }

        Common.responseMessageToTable(tResponseMessage, tGatewayStan, tRF);

        return new JSONMessage(tResponseMessage.toString());
    }

    private JSONObject performInquiry(final JSONObject pRequestMessage, final String pRF) {
        return performEDUPAYMessage(pRequestMessage, pRF, cInquiryProcessingCode);
    }

    private void validInquiry(final JSONObject pRequestMessage) throws SystemException {
        isValidSession(pRequestMessage, SystemConfig.getNameSpace(cModuleNameSpace).getStringParameter(cLoadedKey));
        final Map<String, String> tJsonRequestMap = jsonToMap(pRequestMessage.toString());
        final List<String> tValidInquiryField = Arrays.asList(cValidInquiryField);
        final boolean isValidInquiryRequest = isValidFieldElements(tValidInquiryField, tJsonRequestMap);

        if (!isValidInquiryRequest) {
            throw new SystemException(ResponseCode.ERROR_INVALID_MESSAGE, "Invalid JSON format message : " + pRequestMessage);
        }

        final List<String> tValidModulParameterField = Arrays.asList(cValidInquiryModulParameter);
        final Map<String, String> tMPFieldMap = jsonToMap(pRequestMessage.getJSONObject(cModuleParamKey).toString());
        final boolean isValidMP = isValidFieldElements(tValidModulParameterField, tMPFieldMap);

        if (!isValidMP) {
            throw new SystemException(ResponseCode.ERROR_INVALID_MESSAGE, "Invalid Modul Parameter JSON format message : " + pRequestMessage);
        }

        final JSONObject tMP = pRequestMessage.getJSONObject(cModuleParamKey);
        final String tAccountNumberSource = tMP.getString(cACCSRCKey);
        final boolean isMultiAccountRegistered = isMultiAccountRegistered(cModuleNameSpace, cLoadedKey, pRequestMessage);

        if (!isMultiAccountRegistered) {
            throw new SystemException(ResponseCode.ERROR_UNREGISTERED_ACCOUNT, "Account not registered[" + tAccountNumberSource + "]");
        }
    }
}

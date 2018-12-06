package id.co.vsi.switcher.bjb.edupay;

import id.co.vsi.common.ISO8583.ISO8583Message;
import static id.co.vsi.switcher.bjb.edupay.Common.cInquiryProcessingCode;
import static id.co.vsi.switcher.bjb.edupay.Common.cPaymentProcessingCode;
import org.json.JSONObject;

public class SimulateResponse {

    public ISO8583Message getIsoResponse(ISO8583Message tRequest, JSONObject tMP) {
        ISO8583Message tResponse = new ISO8583Message(tRequest);

        String tProcessingCode = tRequest.getValueForDataElement(3);
        if (tProcessingCode.equals(cInquiryProcessingCode)) {
            StringBuilder de61 = new StringBuilder();
            de61.append(tMP.getString("BILLER").trim());
            de61.append(BjbEdupayMessageHandler.rightSpacePadding(tMP.getString("BILLID").trim(), 20));
            de61.append(BjbEdupayMessageHandler.rightSpacePadding("INDAH SAFITRI", 30));
            de61.append(BjbEdupayMessageHandler.rightSpacePadding("ILMU KOMUNIKASI", 30));
            de61.append(BjbEdupayMessageHandler.rightSpacePadding("SISTEM INFORMASI", 30));
            de61.append(BjbEdupayMessageHandler.leftZeroPadding("750000", 10));
            de61.append(BjbEdupayMessageHandler.leftZeroPadding("1000000", 10));
            de61.append(BjbEdupayMessageHandler.rightSpacePadding("2014/2015", 9));
            de61.append("1");
            de61.append("PMB");
            de61.append(BjbEdupayMessageHandler.rightSpacePadding("12345678901234567890", 50));
            de61.append(BjbEdupayMessageHandler.rightSpacePadding("Terima kasih telah melakukan pembayaran", 120));

            tResponse.setValueForDataElement(4, BjbEdupayMessageHandler.leftZeroPadding("62500", 12));
            tResponse.setValueForDataElement(39, "00");

            tResponse.setValueForDataElement(61, de61.toString());
        } else if (tProcessingCode.equals(cPaymentProcessingCode)) {
            tResponse.setValueForDataElement(39, "00");
        } else {
            tResponse.setValueForDataElement(39, "05");
        }

        return tResponse;
    }

}

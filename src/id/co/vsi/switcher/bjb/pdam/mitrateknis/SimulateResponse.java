package id.co.vsi.switcher.bjb.pdam.mitrateknis;

import id.co.vsi.common.ISO8583.ISO8583Message;
import static id.co.vsi.switcher.bjb.pdam.mitrateknis.Common.cInquiryProcessingCode;
import static id.co.vsi.switcher.bjb.pdam.mitrateknis.Common.cPaymentProcessingCode;

public class SimulateResponse {

    public ISO8583Message getIsoResponse(ISO8583Message tRequest) {
        ISO8583Message tResponse = new ISO8583Message(tRequest);

        String tProcessingCode = tRequest.getValueForDataElement(3);
        if (tProcessingCode.equals(cInquiryProcessingCode)) {
            StringBuilder de61 = new StringBuilder();
            de61.append(BjbPdamMessageHandler.rightSpacePadding(tResponse.getValueForDataElement(61).trim(), 18));

            StringBuilder de62 = new StringBuilder();
            de62.append(BjbPdamMessageHandler.rightSpacePadding(tResponse.getValueForDataElement(61).trim(), 18));
            de62.append(BjbPdamMessageHandler.rightSpacePadding("ROBI TIBON", 25));
            de62.append(BjbPdamMessageHandler.rightSpacePadding("JL. MOH TOHA RAYA NO. 22", 25));
            de62.append("02");
            de62.append(BjbPdamMessageHandler.rightSpacePadding(tRequest.getValueForDataElement(102).trim(), 20));
            de62.append(BjbPdamMessageHandler.leftZeroPadding("45000", 13));
            de62.append(BjbPdamMessageHandler.leftZeroPadding("2500", 13));
            de62.append(BjbPdamMessageHandler.leftZeroPadding("10000", 13));
            de62.append(BjbPdamMessageHandler.leftZeroPadding("5000", 13));
            de62.append(BjbPdamMessageHandler.leftZeroPadding("0", 13));
            de62.append(BjbPdamMessageHandler.leftZeroPadding("0", 13));
            de62.append(BjbPdamMessageHandler.rightSpacePadding("TEST TRANSAKSI", 128));
            de62.append(BjbPdamMessageHandler.rightSpacePadding("201808", 6));

            tResponse.setValueForDataElement(4, BjbPdamMessageHandler.leftZeroPadding("62500", 12));
            tResponse.setValueForDataElement(39, "00");

            tResponse.setValueForDataElement(61, de61.toString());
            tResponse.setValueForDataElement(62, de62.toString());
        } else if (tProcessingCode.equals(cPaymentProcessingCode)) {
            tResponse.setValueForDataElement(39, "00");
        } else {
            tResponse.setValueForDataElement(39, "05");
        }

        return tResponse;
    }

}

package id.co.vsi.switcher.bjb.edupay;

import static id.co.vsi.common.bp.ibank.BJBJsonHandler.cChannelCodeKey;
import static id.co.vsi.common.bp.ibank.BJBJsonHandler.cLoadedKey;
import static id.co.vsi.common.bp.ibank.BJBJsonHandler.cLogCC;
import static id.co.vsi.common.bp.ibank.BJBJsonHandler.logChannel;
import id.co.vsi.common.bp.ibank.DBUtil;
import id.co.vsi.common.log.LogType;
import id.co.vsi.common.log.SystemLog;
import id.co.vsi.common.settings.SystemConfig;
import static id.co.vsi.switcher.bjb.edupay.Common.cModuleNameSpace;
import id.co.vsi.systemcore.jasoncore.JSONMessage;
import id.co.vsi.systemcore.jasoncore.JSONPlugin;
import id.co.vsi.systemcore.jasoncore.JSONPluginHandler;
import java.util.HashMap;

public class BjbEdupayPlugin extends JSONPlugin {

    public static final String cSysPdamInquiryKey = "SysEdupayInquiryKey";
    public static final String cSysPdamOTPKey = "SysEdupayOTPKey";
    public static final String cSysPdamPaymentKey = "SysEdupayPaymentKey";

    public BjbEdupayPlugin() {
        registerHandler(cSysPdamOTPKey, new BjbEdupayOtpHandler(this));
        registerHandler(cSysPdamInquiryKey, new BjbEdupayInquiryHandler(this));
        registerHandler(cSysPdamPaymentKey, new BjbEdupayPaymentHandler(this));
    }

    public static String getPluginLogFolder() {
        return "bjb-payment-pendidikan-edupay";
    }

    @Override
    public boolean canHandleMessage(JSONMessage pMessage) {
        return pMessage.getString("MC").equals("04008");
    }

    @Override
    public Object clone() {
        BjbEdupayPlugin t_Plugin = new BjbEdupayPlugin();

        t_Plugin.setThreadID(this.getThreadID());

        return t_Plugin;
    }

    @Override
    public Object execute(Object pObject) {
        JSONMessage tRequestMessage = (JSONMessage) pObject;

        logChannel(tRequestMessage.getString(cChannelCodeKey));

        SystemLog.getSingleton().log(this, LogType.STREAM, cLogCC + "DOWNLINE REQUEST : " + tRequestMessage.toString());

        for (JSONPluginHandler tHandler : mHandlerHashMap.values()) {
            if (tHandler.canHandleMessage(tRequestMessage)) {
                tHandler.setRequestingAddress(getRequestAddress());

                JSONMessage tResponseMessage = tHandler.handleMessage(tRequestMessage);

                SystemLog.getSingleton().log(this, LogType.STREAM, cLogCC + "DOWNLINE RESPONSE : " + tResponseMessage.getMessageStream());

                String tMessageResponseCode = String.valueOf(tResponseMessage.getString(mThreadID));

                SystemConfig.getNameSpace(cModuleNameSpace).incrementParameter(tMessageResponseCode);

                return tResponseMessage;
            }
        }

        SystemLog.getSingleton().log(this, LogType.ERROR, cLogCC + "No handler for Message : " + pObject + ", returning original request message.");
        SystemLog.getSingleton().log(this, LogType.STREAM, cLogCC + "DOWNLINE RESPONSE : " + tRequestMessage.getMessageStream());

        return tRequestMessage;
    }

    @Override
    public void initialize() {
        final String tDBConfig = (String) SystemConfig.getNameSpace(cModuleNameSpace).getArrayParameter(cLoadedKey).get(0);

        Common.cValidMCAndIP = DBUtil.isValidIPandMC(tDBConfig);
    }

    @Override
    public HashMap<String, HashMap<String, String>> performSelfTest() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getModuleDescription() {
        return "bjb payment edupay 4.0.0";
    }

    @Override
    public String getPluginId() {
        return cModuleNameSpace;
    }
}

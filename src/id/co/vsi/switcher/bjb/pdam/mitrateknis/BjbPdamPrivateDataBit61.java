package id.co.vsi.switcher.bjb.pdam.mitrateknis;

public enum BjbPdamPrivateDataBit61 {

    BILLID(18);

    private int code;

    private BjbPdamPrivateDataBit61(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    int getLength() {
        return code;
    }

    void setLength(int code) {
        this.code = code;
    }
}

package id.co.vsi.switcher.bjb.pdam.mitrateknis;

public enum BjbPdamPrivateDataBit61 {

    BILLID61(20), //beda dgn bit62
    CUSTOMER_NAME61(35), //beda dgn bit62
    ADDRESS61(35), //beda dgn bit62
    GOLONGAN61(2),
    NO_REKENING61(20),
    BIAYA_AIR61(13),
    BIAYA_METER61(13),
    BIAYA_ANGSURAN61(13),
    DENDA61(13),
    BIAYA_LAIN61(13),
    BIAYA_ADMIN61(13),
    BIAYA_LAIN261(13),
    OTHERS61(12),
    FOOTER61(110),
    TERMINAL_ID61(4),
    TERMINAL_NAME61(20),
    FOOTER261(76);

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

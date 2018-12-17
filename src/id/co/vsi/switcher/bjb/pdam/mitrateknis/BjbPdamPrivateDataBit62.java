package id.co.vsi.switcher.bjb.pdam.mitrateknis;

public enum BjbPdamPrivateDataBit62 {

    BILLID(18),
    CUSTOMER_NAME(25),
    ADDRESS(25),
    GOLONGAN(2),
    NO_REKENING(20),
    BIAYA_AIR(13),
    BIAYA_ADMIN(13),
    BIAYA_METER(13),
    BIAYA_ANGSURAN(13),
    DENDA(13),
    BIAYA_LAIN(13),
    FOOTER(128),
    PERIODE_TAGIHAN(6);

    private int code;

    private BjbPdamPrivateDataBit62(int code) {
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

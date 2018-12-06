package id.co.vsi.switcher.bjb.edupay;

public enum BjbEdupayPrivateDataBit61 {

    KODE_UNIVERSITAS(5),
    NOMOR_TAGIHAN(20),
    NAMA_SISWA(30),
    NAMA_FAKULTAS(30),
    NAMA_JURUSAN(30),
    JUMLAH_TAGIHAN(10),
    JUMLAH_TAGIHAN_TERBAYAR(10),
    TAHUN_AKADEMIK(9),
    JENIS_PEMBAYARAN(1),
    JENIS_TAGIHAN(3),
    PIN_DATA(50),
    INFO_DATA(12);

    private int code;

    private BjbEdupayPrivateDataBit61(int code) {
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

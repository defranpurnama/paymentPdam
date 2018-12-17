package id.co.vsi.switcher.bjb.pdam.mitrateknis;

import static id.co.vsi.common.bp.ibank.BJBJsonHandler.cRCDKey;
import static id.co.vsi.common.bp.ibank.BJBJsonHandler.cRFKey;
import static id.co.vsi.common.bp.ibank.BJBJsonHandler.cTRACE_NUMBERKey;
import static id.co.vsi.common.bp.ibank.BJBJsonHandler.formatNumberToIDR;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.json.JSONObject;

public class ReceiptFormat {

    public static String buildReceiptFormat(final JSONObject pMP, final JSONObject pResponseMessage, final String pFitur) {
        final StringBuilder sb = new StringBuilder();

        final String tTanggalTransaksi = new SimpleDateFormat("dd/MM/yyyy").format(new Date());
        final String tJamTransaksi = new SimpleDateFormat("HH:mm:ss").format(new Date());

        /*Header*/
        sb.append("<p style=\"text-align: center;\"><img src=\"https://ib.bankbjb.co.id/app/images/logo.png\" width=\"130\" height=\"82\" /></p>");
        sb.append("<p style=\"text-align: center;font-size: 90%;\">Terima kasih telah menggunakan layanan Bank bjb</p>");
        sb.append("<p style=\"text-align: center;font-size: 90%;\">Berikut informasi transaksi yang telah Anda lakukan pada layanan ").append(pFitur.split("—")[0].trim()).append("</p>");

        sb.append("<table width=\"80%\" style=\"margin-left: auto; margin-right: auto;\">");
        sb.append("<tbody>");

        sb.append("<tr class=\"light\" style=\"height: 28px;\">");
        sb.append("<td style=\"width: 200px; background-color: #254f7a; text-align: center; height: 28px; font-size: 100%;\" colspan=\"3\"><span style=\"color: #ffffff;\"><strong>").append(pFitur.split("—")[1].trim())
                .append(" - ").append(pMP.getString("BILLER_NAME")).append("</strong></span></td>");
        sb.append("</tr>");

        sb.append("<tr class=\"dark\" style=\"height: 4px;\">");
        sb.append("<td style=\"width: 250px; text-align: center; height: 3.21875px;\" colspan=\"3\"></td>");
        sb.append("</tr>");

        /*CONTENT **EDIT HANYA BAGIAN CONTENT=================================*/
        sb.append("<tr class=\"dark\" style=\"height: 18px;\">");
        sb.append("<td style=\"width: 47%; text-align: right; font-size: 90%;\">Tanggal Transaksi</td>");
        sb.append("<td style=\"width: 3%; text-align: center; font-size: 90%;\">:</td>");
        sb.append("<td style=\"width: 50%; text-align: left; font-size: 90%;\">").append(tTanggalTransaksi).append("</td>");
        sb.append("</tr>");

        sb.append("<tr class=\"dark\" style=\"height: 18px;\">");
        sb.append("<td style=\"width: 47%; text-align: right; font-size: 90%;\">Jam Transaksi</td>");
        sb.append("<td style=\"width: 3%; text-align: center; font-size: 90%;\">:</td>");
        sb.append("<td style=\"width: 50%; text-align: left; font-size: 90%;\">").append(tJamTransaksi).append("</td>");
        sb.append("</tr>");

        sb.append("<tr class=\"dark\" style=\"height: 18px;\">");
        sb.append("<td style=\"width: 47%; text-align: right; font-size: 90%;\">No Resi/Trace</td>");
        sb.append("<td style=\"width: 3%; text-align: center; font-size: 90%;\">:</td>");
        sb.append("<td style=\"width: 50%; text-align: left; font-size: 90%;\">").append(pMP.getString(cTRACE_NUMBERKey)).append("</td>");
        sb.append("</tr>");

        sb.append("<tr class=\"dark\" style=\"height: 18px;\">");
        sb.append("<td style=\"width: 47%; text-align: right; font-size: 90%;\">Status</td>");
        sb.append("<td style=\"width: 3%; text-align: center; font-size: 90%;\">:</td>");
        sb.append("<td style=\"width: 50%; text-align: left; font-size: 90%;\">").append(pResponseMessage.getString(cRCDKey)).append("</td>");
        sb.append("</tr>");

        sb.append("<tr class=\"dark\" style=\"height: 18px;\">");
        sb.append("<td style=\"width: 47%; text-align: right; font-size: 90%;\">Dari Nomor Rekening</td>");
        sb.append("<td style=\"width: 3%; text-align: center; font-size: 90%;\">:</td>");
        sb.append("<td style=\"width: 50%; text-align: left; font-size: 90%;\">").append(pMP.getString("ACCSRC")).append("</td>");
        sb.append("</tr>");

        sb.append("<tr class=\"dark\" style=\"height: 18px;\">");
        sb.append("<td style=\"width: 47%; text-align: right; font-size: 90%;\">Nama PDAM/PAM</td>");
        sb.append("<td style=\"width: 3%; text-align: center; font-size: 90%;\">:</td>");
        sb.append("<td style=\"width: 50%; text-align: left; font-size: 90%;\">").append(pMP.getString("BILLER_NAME")).append("</td>");
        sb.append("</tr>");

        sb.append("<tr class=\"dark\" style=\"height: 18px;\">");
        sb.append("<td style=\"width: 47%; text-align: right; font-size: 90%;\">ID Pelanggan</td>");
        sb.append("<td style=\"width: 3%; text-align: center; font-size: 90%;\">:</td>");
        sb.append("<td style=\"width: 50%; text-align: left; font-size: 90%;\">").append(pMP.getString("BILLID")).append("</td>");
        sb.append("</tr>");

        sb.append("<tr class=\"dark\" style=\"height: 18px;\">");
        sb.append("<td style=\"width: 47%; text-align: right; font-size: 90%;\">Nama Pelanggan</td>");
        sb.append("<td style=\"width: 3%; text-align: center; font-size: 90%;\">:</td>");
        sb.append("<td style=\"width: 50%; text-align: left; font-size: 90%;\">").append(pMP.getString("CUSTOMER_NAME")).append("</td>");
        sb.append("</tr>");

        sb.append("<tr class=\"dark\" style=\"height: 18px;\">");
        sb.append("<td style=\"width: 47%; text-align: right; font-size: 90%;\">Alamat</td>");
        sb.append("<td style=\"width: 3%; text-align: center; font-size: 90%;\">:</td>");
        sb.append("<td style=\"width: 50%; text-align: left; font-size: 90%;\">").append(pMP.getString("ADDRESS")).append("</td>");
        sb.append("</tr>");

        sb.append("<tr class=\"dark\" style=\"height: 18px;\">");
        sb.append("<td style=\"width: 47%; text-align: right; font-size: 90%;\">Bulan Tagihan</td>");
        sb.append("<td style=\"width: 3%; text-align: center; font-size: 90%;\">:</td>");
        sb.append("<td style=\"width: 50%; text-align: left; font-size: 90%;\">").append(pMP.getString("PERIODE_TAGIHAN")).append("</td>");
        sb.append("</tr>");

        sb.append("<tr class=\"dark\" style=\"height: 18px;\">");
        sb.append("<td style=\"width: 47%; text-align: right; font-size: 90%;\">Jumlah Tagihan</td>");
        sb.append("<td style=\"width: 3%; text-align: center; font-size: 90%;\">:</td>");
        sb.append("<td style=\"width: 50%; text-align: left; font-size: 90%;\">").append(formatNumberToIDR(pMP.getString("AMOUNT"))).append("</td>");
        sb.append("</tr>");

        sb.append("<tr class=\"dark\" style=\"height: 18px;\">");
        sb.append("<td style=\"width: 47%; text-align: right; font-size: 90%;\">Biaya Administrasi</td>");
        sb.append("<td style=\"width: 3%; text-align: center; font-size: 90%;\">:</td>");
        sb.append("<td style=\"width: 50%; text-align: left; font-size: 90%;\">").append(formatNumberToIDR(pMP.getString("ADMIN"))).append("</td>");
        sb.append("</tr>");

        sb.append("<tr class=\"dark\" style=\"height: 18px;\">");
        sb.append("<td style=\"width: 47%; text-align: right; font-size: 90%;\">Total Bayar</td>");
        sb.append("<td style=\"width: 3%; text-align: center; font-size: 90%;\">:</td>");
        sb.append("<td style=\"width: 50%; text-align: left; font-size: 90%;\">").append(formatNumberToIDR(pMP.getString("TOTAL_AMOUNT"))).append("</td>");
        sb.append("</tr>");

        /*CONTENT **EDIT HANYA BAGIAN CONTENT=================================*/
        sb.append("<tr class=\"light\" style=\"height: 16px;\">");
        sb.append("<td style=\"width: 200px; height: 16px;\" colspan=\"3\"><hr /></td>");
        sb.append("</tr>");

        sb.append("</tbody>");
        sb.append("</table>");

        /*Footer*/
        sb.append("<p style=\"text-align: center;font-size: 90%;\">").append(pMP.getString("FOOTER")).append("</p>");
        sb.append("<p style=\"text-align: center;font-size: 90%;\">Nomor Referensi : ").append(pResponseMessage.getString(cRFKey)).append("</p>");
        sb.append("<p style=\"text-align: center;font-size: 90%;\">Tanggal Cetak : ").append(tTanggalTransaksi).append("</p>");
        sb.append("<p style=\"text-align: center;font-size: 90%;\"><span>Copyright &copy; 2018 bank bjb, All Rights Reserved</span></p>");

        return sb.toString();
    }
}

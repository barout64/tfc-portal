package co.tz.tfc.ifmis.esb;

import co.tz.tfc.ifmis.esb.dto.TfraAgroDealerProfile;
import co.tz.tfc.ifmis.esb.dto.TfraAgroDealerSalesReport;
import co.tz.tfc.ifmis.esb.dto.TfraFarmerDetails;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Everything the portal needs from TFRA, routed through the Gov-ESB gateway. The browser never
 * calls this directly and never sees a warehouse's TFRA client_id/client_secret — only this
 * server-side layer does, using credentials configured per warehouse ({@code tfraWarehouseId}).
 */
public interface TfraClient {

    Optional<TfraFarmerDetails> getFarmerDetails(String farmerNumber);

    Optional<TfraAgroDealerProfile> verifyAgroDealer(String tfraDealerNumber);

    /**
     * Staff-side step 1 — {@code /tfra/sales/scan-qr-code}: submits the sale to TFRA with the bag QR
     * codes being handed over. Returns TFRA's id for the pending sale, or empty if TFRA rejected it.
     */
    Optional<String> submitSale(String tfraWarehouseId, String farmerNumber, String portalReference, List<String> bagQrCodes);

    /** Staff-side step 2 — {@code /tfra/farmers/request-otp}: TFRA texts the farmer an OTP for this pending sale. */
    String requestOtp(String tfraWarehouseId, String farmerNumber, String tfraSaleId);

    /** Staff-side step 3 — {@code /tfra/sales/confirm}: confirms with the farmer's OTP; returns TFRA's final sale reference. */
    Optional<String> confirmSale(String tfraWarehouseId, String tfraSaleId, String otp);

    TfraAgroDealerSalesReport getAgroDealerSales(String tfraDealerNumber, LocalDate from, LocalDate to);
}

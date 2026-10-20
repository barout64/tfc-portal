package co.tz.tfc.ifmis.esb;

import co.tz.tfc.ifmis.esb.dto.TfraAgroDealerProfile;
import co.tz.tfc.ifmis.esb.dto.TfraAgroDealerSalesReport;
import co.tz.tfc.ifmis.esb.dto.TfraFarmerDetails;

import java.time.LocalDate;
import java.util.Optional;

/**
 * Everything the portal needs from TFRA, routed through the Gov-ESB gateway. The browser never
 * calls this directly and never sees a warehouse's TFRA client_id/client_secret — only this
 * server-side layer does, using credentials configured per warehouse.
 */
public interface TfraClient {

    Optional<TfraFarmerDetails> getFarmerDetails(String farmerNumber);

    Optional<TfraAgroDealerProfile> verifyAgroDealer(String tfraDealerNumber);

    /** Sends an OTP SMS to the phone TFRA has on file for this farmer/dealer; returns an opaque request id. */
    String requestOtp(String tfraNumber);

    /** Staff-side: scans the bag QR codes for a completed sale request against TFRA. */
    boolean scanBagQrCodes(String saleRequestReference, java.util.List<String> bagQrCodes);

    /** Staff-side: confirms the sale with the farmer's OTP, which finalises the TFRA-side stock-out. */
    Optional<String> confirmSale(String saleRequestReference, String otp);

    TfraAgroDealerSalesReport getAgroDealerSales(String tfraDealerNumber, LocalDate from, LocalDate to);
}

package co.tz.tfc.ifmis.esb.dto;

import java.time.LocalDate;
import java.util.List;

/** Mirrors the shape of {@code POST /tfra/agro-dealers/sales}. */
public record TfraAgroDealerSalesReport(
        String tfraDealerNumber,
        LocalDate from,
        LocalDate to,
        int bagsSold,
        int bagsScanned,
        int bagsUnscanned,
        List<Line> lines
) {
    public record Line(String productName, int bagsSold, int bagsScanned) {
    }
}

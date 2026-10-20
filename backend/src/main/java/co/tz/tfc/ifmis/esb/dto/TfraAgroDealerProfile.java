package co.tz.tfc.ifmis.esb.dto;

/** Mirrors the shape of {@code POST /tfra/agro-dealers/verify}. */
public record TfraAgroDealerProfile(
        String tfraDealerNumber,
        String businessName,
        String phoneOnFile,
        boolean active
) {
}

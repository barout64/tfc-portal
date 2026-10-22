package co.tz.tfc.ifmis.dealerorder.dto;

import jakarta.validation.constraints.Size;

public record RespondDealerOrderRequest(@Size(max = 1000) String response) {
}

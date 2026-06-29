package com.planteumaflor.conciliador.pluggy.client;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

final class PluggyDto {

    private PluggyDto() {
    }

    record AuthRequest(String clientId, String clientSecret) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record AuthResponse(
            @JsonProperty("apiKey")
            @JsonAlias({"api_key"})
            String apiKey) {}

    record ConnectTokenRequest(ConnectTokenOptions options) {}

    record ConnectTokenOptions(String clientUserId, boolean avoidDuplicates) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ConnectTokenResponse(
            @JsonProperty("accessToken")
            @JsonAlias({"connectToken", "token"})
            String accessToken) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record AccountsResponse(
            @JsonProperty("results")
            @JsonAlias({"data", "items"})
            List<Account> results) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Account(
            String id,
            String itemId,
            String name,
            String marketingName,
            String number,
            String type,
            String subtype,
            BankData bankData) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record BankData(
            String transferNumber,
            String routingNumber) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record TransactionsResponse(
            @JsonProperty("results")
            @JsonAlias({"data", "items"})
            List<Transaction> results,
            String next) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Transaction(
            String id,
            String accountId,
            OffsetDateTime date,
            BigDecimal amount,
            String type,
            String description,
            String descriptionRaw,
            String providerCode,
            PaymentData paymentData) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record PaymentData(
            Participant payer,
            Participant receiver,
            String referenceNumber) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Participant(
            DocumentNumber documentNumber) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record DocumentNumber(String value) {}
}

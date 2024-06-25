package com.irkut.tc.io.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.security.MessageDigest;

@Setter
@Getter
@NoArgsConstructor
public class ArmResponse {

    private String jsonrpc;
    private String method;
    private Params params;

    @JsonCreator
    public ArmResponse
            (
            @JsonProperty("jsonrpc") String jsonrpc,
            @JsonProperty("method") String method,
            @JsonProperty("params") Params params
            )
    {
        this.jsonrpc = jsonrpc;
        this.method = method;
        this.params = params;
    }

    @Setter
    @Getter
    @NoArgsConstructor
    public static class Params {

        private String id;
        private String status;
        private DirectResults [] directResults;

        @JsonCreator
        public Params
                (
                        @JsonProperty("id") String id,
                        @JsonProperty("status") String status,
                        @JsonProperty("directResult") DirectResults [] directResults
                )
        {
            this.id = id;
            this.status = status;
            this.directResults = directResults;
        }
    }

    @Setter
    @Getter
    @NoArgsConstructor
    public static class DirectResults {

        private Long id;
        private Boolean signValid;
        private Signers [] signers;
        private String out;

        @JsonCreator
        public DirectResults
                (@JsonProperty("id") Long id,
                 @JsonProperty("signValid") Boolean signValid,
                 @JsonProperty("signers") Signers [] signers,
                 @JsonProperty("out") String out
                )
        {
            this.id = id;
            this.signValid = signValid;
            this.signers = signers;
            this.out = out;
        }
    }

    @Setter
    @Getter
    @NoArgsConstructor
    public static class Signers {

        private Boolean isValid;
        private Long signingTime;
        private SignerCertificate signerCertificate;

        @JsonCreator
        public Signers
                (
                @JsonProperty("isValid") Boolean isValid,
                @JsonProperty("signingTime") Long signingTime,
                @JsonProperty("signerCertificate") SignerCertificate signerCertificate
                )
        {
            this.isValid = isValid;
            this.signingTime = signingTime;
            this.signerCertificate = signerCertificate;
        }
    }

    @Setter
    @Getter
    @NoArgsConstructor
    public static class SignerCertificate {

        MessageDigest hash;
        String issuerFriendlyName;
        String issuerName;
        String subjectFriendlyName;
        String subjectName;
        Boolean status;
        Long serial;
        Long notAfter;
        Long notBefore;
        Boolean rootCAMinComSvyaz;
    }
}


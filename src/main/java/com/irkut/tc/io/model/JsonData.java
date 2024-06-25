package com.irkut.tc.io.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@NoArgsConstructor
public class JsonData {

    private String jsonrpc = "2.0";
    @Setter
    private Result result;
    @Setter
    private String id = generateTransactionId();

    private static String generateTransactionId() {
        long timestamp = Instant.now().toEpochMilli();
        String randomPart = UUID.randomUUID().toString().substring(0, 6);
        return timestamp + "-" + randomPart;
    }
}

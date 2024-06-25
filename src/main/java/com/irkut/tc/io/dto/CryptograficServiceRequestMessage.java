package com.irkut.tc.io.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.irkut.tc.io.model.Props;
import com.irkut.tc.io.model.Result;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Setter
@Getter
@NoArgsConstructor
public class CryptograficServiceRequestMessage {

    private final String jsonrpc = "2.0";
    private Result result;
    private final String id = UUID.randomUUID().toString();

    @JsonCreator
    public CryptograficServiceRequestMessage
            (
            @JsonProperty("result") Result result
            )
    {
    this.result = result;
    }

    @Setter
    @Getter
    @NoArgsConstructor
    private static class Result {

        private String [] operation;
        private Props props;

        @JsonCreator
        public Result
                (
                @JsonProperty("operation") String [] operation,
                @JsonProperty("props") Props props
                )
        {
            this.operation = operation;
            this.props = props;
        }


    }

}

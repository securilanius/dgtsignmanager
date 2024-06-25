package com.irkut.tc.io.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.validation.annotation.Validated;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import java.util.List;

@Validated
@Getter
@Setter
@NoArgsConstructor
public class SignRequestData {

    @Valid
    @NotEmpty
    private SessionData sessionData;
    @NotBlank
    private List<String> uids;

    @JsonCreator
    public SignRequestData
            (
                    @JsonProperty("sessionData") SessionData sessionData
            ) {
        this.sessionData = sessionData;
    }

    @Setter
    @Getter
    @NoArgsConstructor
    public static class SessionData {

        private String userID;
        private String password;
        private String role;
        private String discriminator;
        private String group;
        private String previousSessionLoginIP;

    }

}

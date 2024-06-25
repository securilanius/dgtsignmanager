package com.irkut.tc.io.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class Extra {

    private String token;
    private String signType;
    private String signStandard;
    private String signEncoding;
    private String timestampOnSign;
}

package com.irkut.tc.io;

import com.irkut.tc.io.session.Locale;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@ConfigurationProperties(prefix = "tc.session")
@Getter
@Setter
public class ConnectionProperties {

    private String host;
    private String login;
    private String password;
    private String group;
    private Locale locale = Locale.en_US;

}

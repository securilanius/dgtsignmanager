package com.irkut.tc.io.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class FileDetails {
    private String name;
    private String url;
    private String id;
    private String urlDetached;
}

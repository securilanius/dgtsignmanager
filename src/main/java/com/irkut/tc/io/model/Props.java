package com.irkut.tc.io.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
@NoArgsConstructor
public class Props {

    private String headerText;
    private String license;
    private List<FileDetails> files;
    private Extra extra;
    private String uploader;
}

package com.irkut.tc.io.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashMap;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class DtoData {

    public String uid;
    public HashMap<String,String> properties = new HashMap<>();

}

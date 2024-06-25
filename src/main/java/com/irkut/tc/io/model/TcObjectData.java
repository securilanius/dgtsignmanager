package com.irkut.tc.io.model;

import com.teamcenter.soa.client.model.ModelObject;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@NoArgsConstructor
public class TcObjectData {


    public ModelObject revision;

    public ArrayList<String> fileList = new ArrayList<String>();

    //map filePath - fileName
    public Map<String,String> fileListMap = new HashMap<String,String>();

}

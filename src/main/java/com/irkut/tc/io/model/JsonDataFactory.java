package com.irkut.tc.io.model;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class JsonDataFactory {

    public static JsonData createJsonData(String destDir,ArrayList<TcObjectData> tcDataList) {
        JsonData jsonData = new JsonData();
        Result result = new Result();
        List<String> value = new ArrayList<>();
        value.add("SIGN");
        result.setOperation(value);
        Extra extra = new Extra();

        Props props = new Props();
        props.setHeaderText("");
        props.setLicense("");
        props.setUploader("file:///C:/Users/infodba/.Trusted/CryptoARM/Documents/");
        //props.setUploader("http://localhost:8080/signing_test/direct-result");

        extra.setToken("9c7101f7");
        extra.setSignType("1");
        extra.setSignStandard("0");
        extra.setSignEncoding("0");
        extra.setTimestampOnSign("false");

        List<FileDetails> files = new ArrayList<>();
        //add attr.json
        FileDetails file = new FileDetails();
        file.setName("attr.json");
        file.setUrl("file:///C:/Users/infodba/.Trusted/CryptoARM/Documents/attr.json");
        file.setId("" + (0));
        file.setUrlDetached("");
        files.add(file);

        for(int i = 0; i < tcDataList.size(); i++){

            TcObjectData tcObjData = tcDataList.get(i);
            for(int j = 0;j<tcObjData.fileList.size(); j++){

                    FileDetails file_2 = new FileDetails();
                    file_2.setName(new File(tcObjData.fileList.get(j)).getName());
                    // старый вариант с destDir
                   // file_2.setUrl(destDir + tcObjData.fileList.get(j));
                    file_2.setUrl(tcObjData.fileList.get(j));
                    file_2.setId("" + (j + 1));
                    file_2.setUrlDetached("");
                    files.add(file_2);
                }
            }

        /*
        for (int i = 0; i<tcDataList.size(); i++) {
            FileDetails file_2 = new FileDetails();
            file_2.setName(fileList.get(i));
            file_2.setUrl(destDir + fileList.get(i));
            file_2.setId("" + (i + 1));
            file_2.setUrlDetached("");
            files.add(file_2);
        }
        */


        props.setFiles(files);
        props.setExtra(extra);
        result.setProps(props);
        jsonData.setResult(result);
        return jsonData;
    }
}

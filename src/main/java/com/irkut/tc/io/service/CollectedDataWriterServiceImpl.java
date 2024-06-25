package com.irkut.tc.io.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.irkut.tc.io.model.JsonData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;

@Service
public class CollectedDataWriterServiceImpl implements CollectedDataWriterService{
    private static final Logger logger = LoggerFactory.getLogger(CollectedDataWriterServiceImpl.class);
    private final String folderPath;

    public CollectedDataWriterServiceImpl(@Value("${CRARM_DOCUMENTS}") String folderPath) {
        this.folderPath = folderPath;
    }

    public void createFile(JsonData jsonData) {

        try {
            logger.info("Старт метода saveJsonAsFile:");
            logger.info("folderPath: " + folderPath);
            String filePath = folderPath + File.separator + "SignTest01.json";
            logger.info("filePath: " + filePath);
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.writeValue(new File(filePath), jsonData);
        } catch (IOException e) {
            logger.error("Error saving JSON file" + e.getMessage());
        }

    }
}

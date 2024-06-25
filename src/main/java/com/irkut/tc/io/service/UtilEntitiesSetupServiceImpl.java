package com.irkut.tc.io.service;

import com.irkut.tc.io.model.TcObjectData;
import com.irkut.tc.io.service.io.FileMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class UtilEntitiesSetupServiceImpl implements UtilEntitiesSetupService{

    private static final Logger logger = LoggerFactory.getLogger(UtilEntitiesSetupServiceImpl.class);

    @Override
    public List<?> clearList(List<?> list) {
        if (!list.isEmpty()) {
            list.clear();
            logger.info(list + "cleared successfully");
        }
        return list;
    }
    @Override
    public FileMonitor stopDirectoryMonitor(FileMonitor directoryMonitor) {
        if(directoryMonitor != null) {
            try {
                directoryMonitor.stop();
                logger.info("Stopped monitor successfully");
            } catch (Exception e) {
                logger.error("Error stopping monitor!");
                throw new RuntimeException(e);
            }
        }
        return directoryMonitor;
    }

}

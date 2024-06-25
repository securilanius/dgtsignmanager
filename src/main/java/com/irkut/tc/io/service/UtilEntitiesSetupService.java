package com.irkut.tc.io.service;

import com.irkut.tc.io.model.TcObjectData;
import com.irkut.tc.io.service.io.FileMonitor;

import java.util.ArrayList;
import java.util.List;

public interface UtilEntitiesSetupService {

    public List<?> clearList(List<?> list);

    public FileMonitor stopDirectoryMonitor(FileMonitor directoryMonitor);

}

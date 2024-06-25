package com.irkut.tc.io.service;

import com.irkut.tc.io.service.io.FileMonitor;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class UtilEntitiesSetupServiceImplTest {
    UtilEntitiesSetupServiceImpl utilEntitiesSetupService;

    @Test
    void testClearList() {
        List<String> list = new ArrayList<>();
        list.add("element1");
        list.add("element2");

        utilEntitiesSetupService.clearList(list);

        assertTrue(list.isEmpty());
    }

    @Test
    void stopDirectoryMonitor() {
        FileMonitor fileMonitor = new FileMonitor(1000);

        utilEntitiesSetupService.stopDirectoryMonitor(fileMonitor);


    }



 }

package com.irkut.tc.io.service.io.watcher;

import java.io.File;
import java.util.EventObject;

public class FileEvent extends EventObject {
    public FileEvent(File file) {
        super(file);
    }
    public File getFile() {
        return (File) getSource();
    }
}

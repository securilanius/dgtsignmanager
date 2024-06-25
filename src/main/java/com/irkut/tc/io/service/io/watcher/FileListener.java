package com.irkut.tc.io.service.io.watcher;

import com.irkut.tc.io.service.io.watcher.FileEvent;

import java.util.EventListener;

public interface FileListener extends EventListener {
    public void onCreated(FileEvent event);
    public void onModified(FileEvent event);
    public void onDeleted(FileEvent event);
}

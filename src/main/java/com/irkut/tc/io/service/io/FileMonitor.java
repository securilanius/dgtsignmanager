package com.irkut.tc.io.service.io;

import org.apache.commons.io.monitor.FileAlterationListener;
import org.apache.commons.io.monitor.FileAlterationMonitor;
import org.apache.commons.io.monitor.FileAlterationObserver;
import java.io.File;

/**
 * Класс "FileMonitor" предоставляет функциональность для мониторинга изменений в файлах.
 * Он использует библиотеку Apache Commons IO для отслеживания изменений и реагирования на них.
 */
public class FileMonitor {
    private final FileAlterationMonitor monitor;

    /**
     * Конструктор класса FileMonitor создает экземпляр монитора изменений в файлах с указанным интервалом.
     * @param interval Интервал в милисекундах.
     */
    public FileMonitor(long interval) {
        monitor = new FileAlterationMonitor(interval);
    }

    /**
     * Добавление слушателя в файл.
     *
     * @param path     file path
     * @param listener file listener
     */
    public void monitor(String path, FileAlterationListener listener) {
        FileAlterationObserver observer = new FileAlterationObserver(new File(path));
        monitor.addObserver(observer);
        observer.addListener(listener);
    }

    public void stop() throws Exception {
        monitor.stop();
        System.out.println("File monitor stop");
    }

    public void start() throws Exception {
        monitor.start();
        System.out.println("File monitor start");
    }

}
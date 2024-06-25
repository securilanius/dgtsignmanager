package com.irkut.tc.io.service.io;

import org.apache.commons.io.monitor.FileAlterationMonitor;

import java.util.concurrent.atomic.AtomicBoolean;

public class DirectoryMonitor implements Runnable{

    private FileAlterationMonitor monitor;

    private final AtomicBoolean signResult = new AtomicBoolean();


    public DirectoryMonitor(FileAlterationMonitor monitor) {
        this.monitor = monitor;
    }

    @Override
    public void run() {
        try {
            this.setSignState(false);
            System.out.println("DirectoryMonitor starting");
            Thread.currentThread().setName("Monitor thread");
            System.out.println("current thread: " + Thread.currentThread().getName());

            monitor.start();

            synchronized (FileListener.lock) {
                FileListener.lock.wait();
            }

            System.out.println("FileListener notify get. Now stopping monitor");
            monitor.stop();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setSignState(boolean state){
        this.signResult.set(state);
    }




}

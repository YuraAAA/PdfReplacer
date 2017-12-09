package controllers;

import play.Logger;

import java.io.File;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class CleanUpWorkhorse {

    public static final String TAG = CleanUpWorkhorse.class.getSimpleName();

    private static final ScheduledExecutorService EXECUTOR = Executors.newScheduledThreadPool(10);

    public CleanUpWorkhorse(String dirName) {
        Logger.info(TAG + "\nRunning for " + dirName);
        EXECUTOR.schedule(new Clean(dirName), 2, TimeUnit.MINUTES);
    }

    private class Clean implements Runnable {


        private String directoryName;

        public Clean(String directoryName) {
            this.directoryName = directoryName;
        }


        @Override
        public void run() {
            Logger.info(TAG + "\nDirectory " + directoryName + " should be removed");
            File file = new File(directoryName);
            boolean delete = false;
            if (file.exists()) {
                File[] listFiles = file.listFiles();
                if (listFiles != null && listFiles.length > 0) {
                    for (File listFile : listFiles) {
                        if (listFile != null && listFile.exists()) {
                            listFile.delete();
                        }
                    }
                }
                delete = file.delete();
            }
            Logger.info(TAG + " \n Directory " + directoryName + " was " + (delete ? "successfully" : "failed") + " removed");
        }
    }


}

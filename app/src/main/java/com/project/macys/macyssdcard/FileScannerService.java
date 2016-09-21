package com.project.macys.macyssdcard;


import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;


public class FileScannerService extends Service {


    static final String TAG = "FileScannerService";
    static final String PROGRESS_UPDATE = "PROGRESS_UPDATE";
    static final String SDCARD_ROOT_DIR_PATH = Environment.getExternalStorageDirectory().getAbsolutePath();

    ArrayList<File> fileList;

    ArrayList<String> largestFileList = new ArrayList<String>();
    ArrayList<String> frequencyList = new ArrayList<String>();
    long average_file_size = 0;

    FileScannerAsyncTask fileScannerAsyncTask;
    static boolean isServiceRunning = false;
    boolean serviceStopped;

    Map<String, Integer> frequencyTypeMap;

    @Override
    public void onCreate() {
        super.onCreate();
        serviceStopped = false;
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        showNotification();

        isServiceRunning = true;
        fileScannerAsyncTask = new FileScannerAsyncTask();
        fileScannerAsyncTask.execute();

        return START_STICKY;
    }


    @Override
    public void onDestroy() {

        super.onDestroy();
        isServiceRunning = false;
        fileScannerAsyncTask.cancel(true);
        serviceStopped = true;
        Log.i(TAG, "Stopping service");
    }


    class FileScannerAsyncTask extends AsyncTask<Void,Void,Void>{

        @Override
        protected Void doInBackground(Void... voids) {
            fileList = getFileListRecursively(new File(SDCARD_ROOT_DIR_PATH));
            if(!fileList.isEmpty()) {

                if(isCancelled())
                    return null;

                Collections.sort(fileList, fileComparator);
                Map<String, Integer> map = getFileTypeFrequency(fileList);

                if(!map.isEmpty())
                    frequencyTypeMap = sortByFrequency(map);

                populateDisplayList();
            }

            if(isCancelled())
                return null;

            sendUpdate();

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            stopSelf();
        }
    }

    public void populateDisplayList(){

        int i;

        // Copy the 10 largest files
        for (i = 0; i < 10; i++)
            largestFileList.add(fileList.get(i).getName() + ";" + "File size: "+formatFileSize(fileList.get(i).length()));


        i = 0;
        // Copy high frequency type;
        if(!frequencyTypeMap.isEmpty()) {
            Iterator iterator = frequencyTypeMap.entrySet().iterator();
            while (iterator.hasNext() && i < 5) {
                Map.Entry entry = (Map.Entry) iterator.next();
                frequencyList.add("File type: " + entry.getKey() + ";" + "Number of occurences: " + String.valueOf(entry.getValue()));
                i++;
            }
        }

        // Copy average file size
        average_file_size = fileList.get(0).length();

        for (i = 1; i < fileList.size(); i++) {
            average_file_size = (average_file_size * (i) + fileList.get(i).length()) / (i + 1);
        }

    }

    /**
     * Function used to sort the frequency map of file extensions
     *
     * @param frequencyMap
     * @return
     */
    private static Map<String, Integer> sortByFrequency(Map<String, Integer> frequencyMap) {
        List<Map.Entry<String, Integer>> list = new LinkedList<Map.Entry<String, Integer>>(frequencyMap.entrySet());
        Collections.sort(list, new Comparator<Map.Entry<String, Integer>>() {
            public int compare(Map.Entry<String, Integer> entry1,
                               Map.Entry<String, Integer> entry2) {
                return (entry2.getValue()).compareTo(entry1.getValue());
            }
        });
        Map<String, Integer> sortedFrequencyMap = new LinkedHashMap<String, Integer>();
        for (Map.Entry<String, Integer> entry : list) {
            sortedFrequencyMap.put(entry.getKey(), entry.getValue());
        }
        return sortedFrequencyMap;
    }

    /**
     * Function used to compute the frequency of each file type
     * @param list
     * @return
     */
    public Map<String, Integer> getFileTypeFrequency(List<File> list) {
        Map<String, Integer> extensionMap = new HashMap<String, Integer>();
        for (File file : list) {
            String ext = getFileExtension(file.getName());
            if (!ext.equals("")) {
                if (!extensionMap.containsKey(ext)) {
                    extensionMap.put(ext, 1);
                } else {
                    Integer cnt = extensionMap.get(ext);
                    extensionMap.put(ext, cnt + 1);
                }
            }
        }
        return extensionMap;
    }

    public String getFileExtension(String fileName) {
        if (fileName == null)
            return "";
        return fileName.substring(fileName.lastIndexOf(".") + 1, fileName.length());
    }

    /**
     * Function used to convert size into formatted display
     * @param sizeInBytes
     * @return
     */
    public String formatFileSize(long sizeInBytes) {
        DecimalFormat formattedSize = new DecimalFormat();
        formattedSize.setMaximumFractionDigits(2);
        try {
            if (sizeInBytes < Constants.SIZE_KB) {
                return formattedSize.format(sizeInBytes) + " B";
            } else if (sizeInBytes < Constants.SIZE_MB) {
                return formattedSize.format(sizeInBytes / Constants.SIZE_KB) + " KB";
            } else if (sizeInBytes < Constants.SIZE_GB) {
                return formattedSize.format(sizeInBytes / Constants.SIZE_MB) + " MB";
            } else {
                return formattedSize.format(sizeInBytes / Constants.SIZE_GB) + " GB";
            }
        } catch (Exception e) {
            return sizeInBytes + " B";
        }
    }

    /**
     * Function to return the file  list from root directory recursively
     *
     * @param parentDir
     * @return
     */
    private ArrayList<File> getFileListRecursively(File parentDir) {
        ArrayList<File> fileList = new ArrayList<File>();
        File[] files = parentDir.listFiles();

        if(files != null) {

            for (File file : files) {
                Log.i(TAG, "File path: "+file.getAbsolutePath());
                if(serviceStopped)
                    break;
                if (file.isDirectory()) {
                    fileList.addAll(getFileListRecursively(file));
                } else {
                    fileList.add(file);
                }
            }
        }
        return fileList;
    }

    Comparator<File> fileComparator = new Comparator<File>() {
        @Override
        public int compare(File file1, File file2) {
            return file1.length() > file2.length() ? -1 : file1.length() < file2.length() ? 1 : 0;
        }
    };

    public void sendUpdate() {

        Intent intent = new Intent();
        intent.setAction(PROGRESS_UPDATE);
        intent.putStringArrayListExtra("LARGEST_FILE_LIST", largestFileList);
        intent.putStringArrayListExtra("FREQUENCY_LIST", frequencyList);
        intent.putExtra("AVG_SIZE", formatFileSize(average_file_size));

        sendBroadcast(intent);
    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    private void showNotification() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 1,
                notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Notification notification = new NotificationCompat.Builder(this)
                .setContentTitle("Macys")
                .setTicker("File Scan Started")
                .setContentText("Scanning SD card...")
                .setContentIntent(pendingIntent)
                .setOngoing(true).build();
        startForeground(1, notification);
    }
}


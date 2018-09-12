package io.github.leetsong.seh;

import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

public class FetcherConfig {

    private static final int DEFAULT_NR_WORKER = 16;

    private final Properties mProperties = new Properties();
    private String mFileName;

    public FetcherConfig(String fileName) {
        this.mFileName = fileName;
        reset();
    }

    public static String convert2ConfigFileName(String[] tags) {
        try {
            // convert tags to legal path characters
            return URLEncoder.encode(
                    String.format("fetcher_%s.seh", String.join("_", tags)),
                    StandardCharsets.UTF_8.toString());
        } catch (Exception e) {
            // ignore, never reach here
            e.printStackTrace();
            System.exit(1);
            return null;
        }
    }

    synchronized public void load()
            throws FileNotFoundException, IOException {
        InputStream inputStream = new FileInputStream(mFileName);
        mProperties.load(inputStream);
    }

    synchronized public void store()
            throws FileNotFoundException, IOException {
        OutputStream outputStream = new FileOutputStream(mFileName);
        mProperties.store(outputStream, "StackOverflow Fetcher configurations");
    }
    
    public void reset() {
        synchronized (mProperties) {
            mProperties.clear();
            // set global
            mProperties.setProperty(property$Global_NrWorker(), Integer.toString(DEFAULT_NR_WORKER));
            // set worker
            for (int i = workerBegin(); i < workerEnd(); i += workerStep()) {
                mProperties.setProperty(property$Worker_Id(i), Integer.toString(i));
                mProperties.setProperty(property$Worker_Page(i), Integer.toString(i));
                mProperties.setProperty(property$Worker_Step(i), Integer.toString(DEFAULT_NR_WORKER));
                mProperties.setProperty(property$Worker_Appender_Type(i), CsvAppender.APPENDER_TYPE);
                mProperties.setProperty(property$Worker_Appender_Path(i), String.format("worker[%d]_appender.csv", i));
            }
            // set result
            mProperties.setProperty(property$Result_NrPage(), Integer.toString(0));
            mProperties.setProperty(property$Result_NrItem(), Integer.toString(0));
        }
    }

    // workers should be iterated by workerBegin, workerStep and workerEnd
    public int workerBegin() { return 1; }

    public int workerStep() { return 1; }

    public int workerEnd() { return DEFAULT_NR_WORKER + 1; }

    // property getters
    public int getNrWorker() {
        return Integer.parseInt(mProperties.getProperty(property$Global_NrWorker()));
    }

    public int getWorkerId(int i) {
        return Integer.parseInt(mProperties.getProperty(property$Worker_Id(i)));
    }

    public int getWorkerPage(int id) {
        return Integer.parseInt(mProperties.getProperty(property$Worker_Page(id)));
    }

    public int getWorkerStep(int id) {
        return Integer.parseInt(mProperties.getProperty(property$Worker_Step(id)));
    }

    public String getWorkerAppenderPath(int id) {
        return mProperties.getProperty(property$Worker_Appender_Path(id));
    }

    public String getWorkerAppenderType(int id) {
        return mProperties.getProperty(property$Worker_Appender_Type(id));
    }

    public int getResultNrPage() {
        return Integer.parseInt(mProperties.getProperty(property$Result_NrPage()));
    }

    public int getResultNrItem() {
        return Integer.parseInt(mProperties.getProperty(property$Result_NrItem()));
    }

    public String getFileName() {
        return mFileName;
    }

    // property setters
    public void setNrWorker(int nrWorker) {
        synchronized (mProperties) {
            mProperties.setProperty(property$Global_NrWorker(), Integer.toString(nrWorker));
        }
    }

    public void setWorkerId(int id) {
        synchronized (mProperties) {
            mProperties.setProperty(property$Worker_Id(id), Integer.toString(id));
        }
    }

    public void setWorkerPage(int id, int page) {
        synchronized (mProperties) {
            mProperties.setProperty(property$Worker_Page(id), Integer.toString(page));
        }
    }

    public void setWorkerStep(int id, int step) {
        synchronized (mProperties) {
            mProperties.setProperty(property$Worker_Step(id), Integer.toString(step));
        }
    }

    public void setWorkerAppenderPath(int id, String name) {
        synchronized (mProperties) {
            mProperties.setProperty(property$Worker_Appender_Path(id), name);
        }
    }

    public void setWorkerAppenderType(int id, String type) {
        synchronized (mProperties) {
            mProperties.setProperty(property$Worker_Appender_Type(id), type);
        }
    }

    public void setResultNrPage(int nrPage) {
        synchronized (mProperties) {
            mProperties.setProperty(property$Result_NrPage(), Integer.toString(nrPage));
        }
    }

    public void setResultNrItem(int nrItem) {
        synchronized (mProperties) {
            mProperties.setProperty(property$Result_NrItem(), Integer.toString(nrItem));
        }
    }

    public void setFileName(String fileName) {
        this.mFileName = fileName;
    }
    
    // property names
    private String property$Global_NrWorker() {
        return "global.nr_worker";
    }

    private String property$Worker_Id(int id) {
        return String.format("worker[%d].id", id);
    }

    private String property$Worker_Page(int id) {
        return String.format("worker[%d].page", id);
    }

    private String property$Worker_Step(int id) {
        return String.format("worker[%d].step", id);
    }

    private String property$Worker_Appender_Type(int id) {
        return String.format("worker[%d].appender.type", id);
    }

    private String property$Worker_Appender_Path(int id) {
        return String.format("worker[%d].appender.path", id);
    }

    private String property$Result_NrPage() {
        return "result.nr_page";
    }

    private String property$Result_NrItem() {
        return "result.nr_item";
    }
}

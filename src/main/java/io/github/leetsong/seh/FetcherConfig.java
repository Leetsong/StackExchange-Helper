package io.github.leetsong.seh;

import java.io.*;
import java.util.Properties;

public class FetcherConfig {

    private static final int DEFAULT_NR_WORKER = 16;

    private final Properties mProperties = new Properties();
    private String mFileName;

    public FetcherConfig(String fileName) {
        this.mFileName = fileName;
        reset();
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
            mProperties.setProperty(propertyGlobalNrWorker(), Integer.toString(DEFAULT_NR_WORKER));
            // set worker
            for (int i = workerBegin(); i < workerEnd(); i += workerStep()) {
                mProperties.setProperty(propertyWorkerId(i), Integer.toString(i));
                mProperties.setProperty(propertyWorkerPage(i), Integer.toString(i));
                mProperties.setProperty(propertyWorkerStep(i), Integer.toString(DEFAULT_NR_WORKER));
                mProperties.setProperty(propertyWorkerAppenderType(i), CsvAppender.APPENDER_TYPE);
                mProperties.setProperty(propertyWorkerAppenderPath(i), String.format("worker[%d]_appender.csv", i));
            }
            // set result
            mProperties.setProperty(propertyResultNrPage(), Integer.toString(0));
            mProperties.setProperty(propertyResultNrItem(), Integer.toString(0));
        }
    }

    // workers should be iterated by workerBegin, workerStep and workerEnd
    public int workerBegin() { return 1; }

    public int workerStep() { return 1; }

    public int workerEnd() { return DEFAULT_NR_WORKER + 1; }

    // property getters
    public int getNrWorker() {
        return Integer.parseInt(mProperties.getProperty(propertyGlobalNrWorker()));
    }

    public int getWorkerId(int i) {
        return Integer.parseInt(mProperties.getProperty(propertyWorkerId(i)));
    }

    public int getWorkerPage(int id) {
        return Integer.parseInt(mProperties.getProperty(propertyWorkerPage(id)));
    }

    public int getWorkerStep(int id) {
        return Integer.parseInt(mProperties.getProperty(propertyWorkerStep(id)));
    }

    public String getWorkerAppenderPath(int id) {
        return mProperties.getProperty(propertyWorkerAppenderPath(id));
    }

    public String getWorkerAppenderType(int id) {
        return mProperties.getProperty(propertyWorkerAppenderType(id));
    }

    public int getResultNrPage() {
        return Integer.parseInt(mProperties.getProperty(propertyResultNrPage()));
    }

    public int getResultNrItem() {
        return Integer.parseInt(mProperties.getProperty(propertyResultNrItem()));
    }

    public String getFileName() {
        return mFileName;
    }

    // property setters
    public void setNrWorker(int nrWorker) {
        synchronized (mProperties) {
            mProperties.setProperty(propertyGlobalNrWorker(), Integer.toString(nrWorker));
        }
    }

    public void setWorkerId(int id) {
        synchronized (mProperties) {
            mProperties.setProperty(propertyWorkerId(id), Integer.toString(id));
        }
    }

    public void setWorkerPage(int id, int page) {
        synchronized (mProperties) {
            mProperties.setProperty(propertyWorkerPage(id), Integer.toString(page));
        }
    }

    public void setWorkerStep(int id, int step) {
        synchronized (mProperties) {
            mProperties.setProperty(propertyWorkerStep(id), Integer.toString(step));
        }
    }

    public void setWorkerAppenderPath(int id, String name) {
        synchronized (mProperties) {
            mProperties.setProperty(propertyWorkerAppenderPath(id), name);
        }
    }

    public void setWorkerAppenderType(int id, String type) {
        synchronized (mProperties) {
            mProperties.setProperty(propertyWorkerAppenderType(id), type);
        }
    }

    public void setResultNrPage(int nrPage) {
        synchronized (mProperties) {
            mProperties.setProperty(propertyResultNrPage(), Integer.toString(nrPage));
        }
    }

    public void setResultNrItem(int nrItem) {
        synchronized (mProperties) {
            mProperties.setProperty(propertyResultNrItem(), Integer.toString(nrItem));
        }
    }

    public void setFileName(String fileName) {
        this.mFileName = fileName;
    }
    
    // property names
    private String propertyGlobalNrWorker() {
        return "global.nr_worker";
    }

    private String propertyWorkerId(int id) {
        return String.format("worker[%d].id", id);
    }

    private String propertyWorkerPage(int id) {
        return String.format("worker[%d].page", id);
    }

    private String propertyWorkerStep(int id) {
        return String.format("worker[%d].step", id);
    }

    private String propertyWorkerAppenderType(int id) {
        return String.format("worker[%d].appender.type", id);
    }

    private String propertyWorkerAppenderPath(int id) {
        return String.format("worker[%d].appender.path", id);
    }

    private String propertyResultNrPage() {
        return "result.nr_page";
    }

    private String propertyResultNrItem() {
        return "result.nr_item";
    }
}

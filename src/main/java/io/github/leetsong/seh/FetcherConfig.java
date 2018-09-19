package io.github.leetsong.seh;

import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

public class FetcherConfig {

    private static final int DEFAULT_NR_WORKER = 16;
    private static final int DEFAULT_PAGE_SIZE = 30;

    private final Properties mProperties = new Properties();
    private String mFileName;

    public FetcherConfig(String fileName) {
        this.mFileName = fileName;
        reset();
    }

    public static String convert2ConfigFileName(String[] x) {
        try {
            // convert tags to legal path characters
            return URLEncoder.encode(
                    String.format("fetcher_%s.seh", String.join("_", x)),
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
        mProperties.store(outputStream, "StackExchange-Helper configurations");
    }
    
    public void reset() {
        synchronized (mProperties) {
            mProperties.clear();
            // set global
            mProperties.setProperty(property$Global_NrWorker(), Integer.toString(DEFAULT_NR_WORKER));
            // set ApiFetcher worker
            for (int i = apiFetcherWorkerBegin(); i < apiFetcherWorkerEnd(); i += apiFetcherWorkerStep()) {
                mProperties.setProperty(property$ApiFetcher_Worker_Id(i), Integer.toString(i));
                mProperties.setProperty(property$ApiFetcher_Worker_Page(i), Integer.toString(i));
                mProperties.setProperty(property$ApiFetcher_Worker_Step(i), Integer.toString(DEFAULT_NR_WORKER));
                mProperties.setProperty(property$ApiFetcher_Worker_Appender_Type(i), CsvAppender.APPENDER_TYPE);
                mProperties.setProperty(property$ApiFetcher_Worker_Appender_Path(i),
                        String.format("apifetcher_worker[%d]_appender.csv", i));
            }
            // set ApiFetcher result
            mProperties.setProperty(property$ApiFetcher_Result_NrPage(), Integer.toString(0));
            mProperties.setProperty(property$ApiFetcher_Result_NrItem(), Integer.toString(0));
            // set GooFetcher appender worker
            mProperties.setProperty(property$GooFetcher_AppenderWorker_Appender_Type(), CsvAppender.APPENDER_TYPE);
            mProperties.setProperty(property$GooFetcher_AppenderWorker_Appender_Path(),
                    "goofetcher_worker_appender.csv");
            // set GooFetcher result
            mProperties.setProperty(property$GooFetcher_Result_Start(), Integer.toString(0));
            mProperties.setProperty(property$GooFetcher_Result_PageSize(), Integer.toString(DEFAULT_PAGE_SIZE));
        }
    }

    // workers should be iterated by apiFetcherWorkerBegin, apiFetcherWorkerStep and apiFetcherWorkerEnd
    public int apiFetcherWorkerBegin() { return 1; }

    public int apiFetcherWorkerStep() { return 1; }

    public int apiFetcherWorkerEnd() { return DEFAULT_NR_WORKER + 1; }

    // property getters
    public int getNrWorker() {
        return Integer.parseInt(mProperties.getProperty(property$Global_NrWorker()));
    }

    public int getApiFetcherWorkerId(int i) {
        return Integer.parseInt(mProperties.getProperty(property$ApiFetcher_Worker_Id(i)));
    }

    public int getApiFetcherWorkerPage(int id) {
        return Integer.parseInt(mProperties.getProperty(property$ApiFetcher_Worker_Page(id)));
    }

    public int getApiFetcherWorkerStep(int id) {
        return Integer.parseInt(mProperties.getProperty(property$ApiFetcher_Worker_Step(id)));
    }

    public String getApiFetcherWorkerAppenderPath(int id) {
        return mProperties.getProperty(property$ApiFetcher_Worker_Appender_Path(id));
    }

    public String getApiFetcherWorkerAppenderType(int id) {
        return mProperties.getProperty(property$ApiFetcher_Worker_Appender_Type(id));
    }

    public int getApiFetcherResultNrPage() {
        return Integer.parseInt(mProperties.getProperty(property$ApiFetcher_Result_NrPage()));
    }

    public int getApiFetcherResultNrItem() {
        return Integer.parseInt(mProperties.getProperty(property$ApiFetcher_Result_NrItem()));
    }

    public String getGooFetcherAppenderWorkerAppenderPath() {
        return mProperties.getProperty(property$GooFetcher_AppenderWorker_Appender_Path());
    }

    public String getGooFetcherAppenderWorkerAppenderType() {
        return mProperties.getProperty(property$GooFetcher_AppenderWorker_Appender_Type());
    }

    public int getGooFetcherResultStart() {
        return Integer.parseInt(mProperties.getProperty(property$GooFetcher_Result_Start()));
    }

    public int getGooFetcherResultPageSize() {
        return Integer.parseInt(mProperties.getProperty(property$GooFetcher_Result_PageSize()));
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

    public void setApiFetcherWorkerId(int id) {
        synchronized (mProperties) {
            mProperties.setProperty(property$ApiFetcher_Worker_Id(id), Integer.toString(id));
        }
    }

    public void setApiFetcherWorkerPage(int id, int page) {
        synchronized (mProperties) {
            mProperties.setProperty(property$ApiFetcher_Worker_Page(id), Integer.toString(page));
        }
    }

    public void setApiFetcherWorkerStep(int id, int step) {
        synchronized (mProperties) {
            mProperties.setProperty(property$ApiFetcher_Worker_Step(id), Integer.toString(step));
        }
    }

    public void setApiFetcherWorkerAppenderPath(int id, String name) {
        synchronized (mProperties) {
            mProperties.setProperty(property$ApiFetcher_Worker_Appender_Path(id), name);
        }
    }

    public void setApiFetcherWorkerAppenderType(int id, String type) {
        synchronized (mProperties) {
            mProperties.setProperty(property$ApiFetcher_Worker_Appender_Type(id), type);
        }
    }

    public void setApiFetcherResultNrPage(int nrPage) {
        synchronized (mProperties) {
            mProperties.setProperty(property$ApiFetcher_Result_NrPage(), Integer.toString(nrPage));
        }
    }

    public void setApiFetcherResultNrItem(int nrItem) {
        synchronized (mProperties) {
            mProperties.setProperty(property$ApiFetcher_Result_NrItem(), Integer.toString(nrItem));
        }
    }

    public void setGooFetcherAppenderWorkerAppenderType(String type) {
        synchronized (mProperties) {
            mProperties.setProperty(property$GooFetcher_AppenderWorker_Appender_Type(), type);
        }
    }

    public void setGooFetcherAppenderWorkerAppenderPath(String path) {
        synchronized (mProperties) {
            mProperties.setProperty(property$GooFetcher_AppenderWorker_Appender_Path(), path);
        }
    }

    public void setGooFetcherResultStart(int start) {
        synchronized (mProperties) {
            mProperties.setProperty(property$GooFetcher_Result_Start(), Integer.toString(start));
        }
    }

    public void setGooFetcherResultPageSize(int pageSize) {
        synchronized (mProperties) {
            mProperties.setProperty(property$GooFetcher_Result_PageSize(), Integer.toString(pageSize));
        }
    }

    public void setFileName(String fileName) {
        this.mFileName = fileName;
    }
    
    // property names
    private String property$Global_NrWorker() {
        return "global.nr_worker";
    }

    private String property$ApiFetcher_Worker_Id(int id) {
        return String.format("api_fetcher.worker[%d].id", id);
    }

    private String property$ApiFetcher_Worker_Page(int id) {
        return String.format("api_fetcher.worker[%d].page", id);
    }

    private String property$ApiFetcher_Worker_Step(int id) {
        return String.format("api_fetcher.worker[%d].step", id);
    }

    private String property$ApiFetcher_Worker_Appender_Type(int id) {
        return String.format("api_fetcher.worker[%d].appender.type", id);
    }

    private String property$ApiFetcher_Worker_Appender_Path(int id) {
        return String.format("api_fetcher.worker[%d].appender.path", id);
    }

    private String property$ApiFetcher_Result_NrPage() {
        return "api_fetcher.result.nr_page";
    }

    private String property$ApiFetcher_Result_NrItem() {
        return "api_fetcher.result.nr_item";
    }

    private String property$GooFetcher_AppenderWorker_Appender_Type() {
        return "goo_fetcher.appender_worker.appender.type";
    }

    private String property$GooFetcher_AppenderWorker_Appender_Path() {
        return "goo_fetcher.appender_worker.appender.path";
    }

    private String property$GooFetcher_Result_Start() {
        return "goo_fetcher.result.start";
    }

    private String property$GooFetcher_Result_PageSize() {
        return "goo_fetcher.result.page_size";
    }
}

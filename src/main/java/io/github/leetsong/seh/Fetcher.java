package io.github.leetsong.seh;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import retrofit2.Response;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Fetcher {

    public static class FetcherResult {
        public int nrPage = 0;
        public int nrItem = 0;
        public Map<Integer, Response> errorResponses = new HashMap<>();
    }

    static {
        // initialize AppenderFactory
        AppenderFactory.initialize();
    }

    private Logger logger = LoggerFactory.getLogger(Fetcher.class);

    private Thread mMonitor;
    private final Object mMonitorLock = new Object();

    private int mNrWorker;
    private ExecutorService mWorkers;
    private int mNrDiedWorker = 0;
    private Lock mNrDiedWorkerLock = new ReentrantLock();
    private int mNrCompletedWorker = 0;
    private Lock mNrCompletedWorkerLock = new ReentrantLock();

    private String[] mTags;
    private final FetcherConfig mConfig = new FetcherConfig("fetcherconfig.properties");
    private final FetcherResult mFetcherResult = new FetcherResult();

    public class Worker implements Runnable {

        private int mWorkerId;

        public Worker(int workerId) {
            this.mWorkerId = workerId;
        }

        @Override
        public void run() {
            StackOverflowClient client = StackOverflowClient.getClient();
            StackOverflowService service = client.getStackOverflowService();
            Appender appender = AppenderFactory.getAppender(
                    mConfig.getWorkerAppenderType(mWorkerId),
                    mConfig.getWorkerAppenderPath(mWorkerId));

            int page = mConfig.getWorkerPage(mWorkerId);
            int step = mConfig.getWorkerStep(mWorkerId);

            while (true) {
                try {
                    Response<SearchResult> response = service.search(page, mTags).execute();
                    if (response.isSuccessful()) {
                        SearchResult result = response.body();
                        logger.info(String.format("Worker %d, page: %d, item: %d",
                                mWorkerId, page, result.getItems().size()));

                        // handle result & update mFetchResult
                        appender.append(result);
                        synchronized (mFetcherResult) {
                            mFetcherResult.nrPage += 1;
                            mFetcherResult.nrItem += result.getItems().size();
                        }

                        if (!result.isHasMore()) {
                            logger.info(String.format(
                                    "Worker %d has completed work", mWorkerId));

                            // save results, next time from page+step
                            mConfig.setWorkerId(mWorkerId);
                            mConfig.setWorkerPage(mWorkerId, page + step);
                            mConfig.setWorkerStep(mWorkerId, step);

                            mNrCompletedWorkerLock.lock();
                            mNrCompletedWorker += 1;
                            mNrCompletedWorkerLock.unlock();
                            // notify mMonitor
                            synchronized (mMonitorLock) {
                                mMonitorLock.notify();
                            }

                            // close appender
                            appender.close();

                            // exit
                            break;
                        } else {
                            page += step;
                        }
                    } else {
                        logger.info(String.format(
                                "Worker %d has encountered error", mWorkerId));

                        // save results, next time from page
                        mConfig.setWorkerId(mWorkerId);
                        mConfig.setWorkerPage(mWorkerId, page);
                        mConfig.setWorkerStep(mWorkerId, step);

                        mNrDiedWorkerLock.lock();
                        mNrDiedWorker++;
                        mNrDiedWorkerLock.unlock();
                        synchronized (mFetcherResult) {
                            mFetcherResult.errorResponses.put(mWorkerId, response);
                        }
                        // notify mMonitor
                        synchronized (mMonitorLock) {
                            mMonitorLock.notify();
                        }

                        // close appender
                        appender.close();

                        // exit
                        break;
                    }
                } catch (IOException e) {
                    // network error, try again
                    e.printStackTrace();
                }
            }
        }
    }

    public Fetcher(String[] tags) {
        this.mTags = tags;
    }

    public void fetch() {
        tryRestart();
        startMonitor();
        mWorkers = Executors.newFixedThreadPool(mNrWorker);

        // workers should be iterated by workerBegin, workerStep and workerEnd
        for (int i = mConfig.workerBegin(); i < mConfig.workerEnd(); i += mConfig.workerStep()) {
            mWorkers.submit(new Worker(mConfig.getWorkerId(i)));
        }
    }

    private boolean tryRestart() {
        try {
            // start according to the configuration files
            mConfig.load();
            mFetcherResult.nrPage = mConfig.getResultNrPage();
            mFetcherResult.nrItem = mConfig.getResultNrItem();
        } catch (FileNotFoundException e) {
            // do not need to restart, start from scratch
            mConfig.reset();
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } finally {
            mNrWorker = mConfig.getNrWorker();
        }

        return false;
    }

    private void startMonitor() {
        mMonitor = new Thread(() -> {
            try {
                long startTime = System.currentTimeMillis();
                do {
                    // wait until others notify
                    synchronized (mMonitorLock) {
                        mMonitorLock.wait();
                    }

                    if (mNrCompletedWorker + mNrDiedWorker == mConfig.getNrWorker()) {
                        long endTime = System.currentTimeMillis();

                        // shutdown all workers
                        mWorkers.shutdownNow();

                        // save to configurations
                        mConfig.setResultNrPage(mFetcherResult.nrPage);
                        mConfig.setResultNrItem(mFetcherResult.nrItem);
                        mConfig.store();

                        // output result
                        if (mNrDiedWorker == 0) {
                            logger.info("Succeeded, the results:");
                            logger.info("  - used time: " + Utility.timeInterval(startTime, endTime));
                            logger.info("  - total pages: " + mFetcherResult.nrPage);
                            logger.info("  - total items: " + mFetcherResult.nrItem);
                        } else {
                            logger.info("Failed, the results:");
                            logger.info("  - used time: " + Utility.timeInterval(startTime, endTime));
                            logger.info("  - total pages: " + mFetcherResult.nrPage);
                            logger.info("  - total items: " + mFetcherResult.nrItem);
                            for (Map.Entry<Integer, Response> entry : mFetcherResult.errorResponses.entrySet()) {
                                logger.info("  - worker: " + entry.getKey());
                                logger.info("    - code: " + entry.getValue().code());
                                logger.info("    - message: " + entry.getValue().message());
                                logger.info("    - toString: " + entry.getValue().toString());
                            }
                        }

                        // exit
                        break;
                    }
                } while (true);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        mMonitor.start();
    }

    public static void main(String[] args) {
        new Fetcher(new String[]{ "android" }).fetch();
    }
}

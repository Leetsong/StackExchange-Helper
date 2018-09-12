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
    private final FetcherConfig mFetcherConfig;
    private final FetcherResult mFetcherResult;

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
                    mFetcherConfig.getWorkerAppenderType(mWorkerId),
                    mFetcherConfig.getWorkerAppenderPath(mWorkerId));

            int page = mFetcherConfig.getWorkerPage(mWorkerId);
            int step = mFetcherConfig.getWorkerStep(mWorkerId);

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
                            mFetcherConfig.setWorkerId(mWorkerId);
                            mFetcherConfig.setWorkerPage(mWorkerId, page + step);
                            mFetcherConfig.setWorkerStep(mWorkerId, step);

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
                        mFetcherConfig.setWorkerId(mWorkerId);
                        mFetcherConfig.setWorkerPage(mWorkerId, page);
                        mFetcherConfig.setWorkerStep(mWorkerId, step);

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
        this.mFetcherConfig = new FetcherConfig(FetcherConfig.convert2ConfigFileName(tags));
        this.mFetcherResult = new FetcherResult();
    }

    public void fetch() {
        tryRestart();
        startMonitor();
        mWorkers = Executors.newFixedThreadPool(mNrWorker);

        // workers should be iterated by workerBegin, workerStep and workerEnd
        for (int i = mFetcherConfig.workerBegin();
             i < mFetcherConfig.workerEnd();
             i += mFetcherConfig.workerStep()) {
            mWorkers.submit(new Worker(mFetcherConfig.getWorkerId(i)));
        }

        try {
            // wait until monitor stops
            mMonitor.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private boolean tryRestart() {
        try {
            // start according to the configuration files
            mFetcherConfig.load();
            mFetcherResult.nrPage = mFetcherConfig.getResultNrPage();
            mFetcherResult.nrItem = mFetcherConfig.getResultNrItem();
        } catch (FileNotFoundException e) {
            // do not need to restart, start from scratch
            mFetcherConfig.reset();
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } finally {
            mNrWorker = mFetcherConfig.getNrWorker();
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

                    if (mNrCompletedWorker + mNrDiedWorker == mFetcherConfig.getNrWorker()) {
                        long endTime = System.currentTimeMillis();

                        // shutdown all workers
                        mWorkers.shutdownNow();

                        // save to configurations
                        mFetcherConfig.setResultNrPage(mFetcherResult.nrPage);
                        mFetcherConfig.setResultNrItem(mFetcherResult.nrItem);
                        mFetcherConfig.store();

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

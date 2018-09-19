package io.github.leetsong.seh;

import io.github.leetsong.seh.data.stackexchange.ItemContainer;
import io.github.leetsong.seh.data.stackexchange.SearchItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import retrofit2.Response;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ApiFetcher extends Fetcher {

    public static class FetcherResult {
        public int nrPage = 0;
        public int nrItem = 0;
        public Map<Integer, Response> errorResponses = new HashMap<>();
    }

    private Logger logger = LoggerFactory.getLogger(ApiFetcher.class);

    private Thread mMonitor;
    private final Object mMonitorLock = new Object();

    private int mNrWorker;
    private ExecutorService mWorkers;
    private int mNrDiedWorker = 0;
    private Lock mNrDiedWorkerLock = new ReentrantLock();
    private int mNrCompletedWorker = 0;
    private Lock mNrCompletedWorkerLock = new ReentrantLock();

    private String[] mTags;
    private String[] mSynonyms;
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
                    mFetcherConfig.getApiFetcherWorkerAppenderType(mWorkerId),
                    mFetcherConfig.getApiFetcherWorkerAppenderPath(mWorkerId));

            int page = mFetcherConfig.getApiFetcherWorkerPage(mWorkerId);
            int step = mFetcherConfig.getApiFetcherWorkerStep(mWorkerId);
            String[] tags = new String[mTags.length + mSynonyms.length];
            System.arraycopy(mTags, 0, tags, 0, mTags.length);
            System.arraycopy(mSynonyms, 0, tags, mTags.length, mSynonyms.length);

            while (true) {
                try {
                    Response<ItemContainer<SearchItem>> response = service.search(page, tags).execute();
                    if (response.isSuccessful()) {
                        ItemContainer<SearchItem> result = response.body();
                        if (result == null) { continue;}
                        logger.info(String.format("Worker %d, page: %d, item: %d",
                                mWorkerId, page, result.getItems().size()));

                        // handle result & update mFetchResult
                        appender.append(result.getItems());
                        synchronized (mFetcherResult) {
                            mFetcherResult.nrPage += 1;
                            mFetcherResult.nrItem += result.getItems().size();
                        }

                        if (!result.isHasMore()) {
                            logger.info(String.format(
                                    "Worker %d has completed work", mWorkerId));

                            // save results, next time from page+step
                            mFetcherConfig.setApiFetcherWorkerId(mWorkerId);
                            mFetcherConfig.setApiFetcherWorkerPage(mWorkerId, page + step);
                            mFetcherConfig.setApiFetcherWorkerStep(mWorkerId, step);

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
                        logger.error(String.format(
                                "Worker %d has encountered error", mWorkerId));

                        // save results, next time from page
                        mFetcherConfig.setApiFetcherWorkerId(mWorkerId);
                        mFetcherConfig.setApiFetcherWorkerPage(mWorkerId, page);
                        mFetcherConfig.setApiFetcherWorkerStep(mWorkerId, step);

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

    public ApiFetcher(String[] tags) {
        this.mTags = tags;
        this.mSynonyms = new String[0];
        this.mFetcherConfig = new FetcherConfig(FetcherConfig.convert2ConfigFileName(tags));
        this.mFetcherResult = new FetcherResult();
    }

    @Override
    public void fetch() {
        // try restart
        if (tryRestart()) {
            logger.info("ApiFetcher restarted from last state");
        } else {
            logger.info("ApiFetcher started from scratch");
        }

        // start monitor to monitor workers
        startMonitor();

        // fill synonyms
        mSynonyms = fillSynonyms();

        // create worker pool
        mWorkers = Executors.newFixedThreadPool(mNrWorker);

        // workers should be iterated by apiFetcherWorkerBegin, apiFetcherWorkerStep and apiFetcherWorkerEnd
        for (int i = mFetcherConfig.apiFetcherWorkerBegin();
             i < mFetcherConfig.apiFetcherWorkerEnd();
             i += mFetcherConfig.apiFetcherWorkerStep()) {
            mWorkers.submit(new Worker(mFetcherConfig.getApiFetcherWorkerId(i)));
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
            mFetcherResult.nrPage = mFetcherConfig.getApiFetcherResultNrPage();
            mFetcherResult.nrItem = mFetcherConfig.getApiFetcherResultNrItem();
            return true;
        } catch (FileNotFoundException e) {
            // do not need to restart, start from scratch
            mFetcherConfig.reset();
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            // errors happen, start from scratch
            mFetcherConfig.reset();
            return false;
        } finally {
            mNrWorker = mFetcherConfig.getNrWorker();
        }
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
                        mFetcherConfig.setApiFetcherResultNrPage(mFetcherResult.nrPage);
                        mFetcherConfig.setApiFetcherResultNrItem(mFetcherResult.nrItem);
                        mFetcherConfig.store();

                        // output result
                        if (mNrDiedWorker == 0) {
                            logger.info("Succeeded, the results:");
                            logger.info("  - used time: " + Utility.timeInterval(startTime, endTime));
                            logger.info("  - total pages: " + mFetcherResult.nrPage);
                            logger.info("  - total items: " + mFetcherResult.nrItem);
                        } else {
                            logger.error("Failed, the results:");
                            logger.error("  - used time: " + Utility.timeInterval(startTime, endTime));
                            logger.error("  - total pages: " + mFetcherResult.nrPage);
                            logger.error("  - total items: " + mFetcherResult.nrItem);
                            for (Map.Entry<Integer, Response> entry : mFetcherResult.errorResponses.entrySet()) {
                                logger.error("  - worker: " + entry.getKey());
                                logger.error("    - code: " + entry.getValue().code());
                                logger.error("    - message: " + entry.getValue().message());
                                logger.error("    - toString: " + entry.getValue().toString());
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
        new ApiFetcher(new String[]{ "android" }).fetch();
    }
}

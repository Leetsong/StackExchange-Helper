package com.example.stackoverflow.fetcher;

import retrofit2.Response;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Fetcher {

    public static class FetchResult {
        public int nrPage = 0;
        public int nrItem = 0;
        public Map<Integer, Response> errorResponses = new HashMap<>();
    }

    private static final int NR_WORKER = 16;

    private Thread monitor;
    private final Object monitorLock = new Object();

    private ExecutorService workers = Executors.newFixedThreadPool(NR_WORKER);
    private int nrDiedWorker = 0;
    private Lock nrDiedWorkerLock = new ReentrantLock();
    private int nrCompletedWorker = 0;
    private Lock nrCompletedWorkerLock = new ReentrantLock();

    private final FetchResult fetchResult = new FetchResult();

    public class Worker implements Runnable {

        private int workerId;

        public Worker(int workerId) {
            this.workerId = workerId;
        }

        @Override
        public void run() {
            StackOverflowClient client = StackOverflowClient.getClient();
            StackOverflowService service = client.getStackOverflowService();

            int page = workerId;
            int step = NR_WORKER;

            while (true) {
                try {
                    Response<SearchResult> response = service.search(page, "android").execute();
                    if (response.isSuccessful()) {
                        SearchResult result = response.body();
                        System.out.println(String.format("Worker %d, page: %d, item: %d",
                                workerId, page, result.getItems().size()));
                        synchronized (fetchResult) {
                            fetchResult.nrPage += 1;
                            fetchResult.nrItem += result.getItems().size();
                        }

                        if (!result.isHasMore()) {
                            System.out.println(String.format(
                                    "Worker %d have completed work", workerId));

                            nrCompletedWorkerLock.lock();
                            nrCompletedWorker ++;
                            nrCompletedWorkerLock.unlock();
                            // notify monitor
                            synchronized (monitorLock) {
                                monitorLock.notify();
                            }

                            // exit
                            break;
                        } else {
                            page += step;
                        }
                    } else {
                        System.out.println(String.format(
                                "Worker %d have encountered error", workerId));

                        nrDiedWorkerLock.lock();
                        nrDiedWorker ++;
                        nrDiedWorkerLock.unlock();
                        synchronized (fetchResult) {
                            fetchResult.errorResponses.put(workerId, response);
                        }
                        // notify monitor
                        synchronized (monitorLock) {
                            monitorLock.notify();
                        }

                        // exit
                        break;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void startMonitor() {
        monitor = new Thread(() -> {
            try {
                do {
                    long startTime = System.currentTimeMillis();

                    // wait until others notify
                    synchronized (monitorLock) {
                        monitorLock.wait();
                    }

                    if (nrCompletedWorker + nrDiedWorker == NR_WORKER) {
                        long endTime = System.currentTimeMillis();
                        workers.shutdownNow();
                        if (nrDiedWorker == 0) {
                            System.out.println("Succeeded, the results:");
                            System.out.println("  - used time: " + ((double)(endTime - startTime) / 1000) + "s");
                            System.out.println("  - total pages: " + fetchResult.nrPage);
                            System.out.println("  - total items: " + fetchResult.nrItem);
                        } else {
                            System.out.println("Failed, the results:");
                            System.out.println("  - used time: " + ((double)(endTime - startTime) / 1000) + "s");
                            System.out.println("  - total pages: " + fetchResult.nrPage);
                            System.out.println("  - total items: " + fetchResult.nrItem);
                            for (Map.Entry<Integer, Response> entry : fetchResult.errorResponses.entrySet()) {
                                System.out.println("  - worker: " + entry.getKey());
                                System.out.println("    - code: " + entry.getValue().code());
                                System.out.println("    - message: " + entry.getValue().message());
                                System.out.println("    - toString: " + entry.getValue().toString());
                            }
                        }
                        break;
                    }
                } while (true);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        monitor.start();
    }

    public Fetcher() {}

    public void fetch() {
        startMonitor();
        for (int i = 0; i < NR_WORKER; i++) {
            workers.submit(new Worker(i + 1));
        }
    }

    public static void main(String[] args) {
        new Fetcher().fetch();
    }
}

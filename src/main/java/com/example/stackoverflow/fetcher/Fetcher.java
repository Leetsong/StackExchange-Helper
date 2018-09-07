package com.example.stackoverflow.fetcher;

import retrofit2.Response;

import java.io.IOException;
import java.util.concurrent.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Fetcher {

    public static class FetchResult {
        public int nrPage = 0;
        public int nrItem = 0;
        public Response errorResponse = null;
    }

    private static final int QUEUE_SIZE = 32;

    private Thread monitor;
    private final Object monitorLock = new Object();

    private int nrWorker = QUEUE_SIZE;
    private Lock nrWorkerLock = new ReentrantLock();
    private int nrWaitedWorker = 0;
    private Lock nrWaitedWorkerLock = new ReentrantLock();

    private ExecutorService workers = Executors.newFixedThreadPool(QUEUE_SIZE);
    private Object workersLock = new Object();
    private BlockingQueue<Integer> queue = new LinkedBlockingQueue<>(QUEUE_SIZE);

    private final FetchResult fetchResult = new FetchResult();

    public Fetcher() {
        try {
            for (int i = 0; i < QUEUE_SIZE; i ++) {
                // we assume that, the number of pages of searched results is at least 32 pages
                queue.put(i + 1);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    public void fetch() {
        startMonitor();
        StackOverflowClient client = StackOverflowClient.getClient();
        StackOverflowService service = client.getStackOverflowService();

        for (int i = 0; i < QUEUE_SIZE; i ++) {
            workers.submit(() -> {
                while (true) {
                    try {
                        while (queue.peek() == null) {
                            // ready to be into waited
                            nrWaitedWorkerLock.lock();
                            nrWaitedWorker ++;
                            nrWaitedWorkerLock.unlock();

                            // notify the monitor
                            synchronized (monitorLock) {
                                monitorLock.notify();
                            }

                            // wait
                            synchronized (workersLock) {
                                workersLock.wait();
                            }

                            // when notified, it is no longer a waited one
                            nrWaitedWorkerLock.lock();
                            nrWaitedWorker --;
                            nrWaitedWorkerLock.unlock();
                        }

                        int page = queue.take();
                        Response<SearchResult> response = service.search(page, "android").execute();
                        if (response.isSuccessful()) {
                            SearchResult result = response.body();
                            System.out.println(String.format("thread: %d, page: %d, item: %d",
                                    Thread.currentThread().getId(), page, result.getItems().size()));
                            if (result.isHasMore() & page >= QUEUE_SIZE) {
                                synchronized (fetchResult) {
                                    fetchResult.nrPage += 1;
                                    fetchResult.nrItem += result.getItems().size();
                                }
                                queue.put(page + 1);

                                synchronized (workersLock) {
                                    workersLock.notifyAll();
                                }
                            }
                        } else {
                            // page is too large, this can only happen when page <= QUEUE_SIZE,
                            // if no network errors and throttle
                            synchronized (fetchResult) {
                                fetchResult.errorResponse = response;
                            }

                            // a worker died
                            nrWorkerLock.lock();
                            nrWorker --;
                            nrWorkerLock.unlock();

                            // awake the monitor
                            synchronized (monitorLock) {
                                monitorLock.notify();
                            }

                            // exit
                            break;
                        }
                    } catch (InterruptedException e) {
                        System.out.println("THREAD INTERRUPTED");
                        e.printStackTrace();
                    } catch (IOException e) {
                        System.out.println("NETWORK FAILED");
                        e.printStackTrace();
                    }
                }
            });
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

                    if (nrWorker == nrWaitedWorker) {
                        workers.shutdownNow();
                        if (fetchResult.errorResponse == null) {
                            long endTime = System.currentTimeMillis();
                            System.out.println("Succeeded, the results:");
                            System.out.println(" - used time: " + ((endTime - startTime) / 1000) + "s");
                            System.out.println(" - total pages: " + fetchResult.nrPage);
                            System.out.println(" - total items: " + fetchResult.nrItem);
                        } else {
                            System.out.println("Failed, server responses with:");
                            System.out.println(" - code: " + fetchResult.errorResponse.code());
                            System.out.println(" - message: " + fetchResult.errorResponse.message());
                            System.out.println(" - toString: " + fetchResult.errorResponse.toString());
                        }
                        break;
                    }
                } while (true);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        monitor.start();
    }

    public static void main(String[] args) {
        new Fetcher().fetch();
    }
}

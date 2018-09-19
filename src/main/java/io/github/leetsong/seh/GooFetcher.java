package io.github.leetsong.seh;

import io.github.leetsong.seh.data.stackexchange.GooGItem;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class GooFetcher extends SoupBasedFetcher {

    private Logger logger = LoggerFactory.getLogger(GooFetcher.class);

    private static final int DEFAULT_RETRY_COUNT = 3;
    private static final int DEFAULT_TIMEOUT_S = 1;
    private static final int DEFAULT_QUEUE_CAPACITY = 32;

    private String   mQuery;
    private String[] mSynonyms;
    private int      mStart;
    private int      mPageSize;
    private int      mTotal;

    private final BlockingQueue<String> mLinksQueue;
    private final BlockingQueue<GooGItem> mGItemsQueue;

    private int mNrConsumerWorker;
    private ProducerWorker mProducerWorker;
    private AppenderWorker mAppenderWorker;
    private ExecutorService mConsumerWorkers;
    private final Object mAppenderWorkerLock;
    private boolean mLooperCompleted;

    private int mNrLink;
    private int mNrItem;
    private FetcherConfig mFetcherConfig;

    public class ProducerWorker extends Thread {

        private long mWorkerId;
        private boolean mIsCompleted;

        public ProducerWorker() {
            this.mIsCompleted = false;
        }

        @Override
        public void run() {
            // we assume in here that, the page DOM is strictly in consistent with
            // what we write here, i.e., we will use select(), as well as directly
            // use select()[x], as we are confirmed that (1) select will return a
            // none-zero-size array, and (2) x is the correct index. We mean that,
            // no NullPointerExceptions will be thrown.
            // we will only retry 3 times for network error

            // get id, ProducerWorker is identified by its thread
            this.mWorkerId = Thread.currentThread().getId();

            int retry = 0;

            // ProducerWorker -> get coarse information
            while (mNrLink < mTotal && retry < DEFAULT_RETRY_COUNT) {
                try {
                    // get the response
                    Connection.Response response = Jsoup.connect(searchUrl()).execute();
                    Document doc = response.parse();

                    // each result is wrapped in a .g
                    List<Element> gElements = doc.select(".g");
                    int nrLinkThisTime = 0;
                    for (Element gElement : gElements) {
                        Element linkElement = gElement.selectFirst(".r a");
                        if (linkElement != null) {
                            String link = linkElement.attr("href");
                            // only add questions
                            if (link.substring("https://stackoverflow\\.com/questions/".length())
                                    .matches("\\d.*?/.+")) {
                                try {
                                    mLinksQueue.put(link);
                                    logger.info(String.format(
                                            "ProducerWorker %d produces a new link %s",
                                            mWorkerId, link));
                                    nrLinkThisTime += 1;
                                    mNrLink += 1;
                                    mStart += 1;
                                } catch (InterruptedException e) {
                                    logger.error(String.format(
                                            "ProducerWorker %d is interrupted while putting a new link %s",
                                            mWorkerId,
                                            link));
                                    e.printStackTrace();
                                }
                            }
                        }
                    }

                    // each succeeded, the retry is reset
                    retry = 0;

                    logger.info(String.format("ProducerWorker %d produces %d/%d links this time, start from %d",
                            mWorkerId, nrLinkThisTime, mNrLink, mStart - nrLinkThisTime));
                } catch (IOException e) {
                    logger.error(String.format("ProducerWorker %d failed to get the document of %s, retry the %d-th time",
                            mWorkerId, searchUrl(), retry));
                    e.printStackTrace();
                    // start retry
                    retry += 1;
                }
            }

            this.mIsCompleted = true;
            logger.info(String.format("ProducerWorker %d has completed work", mWorkerId));

            // save results
            mFetcherConfig.setGooFetcherResultStart(mStart);
            mFetcherConfig.setGooFetcherResultPageSize(mPageSize);
        }

        public boolean isCompleted() {
            return this.mIsCompleted;
        }
    }

    public class AppenderWorker extends Thread {

        private long mWorkerId;
        private CsvAppender mAppender;

        public AppenderWorker(String path) {
            this.mAppender = CsvAppender.newInstance(path);
        }

        @Override
        public void run() {
            this.mWorkerId = Thread.currentThread().getId();
            // wait until looper completed, and all ConsumerWorkers completed
            while (!(mLooperCompleted && mConsumerWorkers.isTerminated())) {
                synchronized (mAppenderWorkerLock) {
                    try {
                        // wait until others notify it
                        mAppenderWorkerLock.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                synchronized (mGItemsQueue) {
                    logger.info(String.format("AppenderWorker %d is writing to %s",
                            mWorkerId, mAppender.getPath()));
                    // append them
                    mAppender.append(new ArrayList<>(mGItemsQueue));
                    // clear
                    mNrItem += mGItemsQueue.size();
                    mGItemsQueue.clear();
                    logger.info(String.format("AppenderWorker %d succeeded in writing to %s",
                            mWorkerId, mAppender.getPath()));
                }
            }
            logger.info(String.format("AppenderWorker %d has completed work", mWorkerId));
        }

        public void close() {
            this.mAppender.close();
        }
    }

    public class ConsumerWorker implements Runnable {

        private long mWorkerId;
        private String mLink;

        public ConsumerWorker(String link) {
            this.mLink = link;
        }

        @Override
        public void run() {
            // we assume in here that, the page DOM is strictly in consistent with
            // what we write here, i.e., we will use select(), as well as directly
            // use select()[x], as we are confirmed that (1) select will return a
            // none-zero-size array, and (2) x is the correct index. We mean that,
            // no NullPointerExceptions will be thrown.
            // we will only retry 3 times for network error

            // get id, each worker is identified via its living thread
            this.mWorkerId = Thread.currentThread().getId();

            // every worker can retry DEFAULT_RETRY_COUNT times
            int retry = 0;
            do {
                logger.info(String.format("ConsumerWorker %d is parsing link %s", mWorkerId, mLink));
                try {
                    Connection.Response response = Jsoup.connect(mLink).execute();
                    Document doc = response.parse();

                    // get each prop & element
                    long questionId = -1;
                    try {
                        questionId = Integer.parseInt(mLink.split("/")[4]);
                    } catch (Exception e) {
                        logger.error("Failed to parse questionId of: " + mLink);
                        e.printStackTrace();
                    }

                    // title is wrapped in header
                    Element questionHeaderElement = doc.getElementById("question-header");
                    Element questionTitleElement = questionHeaderElement.selectFirst("h1 a");
                    String questionTitle = questionTitleElement.html();

                    // tags, score, props are wrapped in question
                    Element questionElement = doc.getElementById("question");

                    Elements tagsElement = questionElement.select("div.grid.ps-relative a.post-tag");
                    List<String> tags = new ArrayList<>(tagsElement.size());
                    for (Element tagElement : tagsElement) {
                        if (tagElement.childNodeSize() > 1) {
                            tags.add(tagElement.text());
                        } else {
                            tags.add(tagElement.text());
                        }
                    }

                    Element scoreElement = questionElement.selectFirst("span.vote-count-post");
                    int score = Integer.parseInt(scoreElement.html());

                    // viewCount, creationDate are wrapped in sidebar/qinfo/td
                    Element qinfoElement = doc.getElementById("qinfo");
                    Elements tdElements = qinfoElement.select("td");

                    Element viewCountElement = tdElements.get(3).selectFirst("b");
                    int viewCount = Integer.parseInt(
                            viewCountElement.html().split(" ")[0].replace(",", ""));

                    Element creationDateElement = tdElements.get(1).selectFirst("p");
                    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
                    String creationDateInString = creationDateElement.attr("title");
                    long creationDate = 0;
                    try {
                        creationDate = simpleDateFormat.parse(creationDateInString).getTime();
                    } catch (ParseException e) {
                        logger.error("Failed to parse date: " + creationDateInString);
                        e.printStackTrace();
                    }

                    // insert the GooGItem
                    try {
                        mGItemsQueue.put(new GooGItem.Builder()
                                .withQuestionId(questionId)
                                .withTitle(questionTitle)
                                .withTags(tags)
                                .withViewCount(viewCount)
                                .withScore(score)
                                .withCreationDate(creationDate)
                                .withLink(mLink)
                                .build());

                        // notify AppenderWorker to write when the queue is full
                        synchronized (mGItemsQueue) {
                            if (mGItemsQueue.size() == DEFAULT_QUEUE_CAPACITY) {
                                synchronized (mAppenderWorkerLock) {
                                    mAppenderWorkerLock.notify();
                                }
                            }
                        }
                    } catch (InterruptedException e) {
                        logger.error(String.format(
                                "ConsumerWorker %d is interrupted while parsing link %s", mWorkerId, mLink));
                        e.printStackTrace();
                    }

                    // each time succeeded, the retry is reset
                    retry = 0;
                } catch (IOException e) {
                    logger.error(String.format(
                            "ConsumerWorker %d failed to get the document of %s, retry for the %dth time",
                            mWorkerId, mLink, retry));
                    e.printStackTrace();
                    // retry one time more
                    retry += 1;
                }
            } while (retry != 0 && retry < DEFAULT_RETRY_COUNT);

            if (retry != 0) {
                logger.error(String.format("ConsumerWorker %d failed to consume link %s", mWorkerId, mLink));
            } else {
                logger.info(String.format("ConsumerWorker %d succeeded in consuming link %s", mWorkerId, mLink));
            }
        }
    }

    public GooFetcher(String query, int total) {
        this.mQuery = query;
        this.mTotal = total;
        this.mSynonyms = new String[0];
        this.mAppenderWorkerLock = new Object();
        this.mLinksQueue = new ArrayBlockingQueue<>(DEFAULT_QUEUE_CAPACITY);
        this.mGItemsQueue = new ArrayBlockingQueue<>(DEFAULT_QUEUE_CAPACITY);
        this.mLooperCompleted = false;
        this.mNrLink = 0;
        this.mNrItem = 0;
        this.mFetcherConfig = new FetcherConfig(FetcherConfig.convert2ConfigFileName(
                new String[] { mQuery}));
    }

    @Override
    public void fetch() {
        // This is the LooperWorker

        // restart
        if (tryRestart()) {
            logger.info("GooFetcher restarted from last state");
        } else {
            logger.info("GooFetcher started from scratch");
        }

        // fill synonyms
        mSynonyms = fillSynonyms();

        // create the workers (and the pool)
        this.mProducerWorker = new ProducerWorker();
        this.mAppenderWorker = new AppenderWorker(mFetcherConfig.getGooFetcherAppenderWorkerAppenderPath());
        this.mConsumerWorkers = Executors.newFixedThreadPool(mNrConsumerWorker);

        long startTime = System.currentTimeMillis();

        // start ProducerWorker
        this.mProducerWorker.start();
        // start AppenderWorker
        this.mAppenderWorker.start();

        // loop until ProducerWorker completed, and all links were dispatched to ConsumerWorkers
        while (!(this.mProducerWorker.isCompleted() && mLinksQueue.isEmpty())) {
            try {
                String link = mLinksQueue.poll(DEFAULT_TIMEOUT_S, TimeUnit.SECONDS);

                // consume it
                if (null != link) {
                    mConsumerWorkers.execute(new ConsumerWorker(link));
                }
            } catch (InterruptedException e) {
                logger.error("LopperWorker is interrupted while looping");
            }
        }

        // looper completed
        mLooperCompleted = true;
        logger.info("LooperWorker has completed work");

        try {
            // wait until they complete their work
            logger.info("Wait until {Producer,Consumer,Appender}Workers complete their work");

            // safely terminate ConsumerWorkers
            this.mConsumerWorkers.shutdown();
            try {
                if (!this.mConsumerWorkers.awaitTermination(DEFAULT_TIMEOUT_S * 10, TimeUnit.SECONDS)) {
                    this.mConsumerWorkers.shutdownNow();
                }
            } catch (InterruptedException e) {
                this.mConsumerWorkers.shutdownNow();
            }

            // wait ProducerWorker until it completes
            this.mProducerWorker.join();

            // notify AppenderWorker and wait until it completes
            synchronized (mAppenderWorkerLock) {
                this.mAppenderWorkerLock.notify();
            }
            this.mAppenderWorker.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            this.mAppenderWorker.close();
        }

        long endTime = System.currentTimeMillis();
        logger.info("Succeeded, the results:");
        logger.info("  - used time: " + Utility.timeInterval(startTime, endTime));
        logger.info("  - total links: " + mNrLink);
        logger.info("  - total items: " + mNrItem);

        // save to configurations
        try {
            mFetcherConfig.store();
        } catch (IOException e) {
            logger.error("Failed to store the properties due to:");
            e.printStackTrace();
        }
    }

    @Override
    protected String searchUrl() {
        String[] queries = new String[mSynonyms.length + 1];
        queries[0] = mQuery;
        System.arraycopy(mSynonyms, 0, queries, 1, mSynonyms.length);

        String encodedQueries;
        try {
            encodedQueries = URLEncoder.encode(
                    String.join(" OR ", queries), StandardCharsets.UTF_8.toString());
        } catch (UnsupportedEncodingException e) {
            // ignore, never reach here
            e.printStackTrace();
            encodedQueries = String.join(" OR ", queries);
        }

        return String.format("https://www.google.com/search?q=site:stackoverflow.com/questions+%s&start=%d&num=%d",
                encodedQueries, mStart, mPageSize);
    }

    private boolean tryRestart() {
        try {
            // start according to the configuration files
            mFetcherConfig.load();
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
            mStart = mFetcherConfig.getGooFetcherResultStart();
            mPageSize = mFetcherConfig.getGooFetcherResultPageSize();
            mNrConsumerWorker = mFetcherConfig.getNrWorker();
        }
    }

    public static void main(String[] args) {
        GooFetcher gooFetcher = new GooFetcher("ios", 5);
        gooFetcher.fetch();
    }
}

package io.github.leetsong.seh;

import io.github.leetsong.seh.data.stackexchange.GooGItem;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

public class GooFetcher {

    // initialize AppenderFactory
    static {
        AppenderFactory.initialize();
    }

    private Logger logger = LoggerFactory.getLogger(GooFetcher.class);

    private static final int DEFAULT_PAGE_SIZE = 30;
    private static final int DEFAULT_RETRY_COUNT = 3;

    private String mQuery;
    private int mStart;
    private int mPageSize;
    private int mTotal;

    public GooFetcher(String query, int total) {
        this.mQuery = query;
        this.mStart = 0;
        this.mPageSize = DEFAULT_PAGE_SIZE;
        this.mTotal = total;
    }

    public String searchUrl() {
        return String.format("https://www.google.com/search?q=site:stackoverflow.com/questions+%s&start=%d&num=%d",
            mQuery, mStart, mPageSize);
    }

    public void fetch() {
        // we assume in here that, the page DOM is strictly in consistent with
        // what we write here, i.e., we will use select(), as well as directly 
        // use select()[x], as we are confirmed that (1) select will return a
        // none-zero-size array, and (2) x is the correct index. We mean that, 
        // no NullPointerExceptions will be thrown.
        List<String> links = new ArrayList<>(mTotal);
        // we will only retry 3 times for network error
        int retry = 0;

        // first, get coarse information
        while (mStart < mTotal && retry < DEFAULT_RETRY_COUNT) {
            try {
                // get the response
                Connection.Response response = Jsoup.connect(searchUrl()).execute();
                Document doc = response.parse();

                // each result is wrapped in a .g
                List<Element> gElements = doc.select(".g");
                int nrLinkThisTime = 0;
                for (int i = 0; i < gElements.size(); i ++) {
                    Element gElement = gElements.get(i);
                    Element linkElement = gElement.selectFirst(".r a");
                    String link = linkElement.attr("href");
                    // only add questions
                    if (link.substring("https://stackoverflow\\.com/questions/".length())
                            .matches("\\d.*?/.+")) {
                        links.add(link);
                        nrLinkThisTime += 1;
                    }
                }

                // each succeeded, the retry is reset
                retry = 0;
                // continue
                mStart += nrLinkThisTime;
                logger.info(String.format("mStart: %d, nrLinkThisTime: %d", mStart, nrLinkThisTime));
            } catch (IOException e) {
                logger.error("Failed to get the document of: " + searchUrl());
                e.printStackTrace();
                // start retry
                retry += 1;
            }
        }

        // second, enter stackoverflow and get detailed information
        List<GooGItem> gItems = new ArrayList<>(links.size());
        for (String lnk : links) {
            retry = 0;
            do {
                logger.info("parse " + lnk);
                try {
                    Connection.Response response = Jsoup.connect(lnk).execute();
                    Document doc = response.parse();

                    // get each prop & element
                    long questionId = -1;
                    try {
                        questionId = Integer.parseInt(lnk.split("/")[4]);
                    } catch (Exception e) {
                        logger.error("Failed to parse questionId of: " + lnk);
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

                    // create the GooGItem
                    gItems.add(new GooGItem.Builder()
                            .withQuestionId(questionId)
                            .withTitle(questionTitle)
                            .withTags(tags)
                            .withViewCount(viewCount)
                            .withScore(score)
                            .withCreationDate(creationDate)
                            .withLink(lnk)
                            .build());

                    // each time succeeded, the retry is reset
                    retry = 0;
                } catch (IOException e) {
                    logger.error("Failed to get the document of: " + lnk);
                    e.printStackTrace();
                    // retry one time more
                    retry += 1;
                }
            } while (retry != 0 && retry < DEFAULT_RETRY_COUNT);
        }

        // append it out
        CsvAppender appender = CsvAppender.newInstance("test.csv");
        appender.append(gItems);
        appender.close();
    }

    public static void main(String[] args) {
        GooFetcher gooFetcher = new GooFetcher("android", 50);
        gooFetcher.fetch();
    }
}

package io.github.leetsong.seh.data.stackexchange;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

public class CsvItem implements Serializable {

    public static class Builder {

        private CsvItem csvItem = new CsvItem();

        public Builder withQuestionId(long questionId) {
            csvItem.questionId = questionId;
            return this;
        }

        public Builder withTitle(String title) {
            csvItem.title = title;
            return this;
        }

        public Builder withTags(List<String> tags) {
            csvItem.tags = tags;
            return this;
        }

        public Builder withViewCount(int viewCount) {
            csvItem.viewCount = viewCount;
            return this;
        }

        public Builder withScore(int score) {
            csvItem.score = score;
            return this;
        }

        public Builder withCreationDate(long creationDate) {
            csvItem.creationDate = creationDate;
            return this;
        }

        public Builder withLink(String link) {
            csvItem.link = link;
            return this;
        }

        public CsvItem build() {
            return csvItem;
        }
    }

    // header of the csv file
    public static final String[] CSV_ITEM_HEADER = new String[] {
            "ID", "Title", "Tags", "View Count", "Score", "Creation Date", "Link"
    };

    private long questionId;
    private String title;
    private List<String> tags;
    private int viewCount;
    private int score;
    private long creationDate;
    private String link;

    public long getQuestionId() {
        return questionId;
    }

    public void setQuestionId(long questionId) {
        this.questionId = questionId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public int getViewCount() {
        return viewCount;
    }

    public void setViewCount(int viewCount) {
        this.viewCount = viewCount;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public long getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(long creationDate) {
        this.creationDate = creationDate;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public String[] toStringArray() {
        return new String[] {
                Long.toString(questionId),
                title,
                String.join(";", tags),
                Integer.toString(viewCount),
                Integer.toString(score),
                new Date(creationDate).toString(),
                link
        };
    }
}

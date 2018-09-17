package io.github.leetsong.seh.data.stackexchange;

import java.util.List;

public class GooGItem implements CsvItemable {

    public static class Builder {

        private GooGItem gooGItem = new GooGItem();

        public Builder withQuestionId(long questionId) {
            gooGItem.questionId = questionId;
            return this;
        }

        public Builder withTitle(String title) {
            gooGItem.title = title;
            return this;
        }

        public Builder withTags(List<String> tags) {
            gooGItem.tags = tags;
            return this;
        }

        public Builder withViewCount(int viewCount) {
            gooGItem.viewCount = viewCount;
            return this;
        }

        public Builder withScore(int score) {
            gooGItem.score = score;
            return this;
        }

        public Builder withCreationDate(long creationDate) {
            gooGItem.creationDate = creationDate;
            return this;
        }

        public Builder withLink(String link) {
            gooGItem.link = link;
            return this;
        }

        public GooGItem build() {
            return gooGItem;
        }
    }

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

    @Override
    public CsvItem toCsvItem() {
        return new CsvItem.Builder()
                .withQuestionId(questionId)
                .withTitle(title)
                .withTags(tags)
                .withScore(score)
                .withViewCount(viewCount)
                .withLink(link)
                .withCreationDate(creationDate)
                .build();
    }
}

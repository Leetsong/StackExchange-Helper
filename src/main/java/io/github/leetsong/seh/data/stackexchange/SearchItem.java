package io.github.leetsong.seh.data.stackexchange;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.List;

public class SearchItem implements CsvItemable {

    private class Owner implements Serializable {
        @SerializedName("reputation")
        int reputation;
        @SerializedName("user_id")
        long userId;
        @SerializedName("user_type")
        String userType;
        @SerializedName("accept_rate")
        int acceptRate;
        @SerializedName("profile_image")
        String profileImage;
        @SerializedName("display_name")
        String displayName;
        @SerializedName("link")
        String link;

        public int getReputation() {
            return reputation;
        }

        public void setReputation(int reputation) {
            this.reputation = reputation;
        }

        public long getUserId() {
            return userId;
        }

        public void setUserId(long userId) {
            this.userId = userId;
        }

        public String getUserType() {
            return userType;
        }

        public void setUserType(String userType) {
            this.userType = userType;
        }

        public int getAcceptRate() {
            return acceptRate;
        }

        public void setAcceptRate(int acceptRate) {
            this.acceptRate = acceptRate;
        }

        public String getProfileImage() {
            return profileImage;
        }

        public void setProfileImage(String profileImage) {
            this.profileImage = profileImage;
        }

        public String getDisplayName() {
            return displayName;
        }

        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }

        public String getLink() {
            return link;
        }

        public void setLink(String link) {
            this.link = link;
        }
    }

    @SerializedName("tags")
    List<String> tags;
    @SerializedName("owner")
    Owner owner;
    @SerializedName("is_answered")
    boolean isAnswered;
    @SerializedName("view_count")
    int viewCount;
    @SerializedName("score")
    int score;
    @SerializedName("last_activity_date")
    long lastActivityDate;
    @SerializedName("creation_date")
    long creationDate;
    @SerializedName("question_id")
    long questionId;
    @SerializedName("link")
    String link;
    @SerializedName("title")
    String title;

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public Owner getOwner() {
        return owner;
    }

    public void setOwner(Owner owner) {
        this.owner = owner;
    }

    public boolean isAnswered() {
        return isAnswered;
    }

    public void setAnswered(boolean answered) {
        isAnswered = answered;
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

    public long getLastActivityDate() {
        return lastActivityDate;
    }

    public void setLastActivityDate(long lastActivityDate) {
        this.lastActivityDate = lastActivityDate;
    }

    public long getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(long creationDate) {
        this.creationDate = creationDate;
    }

    public long getQuestionId() {
        return questionId;
    }

    public void setQuestionId(long questionId) {
        this.questionId = questionId;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
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

package io.github.leetsong.seh.data.stackexchange;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class SynonymItem implements Serializable {

    @SerializedName("creation_date")
    long creationDate;
    @SerializedName("last_applied_date")
    long lastAppliedDate;
    @SerializedName("applied_count")
    int appliedCount;
    @SerializedName("to_tag")
    String toTag;
    @SerializedName("from_tag")
    String fromTag;

    public long getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(long creationDate) {
        this.creationDate = creationDate;
    }

    public long getLastAppliedDate() {
        return lastAppliedDate;
    }

    public void setLastAppliedDate(long lastAppliedDate) {
        this.lastAppliedDate = lastAppliedDate;
    }

    public int getAppliedCount() {
        return appliedCount;
    }

    public void setAppliedCount(int appliedCount) {
        this.appliedCount = appliedCount;
    }

    public String getToTag() {
        return toTag;
    }

    public void setToTag(String toTag) {
        this.toTag = toTag;
    }

    public String getFromTag() {
        return fromTag;
    }

    public void setFromTag(String fromTag) {
        this.fromTag = fromTag;
    }
}

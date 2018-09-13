package io.github.leetsong.seh.data.stackexchange;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.List;

public class ItemContainer<T> implements Serializable {

    @SerializedName("items")
    List<T> items;
    @SerializedName("has_more")
    boolean hasMore;
    @SerializedName("quota_max")
    int quotaMax;
    @SerializedName("quota_remaining")
    int quotaRemaining;

    public List<T> getItems() {
        return items;
    }

    public void setItems(List<T> items) {
        this.items = items;
    }

    public boolean isHasMore() {
        return hasMore;
    }

    public void setHasMore(boolean hasMore) {
        this.hasMore = hasMore;
    }

    public int getQuotaMax() {
        return quotaMax;
    }

    public void setQuotaMax(int quotaMax) {
        this.quotaMax = quotaMax;
    }

    public int getQuotaRemaining() {
        return quotaRemaining;
    }

    public void setQuotaRemaining(int quotaRemaining) {
        this.quotaRemaining = quotaRemaining;
    }
}

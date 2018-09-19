package io.github.leetsong.seh;

import io.github.leetsong.seh.data.stackexchange.AppendableItem;

public abstract class AbstractAppender<T extends AppendableItem> implements Appender<T> {

    // path of the appender
    protected String mPath;

    public AbstractAppender(String path) {
        this.mPath = path;
    }

    public String getPath() {
        return mPath;
    }

    public void setPath(String path) {
        this.mPath = path;
    }
}

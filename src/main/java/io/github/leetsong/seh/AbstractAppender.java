package io.github.leetsong.seh;

public abstract class AbstractAppender implements Appender {

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

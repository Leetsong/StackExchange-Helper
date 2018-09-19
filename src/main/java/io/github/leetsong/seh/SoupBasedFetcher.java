package io.github.leetsong.seh;

public abstract class SoupBasedFetcher extends Fetcher {

    /**
     * getSearchUrl returns the search url, like google.com/search?q=
     * @return search url
     */
    protected abstract String searchUrl();
}

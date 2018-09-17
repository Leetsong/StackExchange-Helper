package io.github.leetsong.seh;

public interface SoupFetcher {

    /**
     * getSearchUrl returns the search url, like google.com/search?q=
     * @return search url
     */
    String searchUrl(String... args);

    /**
     * fetch fetches and return the doc
     */
    void fetch();
}

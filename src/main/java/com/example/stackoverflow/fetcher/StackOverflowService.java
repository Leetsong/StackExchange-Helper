package com.example.stackoverflow.fetcher;

import retrofit2.Call;

public class StackOverflowService {

    public static final String SITE = "stackoverflow";
    public static final int PAGESIZE = 30;
    public static final String SORT = "votes";
    public static final String ORDER = "desc";

    // the raw StackExchangeService
    private StackExchangeService stackExchangeService;

    public StackOverflowService(StackExchangeService stackExchangeService) {
        this.stackExchangeService = stackExchangeService;
    }

    public Call<SearchResult> search(int page, String... tags) {
        String tagged = String.join(";", tags);
        return stackExchangeService.search(SITE, page, PAGESIZE, tagged, SORT, ORDER);
    }
}

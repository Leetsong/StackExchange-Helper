package com.example.stackoverflow.fetcher;

import retrofit2.Response;

import java.io.IOException;

public class Fetcher {

    public static void main(String[] args) {
        StackOverflowClient client = StackOverflowClient.getClient();
        StackOverflowService service = client.getStackOverflowService();

        boolean hasMore = true;
        int page = 1;

        long startTime = System.currentTimeMillis();
        while (hasMore) {
            try {
                Response<SearchResult> response = service.search(page, "android").execute();
                if (response.isSuccessful()) {
                    SearchResult result = response.body();
                    if (!result.isHasMore()) {
                        hasMore = false;
                    } else {
                        System.out.println("page: " + page);
                        page += 1;
                    }
                } else {
                    System.out.println("Failed, server responses with code: " + response.code());
                    System.exit(1);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        long endTime = System.currentTimeMillis();
        System.out.println("Succeeded, time used: " + ((endTime - startTime) / 1000) + "s");
    }
}

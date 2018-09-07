package com.example.stackoverflow.fetcher;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class StackOverflowClient {

    // a singleton
    private static StackOverflowClient client;

    // the retrofit client
    private Retrofit retrofit;

    public static StackOverflowClient getClient() {
        if (client == null) {
            client = new StackOverflowClient();
        }
        return client;
    }

    public StackOverflowService getStackOverflowService() {
        StackExchangeService seService = retrofit.create(StackExchangeService.class);
        return new StackOverflowService(seService);
    }

    private static Retrofit createClient() {
        return new Retrofit.Builder()
                .baseUrl("https://api.stackexchange.com/2.2/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
    }

    private StackOverflowClient() {
        this.retrofit = createClient();
    }
}

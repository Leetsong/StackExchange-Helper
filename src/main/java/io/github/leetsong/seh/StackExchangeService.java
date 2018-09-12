package io.github.leetsong.seh;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface StackExchangeService {

    @GET("search")
    Call<SearchResult> search(
            @Query("site") String site,
            @Query("page") int page,
            @Query("pagesize") int pageSize,
            @Query("tagged") String tagged,
            @Query("sort") String sort,
            @Query("order") String order);
}

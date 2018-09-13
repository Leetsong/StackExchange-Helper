package io.github.leetsong.seh;

import io.github.leetsong.seh.data.stackexchange.ItemContainer;
import io.github.leetsong.seh.data.stackexchange.SearchItem;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface StackExchangeService {

    @GET("search")
    Call<ItemContainer<SearchItem>> search(
            @Query("site") String site,
            @Query("page") int page,
            @Query("pagesize") int pageSize,
            @Query("tagged") String tagged,
            @Query("sort") String sort,
            @Query("order") String order);
}

package io.github.leetsong.seh;

import io.github.leetsong.seh.data.stackexchange.ItemContainer;
import io.github.leetsong.seh.data.stackexchange.SearchItem;
import io.github.leetsong.seh.data.stackexchange.SynonymItem;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface StackExchangeService {

    /**
     * search searches a site for any question which fit the given criteria
     * @param site     the site name, "stackoverflow"/...
     * @param page     the page to search
     * @param pageSize the size of per page
     * @param tagged   the tags interested, separated by ';'
     * @param sort     the sort method, can be one of {
     *                   activity -- last_activity_date (default),
     *                   creation -- creation_date,
     *                   votes -- score,
     *                   relevance -- matches the relevance tab on the site itself
     *                  }
     * @param order    the sort order, can be one of { desc, "asc}
     * @return
     */
    @GET("search")
    Call<ItemContainer<SearchItem>> search(
            @Query("site") String site,
            @Query("page") int page,
            @Query("pagesize") int pageSize,
            @Query("tagged") String tagged,
            @Query("sort") String sort,
            @Query("order") String order);

    /**
     * synonyms gets all the synonyms that point to the tags identified in {tags}
     * @param tags     the tags interested, separated by ';'
     * @param site     the site name, "stackoverflow"/...
     * @param page     the page to search
     * @param pageSize the size of per page
     * @param sort     the sort method, can be one of {
     *                   creation – creation_date (default),
     *                   applied – applied_count,
     *                   activity – last_applied_date
     *                 }
     * @param order    the sort order, can be one of { desc, asc}
     * @return
     */
    @GET("tags/{tags}/synonyms")
    Call<ItemContainer<SynonymItem>> synonyms(
            @Path("tags") String tags,
            @Query("site") String site,
            @Query("page") int page,
            @Query("pagesize") int pageSize,
            @Query("sort") String sort,
            @Query("order") String order);
}

package io.github.leetsong.seh;

import io.github.leetsong.seh.data.stackexchange.ItemContainer;
import io.github.leetsong.seh.data.stackexchange.SynonymItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import retrofit2.Response;

import java.io.IOException;
import java.util.List;

public abstract class Fetcher {

    // initialize AppenderFactory
    static {
        AppenderFactory.initialize();
    }

    private Logger logger = LoggerFactory.getLogger(Fetcher.class);

    /**
     * fetch fetches the interested queries
     */
    public abstract void fetch();

    protected String[] fillSynonyms(String... words) {
        StackOverflowClient client = StackOverflowClient.getClient();
        StackOverflowService service = client.getStackOverflowService();
        String[] synonyms = new String[0];

        // only fetch 1 page, it is enough
        try {
            Response<ItemContainer<SynonymItem>> response = service.synonyms(1, words).execute();
            if (response.isSuccessful()) {
                ItemContainer<SynonymItem> result = response.body();
                if (result != null) {
                    List<SynonymItem> items = result.getItems();
                    if (items != null && items.size() != 0) {
                        synonyms = items.stream().map(SynonymItem::getFromTag).toArray(String[]::new);
                    }
                } else {
                    logger.error("Failed to get synonyms due to: no response body is presented");
                }
            } else {
                logger.error("Failed to get synonyms due to: " + response.toString());
            }
        } catch (IOException e) {
            logger.error("Failed to get synonyms due to:");
            e.printStackTrace();
        }

        return synonyms;
    }
}

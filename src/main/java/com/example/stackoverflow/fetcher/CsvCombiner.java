package com.example.stackoverflow.fetcher;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CsvCombiner implements Combiner {

    private static Logger logger = LoggerFactory.getLogger(CsvCombiner.class);

    private List<String> mCombiningFiles;
    private String mCombinedFile;

    public CsvCombiner(List<String> combiningFiles, String combinedFile) {
        this.mCombiningFiles = combiningFiles;
        this.mCombinedFile = combinedFile;
    }

    @Override
    public void combine() {
        List<String[]> combinedResults = new ArrayList<>();
        for (String csvWorkerFile : mCombiningFiles) {
            try (CSVReader reader = new CSVReader(new FileReader(csvWorkerFile))) {
                // skip header
                reader.skip(1);
                combinedResults.addAll(reader.readAll());
            } catch (IOException e) {
                logger.error("Error while reading " + csvWorkerFile);
                e.printStackTrace();
            }
        }

        // sort via view count
        combinedResults.sort((a, b) -> Integer.parseInt(b[3]) - Integer.parseInt(a[3]));

        // write to the combined file
        try (CSVWriter csvWriter = new CSVWriter(new FileWriter(mCombinedFile),
                ',', '"', '\\', "\n")) {
            csvWriter.writeNext(CsvAppender.CSV_HEADER);
            csvWriter.writeAll(combinedResults);
        } catch (IOException e) {
            logger.error("Error while writing to " + mCombinedFile);
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        Combiner combiner = new CsvCombiner(Arrays.asList(
                "worker[1]_appender.csv",
                "worker[2]_appender.csv"
        ), "test.csv");
        combiner.combine();
    }
}

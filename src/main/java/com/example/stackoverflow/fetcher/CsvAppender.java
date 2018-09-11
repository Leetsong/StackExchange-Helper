package com.example.stackoverflow.fetcher;

import com.opencsv.CSVWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;

public class CsvAppender extends AbstractAppender {

    private Logger logger = LoggerFactory.getLogger(CsvAppender.class);

    // header of the csv file
    private static final String[] CSV_HEADER = new String[] {
            "ID", "Title", "Tags", "View Count", "Score", "Creation Date", "Link"
    };

    // type of this appender
    public static final String APPENDER_TYPE = "csv";

    private CSVWriter csvWriter;

    public static Appender newInstance(String path) {
        return new CsvAppender(path);
    }

    @Override
    public void append(SearchResult result) {
        result.getItems().forEach(item -> {
            csvWriter.writeNext(new String[] {
                Long.toString(item.getQuestionId()),
                item.getTitle(),
                String.join(";", item.getTags()),
                Integer.toString(item.getViewCount()),
                Integer.toString(item.getScore()),
                new Date(item.getCreationDate()).toString(),
                item.getLink()
            });
        });
    }

    @Override
    public void close() {
        try {
            csvWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private CsvAppender(String path) {
        super(path);

        File f = new File(path);
        boolean fileExists = f.exists();

        try {
            FileWriter fileWriter = new FileWriter(path, true);
            csvWriter = new CSVWriter(fileWriter,
                    CSVWriter.DEFAULT_SEPARATOR,
                    CSVWriter.NO_QUOTE_CHARACTER,
                    CSVWriter.NO_ESCAPE_CHARACTER,
                    CSVWriter.DEFAULT_LINE_END);
            if (!fileExists) {
                // file is newly created, write the header
                csvWriter.writeNext(CSV_HEADER);
            }
        } catch (IOException e) {
            e.printStackTrace();
            logger.error("File exists but is a directory, " +
                    "or does not exist but cannot be created, " +
                    "or cannot be opened for any other reasons");
        }
    }
}

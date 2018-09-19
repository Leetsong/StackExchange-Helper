package io.github.leetsong.seh;

import com.opencsv.CSVWriter;
import io.github.leetsong.seh.data.stackexchange.CsvItemable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import static io.github.leetsong.seh.data.stackexchange.CsvItem.CSV_ITEM_HEADER;

public class CsvAppender extends AbstractAppender<CsvItemable> {

    private Logger logger = LoggerFactory.getLogger(CsvAppender.class);

    // type of this appender
    public static final String APPENDER_TYPE = "csv";

    private CSVWriter csvWriter;

    public static CsvAppender newInstance(String path) {
        return new CsvAppender(path);
    }

    @Override
    public void append(List<CsvItemable> items) {
        items.forEach(item -> csvWriter.writeNext(item.toCsvItem().toStringArray()));
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
            // use ',' as separator, '"' as quote, and '\\' as escape
            // because CSVReader uses '\\' as default escape,
            // However, CSVWriter uses '"' as default escape
            // Wired!!!
            csvWriter = new CSVWriter(fileWriter,
                    ',', '"', '\\', "\n");
            if (!fileExists) {
                // file is newly created, write the header
                csvWriter.writeNext(CSV_ITEM_HEADER);
            }
        } catch (IOException e) {
            e.printStackTrace();
            logger.error("File exists but is a directory, " +
                    "or does not exist but cannot be created, " +
                    "or cannot be opened for any other reasons");
        }
    }
}

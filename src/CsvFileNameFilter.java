import java.io.File;
import java.io.FilenameFilter;
    public class CsvFileNameFilter implements FilenameFilter {
        @Override
        public boolean accept(File dir, String name) {
            return name.contains(".csv");
        }
    }


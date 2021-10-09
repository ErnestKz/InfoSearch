import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

// https://lucene.apache.org/core/8_9_0/core/overview-summary.html#overview.description

public class Part1 {
    public static void main(String[] args) throws IOException {
		Analyzer analyzer = new StandardAnalyzer();
		Path indexPath = Files.createTempDirectory("tmpIndex");
		Directory directory = FSDirectory.open(indexPath);
    }
}

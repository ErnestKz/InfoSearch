import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.TextField;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
    
public class Part1 {
    private static String INDEX_DIRECTORY = "./index";
    private static String CORPUS_DIRECTORY = "./CranCorpus/";
    private static String QUERY_DIRECTORY = "./CranQueries/";

    private static String SAMPLE_QUERY = " what similarity laws must be obeyed when constructing aeroelastic models of heated high speed aircraft .";
    
    public static void main(String[] args) throws IOException {
	readCorpusWriteIndex();
	loadQueriesExecuteCreateResultsFile();
	runQuery(SAMPLE_QUERY);
    }

    private static void runQuery(String query_string) throws IOException {
	Directory       index_dir = FSDirectory.open(Paths.get(INDEX_DIRECTORY));
	DirectoryReader ir        = DirectoryReader.open(index_dir);
	IndexSearcher   is        = new IndexSearcher(ir);
	
	BooleanQuery.Builder q = new BooleanQuery.Builder();
	Query term = new TermQuery(new Term("content", "aeroelastic"));
	q.add(new BooleanClause(term, BooleanClause.Occur.SHOULD));
	
	ScoreDoc[] hits = is.search(q.build(), 5).scoreDocs;
	
	System.out.println("Documents: " + hits.length);
	for (int i = 0; i < hits.length; i++) {
	    Document hitDoc = is.doc(hits[i].doc);
	    System.out.println(i + ") " + hitDoc.get("id") + " " + hits[i].score);
	}
	
	ir.close();
	index_dir.close();
    }
    
    private static void loadQueriesExecuteCreateResultsFile() throws IOException {
	Directory corpus_dir = FSDirectory.open(Paths.get(QUERY_DIRECTORY));
    }

    private static void readCorpusWriteIndex() throws IOException {
	ArrayList<Document> documents = new ArrayList<Document>();
	Directory corpus_dir = FSDirectory.open(Paths.get(CORPUS_DIRECTORY));
	String[] corpus_files = corpus_dir.listAll();
	
	for (String f : corpus_files) {
	    String content = new String(Files.readAllBytes(Paths.get(CORPUS_DIRECTORY + f)));
	    Document doc = new Document();
	    doc.add(new TextField("id", f, Store.YES));
	    doc.add(new TextField("content", content, Store.YES));
	    documents.add(doc);
	}
	
	Directory index_dir = FSDirectory.open(Paths.get(INDEX_DIRECTORY));
	Analyzer analyzer = new StandardAnalyzer();
	IndexWriterConfig iw_config = new IndexWriterConfig(analyzer);
	iw_config.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
	
	IndexWriter iw = new IndexWriter(index_dir, iw_config);
	iw.addDocuments(documents);
	iw.close();
	index_dir.close();
    }
}

package InfoSearch;

import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.TextField;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.ClassicSimilarity;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.search.similarities.TFIDFSimilarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import static java.util.stream.Collectors.toList;

import java.nio.file.Files;
import java.nio.file.Paths;

import InfoSearch.CranDocs.CranDoc;
import InfoSearch.CranDocs.CranQuery;
import static InfoSearch.Env.INDEX_DIRECTORY;
import static InfoSearch.Env.NUM_DOCUMENTS;
import static InfoSearch.Env.INDEX_DOC_BODY_FIELD;
import static InfoSearch.Env.INDEX_DOC_ID_FIELD;
import static InfoSearch.Env.INDEX_DOC_TITLE_FIELD;
import static InfoSearch.Env.QUERY_RESULTS_FILE;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class Main {
	public static void main(String[] args) throws IOException, ParseException {
	    
	Analyzer analyzer = new EnglishAnalyzer();
	Similarity similarity = new BM25Similarity(2.8f, 0.9f);
	
	Directory indexDirectory = FSDirectory.open(Paths.get(INDEX_DIRECTORY));
	IndexWriterConfig indexWriterConfig = new IndexWriterConfig(analyzer);
	indexWriterConfig.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
	indexWriterConfig.setSimilarity(similarity);
	IndexWriter indexWriter = new IndexWriter(indexDirectory, indexWriterConfig);

	CranDocs cranDocs = new CranDocs();
	cranDocs.parseCranCorpus();
	// List<CranDoc> cranDocList = cranDocs.cranDocuments;
	List<Document> documents = cranDocs.cranBuildDocuments();
	indexWriter.addDocuments(documents);
	indexWriter.close();

	DirectoryReader indexDirectoryReader = DirectoryReader.open(indexDirectory);
	IndexSearcher indexSearcher = new IndexSearcher(indexDirectoryReader);
	indexSearcher.setSimilarity(similarity);
	
	cranDocs.parseCranQueries();
	List<CranQuery> cranQueries = cranDocs.cranQueries;
	// QueryParser queryParser = new QueryParser(INDEX_DOC_BODY_FIELD, analyzer);
	MultiFieldQueryParser queryParser = new MultiFieldQueryParser
	    (new String[]{ INDEX_DOC_BODY_FIELD,
			   INDEX_DOC_TITLE_FIELD }
		, analyzer);
	
	String resultsFileContents = cranQueries.stream()
	    .map(cranQuery -> {
		    Query query = cranQuery.buildQuery(queryParser, analyzer);
		    TopDocs topDocs = queryToSearchResult(query, indexSearcher);
		    String resultString = searchResultToResultString(topDocs, cranQuery.id, indexSearcher);
		    return resultString;
		}).reduce("", (subtotal, queryResultString) ->
			  subtotal + (queryResultString),
			  String::concat);

	File queryResultsFile = new File(QUERY_RESULTS_FILE);
	queryResultsFile.createNewFile();
	try(FileWriter queryResultFileWriter = new FileWriter(queryResultsFile)) {
	    queryResultFileWriter.write(resultsFileContents);
	}
    }

    private static
	TopDocs queryToSearchResult(Query query,
				    IndexSearcher indexSearcher ) {
	TopDocs queryResult = null;
	try { queryResult = indexSearcher.search(query, NUM_DOCUMENTS); }
	catch (IOException e) {
	    e.printStackTrace();
	    System.out.println("Error during query search.");
	}
	return queryResult;
    }

    private static
	String searchResultToResultString(TopDocs topDocs,
					  String queryId,
					  IndexSearcher indexSearcher ) {
	StringBuilder resultString = new StringBuilder();
	try {
	    ScoreDoc[] documentScores = topDocs.scoreDocs;
	
	    for (int i = 0; i < documentScores.length; i++) {
		ScoreDoc hitDocumentScoreObj = documentScores[i];
		Document hitDocument = indexSearcher.doc(hitDocumentScoreObj.doc);
		String hitDocumentId = hitDocument.get(INDEX_DOC_ID_FIELD);
		Float hitDocumentScore = hitDocumentScoreObj.score;
		resultString.append(resultLine(queryId,
					       // "0",
					       hitDocumentId,
					       hitDocumentScore));
	    }
	} catch (Exception e) {
	    e.printStackTrace();
	    System.out.println("Error while retrieving document by index ID.");
	}
	return resultString.toString();
    }
    
    private static
	String resultLine(String queryId,
			  String documentId,
			  Float documentScore ) {
	// trec_eval solely uses the score to sort the documents to determine the rank.
	return(queryId    +
	       " Q0 "     +
	       documentId + " "  +
	       "IGNORED " +
	       documentScore + " " +                             
	       "STANDARD"    + "\n");
    }
}

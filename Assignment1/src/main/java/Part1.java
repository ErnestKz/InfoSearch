import static java.util.stream.Collectors.toList;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import java.nio.file.Files;
import java.nio.file.Paths;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

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

    
public class Part1 {
    private static String INDEX_DIRECTORY = "./index";
    private static String CORPUS_DIRECTORY = "./CranCorpus/";
    private static String QUERY_DIRECTORY = "./CranQueries/";

    private static String QUERY_RESULTS_FILE = "./results.txt";

    private static int NUM_QUERIES = 225;
    private static int NUM_DOCUMENTS = 1400;
    private static String INDEX_DOC_ID_FIELD = "id";
    private static String INDEX_DOC_CONTENT_FIELD = "content";
    
    public static void main(String[] args) throws IOException {
	// Analyzer analyzer = new StandardAnalyzer();
	Analyzer analyzer = new EnglishAnalyzer();
	// Similarity similarity = new BM25Similarity(2.4f, 0.8f);
	Similarity similarity = new BM25Similarity(0.8f, 0.2f);
	// Similarity similarity = new BM25Similarity();
	// Similarity similarity = new ClassicSimilarity();

	long start = System.currentTimeMillis();
	
	IndexWriterConfig indexWriterConfig = new IndexWriterConfig(analyzer);
	indexWriterConfig.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
	indexWriterConfig.setSimilarity(similarity);

	System.out.print((System.currentTimeMillis() - start) / 10);
	System.out.println(" created indexWriter config.");

	Directory indexDirectory = FSDirectory.open(Paths.get(INDEX_DIRECTORY));

	IndexWriter indexWriter = new IndexWriter(indexDirectory, indexWriterConfig);
	List<Document> corpusDocuments = readCorpusAndBuildDocuments();
	indexWriter.addDocuments(corpusDocuments);
	indexWriter.close();

	System.out.print((System.currentTimeMillis() - start) / 10);
	System.out.println(" read and wrote indexed corpus.");

	DirectoryReader indexDirectoryReader = DirectoryReader.open(indexDirectory);
	IndexSearcher indexSearcher = new IndexSearcher(indexDirectoryReader);
	indexSearcher.setSimilarity(similarity);
	
	List<Pair<String, String>> queriesIdAndContent = loadQueriesIdAndContent();
	
	List<Pair<String, List<String>>> queriesIdAndTokens =
	    queriesIdAndContent.stream()
	    .map(idContentPair ->
		 Pair.of(idContentPair.getLeft(),
			 queryStringToTokens(idContentPair.getRight(), analyzer)))
	    .collect(toList());
	
	List<Pair<String, Query>> queriesIdAndQuery =
	    queriesIdAndTokens.stream()
	    .map(idTokensPair ->
		 Pair.of(idTokensPair.getLeft(), tokensToQuery(idTokensPair.getRight())))
	    .collect(toList());

	System.out.print((System.currentTimeMillis() - start) / 10);
	System.out.println(" read, analysed, and constructed queries.");
	    
	List<Pair<String, TopDocs>> queriesIdAndSearchResult =
	    queriesIdAndQuery.stream()
	    .map(idQueryPair ->
		 Pair.of(idQueryPair.getLeft(),
			 queryToSearchResult(idQueryPair.getRight(), indexSearcher)))
	    .collect(toList());

	System.out.print((System.currentTimeMillis() - start) / 10);
	System.out.println(" executed "+ NUM_QUERIES + " queries.");

	List<Pair<String, String>> queriesIdAndResultString =
	    queriesIdAndSearchResult.stream()
	    .map(idTopDocsPair ->
		 Pair.of(idTopDocsPair.getLeft(),
			 searchResultToResultString(idTopDocsPair.getRight(),
						    idTopDocsPair.getLeft(),
						    indexSearcher)))
	    .collect(toList());
	
	System.out.print((System.currentTimeMillis() - start) / 10);
	System.out.println(" constructed results string from "+ NUM_QUERIES + " queries, with " + NUM_DOCUMENTS + " documents retrieved each.");
	
	String resultString = queriesIdAndResultString.stream()
	    .reduce("", (subtotal, queryIdAndResultString) ->
		    subtotal + (queryIdAndResultString.getRight()),
		    String::concat);

	indexDirectory.close();
	indexDirectoryReader.close();

	File queryResultsFile = new File(QUERY_RESULTS_FILE);
	queryResultsFile.createNewFile();
	try(FileWriter queryResultFileWriter = new FileWriter(queryResultsFile)) {
	    queryResultFileWriter.write(resultString);
	}

	// String queryString1 = queriesIdAndContent.get(0).getRight();
	// List<String> queryTokens1 = queryStringToTokens(queryString1, analyzer);
	// queryTokens1.stream()
	//     .forEach(s -> System.out.println(s));

	// System.out.println("\n");
	// System.out.print(readAllContents(QUERY_DIRECTORY + "001"));

	// loadQueriesExecuteCreateResultsFile(analyzer, similarity);
    
    }

    private static ArrayList<Document> readCorpusAndBuildDocuments() throws IOException {
	ArrayList<Document> documents = new ArrayList<Document>();
	try (Directory corpusDirectory = FSDirectory.open(Paths.get(CORPUS_DIRECTORY))){
	    String[] corpusFileNames = corpusDirectory.listAll();
	    
	    for (String corpusFileName : corpusFileNames) {
		String corpusFileContent = readAllContents(CORPUS_DIRECTORY + corpusFileName);

		Document doc = new Document();
		doc.add(new TextField(INDEX_DOC_ID_FIELD, corpusFileName, Store.YES));
		doc.add(new TextField(INDEX_DOC_CONTENT_FIELD, corpusFileContent, Store.YES));
		documents.add(doc);
	    }
	}
	return documents;
    }

    private static
	List<Pair<String, String>> loadQueriesIdAndContent() throws IOException {
	List<Pair<String, String>> queryStrings = new ArrayList<>();
	
	try (Directory queryDirectory = FSDirectory.open(Paths.get(QUERY_DIRECTORY))) {
	    String[] queryFileNames = queryDirectory.listAll();
		
	    for (int i = 0; i < queryFileNames.length; i++) {
		String queryFileName = queryFileNames[i];
		
		String queryString = readAllContents(QUERY_DIRECTORY + queryFileName);
		String queryId = (i + 1) + "";
		queryStrings.add(Pair.of(queryId, queryString));
	    }
	}
	return queryStrings;
    }

    private static
	List<String> queryStringToTokens( String queryString,
					  Analyzer analyzer ) {
	List<String> tokens = new ArrayList<>();
	
	try (TokenStream tokenStream = analyzer
	     .tokenStream(INDEX_DOC_CONTENT_FIELD, queryString)) {
	    CharTermAttribute charTermAttribute = tokenStream
		.addAttribute(CharTermAttribute.class);
	    tokenStream.reset();
	    while (tokenStream.incrementToken()) {
		tokens.add(charTermAttribute.toString());
	    }
	    tokenStream.end();
	} catch (Exception e) {
	    e.printStackTrace();
	    System.out.println("Error during query tokenization.");}
	return tokens;
    }

    private static Query tokensToQuery(List<String> tokens) {
	BooleanQuery.Builder booleanQueryBuilder = new BooleanQuery.Builder();
	tokens.stream().forEach(tokenTerm -> {
		// TODO what is the difference between TermQuery and Term?
		Query queryTerm = new TermQuery(new Term(INDEX_DOC_CONTENT_FIELD, tokenTerm));
		booleanQueryBuilder.add(new BooleanClause(queryTerm, BooleanClause.Occur.SHOULD));
	});
	return booleanQueryBuilder.build();
    }

    private static
	TopDocs queryToSearchResult(Query query,
				    IndexSearcher indexSearcher) {
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
						 IndexSearcher indexSearcher
						 ) {
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
			  Float documentScore) {
	// trec_eval solely uses the score to sort the documents to determine the rank.
	return(queryId    +
	       " Q0 "     +
	       documentId + " "  +
	       "IGNORED " +
	       documentScore + " " +                             
	       "STANDARD"    + "\n");
    }

    private static
	String readAllContents( String filePath ) throws IOException {
	return new String(Files.readAllBytes(Paths.get(filePath))); 
    }
    










    private static
	void loadQueriesExecuteCreateResultsFile ( Analyzer   analyzer,
						   Similarity similarity
						   ) throws IOException {

	StringBuilder resultString = new StringBuilder();
	String[]      queryFileNames;
	IndexSearcher indexSearcher;
	
	try (Directory queryDirectory = FSDirectory.open(Paths.get(QUERY_DIRECTORY));
	     Directory indexDirectory = FSDirectory.open(Paths.get(INDEX_DIRECTORY));
	     DirectoryReader indexDirectoryReader = DirectoryReader.open(indexDirectory)) {
	    
	    indexSearcher = new IndexSearcher(indexDirectoryReader);
	    indexSearcher.setSimilarity(similarity);

	    queryFileNames = queryDirectory.listAll();
	    
	    for (int i = 0; i < queryFileNames.length; i++) {
		String queryFileName = queryFileNames[i];
		String queryString   = readAllContents(QUERY_DIRECTORY + queryFileName);
		String queryId       = (i + 1) + "";
		resultString.append(runQueryAndGetDocumentScores(analyzer,
								 queryString,
								 queryId,
								 indexSearcher));
	    }
	}

	File queryResultsFile = new File(QUERY_RESULTS_FILE);
	queryResultsFile.createNewFile();
	
	try(FileWriter queryResultFileWriter = new FileWriter(queryResultsFile)) {
	    queryResultFileWriter.write(resultString.toString());
	}
    }

    private static
	StringBuilder runQueryAndGetDocumentScores ( Analyzer   analyzer,
						     String     queryString,
						     String     queryId,
						     IndexSearcher indexSearcher
						     ) throws IOException {
	
	// TODO Kind of want to know about the what indexSearcher.search() returns.
	// Maybe I can do some nice functional style stuff.
	
	Query booleanQuery = buildQuery(queryString, analyzer);
	ScoreDoc[] documentScores = indexSearcher
	    .search(booleanQuery, NUM_QUERIES)
	    .scoreDocs;

	StringBuilder resultString = new StringBuilder();
	
	for (int i = 0; i < documentScores.length; i++) {
	    Document hitDoc = indexSearcher.doc(documentScores[i].doc);
	    String documentId    = hitDoc.get(INDEX_DOC_ID_FIELD);
	    Float  documentScore = documentScores[i].score;
	    
	    resultString.append(resultLine(queryId,
					   documentId,
					   documentScore));
	}
	
	return resultString;
    }

    private static
	Query buildQuery(String queryString,
			 Analyzer analyzer) throws IOException {
	// TODO Need to know about how token streams work
	// Wondering if the below mess is necessary.
	BooleanQuery.Builder booleanQueryBuilder = new BooleanQuery.Builder();
	TokenStream tokenStream = analyzer.tokenStream(INDEX_DOC_CONTENT_FIELD, queryString);
	CharTermAttribute attr = tokenStream.addAttribute(CharTermAttribute.class);
	tokenStream.reset();
	while(tokenStream.incrementToken()) {
	    Query term = new TermQuery(new Term(INDEX_DOC_CONTENT_FIELD, attr.toString()));
	    booleanQueryBuilder.add(new BooleanClause(term, BooleanClause.Occur.SHOULD));
	}       
	tokenStream.close();
	return booleanQueryBuilder.build();
    }
}

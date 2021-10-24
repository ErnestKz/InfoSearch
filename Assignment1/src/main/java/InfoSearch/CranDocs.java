package InfoSearch;

import static InfoSearch.Env.CORPORA_PATH;
import static InfoSearch.Env.QUERIES_PATH;

import static InfoSearch.Env.INDEX_DOC_ID_FIELD;
import static InfoSearch.Env.INDEX_DOC_BODY_FIELD;
import static InfoSearch.Env.INDEX_DOC_AUTHOR_FIELD;
import static InfoSearch.Env.INDEX_DOC_TITLE_FIELD;
import static InfoSearch.Env.INDEX_DOC_BIB_FIELD;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.TextField;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.Query;

public class CranDocs {
    List<CranDoc> cranDocuments;
    List<CranQuery> cranQueries;

    public CranDocs() {
	cranDocuments = new ArrayList<CranDoc>();
	cranQueries = new ArrayList<CranQuery>();
    }

    public List<Document> cranBuildDocuments() {
	ArrayList<Document> documents = new ArrayList<Document>();
	for (CranDoc cranDoc : this.cranDocuments) {
	    Document doc = new Document();
	    doc.add(new TextField(INDEX_DOC_ID_FIELD, cranDoc.id, Store.YES));
	    doc.add(new TextField(INDEX_DOC_BODY_FIELD, cranDoc.body, Store.YES));
	    doc.add(new TextField(INDEX_DOC_AUTHOR_FIELD, cranDoc.author, Store.YES));
	    doc.add(new TextField(INDEX_DOC_TITLE_FIELD, cranDoc.title, Store.YES));
	    doc.add(new TextField(INDEX_DOC_BIB_FIELD, cranDoc.bib, Store.YES));
	    documents.add(doc);
	}
	return documents;
    }

    public List<Query> cranBuildQueries(Analyzer analyzer) throws ParseException {
	ArrayList<Query> queries = new ArrayList<Query>();
	QueryParser queryParser = new QueryParser(INDEX_DOC_BODY_FIELD, analyzer);
	
	for (CranQuery cranQuery : this.cranQueries) {
	    Query query = queryParser.parse(cranQuery.body);
	    queries.add(query);
	}
	return queries;
    }

    public void parseCranCorpus() throws IOException {
	String cranCorpusContents = new String(Files.readAllBytes(Paths.get(CORPORA_PATH)));
	String cranCorpusSections[] = cranCorpusContents.split("\n[.]");
	cranCorpusSections[0] = cranCorpusSections[0].substring(1); // remove the . from the first line

	CranDoc currentDoc = new CranDoc();
	for (String section : cranCorpusSections) {
	    String sectionContent = section.substring(1).trim();
	    switch (section.charAt(0)) {
	    case 'I':
		currentDoc.id = sectionContent;
		break;
	    case 'T':
		currentDoc.title = sectionContent;
		break;
	    case 'A':
		currentDoc.author = sectionContent;
		break;
	    case 'B':
		currentDoc.bib = sectionContent;
		break;
	    case 'W':
		// had to alter doc 576
		// had to alter doc 578
		currentDoc.body = sectionContent;
		this.cranDocuments.add(currentDoc);
		currentDoc = new CranDoc();
		break;
	    default:
		System.out.println("Error during cran corpus parsing.");
		break;
	    }
	    
	}
    }

    public void parseCranQueries() throws IOException {
	String cranQueriesContents = new String(Files.readAllBytes(Paths.get(QUERIES_PATH)));
	String cranQueriesSections[] = cranQueriesContents.split("\n[.]");
	cranQueriesSections[0] = cranQueriesSections[0].substring(1); // remove the . from the first line

	CranQuery currentDoc = new CranQuery();
	int id = 1;
	for (String section : cranQueriesSections) {
	    String sectionContent = section.substring(1).trim();
	    switch (section.charAt(0)) {
	    case 'I':
		currentDoc.id = id + "";
		break;
	    case 'W':
		currentDoc.body = sectionContent.replace('?', ' ');
		this.cranQueries.add(currentDoc);

		id++;
		currentDoc = new CranQuery();
		break;
	    default:
		System.out.println("Error during cran query parsing.");
		break;
	    }
	}
    }
    
    public class CranDoc {
	String id;
	String title;
	String author;
	String bib;
	String body;

	public String toString(){
	    return "id: " + id + "\n" + "title: " + title;
	}
    }

    public class CranQuery {
	String id;
	String body;

	public String toString(){
	    return "id: " + id + "\n" + "body: " + body;
	}
	
	public Query buildQuery(QueryParser queryParser) {
	    try { return queryParser.parse(body); }
	    catch (ParseException e) {
		e.printStackTrace();
		System.out.println("Error during query parse:" + body);
		}
	    return null;
	}

	public Query buildQuery(MultiFieldQueryParser queryParser, Analyzer analyzer) {
	    String[] queryStrings = {body, body};
	    String[] queryFields = {INDEX_DOC_BODY_FIELD, INDEX_DOC_TITLE_FIELD};
	    try {
		return queryParser.parse(queryStrings, queryFields, analyzer);
	    }
	    catch (ParseException e) {
		e.printStackTrace();
		System.out.println("Error during query parse:" + body);
	    }
	    return null;
	}
    }
}

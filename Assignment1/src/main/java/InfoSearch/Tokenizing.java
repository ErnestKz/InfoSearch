package InfoSearch;

import java.util.ArrayList;
import java.util.List;

import static InfoSearch.Env.INDEX_DOC_BODY_FIELD;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;

class Tokenizing {
    private static
	List<String> queryStringToTokens( String queryString,
					  Analyzer analyzer ) {
	List<String> tokens = new ArrayList<>();
	
	try (TokenStream tokenStream = analyzer
	     .tokenStream(INDEX_DOC_BODY_FIELD, queryString)) {
	    CharTermAttribute charTermAttribute = tokenStream
		.addAttribute(CharTermAttribute.class);
	    tokenStream.reset();
	    while (tokenStream.incrementToken()) {
		tokens.add(charTermAttribute.toString());
	    }
	    tokenStream.end();
	} catch (Exception e) {
	    e.printStackTrace();
	    System.out.println("Error during query tokenization.");
	}
	return tokens;
    }

    private static Query tokensToQuery(List<String> tokens) {
	BooleanQuery.Builder booleanQueryBuilder = new BooleanQuery.Builder();
	tokens.stream().forEach(tokenTerm -> {
		// TODO what is the difference between TermQuery and Term?
		Query queryTerm = new TermQuery(new Term(INDEX_DOC_BODY_FIELD, tokenTerm));
		booleanQueryBuilder.add(new BooleanClause(queryTerm, BooleanClause.Occur.SHOULD));
	    });
	return booleanQueryBuilder.build();
    }

}

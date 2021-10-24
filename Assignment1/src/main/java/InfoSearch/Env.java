package InfoSearch;
	
public class Env {
    public static String CORPORA_PATH = "./Cran/cran.all.1400";
    public static String QUERIES_PATH = "./Cran/cran.qry";
    public static String INDEX_DIRECTORY = "./index";

    public static String QUERY_RESULTS_FILE = "./results.txt";
    public static int NUM_QUERIES = 225;
    public static int NUM_DOCUMENTS = 1400;
    
    public static String INDEX_DOC_ID_FIELD = "id";
    public static String INDEX_DOC_BODY_FIELD = "body";
    public static String INDEX_DOC_AUTHOR_FIELD = "author";
    public static String INDEX_DOC_TITLE_FIELD = "title";
    public static String INDEX_DOC_BIB_FIELD = "bib";
}

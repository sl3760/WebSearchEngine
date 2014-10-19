package edu.nyu.cs.cs2580;

import java.util.Vector;
import java.util.Collections;
import java.util.HashMap;

import edu.nyu.cs.cs2580.QueryHandler.CgiArguments;
import edu.nyu.cs.cs2580.SearchEngine.Options;

/**
 * @CS2580: Implement this class for HW2 based on a refactoring of your favorite
 * Ranker (except RankerPhrase) from HW1. The new Ranker should no longer rely
 * on the instructors' {@link IndexerFullScan}, instead it should use one of
 * your more efficient implementations.
 */
public class RankerFavorite extends Ranker {

  public RankerFavorite(Options options,
      CgiArguments arguments, Indexer indexer) {
    super(options, arguments, indexer);
    System.out.println("Using Ranker: " + this.getClass().getSimpleName());
  }

  @Override
  public Vector<ScoredDocument> runQuery(Query query, int numResults) {
    Vector<ScoredDocument> all = new Vector<ScoredDocument>();
    for (int i = 0; i < _indexer.numDocs(); ++i) {
      all.add(scoreDocument(query, i));
    }
    Collections.sort(all, Collections.reverseOrder());
    Vector<ScoredDocument> results = new Vector<ScoredDocument>();
    for (int i = 0; i < all.size() && i < numResults; ++i) {
      results.add(all.get(i));
    }
    return results;
  }

  private ScoredDocument scoreDocument(Query query, int did) {
    // Process the raw query into tokens.
    //query.processQuery();

    // Get the document tokens.
    Document doc = _indexer.getDoc(did);
    HashMap<String,Integer> termsMap = ((DocumentIndexed) doc).getTerms();
    double sumTerm = 0.0;
    for(String token: termsMap.keySet()){
        int tf = termsMap.get(token);
        int n = _indexer.numDocs();
        int dk = _indexer.corpusDocFrequencyByTerm(token);
        double tf_idf = (double) tf * (1+Math.log((double) n/dk)/Math.log(2));
    	sumTerm += tf_idf*tf_idf;
    }
    double docScore = Math.sqrt(sumTerm);

    // Score the document using cosine.
    double score = 0.0;
    HashMap<String,Integer> queris = new HashMap<String,Integer>();

    for(String queryToken: query._tokens){
        System.out.println(queryToken);
    	if(queris.containsKey(queryToken)){
    		queris.put(queryToken,queris.get(queryToken)+1);
    	}else{
    		queris.put(queryToken,1);
    	}
    }
    double docQueryScore = 0.0;
    double querySum = 0.0;
    for(String queryToken: query._tokens){
    	int tf = _indexer.documentTermFrequency(queryToken,did);
    	int n = _indexer.numDocs();
    	int dk = _indexer.corpusDocFrequencyByTerm(queryToken);
    	double tf_idf = (double) tf * (1+Math.log((double) n/dk)/Math.log(2));
    	docQueryScore += tf_idf*queris.get(queryToken);
    	querySum += queris.get(queryToken)*queris.get(queryToken);
    }
    double queryScore = Math.sqrt(querySum);
    score = docQueryScore/(docScore*queryScore);

    return new ScoredDocument(doc, score);
  }
}

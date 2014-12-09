package edu.nyu.cs.cs2580;

import java.util.Vector;
import java.util.HashMap;
import java.util.Collections;
import java.util.Comparator;

import edu.nyu.cs.cs2580.QueryHandler.CgiArguments;
import edu.nyu.cs.cs2580.SearchEngine.Options;

/**
 * @CS2580: Implement this class for HW3 based on your {@code RankerFavorite}
 * from HW2. The new Ranker should now combine both term features and the
 * document-level features including the PageRank and the NumViews. 
 */
public class RankerComprehensive extends Ranker {

  public RankerComprehensive(Options options,
      CgiArguments arguments, Indexer indexer) {
    super(options, arguments, indexer);
    System.out.println("Using Ranker: " + this.getClass().getSimpleName());
  }

  @Override
  public Vector<ScoredDocument> runQuery(Query query, int numResults) {
    //System.out.println("enter runQuery in RankerComprehensive");
  	    Vector<ScoredDocument> all = new Vector<ScoredDocument>();
    Document doc = _indexer.nextDoc(query,-1);
    System.out.println("the returned doc is null? " + doc==null);
    while(doc!=null){
      
      String url = doc.getUrl();
      //int docid = doc._docid;
      HashMap<String,Integer> queris = new HashMap<String,Integer>();

      for(String queryToken: query._tokens){
        if(queris.containsKey(queryToken)){
          queris.put(queryToken,queris.get(queryToken)+1);
        }else{
          queris.put(queryToken,1);
        }
      }
       double beta = 0.5;
       double score = 1.0;
    
      
      for(String queryToken: query._tokens){      
        int fqi_d = _indexer.documentTermFrequency(queryToken,doc._docid);
        int D =  ((DocumentIndexed)doc).getTotalTerms();
        int cqi = _indexer.corpusTermFrequency(queryToken);
        int C = (int)_indexer._totalTermFrequency;
        score *= (1.0-beta)*(double)fqi_d/(double)D + beta*(double)cqi/(double)C;
      }
      
      // System.out.println("ranking score is " + score);
      Document d = new Document(doc._docid);
      d.setTitle(doc.getTitle());
      d.setPageRank(doc.getPageRank());
      d.setNumViews(doc.getNumViews());
      all.add(new ScoredDocument(d, score));
      //all.add(new ScoredDocument((Document)doc, score));
      //System.out.println("the nextID is current " + ((DocumentIndexed) doc).getDocID());
      doc = _indexer.nextDoc(query,((DocumentIndexed) doc).getDocID());
    }
    
    Collections.sort(all, Collections.reverseOrder());
    Comparator<ScoredDocument> prComparator = new ScoredDocumentPRComparator();
    Comparator<ScoredDocument> numVComparator = new ScoredDocumentNumVComparator();
    Vector<ScoredDocument> results = new Vector<ScoredDocument>();
    Vector<ScoredDocument> prResults = new Vector<ScoredDocument>();
    Vector<ScoredDocument> numVResults = new Vector<ScoredDocument>();
    int iterPR = 5;
    int iterNumV = 3;
    for (int i = 0; i < all.size() && i < numResults*iterPR; ++i) {
      prResults.add(all.get(i));
    }
    Collections.sort(prResults,prComparator);
    for (int i = 0; i < prResults.size() && i < numResults*iterNumV; ++i) {
      numVResults.add(prResults.get(i));
    }
    Collections.sort(numVResults,numVComparator);
    for (int i = 0; i < numVResults.size() && i < numResults; ++i) {
      results.add(numVResults.get(i));
    }

    return results;


  }
  class ScoredDocumentPRComparator implements Comparator<ScoredDocument>
  {  
    public int compare(ScoredDocument o1, ScoredDocument o2)
    {
      Document doc1 = o1.getDoc();
      Document doc2 = o2.getDoc();
      if(doc1.getPageRank() < doc2.getPageRank()){
        return 1;
      }else if(doc1.getPageRank() == doc2.getPageRank()){
        return 0;
      }else{
        return -1;
        }
    }
  }
  class ScoredDocumentNumVComparator implements Comparator<ScoredDocument>
  {  
    public int compare(ScoredDocument o1, ScoredDocument o2)
    {
      Document doc1 = o1.getDoc();
      Document doc2 = o2.getDoc();
      if(doc1.getNumViews() < doc2.getNumViews()){
        return 1;
      }else if(doc1.getNumViews() == doc2.getNumViews()){
        return 0;
      }else{
        return -1;
      }
    }
  }
}

package edu.nyu.cs.cs2580;
import java.util.HashMap;

/**
 * @CS2580: implement this class for HW2 to incorporate any additional
 * information needed for your favorite ranker.
 */
public class DocumentIndexed extends Document {
  private static final long serialVersionUID = 9184892508124423115L;

  private IndexerInvertedCompressed  indexer = null;
  private HashMap<String, Integer> counts = null;

  private int totalTerms;

  public DocumentIndexed(int docid, IndexerInvertedCompressed in) {
    super(docid);
    indexer = in;
  }

  public void setTotalTerms(int i){
  	this.totalTerms = i;
  }

  public int getTotalTerms(){

  	return totalTerms;
  }

  public void setCounts(HashMap<String,Integer> map){
    this.counts = map;
  }

  public HashMap<String,Integer> getCounts(){
    return counts;
  }

}

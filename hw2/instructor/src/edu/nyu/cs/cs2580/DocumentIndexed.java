package edu.nyu.cs.cs2580;
import java.util.HashMap;

import java.util.HashMap;

/**
 * @CS2580: implement this class for HW2 to incorporate any additional
 * information needed for your favorite ranker.
 */
public class DocumentIndexed extends Document {
  private static final long serialVersionUID = 9184892508124423115L;

<<<<<<< HEAD
  
  private HashMap<String, Integer> terms = new HashMap<String,Integer>();

  private int totalTerms;

  public DocumentIndexed(int docid) {
    super(docid);
    
  }

  

  

  public HashMap<String,Integer> getTerms(){
    return this.terms;

  }
  public void setTerms(HashMap<String,Integer> maps){
    this.terms = maps;
    
  }

}

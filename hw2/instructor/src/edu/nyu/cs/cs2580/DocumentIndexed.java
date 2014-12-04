package edu.nyu.cs.cs2580;
import java.util.HashMap;

import java.util.HashMap;

import java.util.HashMap;
/**
 * @CS2580: implement this class for HW2 to incorporate any additional
 * information needed for your favorite ranker.
 */
public class DocumentIndexed extends Document {
  private static final long serialVersionUID = 9184892508124423115L;

  private HashMap<String, Integer> terms = new HashMap<String,Integer>();

  private int totalTerms;

  private int docID;

  public DocumentIndexed(int docid) {
    super(docid);
    
  }

  public HashMap<String,Integer> getTerms(){
    return this.terms;
  }

  public void setTerms(HashMap<String,Integer> maps){
    this.terms = maps;

  }

  public void setDocID(int id){
  	this.docID = id;
  }

  public int getDocID(){
  	return this.docID;
  }

  public void setTotalTerms(int terms){
    this.totalTerms = terms;
  }

  public int getTotalTerms(){
    return this.totalTerms;
  }

}

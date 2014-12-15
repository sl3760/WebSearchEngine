package edu.nyu.cs.cs2580;

import java.util.HashMap; 
/**
 * @CS2580: implement this class for project to incorporate any additional
 * information needed for our Advertisement ranker.
 */

public class Advertisement extends Document {
 	private static final long serialVersionUID = 9184892508124423227L;
  private HashMap<String, Integer> terms = new HashMap<String,Integer>();
  private int docID;
  private int totalTerms;
  private String company_ads;
  private String body;

  public Advertisement(int adid) {
    super(adid);
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

  public void setCompany_ads(String Advertisement){
  	this.company_ads = Advertisement;
  }

  public String getCompany_ads(){
  	return this.company_ads;
  }
  
  public void setBody(String body){
  	this.body = body;
  }

  public String getBody(){
  	return this.body;
  }

  public void setTotalTerms(int terms){
    this.totalTerms = terms;
  }

  public int getTotalTerms(){
    return this.totalTerms;
  }
}

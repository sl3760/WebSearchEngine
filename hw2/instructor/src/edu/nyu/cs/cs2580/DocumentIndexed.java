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

<<<<<<< HEAD
=======

  
>>>>>>> 6eca5c6f0be625c641721d82c5ba67fdc75886a8
  private HashMap<String, Integer> terms = new HashMap<String,Integer>();

  private int totalTerms;

  public DocumentIndexed(int docid) {
    super(docid);
    
<<<<<<< HEAD
  } 
=======
<<<<<<< HEAD
  }

  public HashMap<String,Integer> getTerms(){
    return this.terms;
  }

  public void setTerms(HashMap<String,Integer> maps){
    this.terms = maps;
=======
>>>>>>> 6eca5c6f0be625c641721d82c5ba67fdc75886a8
  }

  

  
>>>>>>> 44528edb4d4d2d3b888cb7f4192b3d7fa4b1337f

  public HashMap<String,Integer> getTerms(){
    return this.terms;
  }
  public void setTerms(HashMap<String,Integer> maps){
    this.terms = maps;
    
  }


}

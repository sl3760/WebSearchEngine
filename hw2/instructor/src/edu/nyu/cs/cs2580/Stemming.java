package edu.nyu.cs.cs2580;

import java.io.Serializable;

public class Stemming implements Serializable{

	private static final long serialVersionUID = 5;
	
	public String stem(String tokens){
		return stemming3(stemming2(stemming1(tokens)));
	}

	//step 1, get rid of plurals
  private String stemming1(String tokens){
    if(tokens.length() > 1){
      if(tokens.endsWith("s")){
        if(tokens.charAt(tokens.length()-2) == 's'){
          return tokens;
        }else if(tokens.charAt(tokens.length()-2) =='e'){
          //need to determine if "es" are plural, check if -ch,-x, -s or -ss
          if(tokens.length()>3){
            if(tokens.charAt(tokens.length()-3) == 'x' || tokens.charAt(tokens.length()-3) == 's' || 
                  tokens.substring(tokens.length()-4, tokens.length()-2).equals("ch")){
              tokens = tokens.substring(0, tokens.length()-2); //remove "es"
              return tokens; 
            }
          }
        }
      tokens = tokens.substring(0, tokens.length()-1);
      }
    } 
    return tokens;
  }
  //step 2 remove ed(ly) and ing(ly)
  private String stemming2(String tokens){
    if(tokens.endsWith("ed")){
      tokens = tokens.substring(0, tokens.length()-2);
    }else if(tokens.endsWith("edly")){
      tokens = tokens.substring(0, tokens.length()-4);
    }else if(tokens.endsWith("ing")){
      tokens = tokens.substring(0, tokens.length()-3);    
    }else if(tokens.endsWith("ingly")){
      tokens = tokens.substring(0, tokens.length()-5);
    }
    return tokens;
  }

	//step3 turns –y to –i
	private String stemming3(String tokens){
	  if(tokens.endsWith("y")){
	    return tokens.substring(0, tokens.length()-1) + "i";
	  }
	  return tokens;
	}
}
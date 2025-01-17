package edu.nyu.cs.cs2580;

import java.util.Scanner;
/**
 * @CS2580: implement this class for HW2 to handle phrase. If the raw query is
 * ["new york city"], the presence of the phrase "new york city" must be
 * recorded here and be used in indexing and ranking.
 */
public class QueryPhrase extends Query {

  private Stemming stemming = new Stemming();

  public QueryPhrase(String query) {
    super(query);
  }

  @Override
  public void processQuery() {
  	if (_query == null) {
      return;
    }
    int openQuote = _query.indexOf("\"");
    if(openQuote!=-1){
    	int closeQuote = _query.indexOf("\"",openQuote+1);
    	if(closeQuote!=-1){
    		String start = _query.substring(0,openQuote);
    		start = start.trim();
    		String phrase = _query.substring(openQuote+1,closeQuote);
    		String last = _query.substring(closeQuote+1,_query.length());
    		last = last.trim();
    		if(start.length()!=0){
    			Scanner s = new Scanner(start);
	    		while(s.hasNext()){
            String queryToken = stemming.stem(s.next());
	    			_tokens.add(queryToken);
	    		}
	    		s.close();
    		}
        if(phrase.length()!=0){
          StringBuilder sb = new StringBuilder();
          Scanner s = new Scanner(phrase);
          while(s.hasNext()){
            String queryToken = stemming.stem(s.next());
            sb.append(queryToken+" ");
          }
          s.close();
          _tokens.add(sb.toString().trim());
        }		   		
    		if(last.length()!=0){
    			Scanner s = new Scanner(last);
	    		while(s.hasNext()){
            String queryToken = stemming.stem(s.next());
	    			_tokens.add(queryToken);
	    		}
	    		s.close();
    		}
    	}
    }else {
    	Scanner s = new Scanner(_query);
    	while(s.hasNext()){
        String queryToken = stemming.stem(s.next());
    		_tokens.add(queryToken);
    	}
    	s.close();
    } 
  }
}

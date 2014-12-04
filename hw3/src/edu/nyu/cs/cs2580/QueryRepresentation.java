package edu.nyu.cs.cs2580;

import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.Map;
import java.util.HashMap;
import java.util.Vector;
import java.io.FileNotFoundException;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.io.FileInputStream;
import java.io.Reader;

public class QueryRepresentation{

	int _numdocs;
	int _numterms;
	Ranker _ranker;
	Query _query;
	Indexer _indexer;
	final int DOC_WRITE_SIZE = 500;
	private static int docNum;
	private Vector<DocumentIndexed> _fullDocuments = new Vector<DocumentIndexed>();

	public QueryRepresentation(int numdocs, int numterms, Ranker ranker, Query query, Indexer indexer) throws FileNotFoundException{
		this._numdocs = numdocs;
		this._numterms = numterms;
		this._ranker = ranker;
		this._query = query;
		this._indexer = indexer;
		fectchDoc(0);
	}
  
	private DocumentIndexed fectchDoc(int did) throws FileNotFoundException{
		docNum = did/DOC_WRITE_SIZE;
    String fileName = _indexer._options._indexPrefix+ "/docList" + (docNum+1); 
    Reader reader = new InputStreamReader(new FileInputStream(fileName));
    Gson gson = new Gson();  
    Vector<DocumentIndexed> docList = gson.fromJson(reader,
            new TypeToken<Vector<DocumentIndexed>>() {}.getType());
    _fullDocuments.clear();
    _fullDocuments = docList;
    return _fullDocuments.get(did % DOC_WRITE_SIZE);
	} 

  public HashMap<String,Double> computeRepresentations() throws FileNotFoundException{

  	class QueryCount{
			String q;
			double f;
			public QueryCount(String q, int f){
				this.q = q;
				this.f = f;
			}
		}

		PriorityQueue<QueryCount> priorityQueue = new PriorityQueue<QueryCount>(_numterms,new Comparator<QueryCount>(){
			@Override
			public int compare(QueryCount qc1, QueryCount qc2){
				if(qc1.f<qc2.f){
					return -1;
				}else if(qc1.f==qc2.f){
					return 0;
				}else{
					return 1;
				}
			}
		});

  	HashMap<String,Integer> termsFreq = new HashMap<String,Integer>();
	  Vector<ScoredDocument> scoredDocs = _ranker.runQuery(_query,_numdocs);
	  for(ScoredDocument scoredDoc:scoredDocs){
	  	Document doc = scoredDoc.getDoc();
	  	int did = doc._docid;  
	  	DocumentIndexed idoc;  	  	
	  	if(did/DOC_WRITE_SIZE!=docNum){
	  		idoc = fectchDoc(did);
	  	} else{
	  		idoc = _fullDocuments.get(did % DOC_WRITE_SIZE);
	  	}
	  	HashMap<String,Integer> map = idoc.getTerms();
	  	for(Map.Entry<String,Integer> entry:map.entrySet()){
	  		String s = entry.getKey();
	  		if(termsFreq.containsKey(s)){
	  			termsFreq.put(s,termsFreq.get(s)+entry.getValue());
	  		}else{
	  			termsFreq.put(s,entry.getValue());
	  		}
	  	}
	  }

	  double uniqueWordSum = 0.0;
	  for(Map.Entry<String,Integer> entry:termsFreq.entrySet()){
	  	uniqueWordSum+=entry.getValue();
	  	QueryCount qc = new QueryCount(entry.getKey(),entry.getValue());
	  	priorityQueue.add(qc);
	  	if(priorityQueue.size()>_numterms){
	  		priorityQueue.poll();
	  	}
	  }

	  double total = 0.0;
	  for(QueryCount qc : priorityQueue){
	  	qc.f = qc.f/uniqueWordSum;
	  	total += qc.f;
	  }

	  HashMap<String,Double> result = new HashMap<String,Double>();
	  while(!priorityQueue.isEmpty()){
	  	QueryCount qc = priorityQueue.poll();
	  	qc.f = qc.f/total;
	  	result.put(qc.q,qc.f);
	  }
	  return result;
  }

}
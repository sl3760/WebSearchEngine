package edu.nyu.cs.cs2580;

import java.util.Vector;
import java.util.Scanner;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

class Ranker {
  private Index _index;

  public Ranker(String index_source){
    _index = new Index(index_source);
  }
  //the result vector is prioritized, the doc with highest score is the top, also need to pass in 
  //rank method to runquery function
  public Vector < ScoredDocument > runquery(String query, String ranker){

    Vector < ScoredDocument > retrieval_results = new Vector < ScoredDocument > ();
    for (int i = 0; i < _index.numDocs(); ++i){
      retrieval_results.add(runquery(query, i, ranker));
    }    
    Comparator<ScoredDocument> comparator = new Rev_ScoredDocumentComparator();
    Collections.sort(retrieval_results, comparator);
    return retrieval_results;
  }

  public ScoredDocument runquery(String query, int did, String ranker){
    // Build query vector    
    Scanner s = new Scanner(query);
   // s.useDelimiter("+");
    Vector < String > qv = new Vector < String > ();
    while (s.hasNext()){
      String term = s.next();
        qv.add(term);
    }

    // Get the document vector. For hw1, you don't have to worry about the
    // details of how index works.
    Document d = _index.getDoc(did);

    if(ranker.equals("phrase")){
        return new ScoredDocument(did, d.get_title_string(), phraseRanker(qv,d));
      }else if(ranker.equals("numviews")){
        return new ScoredDocument(did, d.get_title_string(), numViewRanker(qv, d));
      }else if(ranker.equals("cosine")){
        return new ScoredDocument(did, d.get_title_string(), cosineRanker(qv, d));
      }else if(ranker.equals("QL")){
        return new ScoredDocument(did, d.get_title_string(), qlRanker(qv, d));
      }else if(ranker.equals("linear")){
        return new ScoredDocument(did, d.get_title_string(), linearRanker(qv, d));
      }
  return new ScoredDocument(did, d.get_title_string(), 0.0);
  }

  //cosine ranking
  private double cosineRanker(Vector<String> qv, Document d){
    Vector < String > dv= d.get_title_vector();
    Vector < String > db = d.get_body_vector();
    dv.addAll(db);
    
    HashMap<String,Integer> words = new HashMap<String,Integer>();
    HashMap<String,Integer> queries = new HashMap<String,Integer>();
    Vector <Double> dd = new Vector <Double> ();
    Vector <Double> qd = new Vector <Double> ();
    for(int i=0; i<dv.size(); ++i){
      if(words.containsKey(dv.get(i))){
        words.put(dv.get(i),words.get(dv.get(i))+1);
      } else{
        words.put(dv.get(i),1);
      }
    }
    for(int i=0; i<qv.size(); ++i){
      if(queries.containsKey(qv.get(i))){
        queries.put(qv.get(i),queries.get(qv.get(i))+1);
      } else{
        queries.put(qv.get(i),1);
      }
    }
    Vector < String > dictionary = Document.getDictionary();
    for(int i=0; i<dictionary.size(); ++i){
      String word = dictionary.get(i);
      if(words.containsKey(word)){
        int tf = words.get(word);
        int n = _index.numDocs();
        int dk = Document.documentFrequency(word);
        double tf_idf =(double) tf * (1+Math.log((double) n/dk)/Math.log(2)); 
        dd.add(tf_idf);
      }else{
        dd.add(0.0);
      }
      if(queries.containsKey(word)){
        qd.add((double) queries.get(word));
      }else{
        qd.add(0.0);
      }
    }

    double xy = 0.0;
    double sumx = 0.0;
    double sumy = 0.0;
    for(int i=0; i<dd.size(); ++i){
      xy+=dd.get(i)*qd.get(i);
      sumx+=dd.get(i)*dd.get(i);
      sumy+=qd.get(i)*qd.get(i);
    }
    double xx = Math.sqrt(sumx);
    double yy = Math.sqrt(sumy);
    double score = (double) xy/(xx*yy);
    return score;
    //return new ScoredDocument(did, d.get_title_string(), score);


  }
  
  //query likelihood model
  private double qlRanker(Vector<String> query, Document d ){
    double beta = 0.5;
    double score = 1.0;
    for(int i =0;i<query.size();i++){
      String word = query.get(i);
      int fqi_d = d.documentFrequency(word);
      int D = d.get_title_vector().size()+d.get_body_vector().size();
      int cqi = d.termFrequency(word);
      int C = d.termFrequency();
     score *= (1.0-beta)*(double)fqi_d/(double)D + beta*(double)cqi/(double)C;
    }
  return score;
  }

 //loop through the whole doc to first locate the first term in the phrase, then check if the whole 
  //phrase matched, if yes, count 1, else move forward
  private double phraseRanker(Vector <String> query, Document d){

    Vector < String > d_tit = d.get_title_vector(); //get title vector
    Vector < String > d_con = d.get_body_vector(); //get body vector
    double score = 0.0;
    //first check match in title
    if(d_tit.containsAll(query)){
      
      for(int i = 0; i < (d_tit.size() - query.size()+1);){
        if(d_tit.get(i).equals(query.get(0))){
          //the first term in the query is matched, we will check the rest in order
          i++;
          for(int j = 1; j < query.size();){
            if(d_tit.get(i).equals(query.get(j))){
              i++;
              j++;
            }else{
              break; //if the word does not match, stop comparison
            }
          }
          score+=1; //find one match
        }else{
          i++;
        }
      }
    } 

    //then check match in content
    if(d_con.containsAll(query)){
      for(int i = 0; i < (d_con.size() - query.size()+1);){
        if(d_con.get(i).equals(query.get(0))){
          //the first term in the query is matched, we will check the rest in order
          i++;
          for(int j = 1; j < query.size();){
            if(d_con.get(i).equals(query.get(j))){
              i++;
              j++;
            }else{
              break; //if the word does not match, stop comparison
            }
          }
          score+=1; //find one match
        }else{
          i++; 
        }
      }
    }
    return score;
  }

  private double numViewRanker(Vector <String> query, Document d){
    return (double)d.get_numviews();
  }

  //combine the score of each doc from all other ranking methods. Using a map to help update the score of each doc
 private double linearRanker(Vector <String> query, Document d){
    double B_cos = 0.6;
    double B_ql = 0.39;
    double B_ph = 0.0099;
    double B_nv = 0.0001;
    double score = B_cos * cosineRanker(query,d) + B_ql * qlRanker(query,d) +  B_ph * phraseRanker(query, d) 
                    + B_nv * numViewRanker(query, d);
    return score;
  } 


}
//the comparator helps sort result in descending order
class Rev_ScoredDocumentComparator implements Comparator<ScoredDocument>
{
    public int compare(ScoredDocument o1, ScoredDocument o2)
    {
        if(o1._score < o2._score){
          return 1;
        }else if(o1._score == o2._score){
          return 0;
        }else{
          return -1;
        }
    }
}

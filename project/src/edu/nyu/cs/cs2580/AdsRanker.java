package edu.nyu.cs.cs2580;

import java.util.Map;
import java.util.Vector;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Collections;
import java.util.Comparator;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.Reader;
import java.io.InputStreamReader;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.io.FileNotFoundException;
import java.io.IOException;

import edu.nyu.cs.cs2580.QueryHandler.CgiArguments;
import edu.nyu.cs.cs2580.SearchEngine.Options;

/**
 * @CS2580: The ranker ranks ads based on quality score and auction price
 */
public class AdsRanker extends Ranker {

  public AdsRanker(Options options,
      CgiArguments arguments, Indexer indexer) {
    super(options, arguments, indexer);
    System.out.println("Using Ranker: " + this.getClass().getSimpleName());
  }

  @Override
  public Vector<ScoredDocument> runQuery(Query query, int numResults) {
  	    Vector<ScoredDocument> all = new Vector<ScoredDocument>();
    //load auction file into map
    Gson gson = new Gson();
    String file = _options._adsPrefix+ "/ad.json";
    System.out.println("read auction info from " + file);
    Map<String, Map<String,String>> auctionList = new HashMap<String, Map<String, String>>();
    try{
          Reader reader = new InputStreamReader(new FileInputStream(file));
          auctionList = gson.fromJson(reader,
                  new TypeToken<Map<String, Map<String, String>>>() {}.getType()); 
      System.out.println("auctionList map has entryset " + auctionList.entrySet().size());
      reader.close();
    }catch(IOException e){
        System.out.println("error happened when load auctionList");
    }

    //get all relevant ads id for each query token, the key is company name and value is id_price
    Map<String,String> targetAds = new HashMap<String,String>();
    Map<Integer, Double> priceList = new HashMap<Integer, Double>();
    for(String term: query._tokens){
      System.out.println("the query term is: " + term);
      targetAds = auctionList.get(term);
      for(Map.Entry<String, String> entry : targetAds.entrySet()){
          String company = entry.getKey();
          System.out.println("find one auction company: " + company);
          System.out.println("the value is " + entry.getValue());
          String[] data = entry.getValue().split("\t");
          System.out.println(data.length);
          int subid = Integer.valueOf(data[0].trim());
          double price = Double.valueOf(data[1].trim());
          //get relevence score
          Query c = new Query(company);
          Document ad = _indexer.nextDoc(c,subid);
          System.out.println("find one matching ads w id " + ad._docid);
          String url = ad.getUrl();
          int adid = ad._docid;
          if(priceList.containsKey(adid)){
            price+=priceList.get(adid);
            priceList.put(adid, price);
          }else{
            priceList.put(adid, price);
          }

          HashMap<String,Integer> queris = new HashMap<String,Integer>();

          for(String queryToken: query._tokens){
            if(queris.containsKey(queryToken)){
              queris.put(queryToken,queris.get(queryToken)+1);
            }else{
              queris.put(queryToken,1);
            }
          }
           double beta = 0.5;
           double relev_score = 1.0;
        
          
          for(String queryToken: query._tokens){      
            int fqi_d = _indexer.documentTermFrequency(queryToken,ad._docid);
            int D =  ((Advertisement)ad).getTotalTerms();
            int cqi = _indexer.corpusTermFrequency(queryToken);
            int C = (int)_indexer._totalTermFrequency;
            relev_score *= (1.0-beta)*(double)fqi_d/(double)D + beta*(double)cqi/(double)C;
          }
          //
          double ctr = getCTR();
          double qscore = getQScore(relev_score, ctr);
         // double score = getFinalScore(qscore, price);

          // System.out.println("ranking score is " + score);
          Document d = new Document(ad._docid);
          d.setTitle(ad.getTitle());
          //d.setPageRank(ad.getPageRank());
         // d.setNumViews(ad.getNumViews());
          all.add(new ScoredDocument(d, qscore));
      }
    }

    //update final score
    for(ScoredDocument d: all){        
      int id = d.getDoc()._docid;
      if(priceList.containsKey(id)){
        d.setScore(getFinalScore(d.getScore(), priceList.get(id)));
        priceList.remove(id);
      }else{
        //set the score as 0
        d.setScore(0);
      }
    }
    Collections.sort(all, Collections.reverseOrder());
    //remove result which has 0 score
    int count  = 0;
    for(int i = all.size()-1; i>=0; i--){
      if(all.get(i).getScore()==0){
          all.removeElementAt(i);
      }else{
        break;
      }
    }
    return all;
  }

//need to fill in
  private double getCTR(){
    return 0.1;
  }

//need an actually equation
  private double getQScore(double revelance, double CTR){
    return (0.5*revelance + CTR);

  }
//need an actuallu equation
  private double getFinalScore(double qscore, double price){
    return qscore * price;
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

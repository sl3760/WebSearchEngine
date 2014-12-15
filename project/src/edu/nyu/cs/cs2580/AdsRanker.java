package edu.nyu.cs.cs2580;

import java.lang.StringBuilder;
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
import java.io.Writer;
import java.io.OutputStreamWriter;

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
        // map for computing average QS
        HashMap<Integer, Vector<ScoredDocument>> qs_map = new HashMap<Integer,Vector<ScoredDocument>>(); 

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
    // loading log information
    
     file = _options._adsPrefix+ "/log.json";
    System.out.println("read log from " + file);
    Map<String, Map<String,String>> logInfo = new HashMap<String, Map<String, String>>();
    try{
          Reader reader = new InputStreamReader(new FileInputStream(file));
          logInfo = gson.fromJson(reader,
                  new TypeToken<Map<String, Map<String, String>>>() {}.getType()); 
      System.out.println("auctionList map has entryset " + auctionList.entrySet().size());
      reader.close();
    }catch(IOException e){
        System.out.println("error happened when load log");
    }
    //System.out.println("loading log done!");
     
    Map<String, String> logs = new HashMap<String,String>();
    if(logInfo!=null)
      logs = logInfo.get("adLogs");
    System.out.println("loading log done!");


    file = _options._adsPrefix+ "/CTR.json";
    System.out.println("read CTR from " + file);
    Map<String, Map<String,String>> ctrLog = new HashMap<String, Map<String, String>>();
    try{
          Reader reader = new InputStreamReader(new FileInputStream(file));
          ctrLog = gson.fromJson(reader,
                  new TypeToken<Map<String, Map<String, String>>>() {}.getType()); 
      System.out.println("CTR has entryset " + ctrLog.entrySet().size());
      reader.close();
    }catch(IOException e){
        System.out.println("error happened when load CTR");
    }


    //get all relevant ads id for each query token, the key is company name and value is id_price
    Map<String,String> targetAds = new HashMap<String,String>();
    Map<Integer, Double> priceList = new HashMap<Integer, Double>();
    for(String term: query._tokens){
      //System.out.println("the query term is: " + term);
      if(auctionList.containsKey(term)){
      targetAds = auctionList.get(term);
      for(Map.Entry<String, String> entry : targetAds.entrySet()){
          String company = entry.getKey();
          System.out.println("find one auction company: " + company);
          System.out.println("the value is " + entry.getValue());
          String[] data = entry.getValue().split("\t");
          //System.out.println(data.length);
          int subid = Integer.valueOf(data[0].trim());
          double price = Double.valueOf(data[1].trim());
          //get relevence score
          Query c = new Query(company);
          Document ad = _indexer.nextDoc(c,subid);
          if(ad==null) continue;
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
          System.out.println("QL result:"+relev_score);

          double cosine = getTitleSimilarity(ad.getTitle(),query._tokens);
          relev_score += cosine;
          //
          String query_s = convertToString(query._tokens);

         // System.out.println("before loading ctr!");
          //double ctr = getCTR(query_s,term,ad.getTitle(),logs,company);
          StringBuilder tmp = new StringBuilder();

          String company_ad = tmp.append(company).append("_").append(subid).toString();
          double ctr = getCTR(ctrLog,term,company_ad);
         // System.out.println("ctrLOg after update:" + ctrLog);
          //System.out.println("after loading ctr!");

          //System.out.println("before loading ctr!");
          //double ctr = getCTR(query_s,term,ad.getTitle(),logs,company);
          //System.out.println("after loading ctr!");

          double qscore = getQScore(relev_score, ctr);
         // double score = getFinalScore(qscore, price);

          // System.out.println("ranking score is " + score);
          //Document d = new Advertisement(ad._docid);
          //d.setTitle(ad.getTitle());          
          //d.setCompany_ads(ad)
          //d.setPageRank(ad.getPageRank());
         // d.setNumViews(ad.getNumViews());
          //all.add(new ScoredDocument(d, qscore));
          ScoredDocument sd = new ScoredDocument(ad,qscore);
          if(!qs_map.containsKey(ad._docid)){
               Vector<ScoredDocument> v = new Vector<ScoredDocument>();
               v.add(sd);
               qs_map.put(ad._docid,v);
          }
          else{
            Vector<ScoredDocument> v = qs_map.get(ad._docid);
               v.add(sd);
               qs_map.put(ad._docid,v);

          }
            

      }
    }
  }

   // get the average QS
   for(Map.Entry<Integer, Vector<ScoredDocument>> entry: qs_map.entrySet()){
       double sum = 0.0;
       Document d = null;
       Vector<ScoredDocument> v = entry.getValue();
       for(ScoredDocument sd: v){
             sum += sd.getScore();
             d = sd.getDoc();
       }
       all.add(new ScoredDocument(d, sum /  v.size()));



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
    if(all.size()>3){
      Vector<ScoredDocument> res = new Vector<ScoredDocument>();
      for(int i = 0 ; i < 3 ; i++){
        res.add(all.get(i));
      }
      saveCtrLog(ctrLog);
      return res;
    }
    saveCtrLog(ctrLog);

    return all;
  }

//need to fill in
  /*
  private double getCTR(String query, String token, String title, Map<String,String> logs,String company){
    // No log information at all!
    if(logs == null )
        return 0.3;
      int q_impression = 0;
      int  q_click  = 0;
      int t_impression = 0;
      int t_click = 0;

      for(String log : logs.values()){
           String[] tmp = log.split("\t");
           if(tmp.length>1){
           if(tmp[0].equals(query)){           // Query log
              if(tmp[1].contains(title)){
                     q_impression++;
                     if(tmp.length == 3){
                      if(tmp[2].equals(title))
                          q_click++;
                     }
                }
              continue;


          }

            if(tmp[0].equals(token)){             // token log
                if(tmp[1].contains(title)){
                     t_impression++;
                     if(tmp.length == 3){
                      if(tmp[2].equals(title))
                          t_click++;
                     }
                }


          }
    }
  }

    if(q_impression ==0 && t_impression == 0){     // Brand new, check this company's other ads;
        double sum = 0.0;
       
        Vector<String> ads = getOtherAdsInCompany(company,title);
        for(String ad : ads){
          int impression = 0;
          int click = 0;
        for(String log : logs.values()){
           String[] tmp = log.split("\t");

               if(tmp.length > 1){    
              if(tmp[1].contains(ad)){
                     impression++;
                     if(tmp.length == 3){
                      if(tmp[2].equals(ad))
                          click++;
                     }
                }
              }
              
              
          }
          if(impression == 0 ) 
            sum+=0.3;
          else  
            sum+= (double)click / (double)impression;
        }
        if(sum == 0 ) return 0.3;
        else 
          return sum / ads.size();


    }

    if(q_impression != 0 ) 
     return (double)q_click / (double)q_impression;
   if(t_impression != 0 ) 
     return (double)t_click / (double)t_impression;

   return 0.3;

    

    
  }
  */

  private double getCTR(Map<String,Map<String,String>> ctrLog, String token, String ad){
         Map<String,String> ads_ctr = ctrLog.get(token);
         String ctr_info = ads_ctr.get(ad);
         String[] tmp = ctr_info.split("\\+");
         double res = Double.parseDouble(tmp[0]);
         String view = tmp[1];
         String click = tmp[2];
         if(view.charAt(0) == 'T'){
          if(click.charAt(0) == 'T'){
            res += res/10;
            if(res>=1.0)
              res =1.0;

          }
          else 
            res -= res/10;

          StringBuilder sb = new StringBuilder();
          sb.append(Double.toString(res));
          sb.append("+");
          sb.append(tmp[1]);
          sb.append("+");
          sb.append(tmp[2]);
          ads_ctr.put(ad,sb.toString());
          ctrLog.put(token,ads_ctr);
          //System.out.println("ctrLOg after update:" + ctrLog);
         }
         System.out.println("new CTR for ad"+ad+":"+res);

         return res;

  }

  public void saveCtrLog(Map<String,Map<String, String>> ctrLog) {
    String name = _options._adsPrefix+ "/CTR.json";
    System.out.println("ctrLOg after update:" + ctrLog);
    try{
    Writer writer = new OutputStreamWriter(new FileOutputStream(name));
    Gson gson = new GsonBuilder().create();
    gson.toJson(ctrLog, writer);
    writer.close();
    
    ctrLog.clear();
  }catch(IOException e){
    System.out.println("error saving the ctrlog!");

  }
    
  }

  private Vector<String> getOtherAdsInCompany(String company, String title){
    Vector<String> res = new Vector<String>();
    Vector<Advertisement> all = ((AdsIndex)_indexer).getDocuments();
    for(int i  = 0 ; i < all.size(); i++){
         String company_ads = all.get(i).getCompany_ads();
         String name = company_ads.substring(0,company_ads.length()-2);
         if(name.equals(company)){
          String candidate = all.get(i).getTitle();
          if(!candidate.equals(title))
            res.add(candidate);
         }
    }
    return res;

  }

  private double getTitleSimilarity(String title, Vector<String> query){
    HashMap<String, Integer> counts = new HashMap<String, Integer>();
    HashMap<String, Integer> query_count = new HashMap<String,Integer>();
    int sum = 0;
    int query_mold = 0;
    int title_mold = 0;
    String[] tmp = title.split("\\s+");
    for(int i = 0; i < tmp.length; i ++){
      if(counts.containsKey(tmp[i]))
          counts.put(tmp[i],counts.get(tmp[i])+1);
      else
         counts.put(tmp[i],1);
    }
    for(int i =0 ; i < query.size(); i ++){
      if(query_count.containsKey(query.get(i)))
         query_count.put(query.get(i),query_count.get(query.get(i))+1);
    
     else query_count.put(query.get(i),1);
   }
   for(Map.Entry<String,Integer> entry:query_count.entrySet()){
        String token = entry.getKey();
        if(counts.containsKey(token)){
          sum += entry.getValue() * counts.get(token);
        }
        query_mold += entry.getValue() * entry.getValue();

   }
   for(Map.Entry<String,Integer> entry:counts.entrySet()){
        
        title_mold += entry.getValue() * entry.getValue();

   }

   double res = (double)sum / Math.sqrt(query_mold) * Math.sqrt(title_mold);
   return res;



  }

  private String convertToString(Vector<String> v){
       StringBuilder sb = new StringBuilder();
       for(String s : v)
         sb.append(s);
       return(sb.toString());
  }

//need an actually equation
  private double getQScore(double revelance, double CTR){
    return (0.3*revelance + 0.7*CTR);

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

package edu.nyu.cs.cs2580;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.Writer;
import java.io.Reader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.Vector;
import java.io.File;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.Map.Entry;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.io.FileNotFoundException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.ArrayList;

import edu.nyu.cs.cs2580.SearchEngine.Options;

/**
 * @CS2580: Implement this class for HW2.
 */
public class AdsIndex extends Indexer implements Serializable{
  private static final long serialVersionUID = 1;

  private Map<Character,Map<String,Vector<Integer>>> _charMapList = new HashMap<Character,Map<String,Vector<Integer>>>();

  private Map<String, Integer> _termCorpusDoc = new HashMap<String, Integer>();

  private Map<String, Vector<Integer>> _cacheList = new HashMap<String, Vector<Integer>>();

  //Maps each term to their index list, index list contains adid only
  private Map<String, Vector<Integer>> _indexList = new HashMap<String, Vector<Integer>>();
  //Maps adid with its Advertisement instance
  private Map<String, Integer> _termCorpusFreq = new HashMap<String, Integer>();
  //stopwords set
  private Set<String> stopwords = new HashSet<String>();
  //raw Advertisement
  private Vector<Advertisement> _ads = new Vector<Advertisement>();

  private Vector<Advertisement> _fullAds = new Vector<Advertisement>();
  
  final int DOC_WRITE_SIZE = 500;
  final int maxFileNum = 1000;

  private static int docNum = 0;
  private static int docFileId = 0;

  private Stemming stemming = new Stemming();

  //map company_ads to adid
  private Map<String,Integer> _docIDs = new HashMap<String,Integer>();

   // Provided for serialization
  public AdsIndex() { 
  }

  public AdsIndex(Options options) {
    super(options);
    StopWord  stopWord = new StopWord();
    stopwords = stopWord.getStopWords();
    System.out.println("Using Indexer: " + this.getClass().getSimpleName());
  }

  public Vector<Advertisement> getDocuments(){
    return this._ads;
  }

  //temp storage for Advertisement
  //private HashMap<Integer,Advertisement>_docCache = new HashMap<Integer,Advertisement>(50);

  @Override

  public void constructIndex() throws IOException{

    int fileCount = 0;
   // File corpusDirectory = new File(_options._corpusPrefix);
    String corpusFile = _options._adsPrefix + "/advertisement.tsv";
    System.out.println("Construct Advertisement index from: " + corpusFile);      

    BufferedReader reader = new BufferedReader(new FileReader(corpusFile));
    try {
      String line = null;
      while ((line = reader.readLine()) != null) {
        processDocument(fileCount, line);
        fileCount++;
        if(fileCount>0&&fileCount%maxFileNum==0){
          writeFile(_charMapList);
          _charMapList.clear();
        }
       
        if(fileCount>0&&fileCount%DOC_WRITE_SIZE==0){
          docFileId++;
          saveDocList(docFileId);
        }
      }
    } finally {
      reader.close();
    }

    if(!_charMapList.isEmpty()){
      writeFile(_charMapList);
      _charMapList.clear();
    }

    mergeAll();
    for (int freq : _termCorpusFreq.values()) {
      this._totalTermFrequency += freq;
    }
    _docIDs.put("_numDocs", fileCount);    
    _docIDs.put("_totalTermFrequency", (int)_totalTermFrequency);

    System.out.println("Indexed " + fileCount + " docs with " + Long.toString(this._totalTermFrequency) + " terms.");
    saveCompanyAds();
    saveDocList(++docFileId);
    saveDocuments();
    saveTermsFreq();
    saveTermsDoc();
    saveIndex();
    //testWrite();
    System.gc();
    System.out.println("finish write");
  }

  //process each document for all terms it conains
  private void processDocument(int id, String content) { 
    int adid = id;  
    Advertisement Advertisement = new Advertisement(adid);
    Advertisement fullDoc = new Advertisement(adid);
    HashMap<String,Integer> map = new HashMap<String, Integer>();
    HashSet<String> set = new HashSet<String>();   
    Scanner s = new Scanner(content).useDelimiter("\t");
    String company_ads = s.next();
    String title = s.next(); 
    String url = title; 
    String body = s.next();
    Advertisement.setCompany_ads(company_ads); 
    Advertisement.setTitle(title);
    Advertisement.setUrl(url);
    int a = readTerms(title, adid, map, set);
    int b = readTerms(body,adid, map, set);
    Advertisement.setTotalTerms(a+b);
    Advertisement.setDocID(adid);
    fullDoc.setDocID(adid);
    fullDoc.setTerms(map);
    //Advertisement.setTerms(map);
    _ads.add(Advertisement);
    _fullAds.add(fullDoc);
    
    if(_docIDs.containsKey(company_ads)){
      System.out.println(company_ads + " is already existed in map w id " + _docIDs.get(company_ads));
      System.exit(1);
    }
    _docIDs.put(company_ads,adid);
  }

  //the input is title and body content. For each term, stem first, and then add to indexedlist, each adid will be added only once
  //also need to update term corpus freqeuncy 
  private int readTerms(String str, int adid, HashMap<String, Integer> map, HashSet<String> set){

    Scanner scan = new Scanner(str);  // Uses white space by default.
    int total = 0;
    while (scan.hasNext()) {
      String token = scan.next();
      String s = stemming.stem(token);
      s = s.toLowerCase();
      if(stopwords.contains(s)){
        continue;
      }
      if(_termCorpusFreq.containsKey(s)){
        int termFreq = _termCorpusFreq.get(s)+1;
        _termCorpusFreq.put(s,termFreq);
      }else{
        _termCorpusFreq.put(s,1);
      }
      if(map.containsKey(s)){
        map.put(s, map.get(s)+1);
      }else{
        map.put(s, 1);
      }

      if(!set.contains(s)){
        if(_termCorpusDoc.containsKey(s)){
          _termCorpusDoc.put(s,_termCorpusDoc.get(s)+1);
        }else{
          _termCorpusDoc.put(s,1);
        }    
      }
      if(s.length()>0){
        Character c = s.charAt(0);
        if(_charMapList.containsKey(c)){
          Map<String,Vector<Integer>> tempMap = _charMapList.get(c);
           
          if(tempMap.containsKey(s)){
             Vector<Integer> vector = tempMap.get(s);
             int size = vector.size();

            if(adid != vector.get(size-2)){  // first time occurs in this Advertisement
              
              vector.add(adid);
              vector.add(1);
              tempMap.put(s,vector);
            }
            else{
                   vector.set(size-1,vector.get(size-1)+1);

            }
          }else{
            tempMap.put(s,new Vector<Integer>());
            Vector<Integer> vector = tempMap.get(s);
            vector.add(adid);
            vector.add(1);
            tempMap.put(s,vector);
          }
          _charMapList.put(c,tempMap);
        }else{
          Map<String,Vector<Integer>> tempMap = new HashMap<String,Vector<Integer>>();
          tempMap.put(s,new Vector<Integer>());
          Vector<Integer> vector = tempMap.get(s);
          vector.add(adid);
          vector.add(1);
          tempMap.put(s,vector);
          _charMapList.put(c,tempMap);
        }
      }
      total++;
    }
    return total;
    
  }

  public void writeFile(Map<Character, Map<String, Vector<Integer>>> charMapList) throws IOException {
    for (Map.Entry<Character, Map<String, Vector<Integer>>> entry : charMapList.entrySet()) {
      String path = _options._adsIndexPrefix + "/" + entry.getKey() + ".idx";
      File file = new File(path);
      BufferedWriter write = new BufferedWriter(new FileWriter(file, true));
      Map<String, Vector<Integer>> tempMap = entry.getValue();
      for(Map.Entry<String,Vector<Integer>> entryString : tempMap.entrySet()){
        String word = entryString.getKey();
        Vector<Integer> vector = entryString.getValue();
        write.write(word);
        StringBuffer sb = new StringBuffer();
        sb.append(":");
        Iterator it = vector.iterator();
        while(it.hasNext()){
          sb.append(String.valueOf(it.next())).append(",");
        }
        write.write(sb.toString());
        write.write("\n");
      }
      write.close();
    }
  }

  public void mergeAll() throws IOException{
    File indexDirectory = new File(_options._adsIndexPrefix);
    if(indexDirectory.isDirectory()){
      File[] indexes = indexDirectory.listFiles();
      for(int i=0;i<indexes.length;i++){
        String fileName = indexes[i].getName();
        if(fileName.endsWith(".idx")){
          System.out.println("Merging ... " + fileName);
          Map<Character, Map<String, Vector<Integer>>> charMapList = readAll(fileName);
          String filePath = _options._adsIndexPrefix + "/" + fileName;
          File charFile = new File(filePath);
          charFile.delete();
          writeFile(charMapList);
        }
      }
    }
  }

  public Map<Character, Map<String, Vector<Integer>>> readAll(String fileName) throws IOException{
    String file = _options._adsIndexPrefix + "/" + fileName;
    Scanner scan = new Scanner(new File(file));
    Map<Character, Map<String, Vector<Integer>>> charMapList = new HashMap<Character, Map<String, Vector<Integer>>>();
    Map<String, Vector<Integer>> tempMap = new HashMap<String, Vector<Integer>>();
    while(scan.hasNextLine()){
      String line = scan.nextLine();
      String lineArray[] = line.split(":");
      String word = lineArray[0];
      Vector<Integer> vector = new Vector<Integer>();
      String[] tempSt = lineArray[lineArray.length-1].split(",");
      for(int i=0;i<tempSt.length;i++){
        if(tempSt[i]!=null&&tempSt[i].length()!=0&&!tempSt[i].equals("\n")){
          vector.add(Integer.parseInt(tempSt[i]));
        }       
      }
      if (tempMap.containsKey(word)) {
        Vector<Integer> temp = tempMap.get(word);
        temp.addAll(vector);
        tempMap.put(word, temp);
      } else {
        tempMap.put(word, vector);
      }
      if(tempMap.get(word).size()>2000){
        _cacheList.put(word,tempMap.get(word));
      }
    }
    charMapList.put(fileName.charAt(0), tempMap);
    return charMapList;
  }


  public void saveDocList(int id) throws IOException{
    String name = _options._adsIndexPrefix+ "/docList" + id;
    Writer writer = new OutputStreamWriter(new FileOutputStream(name));
    Gson gson = new GsonBuilder().create();
    gson.toJson(_fullAds, writer);
    writer.close();
    _fullAds.clear();
  }

  public void saveCompanyAds() throws IOException{
    String name = _options._adsIndexPrefix+ "/docURL";
    Writer writer = new OutputStreamWriter(new FileOutputStream(name));
    Gson gson = new GsonBuilder().create();
    gson.toJson(_docIDs, writer);
    writer.close();
    System.out.println("befroe write, company_ads maps has " + _docIDs.size() + "entries");
    _docIDs.clear();
  }

  public void saveTermsFreq() throws IOException{
    String name = _options._adsIndexPrefix+ "/TermCorps";
    Writer writer = new OutputStreamWriter(new FileOutputStream(name));
    Gson gson = new GsonBuilder().create();
    gson.toJson(_termCorpusFreq, writer);
    writer.close();
    _termCorpusFreq.clear();
  }

  public void saveTermsDoc() throws IOException{
    String name = _options._adsIndexPrefix+ "/TermDoc";
    Writer writer = new OutputStreamWriter(new FileOutputStream(name));
    Gson gson = new GsonBuilder().create();
    gson.toJson(_termCorpusDoc, writer);
    writer.close();
    _termCorpusDoc.clear();
  }

  public void saveIndex() throws IOException{
    String name = _options._adsIndexPrefix+ "/cache";
    Writer writer = new OutputStreamWriter(new FileOutputStream(name));
    Gson gson = new GsonBuilder().create();
    gson.toJson(_cacheList, writer);
    writer.close();
    _cacheList.clear();
  }
  public void saveDocuments() throws IOException{
    String name = _options._adsIndexPrefix+ "/allDocuments";
    Writer writer = new OutputStreamWriter(new FileOutputStream(name));
    Gson gson = new GsonBuilder().create();
    gson.toJson(_ads, writer);
    writer.close();
    _ads.clear();
  }
/*
  public void testWrite() throws IOException{
    Map<String, String> t = new HashMap<String, String>();
    t.put("ScienceNews.org", "1   7");
    t.put("ScienceChannel", "1  10");
    t.put("nyuskirball.org", "1  3");
    Map<String, Map<String, String>> temp = new HashMap<String, Map<String, String>>();
    temp.put("science", t);
    String name = _options._adsPrefix+ "/ad.json";
    Writer writer = new OutputStreamWriter(new FileOutputStream(name));
    Gson gson = new GsonBuilder().create();
    gson.toJson(temp, writer);
    writer.close();
  }
*/
  @Override
  public void loadIndex() throws IOException, ClassNotFoundException {
    Gson gson = new Gson();
    String terms = _options._adsIndexPrefix+ "/TermCorps";
    Reader reader = new InputStreamReader(new FileInputStream(terms));
    Map<String, Integer> termCorpusFreq = gson.fromJson(reader,
                      new TypeToken<Map<String, Integer>>() {}.getType()); 
    System.out.println("term corp map has entryset " + termCorpusFreq.entrySet().size());
    reader.close();

    terms = _options._adsIndexPrefix+ "/TermDoc";
    reader = new InputStreamReader(new FileInputStream(terms));
    Map<String, Integer> termCorpusDoc = gson.fromJson(reader,
                      new TypeToken<Map<String, Integer>>() {}.getType()); 
    reader.close();
    terms = _options._adsIndexPrefix+ "/allDocuments";
    reader = new InputStreamReader(new FileInputStream(terms));
    Vector<Advertisement> loaded_documents = gson.fromJson(reader,
                      new TypeToken<Vector<Advertisement>>() {}.getType()); 
    reader.close();

    String cache = _options._adsIndexPrefix+ "/cache";
    reader = new InputStreamReader(new FileInputStream(cache));
    Map<String, Vector<Integer>> loaded_cache = gson.fromJson(reader,
                      new TypeToken<Map<String, Vector<Integer>>>() {}.getType()); 
    System.out.println("cache has entryset " + loaded_cache.entrySet().size());
    reader.close();

    String urls = _options._adsIndexPrefix+ "/docURL";
    reader = new InputStreamReader(new FileInputStream(urls));
    Map<String, Integer> loaded_docIDs = gson.fromJson(reader,
                      new TypeToken<Map<String, Integer>>() {}.getType()); 
    System.out.println("comapny_ads map has entryset " + loaded_docIDs.entrySet().size());
    reader.close();

    /*
    String cachePath = _options._adsIndexPrefix+ "/cache.idx"; 
    reader = new InputStreamReader(new FileInputStream(cachePath));
    Map<String, Vector<Integer>> cacheList = gson.fromJson(reader,
                      new TypeToken<Map<String, Vector<Integer>>>() {}.getType());
    reader.close();
    System.out.println("cache index map has entryset " + cacheList.entrySet().size());
    */

    String docPath = _options._adsIndexPrefix+ "/docList1"; 
    //reader = new InputStreamReader(new FileInputStream(docPath));
    //Vector<Advertisement> Advertisement  = gson.fromJson(reader,
                     // new TypeToken<Vector<Advertisement>>() {}.getType()); 
    /*
    FileInputStream fis = new FileInputStream(docPath);
    ObjectInputStream ois = new ObjectInputStream(fis); 
    @SuppressWarnings("unchecked")
    Vector<Advertisement> Advertisement = (Vector<Advertisement>)ois.readObject();   
    ois.close();
    System.out.println("docList1  has " + Advertisement.size() + " docs");
    //reader.close();
    */

    this._numDocs = loaded_docIDs.get("_numDocs");
    System.out.println("removed two values from _docIDs");
    //loaded_docIDs.remove("_numDocs");      
    this._totalTermFrequency = loaded_docIDs.get("_totalTermFrequency");
    //loaded_docIDs.remove("_totalTermFrequency");

    this._docIDs = loaded_docIDs;
    this._termCorpusFreq = termCorpusFreq;
    this._termCorpusDoc = termCorpusDoc;
    //this._cacheList = cacheList;
    //this._ads = Advertisement;
    this._cacheList = loaded_cache;
    this._ads = loaded_documents;

    System.out.println(Integer.toString(_numDocs) + " Advertisement loaded with " + Long.toString(_totalTermFrequency) + " terms!");
    System.out.println("loading, docNum is  " +docNum);
  }


  @Override
  public edu.nyu.cs.cs2580.Document getDoc(int adid) {
    if(adid >= _numDocs || adid < 0) {
      return null;
    }
    docNum = adid/DOC_WRITE_SIZE;
    try{
      Gson gson = new Gson();
      String docPath = _options._adsIndexPrefix+ "/docList"+(docNum+1); 
      Reader reader = new InputStreamReader(new FileInputStream(docPath));
      reader = new InputStreamReader(new FileInputStream(docPath));
      _ads  = gson.fromJson(reader,
                        new TypeToken<Vector<Advertisement>>() {}.getType()); 
      reader.close();
    }catch (Exception e){
      e.printStackTrace();
    }
    adid = adid%DOC_WRITE_SIZE;
    return _ads.get(adid);
  }

  @Override
  public edu.nyu.cs.cs2580.Document nextDoc(Query query, int id) {

    String company = query._query;
    String company_ads = company+"_"+id;
    int adid  = _docIDs.get(company_ads);
    return _ads.get(adid);
  }
    
  /**
   *find next document id for given conjunctive query which might contain multiple words, return -1 if there is no next Advertisement
   */
  /*
  private int nextQueryDocId(Query query, int adid){
    if(query._tokens.size()==1){
      return nextTermDocId(query._tokens.get(0),adid);
    }else if(query._tokens.size() > 1){
      int count = query._tokens.size();
      int[] docs = new int[count];
      for(int i = 0; i<count; i++){
        docs[i] = nextTermDocId(query._tokens.get(i),adid);
        if(docs[i] == -1){
          return -1;
        }
      }
      if(isEqual(docs)){
        return docs[0];
      }else{
        int max = findMaxId(docs);
        return nextQueryDocId(query,max-1);
      }
    }
    return -1;
  }

  //check if docids returned for all query terms are same 
  private boolean isEqual(int[] numbers){
    if(numbers.length > 1){
      for(int i =0; i<numbers.length-1; i++){
        if(numbers[i]!=numbers[i+1]){
          return false;
        }
      }
    }
    return true;
  }

  private int findMaxId(int[] numbers){
    if(numbers.length!=0){
      int size = numbers.length;
      int max = numbers[0];
      for(int i = 1; i<size; i++){
        if(numbers[i]>max){
          max = numbers[i];
        }
      }
      return max;
    }
    return -1;
  }

  /**
   * find the next document id for given single term/word, return -1 if there is no next Advertisement
   */ 
  private int nextTermDocId(String term, int adid){
    if(_cacheList.containsKey(term)){
      Vector<Integer> list = _cacheList.get(term);
      /*if(!list.isEmpty() && adid < list.get(list.size()-2)){
        if(list.get(0) > adid){
          return list.get(0);
        }
        int left = 0;
        int right = list.size();
        while(right - left > 1){
          int mid = (left+right) / 2;
          if(list.get(mid)<=adid){
            left = mid;
          }else{
            right = mid;
          }
        }
        return list.get(right);      
      }*/
      if(!list.isEmpty()){
        for(int i = 0 ; i < list.size(); i+=2){
           if(list.get(i)>adid)
              return list.get(i);

        }
          
      }
      return -1;
    }
    if(!_indexList.containsKey(term)){
      String fileName = _options._adsIndexPrefix+"/"+term.charAt(0)+".idx";
      Vector<Integer> result = findTerm(term,fileName);
      if(result==null){
        return -1;
      }
      if(_indexList.size()>1000){
        _indexList.clear();
      }
      _indexList.put(term,result);
    }
    if(_indexList.containsKey(term)){
      Vector<Integer> list = _indexList.get(term);
      for(int i = 0; i < list.size(); i+=2){
        if(list.get(i)>adid)
          return list.get(i);
      }
    }
    return -1;
  }

  public Vector<Integer> findTerm(String term, String fileName) {
    String cmd = "grep -w "+term+" "+fileName;
    Process p;
    Vector<Integer> vector = new Vector<Integer>();
    try{
      p = Runtime.getRuntime().exec(cmd);
      p.waitFor();
      BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
      String line = br.readLine();
      if(line==null||line.length()==0){
        return null;
      }
      Map<String, Vector<Integer>> res = new HashMap<String, Vector<Integer>>();
      String[] arr = line.split(":");
      
      String[] tempSt = arr[arr.length-1].split(",");
      for(int i=0;i<tempSt.length;i++){
        if(tempSt[i]!=null&&tempSt[i].length()!=0&&!tempSt[i].equals("\n")){
          vector.add(Integer.parseInt(tempSt[i]));
        }       
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    return vector;
  }
  
  @Override
  public int corpusDocFrequencyByTerm(String term) {
    if(_cacheList.containsKey(term)){
      return _cacheList.get(term).size()/2;
    }
    return _termCorpusDoc.containsKey(term) ? _termCorpusDoc.get(term) : 0;
  }

  @Override
  public int corpusTermFrequency(String term) {
    return _termCorpusFreq.containsKey(term) ? _termCorpusFreq.get(term) : 0;
  }
 
 /*
  @Override
  public int documentTermFrequency(String term, String url) {
    int adid = _docIDs.get(url);
   // System.out.println("in documentTermFreq, adid is " + adid + " w url "  + url);
    try{
      Advertisement d = null;
      if(_docCache.containsKey(adid)){
        d = _docCache.get(adid);
      }else{
        if((adid/DOC_WRITE_SIZE)!=docNum){
          d = fectchDoc(adid);
        }else{
          adid = adid%DOC_WRITE_SIZE;
          d = _ads.get(adid);
        }
        if(_docCache.size() >= 50){
          _docCache.clear(); 
        }        
        _docCache.put(d.getDocID(), d);
      }     
      if(d!=null){
        if(d.getTerms().containsKey(term)){
          return d.getTerms().get(term);
        }else{
          return 0;
        }
      }
    }catch(Exception e){
      e.printStackTrace();
    }     
    return 0;
  }
  */
  public int documentTermFrequency(String term, int adid) {
    //int adid = _docIDs.get(url);
   // System.out.println("in documentTermFreq, adid is " + adid + " w url "  + url);
    
      if(_cacheList.containsKey(term)){
        Vector<Integer> list = _cacheList.get(term);
        for(int i = 0; i < list.size(); i+=2){
          if(list.get(i) == adid)
             return list.get(i+1);
        }
        return 0;
      }
      if(!_indexList.containsKey(term)){
      String fileName = _options._adsIndexPrefix+"/"+term.charAt(0)+".idx";
      Vector<Integer> result = findTerm(term,fileName);
      if(result==null){
        return 0;
      }
      if(_indexList.size()>1000){
        _indexList.clear();
      }
      _indexList.put(term,result);
    }
    if(_indexList.containsKey(term)){
      Vector<Integer> list = _indexList.get(term);
      for(int i = 0; i < list.size(); i+=2){
        if(list.get(i)==adid)
          return list.get(i+1);
      }
    }
        
    return 0;
  }

  private   String  myGetKey(HashMap<String,Integer> map,  Integer  value)
 {
  for(String key:map.keySet())
   if(map.get(key).equals(value))
    return key;
  return null;
 }

  
  @SuppressWarnings("unchecked")
  private Advertisement fectchDoc(int adid) {
    if(adid >= _numDocs || adid < 0) {
      return null;
    }
    docNum = adid/DOC_WRITE_SIZE;
    //System.out.println("fetching docs, the new adid is " + adid);
    //System.out.println("fetching docs, the new docNum is " + docNum);
    try{      
      _ads.clear();      
      System.gc();
      //Gson gson = new Gson();
      String docPath = _options._adsIndexPrefix+ "/docList"+(docNum+1); 

      FileInputStream fis = new FileInputStream(docPath);
      ObjectInputStream ois = new ObjectInputStream(fis); 

      _ads = (Vector<Advertisement>)ois.readObject();   
      //Reader reader = new InputStreamReader(new FileInputStream(docPath));
      //reader = new InputStreamReader(new FileInputStream(docPath));


     //Thread.sleep(1000);
     // _ads  = gson.fromJson(reader,
                       // new TypeToken<Vector<Advertisement>>() {}.getType()); 

     // reader.close();
      ois.close();
    }catch (Exception e){
      e.printStackTrace();
    }
    adid = adid%DOC_WRITE_SIZE;
    return _ads.get(adid);
  }
 }   
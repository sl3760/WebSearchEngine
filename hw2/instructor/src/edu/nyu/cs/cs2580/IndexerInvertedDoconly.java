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

import edu.nyu.cs.cs2580.SearchEngine.Options;

/**
 * @CS2580: Implement this class for HW2.
 */
public class IndexerInvertedDoconly extends Indexer implements Serializable{
  private static final long serialVersionUID = 1;

  private Map<Character,Map<String,Vector<Integer>>> _charMapList = new HashMap<Character,Map<String,Vector<Integer>>>();

  private Map<String, Integer> _termCorpusDoc = new HashMap<String, Integer>();

  private Map<String, Vector<Integer>> _cacheList = new HashMap<String, Vector<Integer>>();

  //Maps each term to their index list, index list contains docId only
  private Map<String, Vector<Integer>> _indexList = new HashMap<String, Vector<Integer>>();
  //Maps docid with its documentindexed instance
  private Map<String, Integer> _termCorpusFreq = new HashMap<String, Integer>();
  //stopwords set
  private Set<String> stopwords = new HashSet<String>();
  //raw documents
  private Vector<DocumentIndexed> _documents = new Vector<DocumentIndexed>();
  
  final int DOC_WRITE_SIZE = 500;
  final int maxFileNum = 1000;

  private int docNum = 0;
  private int docFileId = 0;

  private Stemming stemming = new Stemming();

  //map url to docid
  private Map<String,Integer> _docIDs = new HashMap<String,Integer>();

   // Provided for serialization
  public IndexerInvertedDoconly() { }

  public IndexerInvertedDoconly(Options options) {
    super(options);
    System.out.println("Using Indexer: " + this.getClass().getSimpleName());
  }

  //temp storage for documents
  private HashMap<Integer,DocumentIndexed>_docCache = new HashMap<Integer,DocumentIndexed>(50);

  @Override

  public void constructIndex() throws IOException{

    int fileCount = 0;
    File corpusDirectory = new File(_options._corpusPrefix);
    System.out.println("Construct index from: " + corpusDirectory);      
    if(corpusDirectory.isDirectory()){
      for(File corpusFile :corpusDirectory.listFiles()){  
        org.jsoup.nodes.Document doc = Jsoup.parse(corpusFile, "UTF-8");
        String title = doc.title().replaceAll(" - Wikipedia, the free encyclopedia", "");
        String body = doc.body().text().replace(title + " From Wikipedia, the free encyclopedia Jump to: navigation, search ", 
            " ").replaceAll("[^a-zA-Z0-9]", " ");
        processDocument(fileCount, title, body);
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
    saveDocURL();
    saveDocList(++docFileId);
    saveTermsFreq();
    saveTermsDoc();
    saveIndex();
    System.gc();
    System.out.println("finish write");
  }

  //process each document for all terms it conains
  private void processDocument(int docID, String title, String body) { 
    int docid = docID;  
    DocumentIndexed doc = new DocumentIndexed(docid);
    HashMap<String,Integer> map = new HashMap<String, Integer>();
    HashSet<String> set = new HashSet<String>();
    readTerms(title, docid, map, set);
    String url = title;
    readTerms(body,docid, map, set);
    doc.setTitle(title);
    doc.setUrl(url);
    doc.setTerms(map);
    _documents.add(doc);
    doc.setDocID(docid);
    _docIDs.put(url,docid);
  }

  //the input is title and body content. For each term, stem first, and then add to indexedlist, each docid will be added only once
  //also need to update term corpus freqeuncy 
  private void readTerms(String str, int docid, HashMap<String, Integer> map, HashSet<String> set){
    Scanner scan = new Scanner(str);  // Uses white space by default.
    while (scan.hasNext()) {
      String token = scan.next();
      String s = stemming.stem(token);
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
            if(docid!=tempMap.get(s).lastElement()){
              Vector<Integer> vector = tempMap.get(s);
              vector.add(docid);
              tempMap.put(s,vector);
            }
          }else{
            tempMap.put(s,new Vector<Integer>());
            Vector<Integer> vector = tempMap.get(s);
            vector.add(docid);
            tempMap.put(s,vector);
          }
          _charMapList.put(c,tempMap);
        }else{
          Map<String,Vector<Integer>> tempMap = new HashMap<String,Vector<Integer>>();
          tempMap.put(s,new Vector<Integer>());
          Vector<Integer> vector = tempMap.get(s);
          vector.add(docid);
          tempMap.put(s,vector);
          _charMapList.put(c,tempMap);
        }
      }
    }
  }

  public void writeFile(Map<Character, Map<String, Vector<Integer>>> charMapList) throws IOException {
    for (Map.Entry<Character, Map<String, Vector<Integer>>> entry : charMapList.entrySet()) {
      String path = _options._indexPrefix + "/" + entry.getKey() + ".idx";
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
    File indexDirectory = new File(_options._indexPrefix);
    if(indexDirectory.isDirectory()){
      File[] indexes = indexDirectory.listFiles();
      for(int i=0;i<indexes.length;i++){
        String fileName = indexes[i].getName();
        if(fileName.endsWith(".idx")){
          System.out.println("Merging ... " + fileName);
          Map<Character, Map<String, Vector<Integer>>> charMapList = readAll(fileName);
          String filePath = _options._indexPrefix + "/" + fileName;
          File charFile = new File(filePath);
          charFile.delete();
          writeFile(charMapList);
        }
      }
    }
  }

  public Map<Character, Map<String, Vector<Integer>>> readAll(String fileName) throws IOException{
    String file = _options._indexPrefix + "/" + fileName;
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
      if(tempMap.get(word).size()>100){
        _cacheList.put(word,tempMap.get(word));
      }
    }
    charMapList.put(fileName.charAt(0), tempMap);
    return charMapList;
  }

  public void saveDocList(int id) throws IOException{
    String name = _options._indexPrefix+ "/docList" + id;
    Writer writer = new OutputStreamWriter(new FileOutputStream(name));
    Gson gson = new GsonBuilder().create();
    gson.toJson(_documents, writer);
    writer.close();
    _documents.clear();
  }

  public void saveDocURL() throws IOException{
    String name = _options._indexPrefix+ "/docURL";
    Writer writer = new OutputStreamWriter(new FileOutputStream(name));
    Gson gson = new GsonBuilder().create();
    gson.toJson(_docIDs, writer);
    writer.close();
    _docIDs.clear();
  }

  public void saveTermsFreq() throws IOException{
    String name = _options._indexPrefix+ "/TermCorps";
    Writer writer = new OutputStreamWriter(new FileOutputStream(name));
    Gson gson = new GsonBuilder().create();
    gson.toJson(_termCorpusFreq, writer);
    writer.close();
    _termCorpusFreq.clear();
  }

  public void saveTermsDoc() throws IOException{
    String name = _options._indexPrefix+ "/TermDoc";
    Writer writer = new OutputStreamWriter(new FileOutputStream(name));
    Gson gson = new GsonBuilder().create();
    gson.toJson(_termCorpusDoc, writer);
    writer.close();
    _termCorpusDoc.clear();
  }

  public void saveIndex() throws IOException{
    String name = _options._indexPrefix+ "/cache.idx";
    Writer writer = new OutputStreamWriter(new FileOutputStream(name));
    Gson gson = new GsonBuilder().create();
    gson.toJson(_cacheList, writer);
    writer.close();
    _cacheList.clear();
  }

  @Override
  public void loadIndex() throws IOException, ClassNotFoundException {
    Gson gson = new Gson();
    String terms = _options._indexPrefix+ "/TermCorps";
    Reader reader = new InputStreamReader(new FileInputStream(terms));
    Map<String, Integer> termCorpusFreq = gson.fromJson(reader,
                      new TypeToken<Map<String, Integer>>() {}.getType()); 
    System.out.println("term corp map has entryset " + termCorpusFreq.entrySet().size());
    reader.close();

    terms = _options._indexPrefix+ "/TermDoc";
    reader = new InputStreamReader(new FileInputStream(terms));
    Map<String, Integer> termCorpusDoc = gson.fromJson(reader,
                      new TypeToken<Map<String, Integer>>() {}.getType()); 
    reader.close();

    String urls = _options._indexPrefix+ "/docURL";
    reader = new InputStreamReader(new FileInputStream(urls));
    Map<String, Integer> loaded_docIDs = gson.fromJson(reader,
                      new TypeToken<Map<String, Integer>>() {}.getType()); 
    System.out.println("url map has entryset " + loaded_docIDs.entrySet().size());
    reader.close();

    String cachePath = _options._indexPrefix+ "/cache.idx"; 
    reader = new InputStreamReader(new FileInputStream(cachePath));
    Map<String, Vector<Integer>> cacheList = gson.fromJson(reader,
                      new TypeToken<Map<String, Vector<Integer>>>() {}.getType());
    reader.close();
    System.out.println("cache index map has entryset " + cacheList.entrySet().size());

    String docPath = _options._indexPrefix+ "/docList1"; 
    reader = new InputStreamReader(new FileInputStream(docPath));
    Vector<DocumentIndexed> documents  = gson.fromJson(reader,
                      new TypeToken<Vector<DocumentIndexed>>() {}.getType()); 
    System.out.println("docList1  has " + documents.size() + " docs");
    reader.close();

    this._numDocs = loaded_docIDs.get("_numDocs");
    //System.out.println("removed two values from _docIDs");
    //loaded_docIDs.remove("_numDocs");      
    this._totalTermFrequency = loaded_docIDs.get("_totalTermFrequency");
    //loaded_docIDs.remove("_totalTermFrequency");

    this._docIDs = loaded_docIDs;
    this._termCorpusFreq = termCorpusFreq;
    this._termCorpusDoc = termCorpusDoc;
    this._cacheList = cacheList;
    this._documents = documents;

    System.out.println(Integer.toString(_numDocs) + " documents loaded with " + Long.toString(_totalTermFrequency) + " terms!");
  }


  @Override
  public edu.nyu.cs.cs2580.Document getDoc(int docid) {
    if(docid >= _numDocs || docid < 0) {
      return null;
    }
    docNum = docid/DOC_WRITE_SIZE;
    try{
      Gson gson = new Gson();
      String docPath = _options._indexPrefix+ "/docList"+(docNum+1); 
      Reader reader = new InputStreamReader(new FileInputStream(docPath));
      reader = new InputStreamReader(new FileInputStream(docPath));
      _documents  = gson.fromJson(reader,
                        new TypeToken<Vector<DocumentIndexed>>() {}.getType()); 
      reader.close();
    }catch (Exception e){
      e.printStackTrace();
    }
    docid = docid%DOC_WRITE_SIZE;
    return _documents.get(docid);
  }

  /**
   * In HW2, you should be using {@link DocumentIndexed}
   * need to differentiate query and phrase query
   */
  @Override
  public edu.nyu.cs.cs2580.Document nextDoc(Query query, int docid) {
    try {
      int nextDocid = nextQueryDocId(query,docid); 
      if(nextDocid > -1){ 
        if(_docCache.containsKey(nextDocid)){
          return _docCache.get(nextDocid);
        }else{
          DocumentIndexed d = null;
          if(nextDocid/DOC_WRITE_SIZE!=docNum){
            d = fectchDoc(nextDocid);
          }else{
            nextDocid = nextDocid%DOC_WRITE_SIZE;
            d = _documents.get(nextDocid);
          }
          if(_docCache.size() == 50){
            _docCache.clear(); 
          }        
          _docCache.put(d.getDocID(), d);
          return d;
        }      
      }
    }catch (Exception e) {
      e.printStackTrace();
    } 
    return null; 
  }
    
  /**
   *find next document id for given conjunctive query which might contain multiple words, return -1 if there is no next doc
   */
  private int nextQueryDocId(Query query, int docid){
    if(query._tokens.size()==1){
      return nextTermDocId(query._tokens.get(0),docid);
    }else if(query._tokens.size() > 1){
      int count = query._tokens.size();
      int[] docs = new int[count];
      for(int i = 0; i<count; i++){
        docs[i] = nextTermDocId(query._tokens.get(i),docid);
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
   * find the next document id for given single term/word, return -1 if there is no next doc
   */ 
  private int nextTermDocId(String term, int docid){
    if(_cacheList.containsKey(term)){
      Vector<Integer> list = _cacheList.get(term);
      if(!list.isEmpty() && docid < list.lastElement()){
        if(list.get(0) > docid){
          return list.get(0);
        }
        int left = 0;
        int right = list.size();
        while(right - left > 1){
          int mid = (left+right) / 2;
          if(list.get(mid)<=docid){
            left = mid;
          }else{
            right = mid;
          }
        }
        return list.get(right);      
      }
    }
    if(!_indexList.containsKey(term)){
      String fileName = _options._indexPrefix+"/"+term.charAt(0)+".idx";
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
      if(!list.isEmpty() && docid < list.lastElement()){
        if(list.get(0) > docid){
          return list.get(0);
        }
        int left = 0;
        int right = list.size();
        while(right - left > 1){
          int mid = (left+right) / 2;
          if(list.get(mid)<=docid){
            left = mid;
          }else{
            right = mid;
          }
        }
        return list.get(right);      
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
      return _cacheList.get(term).size();
    }
    return _termCorpusDoc.containsKey(term) ? _termCorpusDoc.get(term) : 0;
  }

  @Override
  public int corpusTermFrequency(String term) {
    return _termCorpusFreq.containsKey(term) ? _termCorpusFreq.get(term) : 0;
  }
 
  @Override
  public int documentTermFrequency(String term, String url) {
    int docid = _docIDs.get(url);
    try{
      DocumentIndexed d = null;
      if(_docCache.containsKey(docid)){
        d = _docCache.get(docid);
      }else{
        if(docid/DOC_WRITE_SIZE!=docNum){
          d = fectchDoc(docid);
        }else{
          docid = docid%DOC_WRITE_SIZE;
          d = _documents.get(docid);
        }
        if(_docCache.size() == 50){
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
  
  private DocumentIndexed fectchDoc(int docid) {
    if(docid >= _numDocs || docid < 0) {
      return null;
    }
    docNum = docid/DOC_WRITE_SIZE;
    try{
      Gson gson = new Gson();
      String docPath = _options._indexPrefix+ "/docList"+(docNum+1); 
      Reader reader = new InputStreamReader(new FileInputStream(docPath));
      reader = new InputStreamReader(new FileInputStream(docPath));
      _documents.clear();
      System.gc();
      _documents  = gson.fromJson(reader,
                        new TypeToken<Vector<DocumentIndexed>>() {}.getType()); 
      reader.close();
    }catch (Exception e){
      e.printStackTrace();
    }
    docid = docid%DOC_WRITE_SIZE;
    return _documents.get(docid);
  }
}

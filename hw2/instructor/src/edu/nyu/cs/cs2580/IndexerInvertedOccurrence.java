package edu.nyu.cs.cs2580;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.Vector;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map.Entry;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.Writer;
import java.io.Reader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.File;
import java.lang.ProcessBuilder;
//import org.apache.commons.io.IOUtils;
//import org.codehaus.jackson.map.ObjectMapper;
//import org.codehaus.jackson.JsonParseException;
import edu.nyu.cs.cs2580.SearchEngine.Options;

/**
 * @CS2580: Implement this class for HW2.
 */
public class IndexerInvertedOccurrence extends Indexer implements Serializable{
  private static final long serialVersionUID = 3;

  //Maps each term to their index list, index list contains docId only
  private Map<String, Vector<DocOcc>> _indexList = new HashMap<String, Vector<DocOcc>>();

  class DocOcc implements Serializable{
    private static final long serialVersionUID = 2;
    int docid;
    Vector<Integer> _pos;
    DocOcc(int _docid){
      docid = _docid;
    }
  }
  //stopwords set
  private Set<String> stopwords = new HashSet<String>();
  //raw documents
  private Vector<DocumentIndexed> _documents = new Vector<DocumentIndexed>();
  final int DOC_WRITE_SIZE = 100;
  final int maxFileNum = 1000;
  //private HashMap<Integer, DocumentIndexed> _documents = new HashMap<Integer, DocumentIndexed>();
  private Stemming stemming = new Stemming();

  //map url to docid
  Map<String,Integer> _docIDs = new HashMap<String,Integer>();

   // Provided for serialization
  public IndexerInvertedOccurrence(){ }

  public IndexerInvertedOccurrence(Options options) {
    super(options);
    System.out.println("Using Indexer: " + this.getClass().getSimpleName());
  }
  //temp storage for documents
  private HashMap<Integer,DocumentIndexed>_docCache = new HashMap<Integer,DocumentIndexed>(100);


  @Override
  /*
  public void constructIndex() throws IOException {
    String corpusFile = _options._corpusPrefix + "/corpus.tsv";
    System.out.println("Construct index from: " + corpusFile);

    BufferedReader reader = new BufferedReader(new FileReader(corpusFile));
    try {
      String line = null;
      while ((line = reader.readLine()) != null) {
        processDocument(line);
      }
    } finally {
      reader.close();
    }
    for(Vector<DocOcc> v: _indexList.values()){
      for(int i = 0; i<v.size(); i++){
        this._totalTermFrequency += v.get(i)._pos.size();
      }
    }
    System.out.println(
        "Indexed " + Integer.toString(_documents.size()) + " docs with " +
        Long.toString(this._totalTermFrequency) + " terms.");

    String indexFile = _options._indexPrefix + "/corpus.idx";
    System.out.println("Store index to: " + indexFile);
    ObjectOutputStream writer =
        new ObjectOutputStream(new FileOutputStream(indexFile));
    writer.writeObject(this);
    writer.close();
  } */
    public void constructIndex() throws IOException{

    int fileCount = 0;    

    File corpusDirectory = new File(_options._corpusPrefix);
         System.out.println("Construct index from: " + corpusDirectory);      
    int fileId = 0;
    int docFileId = 0;
    if(corpusDirectory.isDirectory()){
      for(File corpusFile :corpusDirectory.listFiles()){
        //processDocument(corpusFile);  
        org.jsoup.nodes.Document doc = Jsoup.parse(corpusFile, "UTF-8");
        String title = doc.title().replaceAll(" - Wikipedia, the free encyclopedia", "");
        String body = doc.body().text().replace(title + " From Wikipedia, the free encyclopedia Jump to: navigation, search ", 
            " ").replaceAll("[^a-zA-Z0-9]", " ");
        processDocument(title, body);
        fileCount++;

        if(fileCount>0&&fileCount%maxFileNum==0){
          fileId++;
          String indexFile = _options._indexPrefix+ "/corpus"+fileId+".idx";
          saveIndex(indexFile);
        }
        if(fileCount>0&&fileCount%DOC_WRITE_SIZE==0){
          docFileId++;
          String indexFile = _options._indexPrefix+ "/corpus"+fileId+".idx";
          saveDocList(docFileId);
        }
        
      }
    }
    for(Vector<DocOcc> v: _indexList.values()){
      for(int i = 0; i<v.size(); i++){
        this._totalTermFrequency += v.get(i)._pos.size();
      }
    }
    _docIDs.put("_numDocs", _numDocs);
    _docIDs.put("_totalTermFrequency", (int)_totalTermFrequency);
    System.out.println(
        "Indexed " + Integer.toString(_documents.size()) + " docs with " +
        Long.toString(this._totalTermFrequency) + " terms.");
    saveDocURL();
    saveDocList(++docFileId);
    System.gc();
    String indexFile = _options._indexPrefix+ "/corpus"+(++fileId)+".idx";
    saveIndex(indexFile);
    try{
        mergeIndex();
        }catch(ClassNotFoundException e){

        }
    System.out.println("finish write");
  }

//process each document for all terms it conains
  private void processDocument(String title, String body) {
    //Scanner s = new Scanner(content).useDelimiter("\t");
    int docid = _documents.size();    
    DocumentIndexed doc = new DocumentIndexed(docid);
    HashMap<String, Integer> maps = new HashMap<String, Integer>();
    int pos = 0;
    //String title = s.next();
    pos = readTerms(title, docid, pos, maps);
    //************need to set url
    String url = title;
    //String body = s.next();
    pos = readTerms(body,docid, pos, maps);
    //s.close();
    //construct a new Document object and add it to documents vector

    doc.setTitle(title);
    doc.setUrl(url);
    doc.setTerms(maps);
    _documents.add(doc);
    doc.setDocID(docid);
    _docIDs.put(url,docid);

  }
//the input is title and body content. For each term, stem first, and then add to indexedlist, each docid will be added only once
  //also need to update term corpus freqeuncy 
  private int readTerms(String str, int did, int pos, HashMap<String, Integer> map){
    Scanner scan = new Scanner(str);  // Uses white space by default.
    while (scan.hasNext()) {
      String token = scan.next();
      String s = stemming.stem(token);
      if (_indexList.containsKey(s)) {
        //check if the docid is already added
        if(did != _indexList.get(s).lastElement().docid){
          DocOcc d = new DocOcc(did);
          d._pos = new Vector<Integer>();
          d._pos.add(pos); 
          _indexList.get(s).add(d);
        }else{
          _indexList.get(s).lastElement()._pos.add(pos); //if the docid is already in indexlist, add the new occurance
        }    
      } else {
        //add the term into indexlist 
        _indexList.put(s, new Vector<DocOcc>());
        _indexList.get(s).add(new DocOcc(did));
        Vector<Integer> occs = new Vector<Integer>();
        occs.add(pos);
        _indexList.get(s).get(0)._pos = occs;
      }
      if(map.containsKey(s)){
        map.put(s, map.get(s)+1);
      }else{
        map.put(s, 1);
      }
      pos++;
    }
    return pos;
  }


  @Override
 /* public void loadIndex() throws IOException, ClassNotFoundException {
    String indexFile = _options._indexPrefix + "/corpus.idx";
    System.out.println("Load index from: " + indexFile);

    ObjectInputStream reader =
        new ObjectInputStream(new FileInputStream(indexFile));
    IndexerInvertedOccurrence loaded = (IndexerInvertedOccurrence) reader.readObject();

    this._documents = loaded._documents;
    this._docIDs = loaded._docIDs;
    //update _numDocs
    this._numDocs = _documents.size();
    //update _totalTermFrequency
    for(Vector<DocOcc> v: loaded._indexList.values()){
      for(int i = 0; i<v.size(); i++){
        this._totalTermFrequency += v.get(i)._pos.size();
      }
    }

    this._indexList = loaded._indexList;
    reader.close();

    System.out.println(Integer.toString(_numDocs) + " documents loaded " +
        "with " + Long.toString(this._totalTermFrequency) + " terms!");
  }
  */
  public void loadIndex() throws IOException, ClassNotFoundException {
    //File indexDirectory = new File(_options._indexPrefix);

    //ObjectInputStream reader = new ObjectInputStream(new FileInputStream(_options._indexPrefix+ "/index.idx"));
    String posting = _options._indexPrefix+ "/corpus.idx";
    Reader reader = new InputStreamReader(new FileInputStream(posting));
    Gson gson = new Gson();
    Map<String, Vector<DocOcc>> _indexList = gson.fromJson(reader,
                      new TypeToken<Map<String, Vector<DocOcc>>>() {}.getType()); 
     reader.close();
     System.out.println("index map has entryset " + _indexList.entrySet().size());
     this._indexList = _indexList;
    String urls = _options._indexPrefix+ "/docURL";
    reader = new InputStreamReader(new FileInputStream(urls));
    Map<String, Integer> _docIDs = gson.fromJson(reader,
                      new TypeToken<Map<String, Integer>>() {}.getType()); 
    System.out.println("url map has entryset " + _docIDs.entrySet().size());
     reader.close();
     this._numDocs = _docIDs.get("_numDocs");
     _docIDs.remove("_numDocs");
     this._totalTermFrequency = _docIDs.get("_totalTermFrequency");
     _docIDs.remove("_totalTermFrequency");
     /*
    String docs = _options._indexPrefix+ "/docList";
    reader = new InputStreamReader(new FileInputStream(docs));
     Vector<DocumentIndexed> _documents = gson.fromJson(reader,
                      new TypeToken<Vector<DocumentIndexed>>() {}.getType()); 
     reader.close();
    this._documents = _documents; */
   // @SuppressWarnings("unchecked") this._indexList = (Map<String,Vector<Integer>>)reader.readObject();
   
    //String indexFile = _options._indexPrefix + "/corpus.idx";
    //System.out.println("Load index from: " + indexFile);



    this._docIDs = _docIDs;


    System.out.println(Integer.toString(_numDocs) + " documents loaded ");// +
        //"with " + Long.toString(_totalTermFrequency) + " terms!");
  }

  @Override
  public edu.nyu.cs.cs2580.Document getDoc(int docid) {
    return (docid >= _numDocs || docid < 0) ? null : (edu.nyu.cs.cs2580.Document)_documents.get(docid); //need to check
  }

  /**
   * In HW2, you should be using {@link DocumentIndexed}.
   */
  @Override
  public edu.nyu.cs.cs2580.Document nextDoc(Query query, int docid) {
    int nextDocid = nextQueryDocId(query,docid);
    System.out.println("nextDocid is " + nextDocid);
    if(nextDocid > -1){
      
      if(_docCache.containsKey(nextDocid)){
        return _docCache.get(nextDocid);
      }else{
        DocumentIndexed d = null;
        try{
          d = fectchDoc(nextDocid);
        }catch(IOException e){
          e.printStackTrace();
        }
        
        System.out.println("updating _docCache now...");
        if(_docCache.size() == 100){
          _docCache.clear();
        }
        _docCache.put(d.getDocID(), d);
        return d;
      } 
      //return _documents.get(nextDocid);
    }
    return null; 
  }

  /**
 *find next document id for given query which might contain conjuctive terms or phrase+terms, return -1 if there is no next doc
 */
private int nextQueryDocId(Query query, int docid){
  //query.processQuery();
  if(query._tokens.size()==1){
    return nextTermDocId(query._tokens.get(0),docid);
  }else if(query._tokens.size() > 1){
    int count = query._tokens.size();
    int[] docs = new int[count];
    for(int i = 0; i<count; i++){
      docs[i] = nextTermDocId(query._tokens.get(i),docid);
      if(docs[i]==-1){
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

//find the next document id for the query token, might be a single term or a prase
private int nextTermDocId(String term, int docid){
  String[] tokens = term.split(" ");
  if(tokens.length==1){
    return nextSingleTermDocId(tokens[0], docid);
  }else{
    return nextPhraseDocId(tokens,docid, -1);
  }
}
/**
 * find the next document id for given single term/word using binary search, return -1 if there is no next doc
 */ 
private int nextSingleTermDocId(String term, int docid){
    if(_indexList.containsKey(term)){
      //check if the docid is the last one
      Vector<DocOcc> list = _indexList.get(term);
      if(list.size()>0 && docid < list.lastElement().docid){
        if(list.get(0).docid > docid){
          return list.get(0).docid;
        }
        //search curent position of the docid
        int left = 0;
        int right = list.size();
        //int curPos = -1;
        while(right - left > 1){
          int mid = (left+right) / 2;
          if(list.get(mid).docid<=docid){
            left = mid;
          }else{
            right = mid;
          }
        }
        return list.get(right).docid;
      }
    }
    return -1;
}

private int nextMultiTermDocId(String[] tokens, int docid){
    int count = tokens.length;
    int[] docs = new int[count];
    for(int i = 0; i<count; i++){
      docs[i] = nextTermDocId(tokens[i],docid);
      if(docs[i]==-1){
        return -1;
      }
    }
    if(isEqual(docs)){
      return docs[0];
    }else{
      int max = findMaxId(docs);
      return nextMultiTermDocId(tokens,max-1);
    }
}

//given a phrase, first find the next docid where all terms in this phrase appear, then determine if the position is consective
private int nextPhraseDocId(String[] tokens, int docid, int pos){
  int nextId = nextMultiTermDocId(tokens,docid);

  if(nextId > -1){
    while(nextId > -1){
      if(nextPhrasePosition(tokens, nextId, pos) > -1){
        return nextId;
      }else{
        nextId = nextMultiTermDocId(tokens,nextId);
      }
    }
  }
  return -1;
}

private int nextPhrasePosition(String[] tokens, int docid, int pos){
    int first = nextTermPosition(tokens[0],docid,pos);
    int original = first;
    if(first == -1){
      return -1;
    }else{ 
         for(int i =1;i<tokens.length;i++){
              int tmp = nextTermPosition(tokens[i],docid,first);
              if(tmp == -1)
                   return -1;
              if (tmp != first+1)
                return nextPhrasePosition(tokens,docid,original);
              else 
                first = tmp;
         }  
      return first;        
    }

}

private int phraseFrequencyInDoc(String[] phase, int docid){
    int res = 0;
    int pos = -1;
    pos = nextPhrasePosition(phase,docid,-1);
    while(pos !=-1){
      res++;
      pos = nextPhrasePosition(phase,docid,pos);
    }
    return res;
}

private int findDocPosition(Vector<DocOcc> list, int docid){
  int left = 0;
  int right = list.size()-1;
  while(left<=right){
    int mid = (left+right)/2;
    if(list.get(mid).docid < docid){
      left = mid+1;
    }else if(list.get(mid).docid > docid){
      right = mid-1;
    }else {
      return mid;
    }
  }
  return -1;
}

private int nextTermPosition(String term, int docid, int pos){
  int index = findDocPosition(_indexList.get(term),docid);
  Vector<Integer> list = _indexList.get(term).get(index)._pos;
  if(list.size()==0 || list.lastElement()<=pos){
    return -1;
  }else if(pos < list.get(0)){
    return list.get(0);
  }else{
    return binarySearch(list, 0, list.size()-1,pos);
    /*
    for(int i=0;i<list.size();i++){
      if(list.get(i)>pos){
        return list.get(i);
      }
    }
    return -1;
    */
  }
}


  @Override
  public int corpusDocFrequencyByTerm(String term) {
    String[] tokens = term.split(" ");
    if(tokens.length==1){
        if(_indexList.containsKey(term)){
          return _indexList.get(term).size();
        }else{
          return 0;
        }
    }else{
      //process phrase
      int res=0;

      int docid = nextPhraseDocId(tokens, -1, -1);
      while(docid !=-1){
           res++;
          docid = nextPhraseDocId(tokens,docid, -1);
      }
      return res;
    }

  }

  @Override
  public int corpusTermFrequency(String term) {
    String[] tokens = term.split(" ");
    if(tokens.length == 1){
      if(_indexList.containsKey(term)){
        int sum = 0;
        for(int i = 0; i< _indexList.get(term).size(); i++){
          sum += _indexList.get(term).get(i)._pos.size();
        }
        return sum;
      }else{
        return 0;
      }
    }else{
      //process phrase
      int res = 0;
      int docid = nextPhraseDocId(tokens,-1, -1);
      while(docid !=-1){
        res += phraseFrequencyInDoc(tokens,docid);
        docid = nextPhraseDocId(tokens,docid, -1);
      }
      return res;
    }

  }

  @Override
  public int documentTermFrequency(String term, String url) {
    int docid = _docIDs.get(url);
    String[] tokens = term.split(" ");
    if(tokens.length == 1){
      //if(_indexList.get(term).indexOf(docid)==-1){
      //  return -1;
      //}else{
        return _documents.get(docid).getTerms().get(term);
      //}
    }else{
      //return phrase frequency
      return phraseFrequencyInDoc(tokens,docid);
    }

  }

  private void constructDocList(){

  }
//****************************************************io function
  public void saveIndex(String name) throws IOException{
      Writer writer = new OutputStreamWriter(new FileOutputStream(name));
      Gson gson = new GsonBuilder().create();
      gson.toJson(_indexList, writer);
      writer.close();
      _indexList.clear();
  }

  public void saveDocList(int id) throws IOException{
     String name = _options._indexPrefix+ "/docList" +id;
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

  public void mergeIndex() throws IOException, ClassNotFoundException{
        File indexDirectory = new File(_options._indexPrefix);
        if(indexDirectory.isDirectory()){
          File[] indexes = indexDirectory.listFiles();
         // ObjectInputStream reader = new ObjectInputStream(new FileInputStream(one));
         // @SuppressWarnings("unchecked") Map<String,Vector<Integer>> base = (Map<String,Vector<Integer>>)reader.readObject();
          Gson gson = new GsonBuilder().create();

          Map<String, Vector<DocOcc>> totalIndex = new HashMap<String, Vector<DocOcc>>();
            for(int i = 0; i<indexes.length; i++){
              if(indexes[i].getName().contains("corpus")){
                 //ObjectInputStream reader2 = new ObjectInputStream(new FileInputStream(indexes[i]));
                  // @SuppressWarnings("unchecked") Map<String,Vector<Integer>> _indexList = (Map<String,Vector<Integer>>)reader.readObject();
                   Reader reader = new InputStreamReader(new FileInputStream(indexes[i]));
                  //Gson gson = new Gson();
                  Map<String, Vector<DocOcc>> _indexList = gson.fromJson(reader,
                      new TypeToken<Map<String, Vector<DocOcc>>>() {}.getType()); 
                    mergeMaps(totalIndex, _indexList);
                    System.out.println("merge ");
              }             
            }
          String indexFile = _options._indexPrefix+ "/corpus.idx";
          Writer writer = new OutputStreamWriter(new FileOutputStream(indexFile));
          gson.toJson(totalIndex, writer);
          writer.close();
          totalIndex.clear();
          System.gc();
        }
  }

  private void mergeMaps(Map<String,Vector<DocOcc>> map1, Map<String,Vector<DocOcc>>map2) throws ClassNotFoundException{
    if(map1.isEmpty()){
        map1.putAll(map2);
    }else{
      for(String key: map2.keySet()){
          Vector<DocOcc> value = map2.get(key);
          if(map1.containsKey(key)){
            Vector<DocOcc> old= map1.get(key);
            old.addAll(value);
            map1.put(key,old);
          }else{
             map1.put(key,value);
          }
         
      }
    }
  }

/*private DocumentIndexed fectchDoc(int did){
  int fileid = (did/1000) + 1;
  String fileName = _options._indexPrefix+ "/docList" + did;
  String cmd = "grep '\\<" + did + "\\>' " + fileName;
  List<String> commands = new ArrayList<String>();
  commands.add("/bin/bash");
  commands.add("-c");
  commands.add(cmd);
  ProcessBuilder pb = new ProcessBuilder(commands);
  Process p;
  p = pb.start();
  InputStreamReader isr = new InputStreamReader(p.getInputStream());
  BufferedReader br = new BufferedReader(isr);
  //String s[];
  //String line = br.readLine();
  String jsonString = br.readLine();
  System.out.println(jsonString);
  DocumentIndexed d = new ObjectMapper().readValue(jsonString, DocumentIndexed.class);
  return d;
} */

private DocumentIndexed fectchDoc(int did) throws FileNotFoundException{
  System.out.println("fectchDoc now for docId " + did);
  int fileid = ((did + 1)/DOC_WRITE_SIZE);
  String fileName = _options._indexPrefix+ "/docList" + fileid;  
  System.out.println("docid is " + did + "searching file " + fileName);
        Reader reader = new InputStreamReader(new FileInputStream(fileName));
        Gson gson = new Gson();  
        Vector<DocumentIndexed> docList = gson.fromJson(reader,
              new TypeToken<Vector<DocumentIndexed>>() {}.getType());
        System.out.println("check the title of returned doc " + docList.get(did % DOC_WRITE_SIZE).getTitle());
          return docList.get(did % DOC_WRITE_SIZE);
//return null;
} 
/*
private DocumentIndexed fectchDoc(int did) throws IOException{
  int fileid = (did/1000) + 1;
  String fileName = _options._indexPrefix+ "/docList" + did;
  String cmd = "grep '\\<" + did + "\\>' " + fileName;
  List<String> commands = new ArrayList<String>();
  commands.add("/bin/bash");
  commands.add("-c");
  commands.add(cmd);
  ProcessBuilder pb = new ProcessBuilder(commands);
  Process p;
  p = pb.start();
  InputStreamReader isr = new InputStreamReader(p.getInputStream());
  BufferedReader br = new BufferedReader(isr);
  String line = org.apache.commons.io.IOUtils.toString(rd);
  //String line = br.readLine();
  System.out.println(line);
  //JsonObject jsonObj = JsonObject.getAsJsonObject(br.readline());
  JsonObject json = (JsonObject)new JsonParser().parse(line);
  Gson gson = new Gson();
  DocumentIndexed d = gson.fromJson(json,DocumentIndexed.class); 

  //String jsonString = br.readLine();
  //System.out.println(jsonString);
  //DocumentIndexed d = new ObjectMapper().readValue(jsonString, DocumentIndexed.class);
  return d;
}  */
//**************************************************** auxilary functions
  //check if docids returned for all query terms are same 
  private boolean isEqual(int[] numbers){
    if(numbers.length !=0){
      for(int i =0; i<numbers.length-1; i++){
        if(numbers[i]!=numbers[i+1]){
          return false;
        }
      }
    }    
    return true;
  }

  private int findMaxId(int[] numbers){
    if(numbers.length > 0){
      int size = numbers.length;
      int max = numbers[0];
      for(int i = 1; i<size; i++){
        if(numbers[i]>max){
          max = numbers[i];
        }
      }
      return max;
    }else{
      return -1;
    }
  }

  private int binarySearch(Vector<Integer> list, int low, int high, int cur){
    while(low<=high){
      int mid = (low+high)/2;
      if(list.get(mid) < cur){
        low = mid+1;
      }else if(list.get(mid) > cur){
        if(mid>=1&&list.get(mid-1)<=cur){
          return list.get(mid);
        }else{
          high = mid-1;
        }
      }else {
        return list.get(mid+1);
      }
    }
    return -1;
  }

}

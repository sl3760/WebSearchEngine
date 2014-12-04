package edu.nyu.cs.cs2580;

import java.util.Map.Entry;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.io.File;
import java.io.Writer;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.Vector;
import java.util.ArrayList;
import java.util.*;  
import java.io.Reader;
import java.io.*;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import edu.nyu.cs.cs2580.SearchEngine.Options;

/**
 * @CS2580: Implement this class for HW2.
 */
public class IndexerInvertedCompressed extends Indexer implements Serializable{
  private static final long serialVersionUID = 3;

  //Maps each term to their index list, index list contains docId only
  //private Map<String, Vector<DocOcc>> _indexList = new HashMap<String, Vector<DocOcc>>();
  private HashMap<Character,HashMap<String,ArrayList<ArrayList<Integer>>>> _charMapList = new HashMap<Character,HashMap<String,ArrayList<ArrayList<Integer>>>>();
  private HashMap<String, ArrayList<ArrayList<Integer>>> _indexList = new HashMap<String,ArrayList<ArrayList<Integer>>>();
  private static ArrayList<Float> pageRank;
  private static ArrayList<Integer> numViews;

  // Store the terms for ench doc
  //private ArrayList<HashMap<String,Integer>> _docTerms  = new ArrayList<HashMap<String,Integer>>();

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
  final int DOC_WRITE_SIZE = 500;
  final int maxFileNum = 1000;
  //private HashMap<Integer, DocumentIndexed> _documents = new HashMap<Integer, DocumentIndexed>();
  private Stemming stemming = new Stemming();

  //map url to docid
  Map<String,Integer> _docIDs = new HashMap<String,Integer>();

   // Provided for serialization
  public IndexerInvertedCompressed(){ }

  public IndexerInvertedCompressed(Options options) {
    super(options);
    System.out.println("Using Indexer: " + this.getClass().getSimpleName());
    StopWord  stopWord = new StopWord();
    stopwords = stopWord.getStopWords();
  }
  //temp storage for documents
  private HashMap<Integer,DocumentIndexed>_docCache = new HashMap<Integer,DocumentIndexed>(100);

  private Vector<DocumentIndexed> _fullDocuments = new Vector<DocumentIndexed>();

  private static int docFileId = 0;


  @Override
    public void constructIndex() throws IOException{

    //load pagerank and numview
    Gson gson = new Gson();
    String path_numView = _options._logPrefix+ "/numView";
    Reader reader = new InputStreamReader(new FileInputStream(path_numView));
    numViews = gson.fromJson(reader,
                      new TypeToken<ArrayList<Integer>>() {}.getType());
    String path_pageRank =  _options._logPrefix+ "/PageRank";
    reader = new InputStreamReader(new FileInputStream(path_pageRank));
    pageRank = gson.fromJson(reader,
                      new TypeToken<ArrayList<Float>>() {}.getType()); 
    reader.close();
    int n1 = numViews.size();
    int n2 = pageRank.size();
    if(n1!=n2){
      System.out.println("numView and pageRank have differnt size: " + numViews.size() + "vs. "+ pageRank.size());
      System.exit(1);
    }

    int fileCount = 0;
    File corpusDirectory = new File(_options._corpusPrefix);
    System.out.println("Construct index from: " + corpusDirectory);      
    if(corpusDirectory.isDirectory()){
      for(File corpusFile :corpusDirectory.listFiles()){  
        org.jsoup.nodes.Document doc = Jsoup.parse(corpusFile, "UTF-8");
        //String title = doc.title().replaceAll(" - Wikipedia, the free encyclopedia", "");
        String title = corpusFile.getName();
       // System.out.println("doc.title() is " + doc.title() + " return " + title);
        String body = doc.body().text().replace(title + " From Wikipedia, the free encyclopedia Jump to: navigation, search ", 
            " ").replaceAll("[^a-zA-Z0-9]", " ");

        processDocument(title, body);
        //System.out.println("Doc "+fileCount +" processed!");
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
    //System.out.println

    if(!_charMapList.isEmpty()){
      writeFile(_charMapList);
      _charMapList.clear();
    }

    mergeAll();
    
    _docIDs.put("_numDocs", fileCount);
    _docIDs.put("_totalTermFrequency", (int)_totalTermFrequency);
    System.out.println("Indexed " + fileCount + " docs with " + Long.toString(this._totalTermFrequency) + " terms.");
    saveDocURL();
    saveDocList(++docFileId);
    saveDocuments();
    
    

    System.gc();
    System.out.println("finish write");
  }

//process each document for all terms it conains
  private void processDocument(String title, String body) {
    //Scanner s = new Scanner(content).useDelimiter("\t");
    int docid = _documents.size();    
    DocumentIndexed doc = new DocumentIndexed(docid);
    DocumentIndexed fullDoc = new DocumentIndexed(docid);
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
    doc.setTotalTerms(pos);
    //_docTerms.add(maps);
    fullDoc.setDocID(docid);
    fullDoc.setTerms(maps);   
    doc.setDocID(docid);
    doc.setPageRank(pageRank.get(docid));
    doc.setNumViews(numViews.get(docid));
    _documents.add(doc);
    _docIDs.put(url,docid);
    _fullDocuments.add(fullDoc);
  }
//the input is title and body content. For each term, stem first, and then add to indexedlist, each docid will be added only once
  //also need to update term corpus freqeuncy 
  private int readTerms(String str, int did, int pos, HashMap<String, Integer> map){
    Scanner scan = new Scanner(str);  // Uses white space by default.
    while (scan.hasNext()) {
      String token = scan.next();
      String s = stemming.stem(token);
      s = s.toLowerCase();
      if(stopwords.contains(s)){
        continue;
      }
    if(s.length() >0){
      if(map.containsKey(s)){
        map.put(s, map.get(s)+1);
      }else{
        map.put(s, 1);
      }
       Character c = s.charAt(0);
       if(_charMapList.containsKey(c)){
        HashMap<String,ArrayList<ArrayList<Integer>>> indexList = _charMapList.get(c);
           if (indexList.containsKey(s)) {
        //check if the docid is already added
                 ArrayList<ArrayList<Integer>> document_list = indexList.get(s);
                 int index = getIndex(did,document_list);
        if(index == -1){
            ArrayList<Integer> offsets = new ArrayList<Integer>();
            offsets.add(did);
            offsets.add(pos);
            document_list.add(offsets);
        }else{
               document_list.get(index).add(pos); //if the docid is already in indexlist, add the new occurance
        }
        indexList.put(s,document_list);    
      } else {
        //add the term into indexlist 
        ArrayList<Integer> offsets = new ArrayList<Integer>();
            offsets.add(did);
            offsets.add(pos);
            ArrayList<ArrayList<Integer>> document_list = new ArrayList<ArrayList<Integer>>();
            document_list.add(offsets);
            indexList.put(s,document_list);

      }
      _charMapList.put(c,indexList);
    }
     else{
           //  this term is new to charMap
            ArrayList<Integer> offsets = new ArrayList<Integer>();
            offsets.add(did);
            offsets.add(pos);
            ArrayList<ArrayList<Integer>> document_list = new ArrayList<ArrayList<Integer>>();
            document_list.add(offsets);
            HashMap<String,ArrayList<ArrayList<Integer>>> indexList = new HashMap<String,ArrayList<ArrayList<Integer>>>();
            indexList.put(s,document_list);
            _charMapList.put(c,indexList);


     }
      
      pos++;
      this._totalTermFrequency++;
    }
    }
    return pos;
  }

  private int getIndex(int did, ArrayList<ArrayList<Integer>> lists){
    
    for(int i = 0 ; i < lists.size(); i++){
          if(did == lists.get(i).get(0)){
                  return i;

          }

    }
    return -1;
  }

  public void writeFile(HashMap<Character, HashMap<String, ArrayList<ArrayList<Integer>>>> charMapList) throws IOException {
    for (Map.Entry<Character, HashMap<String, ArrayList<ArrayList<Integer>>>> entry : charMapList.entrySet()) {
      String path = _options._indexPrefix + "/" + entry.getKey() + ".idx";
      File file = new File(path);
      BufferedWriter write = new BufferedWriter(new FileWriter(file, true));
      HashMap<String, ArrayList<ArrayList<Integer>>> tempMap = entry.getValue();
      for(Map.Entry<String, ArrayList<ArrayList<Integer>>> entryString : tempMap.entrySet()){
        String word = entryString.getKey();
        
        write.write(word);
        ArrayList<ArrayList<Integer>> lists = entryString.getValue();
        
        StringBuffer sb = new StringBuffer();
        sb.append(":");
        for(int i = 0 ; i < lists.size();i++){
          
          ArrayList<Integer> list = lists.get(i);
          for(int j = 0; j<list.size();j++){
            sb.append(String.valueOf(list.get(j)));
            if(j !=list.size()-1)
            sb.append(",");
          }
          sb.append(";");
          

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
          HashMap<Character, HashMap<String, ArrayList<ArrayList<Integer>>>> charMapList = readAll(fileName);
          String filePath = _options._indexPrefix + "/" + fileName;
          File charFile = new File(filePath);
          charFile.delete();
          writeFile(charMapList);
        }
      }
    }
  }

  public HashMap<Character, HashMap<String, ArrayList<ArrayList<Integer>>>> readAll(String fileName) throws IOException{
    String file = _options._indexPrefix + "/" + fileName;
    Scanner scan = new Scanner(new File(file));
    HashMap<Character, HashMap<String, ArrayList<ArrayList<Integer>>>> charMapList = new HashMap<Character, HashMap<String, ArrayList<ArrayList<Integer>>>>();
    HashMap<String, ArrayList<ArrayList<Integer>>> tempMap = new HashMap<String, ArrayList<ArrayList<Integer>>>();
    while(scan.hasNextLine()){
      String line = scan.nextLine();
      String lineArray[] = line.split(":");
      String word = lineArray[0];
      ArrayList<ArrayList<Integer>> lists = new ArrayList<ArrayList<Integer>>();
      String data = lineArray[lineArray.length-1];
      String[] document_list = data.split(";");
      
      for(int i=0;i<document_list.length;i++){
          if(document_list[i]!=null&&document_list[i].length()!=0&&!document_list[i].equals("\n")){
          ArrayList<Integer> offset_list = new ArrayList<Integer>();
            String[] list = document_list[i].split(",");
            for(int j = 0; j < list.length;j++){
              if(list[j] !=null && list[j].length() !=0 && !list[j].equals("\n"))
              offset_list.add(Integer.parseInt(list[j]));
            }
            //System.out.println("Current offset_list:" + offset_list);
            lists.add(offset_list);
        }
     }
      if (tempMap.containsKey(word)) {
        
        ArrayList<ArrayList<Integer>> temp = tempMap.get(word);
        
        
        temp.addAll(lists);
        
        tempMap.put(word, temp);
      } else {
        
        tempMap.put(word, lists);
      }
    }
    charMapList.put(fileName.charAt(0), tempMap);
    return charMapList;
  }


  @Override
 
  public void loadIndex() throws IOException, ClassNotFoundException {
        Gson gson = new Gson();
    

    String urls = _options._indexPrefix+ "/docURL";
    Reader reader = new InputStreamReader(new FileInputStream(urls));
    HashMap<String, Integer> loaded_docIDs = gson.fromJson(reader,
                      new TypeToken<HashMap<String, Integer>>() {}.getType()); 
    System.out.println("url map has entryset " + loaded_docIDs.entrySet().size());
    reader.close();

    String docs = _options._indexPrefix+ "/documents";
      reader = new InputStreamReader(new FileInputStream(docs));
    Vector<DocumentIndexed> loaded_documents = gson.fromJson(reader,
                      new TypeToken<Vector<DocumentIndexed>>() {}.getType()); 
       reader.close();

    this._numDocs = loaded_docIDs.get("_numDocs");
    System.out.println("removed two values from _docIDs");
    loaded_docIDs.remove("_numDocs");      
    this._totalTermFrequency = loaded_docIDs.get("_totalTermFrequency");
    loaded_docIDs.remove("_totalTermFrequency");
   
    this._docIDs = loaded_docIDs;
    this._documents = loaded_documents;

    System.out.println(Integer.toString(_numDocs) + " documents loaded with " + Long.toString(_totalTermFrequency) + " terms!");

  }
    

  @Override
  public edu.nyu.cs.cs2580.Document getDoc(int docid) {
    return (docid >= _numDocs || docid < 0) ? null : _documents.get(docid); //need to check
  }

  /**
   * In HW2, you should be using {@link DocumentIndexed}.
   */
  @Override
  public edu.nyu.cs.cs2580.Document nextDoc(Query query, int docid) {
    //System.out.println("In nextDoc:");
    int nextDocid = -1;
    nextDocid = nextQueryDocId(query,docid);
    //System.out.println("nextDocid is " + nextDocid);
    if(nextDocid > -1){

      return _documents.get(nextDocid);
    }
    return null; 
  }

  /**
 *find next document id for given query which might contain conjuctive terms or phrase+terms, return -1 if there is no next doc
 */
private int nextQueryDocId(Query query, int docid){
  
  //System.out.println("In nextQueryDocId:");
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
  //System.out.println("In nextTermDocId:");
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
private int nextSingleTermDocId(String term, int docid) {
  //System.out.println("In nextSingleTermDocId:");
    if(!_indexList.containsKey(term)) {
           try{
            String fileName = _options._indexPrefix+"/"+term.charAt(0)+".idx";
            
            ArrayList<ArrayList<Integer>> lists = findTerm(term,fileName);
          
            if(lists==null)
                 return -1;
      
      if(_indexList.size()>1000){
           _indexList.clear();
         }
           _indexList.put(term,lists);
       
     } catch (IOException e){

     }
   }
    if(_indexList.containsKey(term)){
      //check if the docid is the last one
      //System.out.println("should be here");
      ArrayList<ArrayList<Integer>> list = _indexList.get(term);
      if(list.size()>0 && docid < list.get(list.size()-1).get(0)){
        if(list.get(0).get(0)> docid){
          return list.get(0).get(0);
        }
        //search curent position of the docid
        int left = 0;
        int right = list.size();
        //int curPos = -1;
        while(right - left > 1){
          int mid = (left+right) / 2;
          if(list.get(mid).get(0)<=docid){
            left = mid;
          }else{
            right = mid;
          }
        }
        return list.get(right).get(0);
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

private int findDocPosition(ArrayList<ArrayList<Integer>> list, int docid){
  int left = 0;
  int right = list.size()-1;
  while(left<=right){
    int mid = (left+right)/2;
    if(list.get(mid).get(0) < docid){
      left = mid+1;
    }else if(list.get(mid).get(0) > docid){
      right = mid-1;
    }else {
      return mid;
    }
  }
  return -1;
}

private int nextTermPosition(String term, int docid, int pos){
  if(!_indexList.containsKey(term)) {
           try{
            String fileName = _options._indexPrefix+"/"+term.charAt(0)+".idx";
            
            ArrayList<ArrayList<Integer>> lists = findTerm(term,fileName);
          
            if(lists==null)
                 return -1;
      
      if(_indexList.size()>1000){
           _indexList.clear();
         }
           _indexList.put(term,lists);
       
     } catch (IOException e){

     }
   }
  int index = findDocPosition(_indexList.get(term),docid);
  ArrayList<Integer> list = _indexList.get(term).get(index);
  if(list.size()<2 || list.get(list.size()-1)<=pos){
    return -1;
  }else if(pos < list.get(1)){
    return list.get(1);
  }else{
    return binarySearch(list, 1, list.size()-1,pos);
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
  public int corpusDocFrequencyByTerm(String term)  {
    String[] tokens = term.split(" ");
    if(tokens.length==1){
        if(!_indexList.containsKey(term)) {
           try{
            String fileName = _options._indexPrefix+"/"+term.charAt(0)+".idx";
            
            ArrayList<ArrayList<Integer>> lists = findTerm(term,fileName);
          
            if(lists==null)
                 return -1;
      
      if(_indexList.size()>1000){
           _indexList.clear();
         }
           _indexList.put(term,lists);
       
     } catch (IOException e){

     }
   }

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
  public int corpusTermFrequency(String term)  {
    String[] tokens = term.split(" ");
    if(tokens.length == 1){

       if(!_indexList.containsKey(term)) {
           try{
            String fileName = _options._indexPrefix+"/"+term.charAt(0)+".idx";
            
            ArrayList<ArrayList<Integer>> lists = findTerm(term,fileName);
          
            if(lists==null)
                 return -1;
      
      if(_indexList.size()>1000){
           _indexList.clear();
         }
           _indexList.put(term,lists);
       
     } catch (IOException e){

     }
   }
      if(_indexList.containsKey(term)){
        int sum = 0;
        for(int i = 0; i< _indexList.get(term).size(); i++){
          sum += _indexList.get(term).get(i).size()-1;
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
  public int documentTermFrequency(String term, int docid)  {
    //int docid = _docIDs.get(url);
    String[] tokens = term.split(" ");
    if(tokens.length == 1){
         if(docid>=0 && docid<_docIDs.size()){
            if(!_indexList.containsKey(term)) {
           try{
            String fileName = _options._indexPrefix+"/"+term.charAt(0)+".idx";
            
            ArrayList<ArrayList<Integer>> lists = findTerm(term,fileName);
          
            if(lists==null)
                 return -1;
      
      if(_indexList.size()>1000){
           _indexList.clear();
         }
           _indexList.put(term,lists);
       
     } catch (IOException e){

     }
   }
          
        
       int idx = findDocPosition(_indexList.get(term),docid);
       return  _indexList.get(term).get(idx).size()-1;

      }
      else
         return 0;

  }
    else{
      //return phrase frequency
      return phraseFrequencyInDoc(tokens,docid);
    }

  }

  
//****************************************************io function
 
public ArrayList<ArrayList<Integer>> findTerm(String term, String fileName) throws IOException {
    String cmd = "grep -w "+term+" "+fileName;
    Process p;
    //System.out.println("commend:"+cmd);
    ArrayList<ArrayList<Integer>> lists = new ArrayList<ArrayList<Integer>>();
    try{
      //System.out.println("1");
      p = Runtime.getRuntime().exec(cmd);
      //System.out.println("2");
     // p.waitFor();
      //System.out.println("3");
      BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
      InputStream error = p.getErrorStream();
      for (int i = 0; i < error.available(); i++) {
            System.out.println("" + error.read());
         }
      //System.out.println("4");
      String line = br.readLine();
      //System.out.println("5");
      if(line==null||line.length()==0){
        //System.out.println("Nothing!");
        return null;
      }
      //System.out.println("The list:"+ line);
      
      String[] arr = line.split(":");
      
      String[] tempSt = arr[arr.length-1].split(";");
      for(int i=0;i<tempSt.length;i++){
        if(tempSt[i]!=null&&tempSt[i].length()!=0&&!tempSt[i].equals("\n")){
             String[] offsets = tempSt[i].split(",");
             ArrayList<Integer> offset_list = new ArrayList<Integer>();
             for(int j = 0; j < offsets.length; j++){
              if(offsets[j] !=null && offsets[j].length() !=0 && !offsets[j].equals("\n"))
                offset_list.add(Integer.parseInt(offsets[j]));
             }
             lists.add(offset_list);
          
        }       
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    return lists;
  }
  
  
  /*
  public ArrayList<ArrayList<Integer>> findTerm(String term, String fileName) throws IOException{
    Scanner scan = new Scanner(new File(fileName));
   
    while(scan.hasNextLine()){
      String line = scan.nextLine();
      
      String[] arr = line.split(":");
      if(term.equals(arr[0])){
         ArrayList<ArrayList<Integer>> lists = new ArrayList<ArrayList<Integer>>();
      String[] tempSt = arr[arr.length-1].split(";");
      for(int i=0;i<tempSt.length;i++){
        if(tempSt[i]!=null&&tempSt[i].length()!=0&&!tempSt[i].equals("\n")){
             String[] offsets = tempSt[i].split(",");
             ArrayList<Integer> offset_list = new ArrayList<Integer>();
             for(int j = 0; j < offsets.length; j++){
              if(offsets[j] !=null && offsets[j].length() !=0 && !offsets[j].equals("\n"))
                offset_list.add(Integer.parseInt(offsets[j]));
             }
             lists.add(offset_list);
          
        }       
      }
      return lists;
    } 
  }
    return null;
  }
  */
  
  /*
  public void saveDocTerms(int id) throws IOException{
    String name = _options._indexPrefix+ "/docList" + id;
    Writer writer = new OutputStreamWriter(new FileOutputStream(name));
    Gson gson = new GsonBuilder().create();
    gson.toJson(_docTerms, writer);
    writer.close();
    _docTerms.clear();
  }
  */

  public void saveDocList(int id) throws IOException{
    String name = _options._indexPrefix+ "/docList" + id;
    Writer writer = new OutputStreamWriter(new FileOutputStream(name));
    Gson gson = new GsonBuilder().create();
    gson.toJson(_fullDocuments, writer);
    writer.close();
    _fullDocuments.clear();
  }

  public void saveDocURL() throws IOException{
      String name = _options._indexPrefix+ "/docURL";
      Writer writer = new OutputStreamWriter(new FileOutputStream(name));
      Gson gson = new GsonBuilder().create();
      gson.toJson(_docIDs, writer);
      writer.close();
      _docIDs.clear();
  }

  public void saveDocuments() throws IOException{
    String name = _options._indexPrefix+ "/documents";
    Writer writer = new OutputStreamWriter(new FileOutputStream(name));
    Gson gson = new GsonBuilder().create();
    gson.toJson(_documents, writer);
    writer.close();
    _documents.clear();
  }

  
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

  private int binarySearch(ArrayList<Integer> list, int low, int high, int cur){
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

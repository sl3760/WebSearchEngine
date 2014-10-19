package edu.nyu.cs.cs2580;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
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

import edu.nyu.cs.cs2580.SearchEngine.Options;

/**
 * @CS2580: Implement this class for HW2.
 */
public class IndexerInvertedDoconly extends Indexer implements Serializable{
  private static final long serialVersionUID = 1;

  //Maps each term to their index list, index list contains docId only
  private Map<String, Vector<Integer>> _indexList = new HashMap<String, Vector<Integer>>();
  //Maps docid with its documentindexed instance
  private Map<String, Integer> _termCorpusFreq = new HashMap<String, Integer>();
  //stopwords set
  private Set<String> stopwords = new HashSet<String>();
  //raw documents
  private Vector<DocumentIndexed> _documents = new Vector<DocumentIndexed>();

   // Provided for serialization
  public IndexerInvertedDoconly() { }

  public IndexerInvertedDoconly(Options options) {
    super(options);
    System.out.println("Using Indexer: " + this.getClass().getSimpleName());
  }

  @Override
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
   // for (Integer freq : _termCorpusFreq.values()) {
    //  this._totalTermFrequency += freq;
   // }
    System.out.println(
        "Indexed " + Integer.toString(_documents.size()) + " docs with " +
        Long.toString(this._totalTermFrequency) + " terms.");

    String indexFile = _options._indexPrefix + "/corpus.idx";
    System.out.println("Store index to: " + indexFile);
    ObjectOutputStream writer =
        new ObjectOutputStream(new FileOutputStream(indexFile)); 
    writer.writeObject(this); //for wiki, might need to write partially 
    writer.close();
  }
 //process each document for all terms it conains
  private void processDocument(String content) {
    Scanner s = new Scanner(content).useDelimiter("\t");
    int docid = _documents.size();    
    DocumentIndexed doc = new DocumentIndexed(docid);
    HashMap<String,Integer> map = new HashMap<String, Integer>();
    String title = s.next();
    readTerms(title, docid, map);
    //************need to set url
    String url = " ";
    String body = s.next();
    readTerms(body,docid, map);
    s.close();
    //construct a new Document object and add it to documents vector

    doc.setTitle(title);
    doc.setUrl(url);
    doc.setTerms(map);
    _documents.add(doc);

    //update stats

  }
//the input is title and body content. For each term, stem first, and then add to indexedlist, each docid will be added only once
  //also need to update term corpus freqeuncy 
  private void readTerms(String str, int docid, HashMap<String, Integer> map){
    Scanner scan = new Scanner(str);  // Uses white space by default.
    while (scan.hasNext()) {
      String token = scan.next();
      String s = stemming(token);
      if (_indexList.containsKey(s)) {
        //check if  the docid is already added
        if(docid != _indexList.get(s).lastElement()){
          _indexList.get(s).add(docid);
        }    
        _termCorpusFreq.put(s, _termCorpusFreq.get(s)+1);
      } else {
        _indexList.put(s, new Vector<Integer>());
        _indexList.get(s).add(docid);
        _termCorpusFreq.put(s, 1);
      }
      if(map.containsKey(s)){
          map.put(s, map.get(s)+1);
      }else{
        map.put(s, 1);
      }
    }
  }
//Tokens are stemmed with Step 1 of the Porter's algorithm
  private String stemming(String tokens){
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



//need to modify
  @Override
  public void loadIndex() throws IOException, ClassNotFoundException {
    String indexFile = _options._indexPrefix + "/corpus.idx";
    System.out.println("Load index from: " + indexFile);

    ObjectInputStream reader =
        new ObjectInputStream(new FileInputStream(indexFile));
    IndexerInvertedDoconly loaded = (IndexerInvertedDoconly) reader.readObject();

    this._documents = loaded._documents;
    this._numDocs = _documents.size();
    for (Integer freq : loaded._termCorpusFreq.values()) {
      this._totalTermFrequency += freq;
    }
    this._indexList = loaded._indexList;
    this._termCorpusFreq = loaded._termCorpusFreq;
    reader.close();

    System.out.println(Integer.toString(_numDocs) + " documents loaded " +
        "with " + Long.toString(_totalTermFrequency) + " terms!");

  }

  @Override
  public Document getDoc(int docid) {
    return (docid >= _documents.size() || docid < 0) ? null : (Document)_documents.get(docid);
  }

  /**
   * In HW2, you should be using {@link DocumentIndexed}
   * need to differentiate query and phrase query
   */
  @Override
  public Document nextDoc(Query query, int docid) {
    int nextDocid = nextQueryDocId(query,docid);
    if(nextDocid > -1){
      return _documents.get(docid);
    }
    return null; 
  }
/**
 *find next document id for given conjunctive query which might contain multiple words, return -1 if there is no next doc
 */
private int nextQueryDocId(Query query, int docid){
  query.processQuery();
  //System.out.println("inside of nextQueryDocId, the query size is " + query._tokens.size());
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
      nextQueryDocId(query,max-1);
    }
  }
  return -1;
}

//check if docids returned for all query terms are same 
private boolean isEqual(int[] numbers){
  if(numbers.length > 1){
    for(int i =0; i<numbers.length; i++){
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
      if(numbers[i] > numbers[i-1]){
        if(numbers[i]>numbers[max]){
          max = numbers[i];
        }
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
    if(_indexList.containsKey(term)){
      //check if the docid is the last one
     // System.out.println("inside of nextTermDocId, " + term + " is contained in list");
      Vector<Integer> list = _indexList.get(term);
     // System.out.println("position list w size " + list.size() + " and the first pos is " + list.get(0));
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

  @Override
  public int corpusDocFrequencyByTerm(String term) {
      return _indexList.containsKey(term) ? _indexList.get(term).size() : 0;
  }

  @Override
  public int corpusTermFrequency(String term) {
    return _termCorpusFreq.containsKey(term) ? _termCorpusFreq.get(term) : 0;
  }

  /**
  @Override
  public int documentTermFrequency(String term, String url) {
    SearchEngine.Check(false, "Not implemented!");
    return 0;
  }
  */

  @Override
  public int documentTermFrequency(String term, int docid){
    if(_indexList.get(term).indexOf(docid)==-1){
      return -1;
    }else{
      return _documents.get(docid).getTerms().get(term);
    }
  }
}

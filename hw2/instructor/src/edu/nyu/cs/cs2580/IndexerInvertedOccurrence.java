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
import java.util.Iterator;

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
  private Vector<Document> _documents = new Vector<Document>();

   // Provided for serialization
  public IndexerInvertedOccurrence(){ }

  public IndexerInvertedOccurrence(Options options) {
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
  }

//process each document for all terms it conains
  private void processDocument(String content) {
    Scanner s = new Scanner(content).useDelimiter("\t");
    int docid = _documents.size();
    int pos = 0;
    String title = s.next();
    pos = readTerms(title, docid, pos);
    //************need to set url
    String url = " ";
    String body = s.next();
    pos = readTerms(body,docid, pos);
    s.close();
    //construct a new Document object and add it to documents vector
    Document doc = new Document(docid);
    doc.setTitle(title);
    doc.setUrl(url);
    _documents.add(doc);

  }
//the input is title and body content. For each term, stem first, and then add to indexedlist, each docid will be added only once
  //also need to update term corpus freqeuncy 
  private int readTerms(String str, int did, int pos){
    Scanner scan = new Scanner(str);  // Uses white space by default.
    while (scan.hasNext()) {
      String token = scan.next();
      String s = stemming(token);
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
      pos++;
    }
    return pos;
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



  @Override
  public void loadIndex() throws IOException, ClassNotFoundException {
    String indexFile = _options._indexPrefix + "/corpus.idx";
    System.out.println("Load index from: " + indexFile);

    ObjectInputStream reader =
        new ObjectInputStream(new FileInputStream(indexFile));
    IndexerInvertedOccurrence loaded = (IndexerInvertedOccurrence) reader.readObject();

    this._documents = loaded._documents;
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

  @Override
  public Document getDoc(int docid) {
    return (docid >= _documents.size() || docid < 0) ? null : _documents.get(docid);
  }

  /**
   * In HW2, you should be using {@link DocumentIndexed}.
   */
  @Override
  public Document nextDoc(Query query, int docid) {
    return null;
  }

  /**
 *find next document id for given query which might contain conjuctive terms or phrase+terms, return -1 if there is no next doc
 */
private int nextQueryDocId(Query query, int docid){
  query.processQuery();
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
      nextQueryDocId(query,max-1);
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
    int first = nextTermPosition(tokens[0],docid,pos);
        if(first == -1){
          return -1;
        }else{ 
             for(int i =1;i<tokens.length;i++){
                  int tmp = nextTermPosition(tokens[i],docid,pos);
                  if(tmp == -1)
                       return -1;
                  if (tmp != first+1)
                    return nextPhraseDocId(tokens,docid,first);
                  else 
                    first = tmp;
             }   
        }
    return docid;
  }else{  
    return -1;
  }
}

private int findDocPosition(Vector<DocOcc> list, int docid){
  int left = 0;
  int right = list.size()-1;
  int curPos = -1;
  while(left<=right){
    int mid = (left+right) /2;
    if(list.get(mid).docid == docid){
      curPos = mid;
      break;
    }else if(list.get(mid).docid < docid){
      left = mid+1;
    }else{
      right = mid-1;
    }
  }
  return curPos;
}

private int nextTermPosition(String term, int docid, int pos){
  int index = findDocPosition(_indexList.get(term),docid);
  Vector<Integer> list = _indexList.get(term).get(index)._pos;
  if(list.size()==0 || list.lastElement()<=pos){
    return -1;
  }else if(pos < list.get(0)){
    return list.get(0);
  }else{
    return binarySearch(list, 0, list.size(),pos);
  }
}


  @Override
  public int corpusDocFrequencyByTerm(String term) {
    if(_indexList.containsKey(term)){
      return _indexList.get(term).size();
    }
    return 0;
  }

  @Override
  public int corpusTermFrequency(String term) {
    if(_indexList.containsKey(term)){
      int sum = 0;
      for(int i = 0; i< _indexList.get(term).size(); i++){
        sum += _indexList.get(term).get(i)._pos.size();
      }
      return sum;
    }
    return 0;
  }

  @Override
  public int documentTermFrequency(String term, String url) {
    SearchEngine.Check(false, "Not implemented!");
    return 0;
  }
//**************************************************** auxilary functions
  //check if docids returned for all query terms are same 
  private boolean isEqual(int[] numbers){
    if(numbers.length !=0){
      for(int i =0; i<numbers.length; i++){
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
        if(numbers[i] > numbers[i-1]){
          if(numbers[i]>numbers[max]){
            max = numbers[i];
          }
        }
      }
      return max;
    }else{
      return -1;
    }
  }

  private int binarySearch(Vector<Integer> list, int low, int high, int cur){
    while(high - low >1){
      int mid = (low + high) /2;
      if(list.get(mid)<= cur){
        low = mid;
      }else{
        high = mid;
      }
    }
    return high;
  }
}

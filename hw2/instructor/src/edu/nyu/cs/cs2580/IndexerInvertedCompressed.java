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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.Vector;
import java.util.ArrayList;

import edu.nyu.cs.cs2580.SearchEngine.Options;

/**
 * @CS2580: Implement this class for HW2.
 */
public class IndexerInvertedCompressed extends Indexer {

  private HashMap<String, ArrayList<ArrayList<Integer>>> invertedIndex = new HashMap<String,ArrayList<ArrayList<Integer>>>();
  private HashMap<String, ArrayList<Integer>>  pointers = new HashMap<String, ArrayList<Integer>>();
  private Vector<DocumentIndexed> documents = new Vector<DocumentIndexed>();
  

  public IndexerInvertedCompressed(Options options) {
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
    System.out.println(
        "Indexed " + Integer.toString(_numDocs) + " docs with " +
        Long.toString(_totalTermFrequency) + " terms.");

    String indexFile = _options._indexPrefix + "/corpus.idx";
    System.out.println("Store index to: " + indexFile);
    ObjectOutputStream writer =
        new ObjectOutputStream(new FileOutputStream(indexFile));
    writer.writeObject(this);
    writer.close();
  }

  @Override
  public void loadIndex() throws IOException, ClassNotFoundException {

    String indexFile = _options._indexPrefix + "/corpus.idx";
    System.out.println("Load index from: " + indexFile);

    ObjectInputStream reader =
        new ObjectInputStream(new FileInputStream(indexFile));
    IndexerInvertedCompressed loaded = (IndexerInvertedCompressed) reader.readObject();

    this.documents = loaded.documents;
    // Compute numDocs and totalTermFrequency b/c Indexer is not serializable.
    this._numDocs = documents.size();
    this._totalTermFrequency = loaded._totalTermFrequency;
    this.invertedIndex = loaded.invertedIndex;
    this.pointers = loaded.pointers;
    this.documents = loaded.documents;
    reader.close();

    System.out.println(Integer.toString(_numDocs) + " documents loaded " +
        "with " + Long.toString(_totalTermFrequency) + " terms!");
  }

  @Override
  public Document getDoc(int docid) {
    return documents.get(docid);
  }

  /**
   * In HW2, you should be using {@link DocumentIndexed}
   */
 

  private void processDocument(String content) {
    Scanner s = new Scanner(content).useDelimiter("\t");
    int offset = 0;

    String title = s.next();
    String body = s.next();
    s.close();

    DocumentIndexed doc = new DocumentIndexed(documents.size());
    doc.setTitle(title);
    int docid = doc._docid;
    HashMap<String,Integer> counts = new HashMap<String,Integer>();
    offset = readPostingList(title,docid,offset,counts);
    offset = readPostingList(body,docid,offset,counts);
    
    doc.setTerms(counts);
    documents.add(doc);
    ++_numDocs;

    
  }

  private int readPostingList(String content,int docid,int offset, HashMap<String,Integer> counts) {
    Scanner s = new Scanner(content);  // Uses white space by default.
    while (s.hasNext()) {
      String token = s.next();
      
      if (invertedIndex.containsKey(token)) {
        ArrayList<Integer> pointer_list = pointers.get(token);
        ArrayList<ArrayList<Integer>> document_list = invertedIndex.get(token);
        if(pointer_list.contains(docid)){
          int idx = pointer_list.indexOf(docid);
          ArrayList<Integer> offset_list = document_list.get(idx);
          
          int previous = 0;
          for(int i =1; i < offset_list.size();i++){
            previous += offset_list.get(i);
          }
          
          offset_list.add(offset-previous);


        }
        else{   
                int pre_doc = pointer_list.get(pointer_list.size()-1);
                pointer_list.add(docid);
                
                ArrayList<Integer> offset_list = new ArrayList<Integer>();
                offset_list.add(docid-pre_doc);
                offset_list.add(offset);
                document_list.add(offset_list);



        }
        
        
      } else {
        ArrayList<Integer> offset_list = new ArrayList<Integer>();
        offset_list.add(docid);
        offset_list.add(offset);
        ArrayList<ArrayList<Integer>> document_list = new ArrayList<ArrayList<Integer>>();
        document_list.add(offset_list);
        invertedIndex.put(token,document_list);
        ArrayList<Integer> pointer_list = new ArrayList<Integer>();
        pointer_list.add(docid);
        pointers.put(token,pointer_list);

      }
      int value = 1;
      if(counts.containsKey(token)){
         value = counts.get(token)+1;

      }
      counts.put(token,value);
      offset++;
      _totalTermFrequency++;
    }
    return offset;
  }
  
  @Override
  public Document nextDoc(Query query, int docid) {
    Vector<String> tokens = query._tokens;
    int first = next(tokens.get(0),docid);
    if (first == -1)
     return null;
   else{
          for(int i =1; i<tokens.size();i++){
            int tmp = next(tokens.get(i),docid);
            if(tmp == -1) return null;
            if(tmp !=first) return nextDoc(query, Math.max(first,tmp)-1);
          }

          return documents.get(first);
   }
    
  }

  private int next(String term, int docid){
    if(term.indexOf("") ==-1){
       if(!invertedIndex.containsKey(term)) return -1;
       else{
             ArrayList<Integer> pointer_list = pointers.get(term);
             int pre = pointer_list.indexOf(docid);
             if(pre == pointer_list.size()-1) 
                return -1;
              else return pointer_list.get(pre+1);

       }

    }
    else return nextDocForPhase(term,docid);


  }
  private int nextDocForPhase(String term, int docid){
    Scanner s = new Scanner(term);
    Vector<String> words = new Vector<String>();
    while(s.hasNext()){
      words.add(s.next());
    }

    int first = next(words.get(0),docid);
    if (first == -1)
     return -1;
   else{
          for(int i =1; i<words.size();i++){
            int tmp = next(words.get(i),docid);
            if(tmp == -1) return -1;
            if(tmp !=first) return nextDocForPhase(term, Math.max(first,tmp)-1);
          }

          int pos = nextPhase(term,first,-1);
          if(pos != -1 ) return first;
          else return nextDocForPhase(term,first);
   }

  }

  private int nextPhase(String term, int docid,int pos){
    Scanner s = new Scanner(term);
    Vector<String> words = new Vector<String>();
    while(s.hasNext()){
      words.add(s.next());
    }
    int first = nextPosition(words.get(0),docid,pos);
    int prev = first;
    if(prev == -1) return -1;
    else{ 
         for(int i =1;i<words.size();i++){
              int tmp = nextPosition(words.get(i),docid,pos);
              if(tmp == -1)
                   return -1;
              if (tmp != prev+1)
                return nextPhase(term,docid,Math.min(prev,tmp));
              else 
                prev = tmp;
         }
         return first;


    }
  }

    private int phaseFrequencyInDoc(String phase, int docid){
      int res = 0;
      int pos = -1;
      pos = nextPosition(phase,docid,-1);
      while(pos !=-1){
        res++;
        pos = nextPosition(phase,docid,pos);
      }
      return res;
    }


  
  private int nextPosition(String term, int docid, int pos){
    int idx = pointers.get(term).indexOf(docid);
    ArrayList<Integer> offset_list = invertedIndex.get(term).get(idx);
    int tmp=0;
    int flag =1;
    for(int i = 1;i <offset_list.size();i++){
      tmp+= offset_list.get(i);
      flag++;
      if(tmp>pos) {
        
        break;
      }
      
    }
    if(tmp>pos){
       return offset_list.get(flag);


    }
    else return -1;


    

  }

  @Override
  public int corpusDocFrequencyByTerm(String term) {
    if(term.indexOf("") == -1){
    if(invertedIndex.containsKey(term)){
      return invertedIndex.get(term).size();
    }

    else
    return 0;
  }
  else{
        int res=0;
        
        int docid = nextDocForPhase(term,-1);
        while(docid !=-1){
             res++;
            docid = nextDocForPhase(term,docid);
        }
        return res;
  }
}

  @Override
  public int corpusTermFrequency(String term) {
    if(term.indexOf("") == -1){
    if(invertedIndex.containsKey(term)){
      int res = 0;
      ArrayList<ArrayList<Integer>> document_list = invertedIndex.get(term);
      for(int i = 0; i < document_list.size(); i++){
        res += document_list.get(i).size()-1;
      }
      return res;

    }
    else
    return 0;
  }
  else{
         int res = 0;
         int docid = nextDocForPhase(term,-1);
         while(docid !=-1){
          res += phaseFrequencyInDoc(term,docid);
          docid = nextDocForPhase(term,docid);
         }
         return res;
  }
}

  /**
   * @CS2580: Implement this for bonus points.
   */


  

  
  
  @Override
  public  int documentTermFrequency(String term, int docid){
      if(term.indexOf("") ==-1){
      if(docid>=0 && docid<documents.size()){
           HashMap<String,Integer> counts = documents.get(docid).getTerms();
           if(counts.containsKey(term))
             return counts.get(term);
           else return 0;

      }
      else
         return 0;

  }
  else{
         return phaseFrequencyInDoc(term,docid);

  }
}

}

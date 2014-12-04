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
import java.util.*;  

import edu.nyu.cs.cs2580.SearchEngine.Options;

/**
 * @CS2580: Implement this class for HW2.
 */
public class IndexerInvertedCompressed extends Indexer implements Serializable{

  private static final long serialVersionUID = 4;

  private HashMap<String, ArrayList<ArrayList<Integer>>> invertedIndex = new HashMap<String,ArrayList<ArrayList<Integer>>>();
  private HashMap<String, ArrayList<Integer>>  pointers = new HashMap<String, ArrayList<Integer>>();
  private Vector<DocumentIndexed> documents = new Vector<DocumentIndexed>();
  private Vector<String> stopwords = new Vector<String>();

  //private int debug =1;

  private Stemming stemming = new Stemming();
  
  HashMap<String,Integer> _docIDs = new HashMap<String,Integer>();

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
    this.invertedIndex = loaded.invertedIndex;
    this._docIDs = loaded._docIDs;

    for(ArrayList<ArrayList<Integer>> list: loaded.invertedIndex.values()){
        for(int i=0;i<list.size();i++){
          this._totalTermFrequency += list.get(i).size()-1;
        }
    }
    
    
    this.pointers = loaded.pointers;
    
    reader.close();

    System.out.println(Integer.toString(_numDocs) + " documents loaded " +
        "with " + Long.toString(this._totalTermFrequency) + " terms!");
  }

  @Override
  public Document getDoc(int docid) {
    return (Document)documents.get(docid);
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
    doc.setUrl(title);
    doc.setDocID(documents.size());
    int docid = doc.getDocID();
    HashMap<String,Integer> counts = new HashMap<String,Integer>();
    offset = readPostingList(title,docid,offset,counts);
    offset = readPostingList(body,docid,offset,counts);
    String url = title;
    doc.setTerms(counts);
    documents.add(doc);
    _docIDs.put(url,docid);
    ++_numDocs;

    
  }

  private int readPostingList(String content,int docid,int offset, HashMap<String,Integer> counts) {
    Scanner s = new Scanner(content);  // Uses white space by default.
    while (s.hasNext()) {
      String word = s.next();
      String token = stemming.stem(word);
      
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
          invertedIndex.put(token,document_list);


        }
        else{   
                int pre_doc = pointer_list.get(pointer_list.size()-1);
                pointer_list.add(docid);
                pointers.put(token,pointer_list);
                
                ArrayList<Integer> offset_list = new ArrayList<Integer>();
                offset_list.add(docid-pre_doc);
                offset_list.add(offset);
                document_list.add(offset_list);
                
                pointers.put(token,pointer_list);
                invertedIndex.put(token,document_list);


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
    if(tokens.size()>1){
    int first = next(tokens.get(0),docid);
    if (first == -1)
     return null;
   else{    
            int tmp=-1;
          for(int i =1; i<tokens.size();i++){
             tmp = next(tokens.get(i),docid);
            if(tmp == -1) return null;
            if(tmp !=first)
              return nextDoc(query, (Math.max(first,tmp))-1);

          }

          return documents.get(first);
   }
    
  }
  else {   
    
    int res = next(tokens.get(0),docid);
    if(res < 0){
      return null;
     }
    else 
      return documents.get(res);

  }
}

  private int next(String term, int docid){
    if(term.indexOf(" ") == -1){
       if(!invertedIndex.containsKey(term)) return -1;
       else{
             ArrayList<Integer> pointer_list = pointers.get(term);
             if(docid !=-1){
             int pre = pointer_list.indexOf(docid);
             if(pre == -1){             
              for(int i =0;i<pointer_list.size();i++){
                    if(pointer_list.get(i)>docid)
                      return pointer_list.get(i);
              }

              return -1;

             }
             if(pre == pointer_list.size()-1 ) 
                return -1;
              else {
                  int res = pointer_list.get(pre+1);
                  
                  return res;
              }
            }
            else{
             int res =  pointer_list.get(0);
             return res;
          }

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

    int first = nextNoPhase(words.get(0),docid);
    if (first == -1)
     return -1;
   else{
          for(int i =1; i<words.size();i++){
            int tmp = nextNoPhase(words.get(i),docid);
            if(tmp == -1) return -1;
            if(tmp !=first) return nextDocForPhase(term, Math.max(first,tmp)-1);
          }

          int pos = nextPhase(term,first,-1);
          if(pos != -1 ) return first;
          else return nextDocForPhase(term,first);
   }

  }

  private int nextNoPhase(String term, int docid){

    if(!invertedIndex.containsKey(term)) return -1;
       else{
             ArrayList<Integer> pointer_list = pointers.get(term);
             
             if(docid !=-1){
             int pre = pointer_list.indexOf(docid);
             
             if(pre == -1){
              
              for(int i =0;i<pointer_list.size();i++){
                    if(pointer_list.get(i)>docid)
                      return pointer_list.get(i);
              }

              return -1;

             }
             if(pre == pointer_list.size()-1 ) 
                return -1;
              else {
                  int res = pointer_list.get(pre+1);
                  
                  return res;
              }
            }
            else{
              
             int res =  pointer_list.get(0);
             return res;
          }

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
      pos = nextPhase(phase,docid,-1);
      while(pos !=-1){
        res++;
        pos = nextPhase(phase,docid,pos);
      }
      return res;
    }


  
  private int nextPosition(String term, int docid, int pos){
    int idx = pointers.get(term).indexOf(docid);
    ArrayList<Integer> offset_list = invertedIndex.get(term).get(idx);
    if(pos !=-1){
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
       return tmp;


    }
    else return -1;

   }

   else return offset_list.get(1);
    

  }

  @Override
  public int corpusDocFrequencyByTerm(String term) {
    if(term.indexOf(" ") == -1){
    if(invertedIndex.containsKey(term)){
       ArrayList<ArrayList<Integer>> l= invertedIndex.get(term);
       int res = l.size();
       return res;
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
    if(term.indexOf(" ") == -1){
    if(invertedIndex.containsKey(term)){
      int res = 0;
      ArrayList<ArrayList<Integer>> document_list = invertedIndex.get(term);
      for(int i = 0; i < document_list.size(); i++){
        ArrayList<Integer> tmp = document_list.get(i);
        int size = tmp.size()-1;
        res += size;

      }
      return res;

    }
    else return 0;
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
  public  int documentTermFrequency(String term, String url){
      
      int docid = _docIDs.get(url);
      if(term.indexOf(" ") ==-1){
        
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

   private void getStopWords(){
    List<Map.Entry<String, ArrayList<ArrayList<Integer>>>> infoIds = new ArrayList<Map.Entry<String, ArrayList<ArrayList<Integer>>>>(invertedIndex.entrySet());  
                    
        Collections.sort(infoIds, new Comparator<Map.Entry<String, ArrayList<ArrayList<Integer>>>>() {  
            public int compare(Map.Entry<String, ArrayList<ArrayList<Integer>>> o1,  
                    Map.Entry<String, ArrayList<ArrayList<Integer>>> o2) {  
                int a = o1.getValue().size();
                int b = o2.getValue().size();
                return b-a;
                //return Integer.toString(a).compareTo(Integer.toString(b));
            }  
        });  
           
        for (int i = 0; i < 50; i++) {  
            String word = infoIds.get(i).getKey().toString();  
            stopwords.add(word);
        }  

   }

}

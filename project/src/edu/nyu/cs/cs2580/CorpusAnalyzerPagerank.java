package edu.nyu.cs.cs2580;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
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
import java.util.ArrayList;
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

import edu.nyu.cs.cs2580.SearchEngine.Options;

/**
 * @CS2580: Implement this class for HW3.
 */
public class CorpusAnalyzerPagerank extends CorpusAnalyzer {

  private Map<String, Integer> docs = new HashMap<String, Integer>();
    private ArrayList<Integer> _docOutLinkCount = new ArrayList<Integer>();
    private ArrayList<Set<Integer> > _docInLink = new ArrayList<Set<Integer> >();
    private ArrayList<Double> _pageRank = new ArrayList<Double> ();
    private double lambda = 0.9;
    private int ite = 2;
  public CorpusAnalyzerPagerank(Options options) {
    super(options);
  }

  /**
   * This function processes the corpus as specified inside {@link _options}
   * and extracts the "internal" graph structure from the pages inside the
   * corpus. Internal means we only store links between two pages that are both
   * inside the corpus.
   * 
   * Note that you will not be implementing a real crawler. Instead, the corpus
   * you are processing can be simply read from the disk. All you need to do is
   * reading the files one by one, parsing them, extracting the links for them,
   * and computing the graph composed of all and only links that connect two
   * pages that are both in the corpus.
   * 
   * Note that you will need to design the data structure for storing the
   * resulting graph, which will be used by the {@link compute} function. Since
   * the graph may be large, it may be necessary to store partial graphs to
   * disk before producing the final graph.
   *
   * @throws IOException
   */
  @Override
  public void prepare() throws IOException {
    System.out.println("Preparing " + this.getClass().getName());
        initiate();
         File folder = new File(_options._corpusPrefix);
        
        for ( File fileEntry : folder.listFiles()) {
            
            
                process(fileEntry.getName());
            
        }
        //        outputInternalGraph();
        return;
  }

  /**
   * This function computes the PageRank based on the internal graph generated
   * by the {@link prepare} function, and stores the PageRank to be used for
   * ranking.
   * 
   * Note that you will have to store the computed PageRank with each document
   * the same way you do the indexing for HW2. I.e., the PageRank information
   * becomes part of the index and can be used for ranking in serve mode. Thus,
   * you should store the whatever is needed inside the same directory as
   * specified by _indexPrefix inside {@link _options}.
   *
   * @throws IOException
   */
  @Override
  public void compute() throws IOException {
    System.out.println("Computing " + this.getClass().getName());
        int totalDocs = _docOutLinkCount.size();
        System.out.println("Totaldocs:"+ totalDocs);
        for (int i = 0; i<_docOutLinkCount.size();i++) {
            _pageRank.add(1.0);
        }
        for (int i = 0; i<ite; i++) {
            for(int j =0; j< _docInLink.size();j++){
              
              double sum = 0.0;
              Set<Integer> tmp = _docInLink.get(j);
              for(Integer q : tmp){
                int sourceId = q;
                int a = _docOutLinkCount.get(sourceId);
                if(a == 0){
                  System.out.println("0 error!");
                  System.out.println("current doc:"+ q);
                  return;
                }
                sum += _pageRank.get(sourceId)/_docOutLinkCount.get(sourceId);

              }
              double newScore = lambda * sum + (1-lambda)/ totalDocs;
              _pageRank.set(j,newScore);
            }
        }

        System.out.println("Saving pagerank:");
        savePageRank();
        return;
  }
  

  /**
   * During indexing mode, this function loads the PageRank values computed
   * during mining mode to be used by the indexer.
   *
   * @throws IOException
   */
  @Override
  public Object load() throws IOException {
    System.out.println("Loading using " + this.getClass().getName());
    String load = _options._indexPrefix+ "/PageRank";
        Reader reader = new InputStreamReader(new FileInputStream(load));
        Gson gson = new Gson();
        ArrayList<Double> res = gson.fromJson(reader,
                          new TypeToken<ArrayList<Double>>() {}.getType()); 
         reader.close();
         return res;
  }

  public void savePageRank() throws IOException{
    String name = _options._logPrefix+ "/PageRank";
    Writer writer = new OutputStreamWriter(new FileOutputStream(name));
    Gson gson = new GsonBuilder().create();
    gson.toJson(_pageRank, writer);
    writer.close();
    _pageRank.clear();
  }


  private void process(String fileName) throws IOException{
        int sourceId = docs.get(fileName);
        int destId = 0;
        String wholePath = _options._corpusPrefix + "/" + fileName;
        File file = new File(wholePath);
        HeuristicLinkExtractor hle = new HeuristicLinkExtractor(file);
        String link = hle.getNextInCorpusLinkTarget();
        while (link!=null) {
            if (docs.get(link)!=null) {
                destId = docs.get(link);
                if (!_docInLink.get(destId).contains(sourceId)) {
                    
                    _docInLink.get(destId).add(sourceId);
                    _docOutLinkCount.set(sourceId,_docOutLinkCount.get(sourceId)+1);
                    //System.out.println("outlinkcount:"+_docOutLinkCount.get(sourceId));
                }
                
            }
            link = hle.getNextInCorpusLinkTarget();
        }
    }

    private void initiate() throws IOException {
         File folder = new File(_options._corpusPrefix);
         int docId = 0;
        if(folder.isDirectory()){
        for ( File fileEntry : folder.listFiles()) {
            if (!fileEntry.isDirectory()) {
                docs.put(fileEntry.getName(),docId++);
                _docOutLinkCount.add(0);
                _docInLink.add( new HashSet<Integer>());
            }
        }
    }
    else{
      System.out.println("Not a directory!");
    }
  }
}

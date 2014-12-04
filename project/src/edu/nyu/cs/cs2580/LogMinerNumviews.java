package edu.nyu.cs.cs2580;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.IllegalArgumentException;
import java.net.URLDecoder;
import java.io.File;
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
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Scanner;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.io.FileNotFoundException;

import edu.nyu.cs.cs2580.SearchEngine.Options;

/**
 * @CS2580: Implement this class for HW3.
 */
public class LogMinerNumviews extends LogMiner {

  public HashMap<String, Integer> docTitles;

  public LogMinerNumviews(Options options) {
    super(options);
  }

  /**
   * This function processes the logs within the log directory as specified by
   * the {@link _options}. The logs are obtained from Wikipedia dumps and have
   * the following format per line: [language]<space>[article]<space>[#views].
   * Those view information are to be extracted for documents in our corpus and
   * stored somewhere to be used during indexing.
   *
   * Note that the log contains view information for all articles in Wikipedia
   * and it is necessary to locate the information about articles within our
   * corpus.
   *
   * @throws IOException
   */
  @Override
  public void compute() throws IOException {
    System.out.println("Computing using " + this.getClass().getName());
    docTitles = getCorpus();
    //System.out.println("got corpus titles number " + docTitles.keySet().size());
    //parse the numVIew file line by line
    //int count = 0;
    int doc_num = docTitles.entrySet().size();
    ArrayList<Integer> numViews = new ArrayList<Integer>(doc_num);
    for(int i = 0; i< doc_num; i++){
      numViews.add(0);
    }
    File log = new File(_options._logPrefix);
    if(log.isDirectory()){
      for(File logFile: log.listFiles()){
        if(logFile.getName().matches("(.*)log")){
            //System.out.println("about to read");
            BufferedReader in = new BufferedReader(new FileReader(logFile));
            String line;
            while ((line = in.readLine()) != null) {
               // process the line.          
             // System.out.println(line);
              String[] tokens = line.split("\\s");          
              if(tokens.length==3){
                if(tokens[2].matches("[0-9]+")){
                 // System.out.println("before try");
                    try{
                        String result = java.net.URLDecoder.decode(tokens[1], "UTF-8");                    
                        if(docTitles.containsKey(result)){
                            //docTitles.put(result, Integer.valueOf(tokens[2]));
                            int docId = docTitles.get(result);
                            //System.out.println(result +": " + docId);
                            int value = Integer.valueOf(tokens[2]);
                            //System.out.println(value);
                            numViews.set(docId, value);
                            //System.out.println(result + ", " + docId + ": " +tokens[2]);
                            //count++;
                        }
                    }catch(IllegalArgumentException e){

                    }
                }
              }
            }
            in.close();
        }

      }
    }
    writeLog(numViews);
    return;
  }

  /*
   *The method read through the corpus and get all doc title and save it to a hashamp
  */

  public HashMap<String, Integer> getCorpus() throws IOException{
    HashMap<String, Integer> corpus = new HashMap<String, Integer>(10000);
    int count = 0;
    File corpusDirectory = new File(_options._corpusPrefix);    
    if(corpusDirectory.isDirectory()){
      for(File corpusFile :corpusDirectory.listFiles()){  
        String title = corpusFile.getName();
        corpus.put(title,count++);
      }
    }
    return corpus;
  }

  public void writeLog(ArrayList<Integer> numViews) throws IOException{
    String name = _options._logPrefix + "/numView";
    Writer writer = new OutputStreamWriter(new FileOutputStream(name));
    Gson gson = new GsonBuilder().create();
    gson.toJson(numViews, writer);
    writer.close();
    //System.out.println(" write numView" );
  }

  /**
   * During indexing mode, this function loads the numViews values computed
   * during mining mode to be used by the indexer.
   * 
   * @throws IOException
   */
  @Override
  public Object load() throws IOException{

    Gson gson = new Gson();
    String logName = _options._logPrefix+ "/numView";
    Reader reader = new InputStreamReader(new FileInputStream(logName));
    ArrayList<Integer> numViews = gson.fromJson(reader,
                      new TypeToken<ArrayList<Integer>>() {}.getType()); 
    reader.close();
    System.out.println("Loading using " + this.getClass().getName());
    return null;
  }
}

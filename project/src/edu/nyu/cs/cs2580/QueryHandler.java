package edu.nyu.cs.cs2580;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Vector;
import java.util.HashMap;
import java.util.Map;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map.Entry;

import java.util.regex.Pattern;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.io.FileOutputStream;
import java.io.Writer;
import java.io.FileInputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.InputStreamReader;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import edu.nyu.cs.cs2580.SearchEngine.Options;

/**
 * Handles each incoming query, students do not need to change this class except
 * to provide more query time CGI arguments and the HTML output.
 * 
 * N.B. This class is not thread-safe. 
 * 
 * @author congyu
 * @author fdiaz
 */
class QueryHandler implements HttpHandler {

  /**
   * CGI arguments provided by the user through the URL. This will determine
   * which Ranker to use and what output format to adopt. For simplicity, all
   * arguments are publicly accessible.
   */
  public static class CgiArguments {
    // The raw user query
    public String _query = "";
    // How many results to return
    private int _numResults = 10;

    private int _numDocs = 10;
    private int _numTerms = 10;
    
    // The type of the ranker we will be using.
    public enum RankerType {
      NONE,
      FULLSCAN,
      CONJUNCTIVE,
      FAVORITE,
      COSINE,
      PHRASE,
      QL,
      LINEAR,
      COMPREHENSIVE,
    }
    public RankerType _rankerType = RankerType.NONE;
    
    // The output format.
    public enum OutputFormat {
      TEXT,
      HTML,
    }
    //public OutputFormat _outputFormat = OutputFormat.TEXT;
    public OutputFormat _outputFormat = OutputFormat.HTML;

    public CgiArguments(String uriQuery) {
      String[] params = uriQuery.split("&");
      for (String param : params) {
        String[] keyval = param.split("=", 2);
        if (keyval.length < 2) {
          continue;
        }
        String key = keyval[0].toLowerCase();
        String val = keyval[1];
        if (key.equals("query")) {
          _query = val;
        } else if (key.equals("num")) {
          try {
            _numResults = Integer.parseInt(val);
          } catch (NumberFormatException e) {
            // Ignored, search engine should never fail upon invalid user input.
          }
        } else if (key.equals("ranker")) {
          try {
            _rankerType = RankerType.valueOf(val.toUpperCase());
          } catch (IllegalArgumentException e) {
            // Ignored, search engine should never fail upon invalid user input.
          }
        } else if (key.equals("format")) {
          try {
            _outputFormat = OutputFormat.valueOf(val.toUpperCase());
          } catch (IllegalArgumentException e) {
            // Ignored, search engine should never fail upon invalid user input.
          }
        } else if (key.equals("numdocs")){
          try{
            _numDocs = Integer.parseInt(val);
          } catch (IllegalArgumentException e) {
            // Ignored, search engine should never fail upon invalid user input.
          }
        } else if (key.equals("numterms")){
          try{
            _numTerms = Integer.parseInt(val);
          } catch (IllegalArgumentException e) {
            // Ignored, search engine should never fail upon invalid user input.
          }
        }
      }  // End of iterating over params
    }
  }

  // For accessing the underlying documents to be used by the Ranker. Since 
  // we are not worried about thread-safety here, the Indexer class must take
  // care of thread-safety.
  private Indexer _indexer;

  public QueryHandler(Options options, Indexer indexer) {
    _indexer = indexer;
  }

  private void respondWithMsg(HttpExchange exchange, final String message)
      throws IOException {
    Headers responseHeaders = exchange.getResponseHeaders();
    responseHeaders.set("Content-Type", "text/html");
    exchange.sendResponseHeaders(200, 0); // arbitrary number of bytes
    OutputStream responseBody = exchange.getResponseBody();
    responseBody.write(message.getBytes());
    responseBody.close();
  }

  private void constructTextOutput(
      final Vector<ScoredDocument> docs, StringBuffer response) {
    for (ScoredDocument doc : docs) {
      response.append(response.length() > 0 ? "\n" : "");
      response.append(doc.asTextResult());
    }
    response.append(response.length() > 0 ? "\n" : "");
  }

  private void constructHTMLOutput(
      final Vector<ScoredDocument> docs, StringBuffer response) {
    response.append("<html><body>");
    response.append("<ul>");
    for (ScoredDocument doc : docs) {
      response.append("<li>");
      response.append(doc.asTextResult());
      response.append("</li>");
    }
    response.append("</ul>");
    response.append("</body></html>");
  }

  public Map<String, Map<String, String>> queryToMap(String query, String name) throws IOException {
    Gson gson = new Gson();
    Reader reader = new InputStreamReader(new FileInputStream(name));
    Map<String, Map<String, String>> adMap = new HashMap<String, Map<String, String>>();
    if(reader.ready()){
      adMap = gson.fromJson(reader,
                      new TypeToken<Map<String, Map<String, String>>>() {}.getType());
    }    
    reader.close();
    String[] params = query.split("&");
    String word = params[0].split("=")[1];
    String companyName = params[1].split("=")[1];
    String advertisingName = params[2].split("=")[1];
    String price = params[3].split("=")[1];
    if(adMap.containsKey(word)){
      Map<String, String> temp = adMap.get(word);     
      temp.put(companyName, advertisingName+"\t"+price);
      adMap.put(word,temp);
    }else{
      Map<String, String> result = new HashMap<String, String>();
      result.put(companyName, advertisingName+"\t"+price);
      adMap.put(word,result);
    }
    return new HashMap<String, Map<String, String>>(adMap);
  }

  public void handle(HttpExchange exchange) throws IOException {
    String requestMethod = exchange.getRequestMethod();

    if (!requestMethod.equalsIgnoreCase("GET")) { // GET requests only.
      return;
    }

    // Print the user request header.
    Headers requestHeaders = exchange.getRequestHeaders();
    System.out.print("Incoming request: ");
    for (String key : requestHeaders.keySet()) {
      System.out.print(key + ":" + requestHeaders.get(key) + "; ");
    }
    System.out.println();

    // Validate the incoming request.
    String uriPath = exchange.getRequestURI().getPath();
    if(uriPath.equals("/ads")){
      StringBuffer response = new StringBuffer();
      response.append("<!DOCTYPE html><html><head><title>Home</title><link rel=\"stylesheet\" href=\"https://maxcdn.bootstrapcdn.com/bootstrap/3.3.1/css/bootstrap.min.css\"></head><body><div class=\"container\"><div class=\"header\"><h3 class=\"text-muted\">Advertising Auction</h3></div><div class=\"jumbotron\"><h3>Choose a word to bid:</h3><br><br><ul class=\"list-group\"><a href=\"http://localhost:25805/ads/car\"><li class=\"list-group-item\">Car</li></a><a href=\"http://localhost:25805/ads/scinece\"><li class=\"list-group-item\">Science</li></a><a href=\"http://localhost:25805/ads/technology\"><li class=\"list-group-item\">Technology</li></a><a href=\"http://localhost:25805/ads/school\"><li class=\"list-group-item\">School</li></a><a href=\"http://localhost:25805/ads/music\"><li class=\"list-group-item\">Music</li></a></ul></div></div></body></html>");
      respondWithMsg(exchange, response.toString());
    }
    if(uriPath.equals("/ads/bid")){
      String name = "data/ads/ad.json";
      Map<String, Map<String, String>> params = queryToMap(exchange.getRequestURI().getQuery(), name);
      Writer writer = new OutputStreamWriter(new FileOutputStream(name));
      Gson gson = new GsonBuilder().create();
      gson.toJson(params, writer);
      writer.close();
      StringBuffer response = new StringBuffer();
      response.append("<!DOCTYPE html><html><head><title>Bidding Done</title><link rel=\"stylesheet\" href=\"https://maxcdn.bootstrapcdn.com/bootstrap/3.3.1/css/bootstrap.min.css\"></head><body><div class=\"container\"><div class=\"header\"><h3 class=\"text-muted\">Advertising Auction</h3></div><div class=\"jumbotron\"><h2>Congratulations! You have successfully bidden!</h2></body></html>");
      respondWithMsg(exchange, response.toString());
    }
    if(Pattern.matches("/ads/.*",uriPath)){
      StringBuffer response = new StringBuffer();
      String[] urls = uriPath.split("/");
      String word = urls[urls.length-1];
      response.append("<!DOCTYPE html><html><head><title>Bid</title><link rel=\"stylesheet\" href=\"https://maxcdn.bootstrapcdn.com/bootstrap/3.3.1/css/bootstrap.min.css\"></head><body><div class=\"container\"><div class=\"header\"><h3 class=\"text-muted\">Advertising Auction</h3></div><div class=\"jumbotron\"><h3>Bidding: "+word+"</h3><br><br><form role=\"form\" action=\"http://localhost:25805/ads/bid\" method=\"GET\" enctype=\"multipart/form-data\"><div><input type=\"hidden\" name=\"word\" value=\""+word+"\"></div><div class=\"form-group\"><label for=\"Company Name\">Company Name</label><input type=\"text\" class=\"form-control\" name=\"companyName\" placeholder=\"Enter compamy name\"></div><div class=\"form-group\"><label for=\"Advertising Name\">Advertising</label><input type=\"text\" class=\"form-control\" name=\"advertisingName\" placeholder=\"Enter advertising name\"></div><div class=\"form-group\"><label for=\"Price\">Bid Price</label><div class=\"input-group\"><span class=\"input-group-addon\">$</span><input type=\"text\" class=\"form-control\" name=\"price\" placeholder=\"Enter bid price\"></div></div><button type=\"submit\" class=\"btn btn-success\">Submit</button></form></div></div></body></html>");
      respondWithMsg(exchange, response.toString());
    }



    String uriQuery = exchange.getRequestURI().getQuery();
    uriQuery = uriQuery.toLowerCase();
    uriQuery = uriQuery.replace('+',' ');
    if (uriPath == null || uriQuery == null) {
      respondWithMsg(exchange, "<html><body>Something wrong with the URI!</body></html>");
    }
    if (!uriPath.equals("/search") && !uriPath.equals("/prf") && !uriPath.equals("/ads")) {
      respondWithMsg(exchange, "<html><body>Only /search, /prf and /ads is handled!</body></html>");
    }
    System.out.println("Query: " + uriQuery);

    // Process the CGI arguments.
    CgiArguments cgiArgs = new CgiArguments(uriQuery);
    if (cgiArgs._query.isEmpty()) {
      respondWithMsg(exchange, "<html><body>No query is given!</body></html>");
    }

    // Create the ranker.
    Ranker ranker = Ranker.Factory.getRankerByArguments(
        cgiArgs, SearchEngine.OPTIONS, _indexer);
    if (ranker == null) {
      respondWithMsg(exchange,
          "<html><body>Ranker " + cgiArgs._rankerType.toString() + " is not valid!</body></html>");
    }

    // Processing the query.
    QueryPhrase processedQuery = new QueryPhrase(cgiArgs._query);
    processedQuery.processQuery();

    if(uriPath.equals("/search")){
      // Ranking.
      Vector<ScoredDocument> scoredDocs =
          ranker.runQuery(processedQuery, cgiArgs._numResults);
      StringBuffer response = new StringBuffer();
      switch (cgiArgs._outputFormat) {
      case TEXT:
        constructTextOutput(scoredDocs, response);
        break;
      case HTML:
        // @CS2580: Plug in your HTML output
        constructHTMLOutput(scoredDocs, response);
        break;
      default:
        // nothing
      }
      respondWithMsg(exchange, response.toString());
      System.out.println("Finished query: " + cgiArgs._query);
    }else if(uriPath.equals("/prf")){
      QueryRepresentation queryRepresentation = new QueryRepresentation(cgiArgs._numDocs,cgiArgs._numTerms,ranker,processedQuery, _indexer);
      HashMap<String, Double> result = queryRepresentation.computeRepresentations();
      Set<Entry<String, Double>> set = result.entrySet();
      List<Entry<String, Double>> list = new ArrayList<Entry<String, Double>>(set);
      Collections.sort( list, new Comparator<Map.Entry<String, Double>>()
      {
        public int compare( Map.Entry<String, Double> o1, Map.Entry<String, Double> o2 )
        {
            return (o2.getValue()).compareTo( o1.getValue() );
        }
      });
      StringBuffer response = new StringBuffer();
      for(Map.Entry<String,Double> entry:list){
        response.append(entry.getKey()+"\t"+entry.getValue()+"\n");
      }
      respondWithMsg(exchange, response.toString());
      System.out.println("Finished query: " + cgiArgs._query);
    }

  }
}


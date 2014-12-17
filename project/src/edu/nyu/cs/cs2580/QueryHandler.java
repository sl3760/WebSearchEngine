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
import java.io.File;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.UUID;

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
      ADS,
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
  private Indexer _docIndexer;
  private Indexer _adsIndexer;


  public QueryHandler(Options options, Indexer indexer1, Indexer indexer2) {
    _docIndexer = indexer1;
    _adsIndexer = indexer2;
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
      final Vector<ScoredDocument> docs, final Vector<ScoredDocument> ads_docs, StringBuffer response) {
    for (ScoredDocument doc : docs) {
      response.append(response.length() > 0 ? "\n" : "");
      response.append(doc.asTextResult());
    }

    for (ScoredDocument doc : ads_docs) {
      response.append(response.length() > 0 ? "\n" : "");
      response.append(doc.asTextResult());
    }
    response.append(response.length() > 0 ? "\n" : "");

  }

  //need to modify
  private void constructHTMLOutput(
      final Vector<ScoredDocument> docs, final Vector<ScoredDocument> ads_docs, String sessionID,  String query, StringBuffer response) {
    response.append("<!DOCTYPE html><html><head><meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\" ><title>Bingle</title><link rel=\"stylesheet\" href=\"https://maxcdn.bootstrapcdn.com/bootstrap/3.3.1/css/bootstrap.min.css\"></head><body><div class=\"container\"><div class=\"header\"><h3 class=\"text-muted\">Bingle</h3></div><form role=\"form\" action=\"http://localhost:25805/search\" method=\"GET\" enctype=\"multipart/form-data\"><div class=\"input-group\"><div><input type=\"hidden\" name=\"ranker\" value=\"comprehensive\"></div><input type=\"text\" class=\"form-control\" name=\"query\"><div class=\"input-group-btn\"><button type=\"submit\" class=\"btn btn-success\">Bingle</button></div></div></form><br><br><div class=\"row\">");
    response.append("<div class=\"col-xs-12 col-md-8\"><ul class=\"list-group\">");
    for (ScoredDocument doc : docs) {
      response.append("<li class=\"list-group-item list-group-item-success\">");
      response.append("<h3><a href=\"http://localhost:25805/search/wiki?title="+doc.asTextResult()+"\">");
      response.append(doc.asTextResult());
      response.append("</a></h3>");
      response.append("</li>");
    }
    response.append("</ul></div>");
    response.append("<div class=\"col-xs-6 col-md-4\"><ul class=\"list-group\">");
    if(ads_docs !=null){
      for (ScoredDocument ad_doc : ads_docs) {
        response.append("<li class=\"list-group-item list-group-item-info\">");
        response.append("<div><h3><a href=\"http://localhost:25805/search/ads?title="+ad_doc.asTextResult()+"&sessionID="+sessionID+"&compamyID="+ad_doc.getCompany_ads()+"&query="+query+"\">");
        response.append(ad_doc.asTextResult());
        response.append("</a></h3></div>");
        response.append("<div><h5>"+ad_doc.getBody()+"</h5></div>");
        response.append("</li>");
      }
    }
    
    response.append("</ul></div></div></body></html>");
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

    String ctrName = "data/ads/CTR.json";
    reader = new InputStreamReader(new FileInputStream(ctrName));
    Map<String, Map<String, String>> ctrMap = new HashMap<String, Map<String, String>>();
    if(reader.ready()){
      ctrMap = gson.fromJson(reader,
                      new TypeToken<Map<String, Map<String, String>>>() {}.getType());
    }    
    reader.close();
    Writer writer = new OutputStreamWriter(new FileOutputStream(ctrName));
    Map<String, String> res = new HashMap<String, String>();

    if(ctrMap.containsKey(word)){
      res = ctrMap.get(word);
    }

    int n = 0;
    Double sumCTR = 0.0;

    for(Map.Entry<String, Map<String, String>> entryMap : ctrMap.entrySet()){
      Map<String, String> crtRes = entryMap.getValue();
      for(Map.Entry<String,String> entry : crtRes.entrySet()){
        String id = entry.getKey();
        String ctr = entry.getValue();
        String[] vals = ctr.split("\\+");
        if(id.indexOf(companyName)!=-1){
          sumCTR += Double.parseDouble(vals[0]);
          n++;
        }
      }
    }
     
    Double oriCTR = 0.1;
    if(n!=0){
      oriCTR = sumCTR/n;
    }

    res.put(companyName+"_"+advertisingName, oriCTR+"+F+F");
    ctrMap.put(word,res);
    gson.toJson(ctrMap, writer);
    writer.close();

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
    if(uriPath.equals("/")){
      StringBuffer response = new StringBuffer();
      response.append("<!DOCTYPE html><html><head><meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\" ><title>Bingle</title><link rel=\"stylesheet\" href=\"https://maxcdn.bootstrapcdn.com/bootstrap/3.3.1/css/bootstrap.min.css\"></head><body><div class=\"container\"><div class=\"jumbotron\"><center><h1>Bingle</h1></center><br><br><form role=\"form\" action=\"http://localhost:25805/search\" method=\"GET\" enctype=\"multipart/form-data\"><div><input type=\"hidden\" name=\"ranker\" value=\"comprehensive\"></div><div class=\"form-group\"><input type=\"text\" class=\"form-control\" name=\"query\"></div><br><br><center><button type=\"submit\" class=\"btn btn-success\">Bingle Search</button></center></form></div></div></body></html>");
      respondWithMsg(exchange, response.toString());
    }
    if(uriPath.equals("/createads")){
      StringBuffer response = new StringBuffer();
      response.append("<!DOCTYPE html><html><head><meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\" ><title>Bid</title><link rel=\"stylesheet\" href=\"https://maxcdn.bootstrapcdn.com/bootstrap/3.3.1/css/bootstrap.min.css\"></head><body><div class=\"container\"><div class=\"header\"><h3 class=\"text-muted\">Advertising Auction</h3></div><div class=\"jumbotron\"><h3>Create Bidding Ads</h3><br><br><form role=\"form\" action=\"http://localhost:25805/ads/create\" method=\"GET\" enctype=\"multipart/form-data\"><div class=\"form-group\"><label for=\"Company Name\">Company Name</label><input type=\"text\" class=\"form-control\" name=\"companyName\" placeholder=\"Enter compamy name\" required></div><div class=\"form-group\"><label for=\"Advertising ID\">Advertising</label><input type=\"text\" class=\"form-control\" name=\"advertisingName\" placeholder=\"Enter advertising id\" required></div><div class=\"form-group\"><label for=\"title\">Title</label><input type=\"text\" class=\"form-control\" name=\"title\" placeholder=\"Enter title\" required></div><div class=\"form-group\"><label for=\"Description\">Description</label><input type=\"text\" class=\"form-control\" name=\"description\" placeholder=\"Enter description\" required></div><div class=\"form-group\"><label for=\"URL\">URL</label><input type=\"text\" class=\"form-control\" name=\"url\" placeholder=\"Enter URL\" required></div><button type=\"submit\" class=\"btn btn-success\">Submit</button></form></div></div></body></html>");
      respondWithMsg(exchange, response.toString());
    }
    if(uriPath.equals("/ads")){
      StringBuffer response = new StringBuffer();
      response.append("<!DOCTYPE html><html><head><meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\" ><title>Home</title><link rel=\"stylesheet\" href=\"https://maxcdn.bootstrapcdn.com/bootstrap/3.3.1/css/bootstrap.min.css\"></head><body><div class=\"container\"><div class=\"header\"><h3 class=\"text-muted\">Advertising Auction</h3></div><div class=\"jumbotron\"><h3>Choose a word to bid:</h3><br><br><ul class=\"list-group\"><a href=\"http://localhost:25805/ads/car\"><li class=\"list-group-item\">Car</li></a><a href=\"http://localhost:25805/ads/science\"><li class=\"list-group-item\">Science</li></a><a href=\"http://localhost:25805/ads/tech\"><li class=\"list-group-item\">Tech</li></a><a href=\"http://localhost:25805/ads/school\"><li class=\"list-group-item\">School</li></a><a href=\"http://localhost:25805/ads/radio\"><li class=\"list-group-item\">Radio</li></a></ul></div></div></body></html>");
      respondWithMsg(exchange, response.toString());
    }
    if(uriPath.equals("/ads/create")){
      String query = exchange.getRequestURI().getQuery();
      query = query.replace('+',' ');
      String[] params = query.split("&");
      String company = params[0].split("=")[1];
      String advertising = params[1].split("=")[1];
      String title = params[2].split("=")[1];
      String description = params[3].split("=")[1];
      String url = params[4].split("=")[1];
      String fileName = "data/ads/ads.tsv";
      File file = new File(fileName);
      BufferedWriter out = new BufferedWriter(new FileWriter(file,true));  
      out.write(company+"_"+advertising+"\t"+title+"\t"+description+"\t"+url+"\n");
      out.close();
      StringBuffer response = new StringBuffer();
      response.append("<!DOCTYPE html><html><head><meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\" ><title>Bidding Done</title><link rel=\"stylesheet\" href=\"https://maxcdn.bootstrapcdn.com/bootstrap/3.3.1/css/bootstrap.min.css\"></head><body><div class=\"container\"><div class=\"header\"><h3 class=\"text-muted\">Advertising Auction</h3></div><div class=\"jumbotron\"><h2>Congratulations! You have successfully created an AD!</h2></body></html>");
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
      response.append("<!DOCTYPE html><html><head><meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\" ><title>Bidding Done</title><link rel=\"stylesheet\" href=\"https://maxcdn.bootstrapcdn.com/bootstrap/3.3.1/css/bootstrap.min.css\"></head><body><div class=\"container\"><div class=\"header\"><h3 class=\"text-muted\">Advertising Auction</h3></div><div class=\"jumbotron\"><h2>Congratulations! You have successfully bidden!</h2></body></html>");
      respondWithMsg(exchange, response.toString());
    }

    if(Pattern.matches("/ads/.*",uriPath)){
      StringBuffer response = new StringBuffer();
      String[] urls = uriPath.split("/");
      String word = urls[urls.length-1];
      response.append("<!DOCTYPE html><html><head><meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\" ><title>Bid</title><link rel=\"stylesheet\" href=\"https://maxcdn.bootstrapcdn.com/bootstrap/3.3.1/css/bootstrap.min.css\"></head><body><div class=\"container\"><div class=\"header\"><h3 class=\"text-muted\">Advertising Auction</h3></div><div class=\"jumbotron\"><h3>Bidding: "+word+"</h3><br><br><form role=\"form\" action=\"http://localhost:25805/ads/bid\" method=\"GET\" enctype=\"multipart/form-data\"><div><input type=\"hidden\" name=\"word\" value=\""+word+"\"></div><div class=\"form-group\"><label for=\"Company Name\">Company Name</label><input type=\"text\" class=\"form-control\" name=\"companyName\" placeholder=\"Enter compamy name\" required></div><div class=\"form-group\"><label for=\"Advertising ID\">Advertising</label><input type=\"text\" class=\"form-control\" name=\"advertisingName\" placeholder=\"Enter advertising id\" required></div><div class=\"form-group\"><label for=\"Price\">Bid Price</label><div class=\"input-group\"><span class=\"input-group-addon\">$</span><input type=\"text\" class=\"form-control\" name=\"price\" placeholder=\"Enter bid price\" required></div></div><button type=\"submit\" class=\"btn btn-success\">Submit</button></form></div></div></body></html>");
      respondWithMsg(exchange, response.toString());
    }

    if(uriPath.equals("/search/wiki")){
      String uriQuery = exchange.getRequestURI().getQuery();
      String title = uriQuery.split("=")[1];
      String fileName = "data/wiki/"+title;
      File file = new File(fileName);
      Headers responseHeaders = exchange.getResponseHeaders();
      responseHeaders.set("Content-Type", "text/html");
      exchange.sendResponseHeaders(200, 0); // arbitrary number of bytes
      OutputStream responseBody = exchange.getResponseBody();
      FileInputStream fs = new FileInputStream(file);
      final byte[] buffer = new byte[0x10000];
      int count = 0;
      while ((count = fs.read(buffer)) >= 0) {
        responseBody.write(buffer,0,count);
      }
      fs.close();
      responseBody.close();
    }
    
    if(uriPath.equals("/search/ads")){
      String uriQuery = exchange.getRequestURI().getQuery();
      String[] paras = uriQuery.split("&");
      String title = paras[0].split("=")[1];
      String sessionID = paras[1].split("=")[1];
      String companyID = paras[2].split("=")[1];
      String query = paras[3].split("=")[1];
      String logName = "data/ads/log.json";
      Gson gson = new Gson();
      Reader reader = new InputStreamReader(new FileInputStream(logName));
      Map<String, Map<String, String>> adLogMap = new HashMap<String, Map<String,String>>();
      adLogMap = gson.fromJson(reader,
                        new TypeToken<Map<String, Map<String, String>>>() {}.getType());
      reader.close();
      Map<String,String> map = adLogMap.get("adLogs");
      if(map.containsKey(sessionID)){
        String ad = map.get(sessionID);
        map.put(sessionID,ad+title);
      }
      adLogMap.put("adLogs",map);
      Map<String, Map<String, String>> res = new HashMap<String, Map<String, String>>(adLogMap);
      Writer writer = new OutputStreamWriter(new FileOutputStream(logName));
      gson = new GsonBuilder().create();
      gson.toJson(res, writer);
      writer.close();

      // update click records
      String ctrName = "data/ads/CTR.json";
      reader = new InputStreamReader(new FileInputStream(ctrName));
      Map<String, Map<String, String>> ctrMap = new HashMap<String, Map<String, String>>();
      if(reader.ready()){
        ctrMap = gson.fromJson(reader,
                        new TypeToken<Map<String, Map<String, String>>>() {}.getType());
      }    
      reader.close();
      for(Map.Entry<String, Map<String, String>> entryMap : ctrMap.entrySet()){
        String key = entryMap.getKey();
        if(query.indexOf(key)!=-1){
          Map<String, String> crtRes = ctrMap.get(key);
          for(Map.Entry<String,String> entry : crtRes.entrySet()){
            String id = entry.getKey();
            String ctr = entry.getValue();
            String[] vals = ctr.split("\\+");
            if(id.equals(companyID)){
              crtRes.put(id,vals[0]+"+"+vals[1]+"+T");
            }else{
              crtRes.put(id,vals[0]+"+"+vals[1]+"+F");
            }
          }
          ctrMap.put(key,crtRes);
          writer = new OutputStreamWriter(new FileOutputStream(ctrName));
          gson.toJson(ctrMap, writer);
          writer.close();
        }
      }
      
      /*
      String fileName = "data/ads/content/"+title;
      File file = new File(fileName);
      Headers responseHeaders = exchange.getResponseHeaders();
      responseHeaders.set("Content-Type", "text/html");
      exchange.sendResponseHeaders(200, 0); // arbitrary number of bytes
      OutputStream responseBody = exchange.getResponseBody();
      FileInputStream fs = new FileInputStream(file);
      final byte[] buffer = new byte[0x10000];
      int count = 0;
      while ((count = fs.read(buffer)) >= 0) {
        responseBody.write(buffer,0,count);
      }
      fs.close();
      responseBody.close();
      */

      String fileName = "data/ads/img/"+title+".jpg";
      File file = new File(fileName);
      Headers responseHeaders = exchange.getResponseHeaders();
      responseHeaders.set("Content-Type", "image/jpg");
      exchange.sendResponseHeaders(200, 0); // arbitrary number of bytes
      OutputStream responseBody = exchange.getResponseBody();
      FileInputStream fs = new FileInputStream(file);
      final byte[] buffer = new byte[0x10000];
      int count = 0;
      while ((count = fs.read(buffer)) >= 0) {
        responseBody.write(buffer,0,count);
      }
      fs.close();
      responseBody.close();
      
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
      StringBuffer response = new StringBuffer();
      response.append("<!DOCTYPE html><html><head><title>Bingle</title><link rel=\"stylesheet\" href=\"https://maxcdn.bootstrapcdn.com/bootstrap/3.3.1/css/bootstrap.min.css\"></head><body><div class=\"container\"><div class=\"jumbotron\"><center><h1>Bingle</h1></center><br><br><form role=\"form\" action=\"http://localhost:25805/search\" method=\"GET\" enctype=\"multipart/form-data\"><div><input type=\"hidden\" name=\"ranker\" value=\"comprehensive\"></div><div class=\"form-group\"><input type=\"text\" class=\"form-control\" name=\"query\"></div><br><br><center><button type=\"submit\" class=\"btn btn-success\">Bingle Search</button></center></form></div></div></body></html>");
      respondWithMsg(exchange, response.toString());
    }

    // Create the ranker.
    Ranker ranker = Ranker.Factory.getRankerByArguments(
        cgiArgs, SearchEngine.OPTIONS, _docIndexer);
    if (ranker == null) {
      respondWithMsg(exchange,
          "<html><body>Ranker " + cgiArgs._rankerType.toString() + " is not valid!</body></html>");
    }
    //create the ads ranker
    Ranker adsRanker = new AdsRanker(SearchEngine.OPTIONS, cgiArgs, _adsIndexer);
    if (adsRanker == null) {
      respondWithMsg(exchange,
          "<html><body>AdsRanker is not valid!</body></html>");
    }
    // Processing the query.
    QueryPhrase processedQuery = new QueryPhrase(cgiArgs._query);
    processedQuery.processQuery();

    if(uriPath.equals("/search")){
      // Ranking.
      Vector<ScoredDocument> scoredDocs =
          ranker.runQuery(processedQuery, cgiArgs._numResults);
      Vector<ScoredDocument> scoredDocs_ads =
          adsRanker.runQuery(processedQuery, cgiArgs._numResults); 

      String logName = "data/ads/log.json";
      Gson gson = new Gson();
      Reader reader = new InputStreamReader(new FileInputStream(logName));
      Map<String, Map<String, String>> adLogMap = new HashMap<String, Map<String, String>>();
      if(reader.ready()){
        adLogMap = gson.fromJson(reader,
                        new TypeToken<Map<String, Map<String, String>>>() {}.getType());
      }    
      if(adLogMap==null||adLogMap.size()==0){
        adLogMap.put("adLogs", new HashMap<String, String>());
      }
      reader.close();
      StringBuilder sb = new StringBuilder();
      sb.append(cgiArgs._query+"\t");
      for(int i=0; i<scoredDocs_ads.size();i++){
        sb.append(scoredDocs_ads.get(i).asTextResult());
        if(i!=scoredDocs_ads.size()-1){
          sb.append("+");
        }
      }
      sb.append("\t");
      sb.append("");
      String sessionID = String.valueOf(UUID.randomUUID());
      Map<String, String> map = adLogMap.get("adLogs");
      map.put(sessionID,sb.toString());     
      adLogMap.put("adLogs",map);
      Writer writer = new OutputStreamWriter(new FileOutputStream(logName));
      gson = new GsonBuilder().create();
      gson.toJson(adLogMap, writer);
      writer.close();
      
      System.out.println("ad size: "+scoredDocs_ads.size());

      // update display records
      String ctrName = "data/ads/CTR.json";
      reader = new InputStreamReader(new FileInputStream(ctrName));
      Map<String, Map<String, String>> ctrMap = new HashMap<String, Map<String, String>>();
      if(reader.ready()){
        ctrMap = gson.fromJson(reader,
                        new TypeToken<Map<String, Map<String, String>>>() {}.getType());
      }    
      reader.close();
      for(ScoredDocument ad:scoredDocs_ads){
        for(Map.Entry<String, Map<String, String>> entry : ctrMap.entrySet()){
          System.out.println("cgiArgs._query: "+cgiArgs._query);
          if(cgiArgs._query.indexOf(entry.getKey())!=-1){
            Map<String, String> res = entry.getValue();
            for(Map.Entry<String,String> entryCTR : res.entrySet()){
              String id = entryCTR.getKey();
              String ctr = entryCTR.getValue();
              String[] vals = ctr.split("\\+");
              if(id.equals(ad.getCompany_ads())){
                res.put(id,vals[0]+"+T+"+vals[2]);
              }
            }
            ctrMap.put(entry.getKey(),res);
          }
        }
      }
      writer = new OutputStreamWriter(new FileOutputStream(ctrName));
      gson.toJson(ctrMap, writer);
      writer.close();

      StringBuffer response = new StringBuffer();
      switch (cgiArgs._outputFormat) {
      case TEXT:
        constructTextOutput(scoredDocs, scoredDocs_ads, response);
        break;
      case HTML:
        // @CS2580: Plug in your HTML output
        //****************************
        //need to pass scoredDocs as well
        constructHTMLOutput(scoredDocs, scoredDocs_ads, sessionID, cgiArgs._query, response);
        break;
      default:
        // nothing
      }
      respondWithMsg(exchange, response.toString());
      System.out.println("Finished query: " + cgiArgs._query);
    }else if(uriPath.equals("/prf")){
      QueryRepresentation queryRepresentation = new QueryRepresentation(cgiArgs._numDocs,cgiArgs._numTerms,ranker,processedQuery, _docIndexer);
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


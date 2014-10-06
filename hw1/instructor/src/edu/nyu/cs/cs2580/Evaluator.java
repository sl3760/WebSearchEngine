package edu.nyu.cs.cs2580;

import java.io.IOException;
import java.io.File;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Vector;
import java.util.HashMap;
import java.util.Scanner;
import java.util.Map;
import java.util.Arrays;
import java.io.File;
import java.io.PrintWriter;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.FileNotFoundException;

class Evaluator {

  public static void main(String[] args) throws IOException {
    //query is the key,  and the value hashmap is the score for relevance files
    HashMap < String , HashMap < Integer , Double > > relevance_judgments =
      new HashMap < String , HashMap < Integer , Double > >();
    HashMap < String , HashMap < Integer , Double > > relevance_judgments_gain =
      new HashMap < String , HashMap < Integer , Double > >();  


    if (args.length < 2){
      System.out.println("need to provide relevance_judgments and path to evaluation output");
      return;
    }
    String p = args[0];
    String path = args[1];
    // first read the relevance judgments into the HashMap
    readRelevanceJudgments(p,relevance_judgments);
    readRelevanceJudgmentsGain(p,relevance_judgments_gain);
    Vector<Integer> ids = new Vector<Integer>();
    HashMap<Integer, Double> relevance = new HashMap<Integer, Double>();
    HashMap<Integer, Double> relevance_gain = new HashMap<Integer, Double>();

    String[] query = new String[1];
    convertSysin(query, ids,relevance_judgments, relevance);
    convertSysin(query,ids,relevance_judgments_gain,relevance_gain);

    String line = query[0] + "\t"; //initiate the line with query
    line = line + evaluatePRF(ids, relevance);
    line = line + evaluatePrecAtRec(relevance, ids);
    line = line + evaluateAvgPrec(relevance, ids) + "\t";
    line = line + evaluateNDCG(relevance_gain,ids,1);
    line = line + evaluateNDCG(relevance_gain,ids,5);
    line = line + evaluateNDCG(relevance,ids,10);
    line = line + evaluateReciprocal(relevance,ids) + "\n";
    writeToFile(path, line);
    //System.out.println(line);

  }

  public static void readRelevanceJudgments(
    String p,HashMap < String , HashMap < Integer , Double > > relevance_judgments){
    try {
      BufferedReader reader = new BufferedReader(new FileReader(p));
      try {
        String line = null;
        while ((line = reader.readLine()) != null){
          // parse the query,did,relevance line
          Scanner s = new Scanner(line).useDelimiter("\t");
          String query = s.next();
          int did = Integer.parseInt(s.next());
          String grade = s.next();
          double rel = 0.0;
          // convert to binary relevance
          if ((grade.equals("Perfect")) ||
            (grade.equals("Excellent")) ||
            (grade.equals("Good"))){
            rel = 1.0;
          }
          if (relevance_judgments.containsKey(query) == false){
            HashMap < Integer , Double > qr = new HashMap < Integer , Double >();
            relevance_judgments.put(query,qr);
          }
          HashMap < Integer , Double > qr = relevance_judgments.get(query);
          qr.put(did,rel);
        }
      } finally {
        reader.close();
      }
    } catch (IOException ioe){
      System.err.println("Oops " + ioe.getMessage());
    }
  }

public static void convertSysin(String[] query, Vector <Integer> scoredDocuments,
        HashMap < String , HashMap < Integer , Double > > relevance_judgments,
        HashMap <Integer,Double> relevance){
       try {
      BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        String line = null;
        while ((line = reader.readLine()) != null){
          Scanner s = new Scanner(line).useDelimiter("\t");
              query[0] = s.next();
             if (relevance_judgments.containsKey(query[0]) == false){
             throw new IOException("query not found");
            }
            int did = Integer.parseInt(s.next());
            String title = s.next();
            String rel = s.next();
            scoredDocuments.add(did);
      }
      query[0] = query[0].replace('+', ' ');
      //System.out.println("query is: " + query[0]);
      HashMap<Integer, Double> qr= relevance_judgments.get(query[0]);
      for( Map.Entry<Integer, Double> t: qr.entrySet()){
        relevance.put(t.getKey(), t.getValue());
      }

      //System.out.println("size of relevance map is " + relevance.values().size());
      
    } catch (Exception e){
      System.err.println("Error:" + e.getMessage());
    }
  }


  //*******************
  //evaluate with average precison method

  public static String evaluateAvgPrec(HashMap < Integer , Double > qr, Vector<Integer> ids){
    double avg = 0.0;
    // only consider one query per call    
      //BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
      //String line = null;
      double RR = 0.0;
      double N = 1.0;
      //double avg = 0.0;
      for(int i=0; i<ids.size();i++){
        int did = ids.get(i);
        if (qr.containsKey(did) != false && qr.get(did) == 1.0){
          RR += 1;
          avg += RR/N;   
          //System.out.println("the current avg is " + avg);
        }
        ++N;
      }
      N-=1;
      if(RR ==0.0){
        avg = 0;
      }else{
        avg = avg / RR;
      }    
      //System.out.println("AVGERAGE IS: " + Double.toString(avg));
      
    return Double.toString(avg);
  } 


  //evaluate with precision at recal points
  public static String evaluatePrecAtRec(HashMap < Integer , Double > qr, Vector<Integer> ids){
    // only consider one query per call  
    double [] recs = {0.0, 0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7,0.8,0.9,1.0};
    double[] pres = new double[recs.length];
    String str = ""; //return precisions in the string
      double RR = 0.0; //the count of relevant docs
      double N = 0.0;
      double RC = 0.0;
      double Prec = 0.0;
      double maxPrec = 1.0;
      int next = 1;
     for(int i = 0; i < ids.size();i++){
      int did = ids.get(i);
        //check how many relevance docs in the judgement file
        int totalRR = 0;
        for(Integer key: qr.keySet()){
          if(qr.get(key) == 1){
            totalRR +=1;
          }
        }
        //System.out.println("totalRR is " + totalRR);
        if(totalRR == 0){
          System.out.println("there is no relevant files based on the judgement");
          for(int j = 0; j < pres.length; j++){
            str = str + pres[j] + "\t";
          }
          break;
        }
        //do evaluation
        ++N; //update total count
        if (qr.containsKey(did) != false && qr.get(did) == 1.0){
          RR += 1; 
          RC = RR / totalRR;
          Prec = RR / N; 
         // System.out.println("the current recall is " + RC + " and precision is  " + Prec);
          if(RC >= recs[next-1]){
            if(RC < recs[next]){
              if(maxPrec < Prec){
                maxPrec = Prec;
              }
            }else{
              //first update precision at preious recall level
              pres[next-1] = maxPrec;
             // System.out.println("got max precision " + pres[next-1] + " at recall level " + recs[next-1]);
              maxPrec = Prec;
              //check if RC is bigger than next recall point
              while(RC - recs[next] > 0 || RC-recs[next] == 0){
                pres[next] = maxPrec;
                //System.out.println("got max precision " + pres[next] + " at recall level " + recs[next]);
                if(next<recs.length -1){
                  next++;
                }else{
                  break;
                }
              }
              if(RC==1){
                //print out precision list
              for(int k = 0; k < pres.length; k++){
                str = str + pres[k] + "\t";
              }
                //System.out.println(str);
                break;
              }
            }
          }
        }
      }
    return str;
  } 

  public static String evaluatePRF(Vector <Integer> results, HashMap < Integer , Double > qr){
    //System.out.println("inside of PRF: check map" + qr.values().size());
    // only consider one query per call    
    //try {
      //File file =  new File("../result/hw1.3-vsm.tsv");
      String evaluateResult="";
      String line = null;
      double RR = 0.0;
      double N = 0.0;
      double RR1 = 0.0;
      double RR5 = 0.0;
      double RR10 = 0.0;
      double P1 = 0.0;
      double P5 = 0.0;
      double P10 = 0.0;
      double R1 = 0.0;
      double R5 = 0.0;
      double R10 = 0.0;
      double F1 = 0.0;
      double F5 = 0.0;
      double F10 = 0.0;
      for(int i=0;i<results.size();i++){
        int did = results.get(i);
        if (qr.containsKey(did) != false){
          RR += qr.get(did);        
        }
        ++N;
        if(N<=1){
          if (qr.containsKey(did) != false){
            RR1 += qr.get(did);        
          }
        }
        if(N<=5){
          if (qr.containsKey(did) != false){
            RR5 += qr.get(did);        
          }
        }
        if(N<=10){
          if (qr.containsKey(did) != false){
            RR10 += qr.get(did);        
          }
        }
        if(N==1){
          P1 = RR/N;
          evaluateResult += Double.toString(P1);
          //System.out.println("P@1: "+Double.toString(P1));
        }
        if(N==5){
          P5 = RR/N;
          evaluateResult += "\t"+Double.toString(P5);
          //System.out.println("P@5: "+Double.toString(P5));
        }
        if(N==10){
          P10 = RR/N;
          evaluateResult += "\t"+Double.toString(P10);
         // System.out.println("P@10: "+Double.toString(P10));
          if(RR!=0.0){
            R1 = RR1/RR;
            R5 = RR5/RR;
            R10 = RR10/RR;
            evaluateResult += "\t"+Double.toString(R1);
            evaluateResult += "\t"+Double.toString(R5);
            evaluateResult += "\t"+Double.toString(R10);
           // System.out.println("R@1: "+Double.toString(R1));
           // System.out.println("R@5: "+Double.toString(R5));
           // System.out.println("R@10: "+Double.toString(R10));
          }else{
            evaluateResult += "\t"+Double.toString(0.0);
            evaluateResult += "\t"+Double.toString(0.0);
            evaluateResult += "\t"+Double.toString(0.0);
           // System.out.println("R@1: 0.0");
          //  System.out.println("R@5: 0.0");
            //System.out.println("R@10: 0.0");
          }
          if(P1+R1!=0.0){
            F1 = (2*P1*R1)/(P1+R1);
            evaluateResult += "\t"+Double.toString(F1);
            //System.out.println("F@1: "+Double.toString(F1));
          }else{
            evaluateResult += "\t"+Double.toString(0.0);
            //System.out.println("F@1: 0.0");
          }
          if(P5+R5!=0.0){
            F5 = (2*P5*R5)/(P5+R5);
            evaluateResult += "\t"+Double.toString(F5);
            //System.out.println("F@5: "+Double.toString(F5));
          }else{
            evaluateResult += "\t"+Double.toString(0.0);
            //System.out.println("F@5: 0.0");
          }
          if(P10+R10!=0.0){
            F10 = (2*P10*R10)/(P10+R10);
            evaluateResult += "\t"+Double.toString(F10)+"\t";
            //System.out.println("F@10: "+Double.toString(F10));
          }else{
            evaluateResult += "\t"+Double.toString(0.0)+"\t";
           // System.out.println("F@10: 0.0");
          }
          break;
        }
      }
    return evaluateResult;
  }

  public static void readRelevanceJudgmentsGain(
    String p,HashMap < String , HashMap < Integer , Double > > relevance_judgments_gain){
    try {
      BufferedReader reader = new BufferedReader(new FileReader(p));
      try {
        String line = null;
        while ((line = reader.readLine()) != null){
          // parse the query,did,relevance line
          Scanner s = new Scanner(line).useDelimiter("\t");
          String query = s.next();
          int did = Integer.parseInt(s.next());
          String grade = s.next();
          double rel = 0.0;
          
          if(grade.equals("Perfect"))
            rel = 10.0;
          if(grade.equals("Excellent"))
            rel =7.0;
          if(grade.equals("Good"))
            rel = 5.0;
          if(grade.equals("Fair"))
            rel = 1.0;

          if (relevance_judgments_gain.containsKey(query) == false){
            HashMap < Integer , Double > qr = new HashMap < Integer , Double >();
            relevance_judgments_gain.put(query,qr);
          }
          HashMap < Integer , Double > qr = relevance_judgments_gain.get(query);
          qr.put(did,rel);
        }
      } finally {
        reader.close();
      }
    } catch (IOException ioe){
      System.err.println("Oops " + ioe.getMessage());
    }
  }

  public static String evaluateReciprocal(
     HashMap < Integer , Double >  relevance,  Vector<Integer> scoredDocuments){
       
      String res = null;
      int flag = 0;
      
      double N = 0.0;
      for (int i =0;i<scoredDocuments.size();i++){
            int did = scoredDocuments.get(i);
             if (relevance.containsKey(did) != false){
              if(relevance.get(did) == 1.0){
              flag = 1;
              break;        
            }
          }
        ++N;
      }
      if(flag ==1){
      System.out.println(Double.toString(1/(N+1)));
      res = Double.toString(1/(N+1))+ "\t";
      
    }
    else 
      res = "0.0"+"\t";
      
    
    return res;
}

public static String evaluateNDCG(
     HashMap < Integer , Double >  relevance,Vector <Integer> scoredDocuments,int k){
       
       String o = null;
    try {
      
      
      String line = null;
      double RR = 0.0;
      int  N = 0;
      double res = 0.0;
      double[] DCG = new double[k];
      double[] IDCG = new double[k];
      while(N<k){
        
        int did = scoredDocuments.get(N);      
        if (relevance.containsKey(did) != false){
              DCG[N] = relevance.get(did);
        }
        else DCG[N] = 0.0;
        ++N;
      }

      if(DCG.length !=1 || DCG[0]!=0){
        if(DCG.length ==1)
         res = 1.0;
       else{
           double[] tmp = DCG.clone();
           
           Arrays.sort(tmp);
           
           for(int i = k-1,j=0;i>=0;i--,j++){
            IDCG[j] = tmp[i];

           }
           double dcg = getDCG(DCG);
           double idcg = getDCG(IDCG);
           res = dcg/idcg;  
        }


      }
      //System.out.println(Double.toString(res));
       o = Double.toString(res)+"\t";
      
    } catch (Exception e){
      System.err.println("Error:" + e.getMessage());
    }
    return o;
}

public static double getDCG(double[] gain){
        double res = gain[0];
        for(int i =1;i<gain.length;i++){
          res += gain[i] / (Math.log(i+1)/Math.log(2));
        }
        return res;
  }

  private static void writeToFile(String filepath, String text) throws FileNotFoundException{
      try(PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(filepath, true)))) {
          out.print(text);
      }catch (IOException e) {
          //exception handling left as an exercise for the reader
      }
  }
}

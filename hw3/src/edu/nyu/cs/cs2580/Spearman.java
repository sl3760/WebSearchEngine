package edu.nyu.cs.cs2580;
import edu.nyu.cs.cs2580.SearchEngine.Options;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.Writer;
import java.io.Reader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.util.Scanner;
import java.util.ArrayList;
import java.io.File;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.Comparator;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.io.FileNotFoundException;


public class Spearman{
	public static ArrayList<Double> pageRank;
	public static ArrayList<Integer> numViews;

	public static void main(String[] args) throws IOException, FileNotFoundException{
		
		if(args.length!=2){
			System.out.println("arguments num is not right, please check");
			System.exit(1);
		}
		System.out.println(args.length);
		String path_pageRank = args[0];
		String path_numView = args[1];
		Gson gson = new Gson();
	    Reader reader = new InputStreamReader(new FileInputStream(path_numView));
	    numViews = gson.fromJson(reader,
	                      new TypeToken<ArrayList<Integer>>() {}.getType()); 
	   	reader = new InputStreamReader(new FileInputStream(path_pageRank));
	   	pageRank = gson.fromJson(reader,
	                      new TypeToken<ArrayList<Double>>() {}.getType()); 
	    reader.close();
	    int n1 = numViews.size();
	    int n2 = pageRank.size();
	    if(n1!=n2){
	    	System.out.println("numView and pageRank have differnt size: " + numViews.size() + "vs. "+ pageRank.size());
	    	System.exit(1);
	    }
	    double[] sortedPageRank = sortPageRank(n1);		
	    System.out.println("sortedPageRank");
	    int[] sortednumViews = sortNumViews(n1);
		System.out.println("sortednumViews");

		double z = getZ(n1);
		double sumN = 0.0;
		double sumXSqr = 0.0;
		double sumYSqr = 0.0;
		for(int i = 0; i<n1; i++){
			sumN +=(sortedPageRank[i]-z) * (sortednumViews[i]-z);
			sumXSqr += (sortedPageRank[i]-z) * (sortedPageRank[i]-z);
			sumYSqr += (sortednumViews[i]-z) * (sortednumViews[i]-z);
		}

		double coe = sumN / (Math.sqrt(sumYSqr * sumYSqr));
		System.out.println("Spearman coeffient is " + coe);
	}

	//sort by pagerank value in descending order, use docid to break ties
	private static int[] sortNumViews(int n){
		ArrayList<Pair_int> temp = new ArrayList<Pair_int>(n);
        for(int i = 0; i<n; i++){
        	temp.add(new Pair_int(i, numViews.get(i)));
        }

		Collections.sort(temp, new Comparator<Pair_int>(){
	        @Override
	        public int compare(Pair_int m1, Pair_int m2){
	            int res = Integer.compare(m2.value, m1.value);
	            if(res ==0)
	            	res = Integer.compare(m1.index, m2.index);
	            return res;
	        }
	      }
	    );

	    int[] numViewsRank = new int[n];
	    for(int i = 0; i<n; i++){
	    	int docID = temp.get(i).index;
	    	numViewsRank[docID] = i+1; //get yi
	    }
	    return numViewsRank;
	}

	//sort by pagerank value in descending order, use docid to break ties
	private static double[] sortPageRank(int n){
		ArrayList<Pair_double> temp = new ArrayList<Pair_double>(n);
        for(int i = 0; i<n; i++){
        	temp.add(new Pair_double(i, pageRank.get(i)));
        }

		Collections.sort(temp, new Comparator<Pair_double>(){
	        @Override
	        public int compare(Pair_double m1, Pair_double m2){
	            int res = Double.compare(m2.value, m1.value);
	            if(res ==0)
	            	res = Integer.compare(m1.index, m2.index);
	            return res;
	        }
	      }
	    );

	    double[] pageRankRank = new double[n];
	    for(int i = 0; i<n; i++){
	    	int docID = temp.get(i).index;
	    	pageRankRank[docID] = i +1; //get xi
	    }
	    return pageRankRank;
	}
	

	private static double getZ(int n){
		int sum = 0;
		for(int i = 1; i<=n; i++){
			sum+=i;
		}
		return (double)sum/n;
	}

}

class Pair_int{
	int index;
	int value;
	public Pair_int(int i, int v){
		index = i;
		value = v;
	}
}

class Pair_double{
	int index;
	double value;
	public Pair_double(int i, double v){
		index = i;
		value = v;
	}
}
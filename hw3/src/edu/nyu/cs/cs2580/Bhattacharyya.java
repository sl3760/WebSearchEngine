package edu.nyu.cs.cs2580;

import java.util.ArrayList;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.File;
import java.io.OutputStream;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;

public class Bhattacharyya{
	public static void main(String[] args) throws IOException{
		if(args.length!=2){
			System.out.println("java -cp src edu.nyu.cs.cs2580.Bhattacharyya prf.tsv qsim.tsv");
		}
		String pathToPRFOutput = args[0];
		String pathToOutput = args[1];
		List<String> queries = new ArrayList<String>();
		HashMap<String,String> queryToFileMap = new HashMap<String,String>();
		BufferedReader reader = new BufferedReader(new FileReader(pathToPRFOutput));
		String line = null;
		while((line = reader.readLine()) != null){
			String[] arr = line.split(":");
			String query = arr[0];
			String fileName = arr[1];
			queries.add(query);
			queryToFileMap.put(query,fileName);
		}
		File fileToOutput = new File(pathToOutput);
		OutputStream out = new FileOutputStream(fileToOutput);
		for(int i=0;i<queries.size();i++){
			double bhattacharyya = 0;
			HashMap<String,Double> map1 = getMap(queryToFileMap.get(queries.get(i)));
			for(int j=i+1;j<queries.size();j++){
				HashMap<String,Double> map2 = getMap(queryToFileMap.get(queries.get(j)));
				HashSet<String> set = getSet(map1,map2);
				for(String w:set){
					double d1 = map1.get(w);
					double d2 = map2.get(w);
					bhattacharyya+=Math.sqrt(d1*d2);
				}
				out.write(queries.get(i).getBytes());
				out.write("\t".getBytes());
				out.write(queries.get(j).getBytes());
				out.write("\t".getBytes());
				out.write(String.valueOf(bhattacharyya).getBytes());
				out.write("\n".getBytes());
			}
		}
	}

	private static HashMap<String,Double> getMap(String fileName) throws IOException,FileNotFoundException{
		HashMap<String,Double> res = new HashMap<String,Double>();
		BufferedReader reader = new BufferedReader(new FileReader(fileName));
		String line = null;
		while((line = reader.readLine()) != null){
			String[] arr = line.split("\t");
			res.put(arr[0],Double.parseDouble(arr[1]));
		}
		return res;
	}

	private static HashSet<String> getSet(HashMap<String,Double> map1, HashMap<String,Double> map2){
		HashSet<String> set = new HashSet<String>();
		for(String s:map1.keySet()){
			if(map2.containsKey(s)){
				set.add(s);
			}
		}
		return set;
	}

}
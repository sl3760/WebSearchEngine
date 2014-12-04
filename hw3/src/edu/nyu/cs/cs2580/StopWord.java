package edu.nyu.cs.cs2580;

import java.util.HashSet;
import java.util.Arrays;

public class StopWord{
	private HashSet<String> list;
	public StopWord(){
		this.list = new HashSet<String>(Arrays.asList(new String[] { "a", "b", "c", 
				 "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", "n", "i", "o", "p", "q", "r",
				 "s", "t", "u", "v", "w", "x", "y", "z", "1", "2" ,"3" ,"4", "5", "6","7","8","9","0", "able", "about", "across", "after", "all" , "almost", "also", "am", "among", "an", "and", "any", "are", "as", "at", "be", "because", "been", "but", "by", "can", "cannot", "could", "dear", "did", "do", "does", "either", "else", "ever", "every", "for", "from", "get", "got", "had", "has", "have", "he", "her", "hers", "him", "his", "how", "however", "if", "in", "into", "is", "it", "its", "just", "least", "let", "like", "likely", "may", "me", "might", "most", "must", "my", "neither", "no", "nor", "not", "of", "off", "often", "on", "only", "or", "other", "our", "own", "rather", "said", "say", "says", "she", "should", "since", "so", "some", "than", "that", "the", "their", "them", "then", "there", "these", "they", "this", "tis", "to", "too", "was", "us", "we", "were", "what", "when", "where", "which", "while", "who", "whom", "why", "will", "with", "would", "yet", "you", "your", "www", "http", "com", "."}));
	}
	public HashSet<String> getStopWords(){
		return this.list;
	}
}
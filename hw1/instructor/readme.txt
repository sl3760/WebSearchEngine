In order to compile the search engine, enter src directory and compile: 

$ javac edu/nyu/cs/cs2580/*.java

Then run the search engine with command: 
$ java edu.nyu.cs.cs2580.SearchEngine 25805 ../data/corpus.tsv

To get ranking result files for 2.1 and 2.2, open second terminal and enter src directory, and run: 
$ curl “http://localhost:25805/search?query=<QUERY>&ranker=<RANKER_TYPE>&format=text” 

Note that for ranker result files, the file path “../result/<file-name>” is already hardcoded in QueryHandler.java file. Each time you run above command, corresponding query and ranker results will be appended to existing result files. To keep the original submitted files, you can move them to another directory. 


The Beta values for 2.2 are: 
Bcosine = 0.6; BLM =0.39; Bphrase = 0.0099; Bnumviews = 0.0001;



To get evaluation result files for 2.3, please run the following command in 2nd terminal: 
$ curl “http://localhost:25805/search?query=<QUERY>&ranker=<RANKER_TYPE>&format=text” | \ 
java edu.nyu.cs.cs2580.Evaluator ../data/qrels.tsv  ../result/<file-name>

Note that you will need to provide the output file path as the second arguments. 






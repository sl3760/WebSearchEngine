

part1: Instructions to run the code, given the current work directory is instructor: 
compile code: 
	javac -cp jsoup-1.8.1.jar:gson-2.3.jar src/edu/nyu/cs/cs2580/*.java 

2. run mining mode: 
	java -cp gson-2.3.jar:src  edu.nyu.cs.cs2580.SearchEngine \ --mode=mining --options=conf/engine.conf 

3. run Spearman correlation:
	java -cp gson-2.3.jar:src edu.nyu.cs.cs2580.Spearman data/log/PageRank data/log/numView

4. run index mode: 
	java -cp jsoup-1.8.1.jar:gson-2.3.jar:src edu.nyu.cs.cs2580.SearchEngine \ --mode=index  --options=conf/engine.conf
	
5. run serve mode:
	java -cp jsoup-1.8.1.jar:gson-2.3.jar:src -Xmx512m edu.nyu.cs.cs2580.SearchEngine \ --mode=serve --port=25805 --options=conf/engine.conf 

6. run query expansion


part2: Spearman correlation 
Below are the correlation we got for the four iteration/lambda scenarios.  The higher the Spearman's rank correlation coefficient is, the more relevant to the users. Thus we choose two iterations with λ = 0.90 which generates the most highest value.  
 
one iteration with λ = 0.10:    0.45303
two iterations with λ = 0.10:   0.45074
one iteration with λ = 0.90:    0.45289
two iterations with λ = 0.90:   0.45424
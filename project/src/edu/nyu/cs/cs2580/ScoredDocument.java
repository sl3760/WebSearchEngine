package edu.nyu.cs.cs2580;

/**
 * Document with score.
 * 
 * @author fdiaz
 * @author congyu
 */
class ScoredDocument implements Comparable<ScoredDocument> {
  private Document _doc;
  private double _score;

  public ScoredDocument(Document doc, double score) {
    _doc = doc;
    _score = score;
  }

  public String asTextResult() {
    StringBuffer buf = new StringBuffer();   
    buf.append(_doc.getTitle());
    /*
    buf.append(_doc._docid).append("\t");
    buf.append(_doc.getTitle()).append("\t");   
    buf.append(_score).append("\t");
    buf.append(_doc.getPageRank()).append("\t");
    buf.append(_doc.getNumViews());
    */
    return buf.toString();
  }

  public String getCompany_ads(){
    return this._doc.getCompany_ads();
  }

  public String getBody(){
    return this._doc.getBody();
  }

  /**
   * @CS2580: Student should implement {@code asHtmlResult} for final project.
   */
  public String asHtmlResult() {
    return "";
  }

  @Override
  public int compareTo(ScoredDocument o) {
    if (this._score == o._score) {
      return 0;
    }
    return (this._score > o._score) ? 1 : -1;
  }

  public Document getDoc(){
    return this._doc;
  }

  public double getScore(){
    return this._score;
  }

  public void setScore(double s){
    this._score = s;
  }
}

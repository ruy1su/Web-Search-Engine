/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package edu.ucla;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.analysis.payloads.PayloadHelper;
import org.apache.lucene.index.FieldInvertState;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.LeafReader;
import org.apache.lucene.index.PostingsEnum;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.index.NumericDocValues;
import org.apache.lucene.index.SortedNumericDocValues;
import org.apache.lucene.search.CollectionStatistics;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.TermStatistics;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.SmallFloat;

import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.common.settings.Settings;


/**
 * A subclass of {@code Similarity} that provides a simplified API 
 * to implement a custom scoring function based on tf, idf, payload and doc-values
 * Subclasses are only required to implement the {@link #score}
 * and {@link #toString()} methods. Implementing
 * {@link #explain(List, BasicStats, int, float, float, long)} is optional,
 * inasmuch as CS246Similarity already provides a basic explanation of the score
 * and the term frequency. However, implementers of a subclass are encouraged to
 * include as much detail about the scoring method as possible.
 * <p>
 * Note: multi-word queries such as phrase queries are scored in a different way
 * than Lucene's default ranking algorithm: whereas it "fakes" an IDF value for
 * the phrase as a whole (since it does not know it), this class instead scores
 * phrases as a summation of the individual term scores.
 */
public class CS246Similarity extends Similarity {
  protected final Settings settings;
  protected String signalField;

  public CS246Similarity(Settings settings) {
    this.settings = settings;
    this.signalField = "clicks";
  }

  @Override
  public String toString() {
    return "CS246Similarity";
  }

  /**
   * Score the document for one term. This function is called for every term in the query.
   * The results from calls to this function are all added to compute the final similarity score.
   * @param stats collection-wise statistics, such as document frequency and total number of documents
   * @param tf  "term frequency" of the current term in the document
   * @param docLen the length of document, |d|
   * @param docValue document-specific signal that can be used for score calculation
   * @return computed similarity score between the current term and the document.
   */
  protected float score(BasicStats stats, float tf, float docLen, long docValue)
  {
    // The first parameter stats has the following collection-level statistics:
    //    stats.numberOfDocuments: the total # of documents in the collection
    //    stats.numberofFieldTokens: the total # of tokens extracted from the field
    //    stats.avgFieldLength: the average length of the field
    //    stats.docFreq: the document frequency
    //    stats.totalTermFreq: the collection frequency 
    //             (= total # of occurence of the term across all documents)
    //    You may use these statistics to compute the idf value, for example.
    // The second parameter tf has "term frequency"
    // The third parameter docLen is the "document length", |d|
    // The last parameter docValue has the value in the field "clicks" of the document.
    //    Note: If we want to use a value from a field other than "clicks", we need to change 
    //    the CS246Similarity constructor by setting "signalField" to the name of the desired field.

    return tf*idf(stats)*docValueBoost(docValue)/docLen;
  }
  
  protected float idf(BasicStats stats)
  {
    float nod = ((float)stats.numberOfDocuments+1);
    float ttf = ((float)stats.docFreq+1);
    float res = (float)(Math.log(nod/ttf)/LOG_2);
    return res;
  }

  protected float docValueBoost(long docValue)
  {
    return (float)(Math.log((float)(docValue+1))/LOG_2);
  }

  /**
   * Add the explanation for all subexpressions of the scoring function
   * Here, we add explanations on tf, idf, 1/|d|, and click signal
   * 
   * @param subExpls explanations for subexpressions should be to this
   * @param stats the corpus level statistics.
   * @param doc the document id.
   * @param freq the term frequency.
   * @param docLen the document length |d|.
   * @param docValue additional signal used for document scoring.
   */
  protected void explain(
      List<Explanation> subExpls, BasicStats stats, int doc, float freq, float docLen, long docValue) {
    float clicks = docValueBoost(docValue);  
    subExpls.add(Explanation.match(freq, "tf"));
    subExpls.add(Explanation.match(idf(stats), "idf"));
    subExpls.add(Explanation.match(docLen, "|d|"));
    if (clicks != 1.0f) {
      subExpls.add(Explanation.match(clicks, "doc_value"));
    }
  }

  /** For {@link #log2(double)}. Precomputed for efficiency reasons. */
  private static final double LOG_2 = Math.log(2);
  
  /** 
   * True if overlap tokens (tokens with a position of increment of zero) are
   * discounted from the document's length.
   */
  protected boolean discountOverlaps = true;
  
  /** Determines whether overlap tokens (Tokens with
   *  0 position increment) are ignored when computing
   *  norm.  By default this is true, meaning overlap
   *  tokens do not count when computing norms.
   *
   * @param v  are overlap tokens ignored?
   * @see #computeNorm
   */
  public void setDiscountOverlaps(boolean v) {
    discountOverlaps = v;
  }

  /**
   * Returns true if overlap tokens are discounted from the document's length. 
   * @see #setDiscountOverlaps 
   * @return discountOverlaps value
   */
  public boolean getDiscountOverlaps() {
    return discountOverlaps;
  }
  
  @Override
  public final SimWeight computeWeight(CollectionStatistics collectionStats, TermStatistics... termStats) {
    BasicStats stats[] = new BasicStats[termStats.length];
    for (int i = 0; i < termStats.length; i++) {
      stats[i] = newStats(collectionStats.field());
      fillBasicStats(stats[i], collectionStats, termStats[i]);
    }
    return stats.length == 1 ? stats[0] : new MultiStats(stats);
  }
  
  /** Factory method to return a custom stats object 
   * @param field name of the SimWeight field
   * @return BasicStats
   */
protected BasicStats newStats(String field) {
    return new BasicStats(field);
  }
  
  /** Fills all member fields defined in {@code BasicStats} in {@code stats}. 
   *  Subclasses can override this method to fill additional stats. 
   * @param stats is used as the output to store relevant statistics
   * @param collectionStats  collection-wide statistics  
   * @param termStats  term-specific statistics
   */
protected void fillBasicStats(BasicStats stats, CollectionStatistics collectionStats, TermStatistics termStats) {
    // #positions(field) must be >= #positions(term)
    assert collectionStats.sumTotalTermFreq() == -1 || collectionStats.sumTotalTermFreq() >= termStats.totalTermFreq();
    long numberOfDocuments = collectionStats.docCount() == -1 ? collectionStats.maxDoc() : collectionStats.docCount();
    
    long docFreq = termStats.docFreq();
    long totalTermFreq = termStats.totalTermFreq();

    // codec does not supply totalTermFreq: substitute docFreq
    if (totalTermFreq == -1) {
      totalTermFreq = docFreq;
    }

    final long numberOfFieldTokens;
    final float avgFieldLength;

    long sumTotalTermFreq = collectionStats.sumTotalTermFreq();

    if (sumTotalTermFreq <= 0) {
      // field does not exist;
      // We have to provide something if codec doesnt supply these measures,
      // or if someone omitted frequencies for the field... negative values cause
      // NaN/Inf for some scorers.
      numberOfFieldTokens = docFreq;
      avgFieldLength = 1;
    } else {
      numberOfFieldTokens = sumTotalTermFreq;
      avgFieldLength = (float)numberOfFieldTokens / numberOfDocuments;
    }
 
    stats.setNumberOfDocuments(numberOfDocuments);
    stats.setNumberOfFieldTokens(numberOfFieldTokens);
    stats.setAvgFieldLength(avgFieldLength);
    stats.setDocFreq(docFreq);
    stats.setTotalTermFreq(totalTermFreq);
    stats.setTerm(new Term(stats.field, termStats.term()));
  }
  
  /**
   * Explains the score. The implementation here provides a basic explanation
   * in the format <em>score(name-of-similarity, doc=doc-id,
   * freq=term-frequency), computed from:</em>, and
   * attaches the score (computed via the {@link #score(BasicStats, float, float, long)}
   * method) and the explanation for the term frequency. Subclasses content with
   * this format may add additional details in
   * {@link #explain(List, BasicStats, int, float, float, long)}.
   *  
   * @param stats the corpus level statistics.
   * @param doc the document id.
   * @param freq the term frequency and its explanation.
   * @param docLen the document length |d|.
   * @param signal additional signal used for document scoring.
   * @return the explanation.
   */
  protected Explanation explain(
      BasicStats stats, int doc, Explanation freq, float docLen, long signal) {
    List<Explanation> subs = new ArrayList<>();
    explain(subs, stats, doc, freq.getValue(), docLen, signal);
    
    return Explanation.match(
        score(stats, freq.getValue(), docLen, signal),
        "score(" + getClass().getSimpleName() + ", doc=" + doc + ", freq=" + freq.getValue() +"), computed from:",
        subs);
  }
  
  @Override
  public SimScorer simScorer(SimWeight stats, LeafReaderContext context) throws IOException {
    if (stats instanceof MultiStats) {
      // a multi term query (e.g. phrase). return the summation, 
      // scoring almost as if it were boolean query
      SimWeight subStats[] = ((MultiStats) stats).subStats;
      SimScorer subScorers[] = new SimScorer[subStats.length];
      for (int i = 0; i < subScorers.length; i++) {
        BasicStats basicstats = (BasicStats) subStats[i];
        subScorers[i] = new CS246SimScorer(basicstats, context, 
          context.reader().getNormValues(basicstats.field), 
          context.reader().getSortedNumericDocValues(signalField));
      }
      return new MultiSimScorer(subScorers);
    } else {
      BasicStats basicstats = (BasicStats) stats;
      return new CS246SimScorer(basicstats, context,
        context.reader().getNormValues(basicstats.field), 
        context.reader().getSortedNumericDocValues(signalField));
    }
  }
  
    // ------------------------------ Norm handling ------------------------------
  
  /** Norm to document length map. */
  private static final float[] NORM_TABLE = new float[256];

  static {
    for (int i = 1; i < 256; i++) {
      float floatNorm = SmallFloat.byte315ToFloat((byte)i);
      NORM_TABLE[i] = 1.0f / (floatNorm * floatNorm);
    }
    NORM_TABLE[0] = 1.0f / NORM_TABLE[255]; // otherwise inf
  }

  /** Encodes the document length in the same way as TFIDFSimilarity. */
  @Override
  public long computeNorm(FieldInvertState state) {
    final float numTerms;
    if (discountOverlaps)
      numTerms = state.getLength() - state.getNumOverlap();
    else
      numTerms = state.getLength();
    return encodeNormValue(state.getBoost(), numTerms);
  }
  
  /** Decodes a normalization factor (document length) stored in an index.
   * @param norm  encoded length normal value
   * @return decoded length normal value
   * @see #encodeNormValue(float,float)
   */
  protected float decodeNormValue(byte norm) {
    return NORM_TABLE[norm & 0xFF];  // & 0xFF maps negative bytes to positive above 127
  }
  
  /** Encodes the length to a byte via SmallFloat. 
   * @param boost  field boost factor
   * @param length field length
   * @return encoded length normal value
   */
  protected byte encodeNormValue(float boost, float length) {
    return SmallFloat.floatToByte315((boost / (float) Math.sqrt(length)));
  }
  
  // ----------------------------- Static methods ------------------------------
  
  /** Returns the base two logarithm of {@code x}. 
   * @param x input
   * @return log2(x)
   */
  public static double log2(double x) {
    // Put this to a 'util' class if we need more of these.
    return Math.log(x) / LOG_2;
  }
  
  // --------------------------------- Classes ---------------------------------
  
  /** Delegates the {@link #score(int, float)} and
   * {@link #explain(int, Explanation)} methods to
   * {@link CS246Similarity#score(BasicStats, float, float, long)} and
   * {@link CS246Similarity#explain(BasicStats, int, Explanation, float, long)},
   * respectively.
   */
  private class CS246SimScorer extends SimScorer {
    private final BasicStats stats;
    private final LeafReaderContext context;
    private final NumericDocValues norms;
    private final SortedNumericDocValues signals;
    
    CS246SimScorer(BasicStats stats, LeafReaderContext context, NumericDocValues norms, SortedNumericDocValues signals) throws IOException {
      this.stats = stats;
      this.context = context;
      this.norms = norms;
      this.signals = signals;
    }

    protected long getSignal(int doc) {
      if (signals == null)  return 1L;
      signals.setDocument(doc);
      if (signals.count() < 1)  return 1L;
      return signals.valueAt(0);
    }

    @Override
    public float score(int doc, float freq) {
      // We have to supply something in case norms are omitted
      return CS246Similarity.this.score(stats, freq, 
          norms == null ? 1F : decodeNormValue((byte)norms.get(doc)), 
          getSignal(doc));
    }

    @Override
    public Explanation explain(int doc, Explanation freq) {
      return CS246Similarity.this.explain(stats, doc, freq, 
          norms == null ? 1F : decodeNormValue((byte)norms.get(doc)), 
          getSignal(doc));
    }

    @Override
    public float computeSlopFactor(int distance) {
      return 1.0f / (distance + 1);
    }

    @Override
    public float computePayloadFactor(int doc, int start, int end, BytesRef payload) {
        if (payload != null) {
            return PayloadHelper.decodeFloat(payload.bytes, payload.offset);
        } else {
            return 1.0f;
        }
    }
  }

  static class MultiSimScorer extends SimScorer {
    private final SimScorer subScorers[];
    
    MultiSimScorer(SimScorer subScorers[]) {
      this.subScorers = subScorers;
    }
    
    @Override
    public float score(int doc, float freq) {
      float sum = 0.0f;
      for (SimScorer subScorer : subScorers) {
        sum += subScorer.score(doc, freq);
      }
      return sum;
    }

    @Override
    public Explanation explain(int doc, Explanation freq) {
      List<Explanation> subs = new ArrayList<>();
      for (SimScorer subScorer : subScorers) {
        subs.add(subScorer.explain(doc, freq));
      }
      return Explanation.match(score(doc, freq.getValue()), "sum of:", subs);
    }

    @Override
    public float computeSlopFactor(int distance) {
      return subScorers[0].computeSlopFactor(distance);
    }

    @Override
    public float computePayloadFactor(int doc, int start, int end, BytesRef payload) {
      return subScorers[0].computePayloadFactor(doc, start, end, payload);
    }
  }

  static class BasicStats extends SimWeight {
  final String field;
  /** The number of documents. */
  protected long numberOfDocuments;
  /** The total number of tokens in the field. */
  protected long numberOfFieldTokens;
  /** The average field length. */
  protected float avgFieldLength;
  /** The document frequency. */
  protected long docFreq;
  /** The total number of occurrences of this term across all documents. */
  protected long totalTermFreq;
  protected Term term;
  
  // -------------------------- Boost-related stuff --------------------------

  /** For most Similarities, the immediate and the top level query boosts are
   * not handled differently. Hence, this field is just the product of the
   * other two. */
  protected float boost;
  
  /** Constructor. */
  BasicStats(String field) {
    this.field = field;
    normalize(1f, 1f);
  }
  
  // ------------------------- Getter/setter methods -------------------------
  
  /** Returns the term. */
  public Term getTerm() {
    return term;
  }
  
  /** Sets the term. */
  public void setTerm(Term term) {
    this.term = term;
  }
  
  /** Returns the number of documents. */
  public long getNumberOfDocuments() {
    return numberOfDocuments;
  }
  
  /** Sets the number of documents. */
  public void setNumberOfDocuments(long numberOfDocuments) {
    this.numberOfDocuments = numberOfDocuments;
  }
  
  /**
   * Returns the total number of tokens in the field.
   * @see Terms#getSumTotalTermFreq()
   */
  public long getNumberOfFieldTokens() {
    return numberOfFieldTokens;
  }
  
  /**
   * Sets the total number of tokens in the field.
   * @see Terms#getSumTotalTermFreq()
   */
  public void setNumberOfFieldTokens(long numberOfFieldTokens) {
    this.numberOfFieldTokens = numberOfFieldTokens;
  }
  
  /** Returns the average field length. */
  public float getAvgFieldLength() {
    return avgFieldLength;
  }
  
  /** Sets the average field length. */
  public void setAvgFieldLength(float avgFieldLength) {
    this.avgFieldLength = avgFieldLength;
  }
  
  /** Returns the document frequency. */
  public long getDocFreq() {
    return docFreq;
  }
  
  /** Sets the document frequency. */
  public void setDocFreq(long docFreq) {
    this.docFreq = docFreq;
  }
  
  /** Returns the total number of occurrences of this term across all documents. */
  public long getTotalTermFreq() {
    return totalTermFreq;
  }
  
  /** Sets the total number of occurrences of this term across all documents. */
  public void setTotalTermFreq(long totalTermFreq) {
    this.totalTermFreq = totalTermFreq;
  }
  
  // -------------------------- Boost-related stuff --------------------------
  
  /** The square of the raw normalization value.
   * @see #rawNormalizationValue() */
  @Override
  public float getValueForNormalization() {
    float rawValue = rawNormalizationValue();
    return rawValue * rawValue;
  }
  
  /** Computes the raw normalization value. This basic implementation returns
   * the query boost. Subclasses may override this method to include other
   * factors (such as idf), or to save the value for inclusion in
   * {@link #normalize(float, float)}, etc.
   */
  protected float rawNormalizationValue() {
    return boost;
  }
  
  /** No normalization is done. {@code boost} is saved in the object, however. */
  @Override
  public void normalize(float queryNorm, float boost) {
    this.boost = boost;
  }
  
  /** Returns the total boost. */
  public float getBoost() {
    return boost;
  }
}


  static class MultiStats extends SimWeight {
    final SimWeight subStats[];
    
    MultiStats(SimWeight subStats[]) {
      this.subStats = subStats;
    }
    
    @Override
    public float getValueForNormalization() {
      float sum = 0.0f;
      for (SimWeight stat : subStats) {
        sum += stat.getValueForNormalization();
      }
      return sum / subStats.length;
    }

    @Override
    public void normalize(float queryNorm, float boost) {
      for (SimWeight stat : subStats) {
        stat.normalize(queryNorm, boost);
      }
    }
  }
}



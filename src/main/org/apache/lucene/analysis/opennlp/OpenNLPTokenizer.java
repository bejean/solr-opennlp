/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.lucene.analysis.opennlp;

import java.io.IOException;
import java.util.Arrays;

import opennlp.tools.util.Span;

import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.opennlp.tools.NLPSentenceDetectorOp;
import org.apache.lucene.analysis.opennlp.tools.NLPTokenizerOp;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.util.AttributeFactory;

/**
 * Run OpenNLP SentenceDetector and/or Tokenizer.
 * 
 * Major problem: Lucene is stream-oriented. But, OpenNLP libraries requires all input at once.
 * This Tokenizer has to read the entire field text, process and cache it.
 * Tokenizers have to support reset() (rewind to the beginning) and
 * being re-used. The setReader() method notifies the Tokenizer it is being
 * re-used, but this method cannot be overridden, so this class has to indirectly
 * notice this.
 */
public final class OpenNLPTokenizer extends Tokenizer {  
  private static int DEFAULT_BUFFER_SIZE = 2048;
  
  private int finalOffset;
  private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);
  private final OffsetAttribute offsetAtt = addAttribute(OffsetAttribute.class);
  
  private Span[] sentences = null;
  private Span[][] words = null;
  private Span[] wordSet = null;
  int indexSentence = 0;
  int indexWord = 0;
  private char[] fullText;
  
  private NLPSentenceDetectorOp sentenceOp = null;
  private NLPTokenizerOp tokenizerOp = null; 
  
  public OpenNLPTokenizer(AttributeFactory factory, NLPSentenceDetectorOp sentenceOp, NLPTokenizerOp tokenizerOp) throws IOException {
    super(factory);
    termAtt.resizeBuffer(DEFAULT_BUFFER_SIZE);
    if (sentenceOp == null && tokenizerOp == null) {
      throw new IllegalArgumentException("OpenNLPTokenizer: need one or both of Sentence Detector and Tokenizer");
    }
    this.sentenceOp = sentenceOp;
    this.tokenizerOp = tokenizerOp;
    restartAtBeginning();
  }
  
  @Override
  public final boolean incrementToken() throws IOException {
    if (sentences == null) {
      loadAll();
    }
    if (sentences.length == 0) {
      return false;
    }
    int sentenceOffset = sentences[indexSentence].getStart();
    if (wordSet == null) {
      wordSet = words[indexSentence];
    }
    clearAttributes();
    while (indexSentence < sentences.length) {
      while (indexWord == wordSet.length) {
        indexSentence++;
        if (indexSentence < sentences.length) {
          wordSet = words[indexSentence];
          indexWord = 0;
          sentenceOffset = sentences[indexSentence].getStart();
        } else {
          return false;
        }
      }
      // set termAtt from private buffer
      Span sentence = sentences[indexSentence];
      Span word = wordSet[indexWord];
      int spot = sentence.getStart() + word.getStart();
      termAtt.setEmpty();
      int termLength = word.getEnd() - word.getStart();
      if (termAtt.buffer().length < termLength) {
        termAtt.resizeBuffer(termLength);
      }
      termAtt.setLength(termLength);
      char[] buffer = termAtt.buffer();
      finalOffset = correctOffset(sentenceOffset + word.getEnd());
      offsetAtt.setOffset(correctOffset(word.getStart() + sentenceOffset), finalOffset);
      for(int i = 0; i < termLength; i++) {
        buffer[i] = fullText[spot + i];
      }
      
      indexWord++;
      return true;
    }
    
    return false;
  }
  
  void restartAtBeginning() throws IOException {
    indexWord = 0;
    indexSentence = 0;
    indexWord = 0;
    finalOffset = 0;
    wordSet = null;
  }
  
  void loadAll() throws IOException {
    fillBuffer();
    detectSentences();
    words = new Span[sentences.length][];
    for(int i = 0; i < sentences.length; i++) {
      splitWords(i);
    }
  }
  
  void splitWords(int i) {
    Span current = sentences[i];
    String sentence = String.copyValueOf(fullText, current.getStart(), current.getEnd() - current.getStart());
    words[i] = tokenizerOp.getTerms(sentence); 
  }
  
  // read all text, turn into sentences
  void detectSentences() throws IOException {
    fullText.hashCode();
    sentences = sentenceOp.splitSentences(new String(fullText));
  }
  
  void fillBuffer() throws IOException {
    int offset = 0;
    fullText = new char[DEFAULT_BUFFER_SIZE];
    int length = input.read(fullText);
    while(length == DEFAULT_BUFFER_SIZE) {
      offset += DEFAULT_BUFFER_SIZE;
      fullText = Arrays.copyOf(fullText, offset + DEFAULT_BUFFER_SIZE);
      length = input.read(fullText, offset, DEFAULT_BUFFER_SIZE);
    }
    if (length == -1) {
      length = 0;
    }
    fullText = Arrays.copyOf(fullText, offset + length);
  }
  
  @Override
  public void close() throws IOException {
    super.close();
    fullText = null;
    sentences = null;
    words = null;
    restartAtBeginning();
  };
  
  @Override
  public final void end() throws IOException {
    super.end();
    // set final offset
    offsetAtt.setOffset(finalOffset, finalOffset);
  }
  
  @Override 
  public void reset() throws IOException {
    super.reset();
    clearAttributes();
    restartAtBeginning();
  }
  
  // hack for unit tests
  public void setBufferSize(int size) {
    DEFAULT_BUFFER_SIZE = size;
  }
}

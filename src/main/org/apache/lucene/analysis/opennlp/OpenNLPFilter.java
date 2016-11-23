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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import opennlp.tools.util.Span;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.opennlp.tools.NLPChunkerOp;
import org.apache.lucene.analysis.opennlp.tools.NLPNERTaggerOp;
import org.apache.lucene.analysis.opennlp.tools.NLPPOSTaggerOp;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;
import org.apache.lucene.util.AttributeSource;

/**
 * Run OpenNLP sentence-processing tools
 * OpenNLP Tokenizer- removed sentence detection
 * Optional: POS tagger or phrase chunker. These tag all terms.
 * Optional: one or more Named Entity Resolution taggers. These tag only some terms.
 * 
 * Use file names as keys for cached models.
 * 
 * Hacks:
 * hack #1: EN POS tagger sometimes tags last word as a period if no period at the end
 * hack #2: tokenizer needs to split words with punctuation and it doesn't
 */
public final class OpenNLPFilter extends TokenFilter {
  
  // TODO: if there's an ICU for this, that's great
  private static String SENTENCE_BREAK = "[.?!]";
  
  private final boolean doPOS;
  private final boolean doChunking;
  private final boolean doNER;
  
  // cloned attrs of all tokens
  private List<AttributeSource> tokenAttrs = new ArrayList<>();
  boolean first = true;
  int tokenNum = 0;

  private final NLPPOSTaggerOp posTaggerOp;
  private final NLPChunkerOp chunkerOp;
  private final List<NLPNERTaggerOp> nerTaggerOps;
  private final TypeAttribute typeAtt = addAttribute(TypeAttribute.class);
  private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);

  public OpenNLPFilter(
                       TokenStream input,
                       NLPPOSTaggerOp posTaggerOp,
                       NLPChunkerOp chunkerOp,
                       ArrayList<NLPNERTaggerOp> nerTaggerOps) throws IOException {
    super(input);
    this.posTaggerOp = posTaggerOp;
    this.chunkerOp = chunkerOp;
    this.nerTaggerOps = nerTaggerOps;
    boolean havePOS = (posTaggerOp != null);
    doChunking = (chunkerOp != null);
    doPOS = doChunking ? false : havePOS;
    doNER = (nerTaggerOps != null);
  }
  
  @Override
  public final boolean incrementToken() throws IOException {
    clearAttributes();
    if (first) {
      String[] words = walkTokens();
      if (words.length == 0) {
        return false;
      }
      createTags(words);
      first = false;
      tokenNum = 0;
    }
    if (tokenNum == tokenAttrs.size()) {
      return false;
    }
    tokenAttrs.get(tokenNum++).copyTo(this);
    return true;
  }
  
  private String[] walkTokens() throws IOException {
    List<String> wordList = new ArrayList<String>();
    while (input.incrementToken()) {
      wordList.add(termAtt.toString());
      tokenAttrs.add(input.cloneAttributes());
    }
    return wordList.toArray(new String[wordList.size()]);
  }
  
  private void createTags(String[] words) {
    String[] appended = appendDot(words);
    if (doPOS) {
      String[] tags = assignPOS(appended);
      assignTokenTypes(tags, words.length);
    }
    else if (doChunking) {
      String[] pos = assignPOS(appended);
      String[] tags = createChunks(words, pos);
      assignTokenTypes(tags, words.length);
    }  
    if (doNER) {
      for(NLPNERTaggerOp op: nerTaggerOps) {
        String[] tags = createAllNER(op, appended);
        assignTokenTypes(tags, words.length);
      }
    }
  }
  
  // Hack #1: taggers expect a sentence break as the final term.
  // This does not make it into the attribute set lists.
  private String[] appendDot(String[] words) {
    int nWords = words.length;
    String lastWord = words[nWords - 1];
    if (lastWord.length() != 1) {
      return words;
    }
    if (lastWord.matches(SENTENCE_BREAK)) {
      return words;
    }
    words = Arrays.copyOf(words, nWords + 1);
    words[nWords] = ".";
    return words;
  }

  private void assignTokenTypes(String[] tags, int length) {
    for (int i = 0 ; i < length ; ++i) {
      tokenAttrs.get(i).getAttribute(TypeAttribute.class).setType(tags[i]);
    }
  }

  private String[] assignPOS(String[] words) {
    return posTaggerOp.getPOSTags(words);
  }
  
  private String[] createChunks(String[] words, String[] pos) {
    return chunkerOp.getChunks(words, pos, null);
  }
  
  private String[] createAllNER(NLPNERTaggerOp nerTagger, String[] words) {
    Span[] nerSpans = nerTagger.getNames(words);
    String[] nerTags = new String[words.length];
    if (nerSpans.length == 0) {
      return nerTags;
    }
    String tag = nerSpans[0].getType();
    for(int i = 0; i < nerSpans.length; i++) {
      Span tagged = nerSpans[i];
      for(int j = tagged.getStart(); j < tagged.getEnd(); j++) {
        nerTags[j] = tag;
      }
    }
    return nerTags;
  }
  
  @Override
  public void reset() throws IOException {
    super.reset();
    tokenNum = 0;
    first = true;
    tokenAttrs.clear();
  }
}

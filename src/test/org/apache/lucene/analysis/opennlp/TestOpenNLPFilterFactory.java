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
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import org.apache.lucene.analysis.BaseTokenStreamTestCase;
import org.apache.lucene.analysis.custom.CustomAnalyzer;
import org.apache.lucene.analysis.payloads.TypeAsPayloadTokenFilter;
import org.apache.lucene.analysis.payloads.TypeAsPayloadTokenFilterFactory;
import org.apache.lucene.analysis.util.ClasspathResourceLoader;

/**
 * Needs the OpenNLP Tokenizer because it creates full streams of punctuation.
 * The POS, Chunking and NER models are based on this tokenization.
 * 
 * Tagging models are created from tiny test data in contrib/opennlp/test-files/training and are not very accurate.
 * Chunking in particular is garbage.
 * NER training generally recognizes sentences that end with "Flashman." The period is required.
 */
public class TestOpenNLPFilterFactory extends OpenNLPStreamTestCase {
  
  static private String SENTENCES = "Sentence number 1 has 6 words. Sentence number 2, 5 words.";
  static private String[] SENTENCES_punc
      = {"Sentence", "number", "1", "has", "6", "words", ".", "Sentence", "number", "2", ",", "5", "words", "."};
  static private int[] SENTENCES_startOffsets = {0, 9, 16, 18, 22, 24, 29, 31, 40, 47, 48, 50, 52, 57};
  static private int[] SENTENCES_endOffsets = {8, 15, 17, 21, 23, 29, 30, 39, 46, 48, 49, 51, 57, 58};
  static private String[] SENTENCES_posTags
      = {"NNS", "NN", "CD", "NNS", "CD", "NNS", ".", "VBD", "IN", "CD", ",", "CD", "NNS", "."};
  static private String[] SENTENCES_chunks
      = { "B-NP", "I-NP", "I-NP", "I-NP", "I-NP", "I-NP", "O", "O", "B-PP", "B-NP", "O", "B-NP", "I-NP", "O" };
  static private String NAMES2 = "Royal Flash is a tale about Harry Flashman.";
  static private String[] NAMES2_punc = {"Royal", "Flash", "is", "a", "tale", "about", "Harry", "Flashman", "."};
  static private String[] NAMES2_OUT = { null, null, null, null, null, null, null, "person", null };

  static private String NO_BREAK = "No period";
  static private String[] NO_BREAK_terms = {"No", "period"};
  static private int[] NO_BREAK_startOffsets = {0, 3};
  static private int[] NO_BREAK_endOffsets = {2, 9};

  private static byte[][] toPayloads(String... strings) {
    return Arrays.stream(strings).map(s -> s == null ? null : s.getBytes(StandardCharsets.UTF_8)).toArray(byte[][]::new);
  }

  public void testBasic() throws IOException {
    CustomAnalyzer analyzer = CustomAnalyzer.builder(new ClasspathResourceLoader(getClass()))
        .withTokenizer("opennlp", "tokenizerModel", "en-test-tokenizer.bin")
        .addTokenFilter("opennlp")
        .build();
    assertAnalyzesTo(analyzer, SENTENCES, SENTENCES_punc, SENTENCES_startOffsets, SENTENCES_endOffsets);
  }

  public void testPOS() throws Exception {
    CustomAnalyzer analyzer = CustomAnalyzer.builder(new ClasspathResourceLoader(getClass()))
        .withTokenizer("opennlp", "tokenizerModel", "en-test-tokenizer.bin")
        .addTokenFilter("opennlp", "posTaggerModel", "en-test-pos-maxent.bin")
        .build();
    assertAnalyzesTo(analyzer, SENTENCES, SENTENCES_punc, SENTENCES_startOffsets, SENTENCES_endOffsets,
        SENTENCES_posTags, null, null, true);

    analyzer = CustomAnalyzer.builder(new ClasspathResourceLoader(getClass()))
        .withTokenizer("opennlp", "tokenizerModel", "en-test-tokenizer.bin")
        .addTokenFilter("opennlp", "posTaggerModel", "en-test-pos-maxent.bin")
        .addTokenFilter(TypeAsPayloadTokenFilterFactory.class)
        .build();
    assertAnalyzesTo(analyzer, SENTENCES, SENTENCES_punc, SENTENCES_startOffsets, SENTENCES_endOffsets,
        null, null, null, true, toPayloads(SENTENCES_posTags));
  }
  
  public void testChunking() throws Exception {
    CustomAnalyzer analyzer = CustomAnalyzer.builder(new ClasspathResourceLoader(getClass()))
        .withTokenizer("opennlp", "tokenizerModel", "en-test-tokenizer.bin")
        .addTokenFilter("opennlp", "posTaggerModel", "en-test-pos-maxent.bin", "chunkerModel", "en-test-chunker.bin")
        .build();
    assertAnalyzesTo(analyzer, SENTENCES, SENTENCES_punc, SENTENCES_startOffsets, SENTENCES_endOffsets,
        SENTENCES_chunks, null, null, true);

    analyzer = CustomAnalyzer.builder(new ClasspathResourceLoader(getClass()))
        .withTokenizer("opennlp", "tokenizerModel", "en-test-tokenizer.bin")
        .addTokenFilter("opennlp", "posTaggerModel", "en-test-pos-maxent.bin", "chunkerModel", "en-test-chunker.bin")
        .addTokenFilter(TypeAsPayloadTokenFilterFactory.class)
        .build();
    assertAnalyzesTo(analyzer, SENTENCES, SENTENCES_punc, SENTENCES_startOffsets, SENTENCES_endOffsets,
        null, null, null, true, toPayloads(SENTENCES_chunks));
  }
  
  public void testNames() throws Exception {
    CustomAnalyzer analyzer = CustomAnalyzer.builder(new ClasspathResourceLoader(getClass()))
        .withTokenizer("opennlp", "tokenizerModel", "en-test-tokenizer.bin")
        .addTokenFilter("opennlp", "nerTaggerModels", "en-test-ner-person.bin")
        .build();
    assertAnalyzesTo(analyzer, NAMES2, NAMES2_punc, null, null, NAMES2_OUT, null, null, true);

    analyzer = CustomAnalyzer.builder(new ClasspathResourceLoader(getClass()))
        .withTokenizer("opennlp", "tokenizerModel", "en-test-tokenizer.bin")
        .addTokenFilter("opennlp", "nerTaggerModels", "en-test-ner-person.bin")
        .addTokenFilter(TypeAsPayloadTokenFilterFactory.class)
        .build();
    assertAnalyzesTo(analyzer, NAMES2, NAMES2_punc, null, null, null, null, null, true, toPayloads(NAMES2_OUT));
  }
  
  public void testNoBreak() throws Exception {
    CustomAnalyzer analyzer = CustomAnalyzer.builder(new ClasspathResourceLoader(getClass()))
        .withTokenizer("opennlp", "tokenizerModel", "en-test-tokenizer.bin")
        .addTokenFilter("opennlp")
        .build();
    assertAnalyzesTo(analyzer, NO_BREAK, NO_BREAK_terms, NO_BREAK_startOffsets, NO_BREAK_endOffsets,
        null, null, null, true);
  }
}

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

import org.apache.lucene.analysis.BaseTokenStreamTestCase;
import org.apache.lucene.analysis.custom.CustomAnalyzer;
import org.apache.lucene.analysis.miscellaneous.KeywordRepeatFilterFactory;
import org.apache.lucene.analysis.miscellaneous.RemoveDuplicatesTokenFilterFactory;
import org.apache.lucene.analysis.util.ClasspathResourceLoader;

public class TestOpenNLPLemmatizerFilterFactory extends BaseTokenStreamTestCase {

  static private String SENTENCES = "They sent him running in the evening.";
  static private String[] SENTENCES_punc = {"they", "send", "he", "run", "in", "the", "evening", "."};
  static private String[] SENTENCES_posTags = {"NNP", "NN", "NN", "NN", "IN", "DT", "VBG", "."};

  static private String[] SENTENCES_keep_orig_punc
      = {"They", "they", "sent", "send", "him", "he", "running", "run", "in", "the", "evening", "."};
  static private String[] SENTENCES_keep_orig_posTags
      = {"NNP", "NNP", "NN", "NN", "NN", "NN", "NN", "NN", "IN", "DT", "VBG", "."};

  public void testBasic() throws Exception {
    CustomAnalyzer analyzer = CustomAnalyzer.builder(new ClasspathResourceLoader(getClass()))
        .withTokenizer("opennlp", "tokenizerModel", "en-test-tokenizer.bin")
        .addTokenFilter("opennlp", "posTaggerModel", "en-test-pos-maxent.bin")
        .addTokenFilter("opennlplemmatizer", "dictionary", "en-test-lemmas.dict")
        .build();
    assertAnalyzesTo(analyzer, SENTENCES, SENTENCES_punc, null, null,
        SENTENCES_posTags, null, null, true);
  }

  public void testKeywordAttributeAwareness() throws Exception {
    CustomAnalyzer analyzer = CustomAnalyzer.builder(new ClasspathResourceLoader(getClass()))
        .withTokenizer("opennlp", "tokenizerModel", "en-test-tokenizer.bin")
        .addTokenFilter("opennlp", "posTaggerModel", "en-test-pos-maxent.bin")
        .addTokenFilter(KeywordRepeatFilterFactory.class)
        .addTokenFilter("opennlplemmatizer", "dictionary", "en-test-lemmas.dict")
        .addTokenFilter(RemoveDuplicatesTokenFilterFactory.class)
        .build();
    assertAnalyzesTo(analyzer, SENTENCES, SENTENCES_keep_orig_punc, null, null,
        SENTENCES_keep_orig_posTags, null, null, true);
  }
}

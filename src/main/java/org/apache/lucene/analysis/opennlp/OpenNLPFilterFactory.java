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
import java.util.Map;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.opennlp.tools.NLPChunkerOp;
import org.apache.lucene.analysis.opennlp.tools.NLPNERTaggerOp;
import org.apache.lucene.analysis.opennlp.tools.NLPPOSTaggerOp;
import org.apache.lucene.analysis.opennlp.tools.OpenNLPOpsFactory;
import org.apache.lucene.analysis.util.ResourceLoader;
import org.apache.lucene.analysis.util.ResourceLoaderAware;
import org.apache.lucene.analysis.util.TokenFilterFactory;

/** 
 * Factory for {@link OpenNLPFilterFactory}. 
 * <pre class="prettyprint" >
 * &lt;fieldType name="text_onlp" class="solr.TextField" positionIncrementGap="100"
 *   &lt;analyzer&gt;
 *   &lt;tokenizer class="solr.OpenNLPFilterFactory"
 *     &lt;posTaggerModel="filename"/&gt;
 *     &lt;chunkerModel="filename"/&gt;
 *     &lt;nerTaggerModels="filename,filename,...,filename"/&gt;
 *   /&gt;
 *   &lt;/analyzer&gt;
 * &lt;/fieldType&gt;</pre>
 * <p>posTaggerModel/chunkerModel/nerTaggerModels are optional.</p>
 * <p>0 or more NER tagger models are accepted. They are run in sequence.</p>
 */
public class OpenNLPFilterFactory extends TokenFilterFactory implements ResourceLoaderAware {
  public static final String POS_TAGGER_MODEL = "posTaggerModel";
  public static final String CHUNKER_MODEL = "chunkerModel";
  public static final String NER_TAGGER_MODELS = "nerTaggerModels";

  private final String posTaggerModelFile;
  private final String chunkerModelFile;
  private final String[] nerTaggerModelFiles;
  
  public OpenNLPFilterFactory(Map<String,String> args) {
    super(args);
    posTaggerModelFile = get(args, POS_TAGGER_MODEL);
    chunkerModelFile = get(args, CHUNKER_MODEL);
    String fileList = get(args, NER_TAGGER_MODELS);
    nerTaggerModelFiles = fileList == null ? new String[0] : fileList.split(",");
    if (!args.isEmpty()) {
      throw new IllegalArgumentException("Unknown parameters: " + args);
    }
  }
  
  @Override
  public OpenNLPFilter create(TokenStream in) {
    try {
      NLPPOSTaggerOp posTaggerOp = null;
      NLPChunkerOp chunkerOp = null;
      ArrayList<NLPNERTaggerOp> nerTaggerOps = null;
      
      if (posTaggerModelFile != null) {
        posTaggerOp = OpenNLPOpsFactory.getPOSTagger(posTaggerModelFile);
      }
      if (chunkerModelFile != null) {
        chunkerOp = OpenNLPOpsFactory.getChunker(chunkerModelFile);
      }
      if (nerTaggerModelFiles != null) {
        nerTaggerOps = new ArrayList<NLPNERTaggerOp>();
        for (String file: nerTaggerModelFiles) {
          NLPNERTaggerOp op = OpenNLPOpsFactory.getNERTagger(file);
          nerTaggerOps.add(op);
        }
      }
      return new OpenNLPFilter(in, posTaggerOp, chunkerOp, nerTaggerOps);
    } catch (IOException e) {
      throw new IllegalArgumentException(e);
    }
  }
  
  @Override
  public void inform(ResourceLoader loader) {
    try {
      // load and register read-only models in cache with file/resource names
      if (posTaggerModelFile != null) {
        OpenNLPOpsFactory.getPOSTaggerModel(posTaggerModelFile, loader.openResource(posTaggerModelFile));
      }
      if (chunkerModelFile != null) {
        OpenNLPOpsFactory.getChunkerModel(chunkerModelFile, loader.openResource(chunkerModelFile));
      }
      if (nerTaggerModelFiles != null) {
        for (String file: nerTaggerModelFiles) {
          OpenNLPOpsFactory.getNERTaggerModel(file, loader.openResource(file));
        }
      }
    } catch (IOException e) {
      throw new IllegalArgumentException(e);
    }
  }
}

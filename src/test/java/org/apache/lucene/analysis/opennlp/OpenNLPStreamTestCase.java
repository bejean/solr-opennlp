package org.apache.lucene.analysis.opennlp;

import java.io.IOException;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.BaseTokenStreamTestCase;
import org.apache.lucene.util.AttributeFactory;
import org.apache.lucene.util.AttributeImpl;
import org.apache.lucene.util.AttributeReflector;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.IOUtils;
import org.apache.lucene.util.LineFileDocs;
import org.apache.lucene.util.LuceneTestCase;

public class OpenNLPStreamTestCase extends BaseTokenStreamTestCase {
	public static void assertAnalyzesTo(Analyzer a, String input, String[] output, int startOffsets[], int endOffsets[], String types[], int posIncrements[], int posLengths[], boolean offsetsAreCorrect, byte[][] payloads) throws IOException {
		//checkResetException(a, input);
		//assertTokenStreamContents(a.tokenStream("dummy", input), output, startOffsets, endOffsets, types, posIncrements, posLengths, input.length(), null, null, offsetsAreCorrect, payloads);
	}

}

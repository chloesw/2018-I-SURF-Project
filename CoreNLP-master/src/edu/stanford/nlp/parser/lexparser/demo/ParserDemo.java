package edu.stanford.nlp.parser.lexparser.demo;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.File;
import java.util.Enumeration;
import java.util.List;
import java.util.Vector;
import java.io.StringReader;
import java.io.FileNotFoundException;

import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.parser.lexparser.LexicalizedParser;
import edu.stanford.nlp.process.CoreLabelTokenFactory;
import edu.stanford.nlp.process.PTBTokenizer;
import edu.stanford.nlp.process.Tokenizer;
import edu.stanford.nlp.process.TokenizerFactory;
import edu.stanford.nlp.trees.GrammaticalStructure;
import edu.stanford.nlp.trees.GrammaticalStructureFactory;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreePrint;
import edu.stanford.nlp.trees.TreebankLanguagePack;
import edu.stanford.nlp.trees.TypedDependency;
import edu.stanford.nlp.util.ScoredObject;

class ParserDemo {

	/**
	 * The main method demonstrates the easiest way to load a parser. Simply call
	 * loadModel and specify the path of a serialized grammar model, which can be a
	 * file, a resource on the classpath, or even a URL. For example, this
	 * demonstrates loading a grammar from the models jar file, which you therefore
	 * need to include on the classpath for ParserDemo to work.
	 *
	 * Usage: {@code java ParserDemo [[model] textFile]} e.g.: java ParserDemo
	 * edu/stanford/nlp/models/lexparser/chineseFactored.ser.gz
	 * data/chinese-onesent-utf8.txt
	 * 
	 * @throws IOException
	 *
	 */
	public static void main(String[] args) throws IOException {
		String parserModel = "edu/stanford/nlp/models/lexparser/englishPCFG.ser.gz";

		if (args.length > 0) {
			parserModel = args[0];
		}
		LexicalizedParser lp = LexicalizedParser.loadModel(parserModel);

		if (args.length == 0) {
			demoAPI(lp);
		} else {
			String textFile = "/Users/zoey/git/2018-I-SURF-Project/CoreNLP-master/src/edu/stanford/nlp/parser/lexparser/demo/input.txt";
			demoDP(lp, textFile);
		}
	}

	/**
	 * demoDP demonstrates turning a file into tokens and then parse trees. Note
	 * that the trees are printed by calling pennPrint on the Tree object. It is
	 * also possible to pass a PrintWriter to pennPrint if you want to capture the
	 * output. This code will work with any supported language.
	 * 
	 * @throws IOException
	 */
	public static void demoDP(LexicalizedParser lp, String filename) throws IOException {

		/*
		 * original demoDP code Tree parse = lp.apply(sentence); parse.pennPrint();
		 * System.out.println();
		 * 
		 * if (gsf != null) { 
		 * GrammaticalStructure gs = gsf.newGrammaticalStructure(parse); 
		 * Collection tdl = gs.typedDependenciesCCprocessed(); System.out.println(tdl);
		 * System.out.println(); }
		 */

		// This option shows loading, sentence-segmenting and tokenizing
		// a file using DocumentPreprocessor.
		// You could also create a tokenizer here (as below) and pass it
		// to DocumentPreprocessor
		File file = new File(filename);
		FileReader fileReader = new FileReader(file);
		BufferedReader bufReader = new BufferedReader(fileReader);
		BufferedWriter writer = new BufferedWriter(new FileWriter("out.txt"));

		String line = "";
		while ((line = bufReader.readLine()) != null) {

			List<ScoredObject<Tree>> parses;
			Tree final_tree = null;
			List<TypedDependency> final_tdl = null;

			// This option shows loading and using an explicit tokenizer

			String sent2 = line;

			TokenizerFactory<CoreLabel> tokenizerFactory = PTBTokenizer.factory(new CoreLabelTokenFactory(), "");
			Tokenizer<CoreLabel> tok = tokenizerFactory.getTokenizer(new StringReader(sent2));
			List<CoreLabel> rawWords2 = tok.tokenize();
			parses = lp.kparse(rawWords2);
			int ii = 0;
			for (ScoredObject<Tree> parse : parses) {
				ii++;
				Tree t = parse.object();
				TreebankLanguagePack tlp1 = lp.treebankLanguagePack(); // PennTreebankLanguagePack for English
				GrammaticalStructureFactory gsf1 = tlp1.grammaticalStructureFactory();
				GrammaticalStructure gs = gsf1.newGrammaticalStructure(t);
				List<TypedDependency> tdl = gs.typedDependenciesCCprocessed();
				for (int i = 0; i < tdl.size(); i++) {
					String extractElement = tdl.get(i).reln().toString();
					if (extractElement.equals("dobj")) {
						writer.write("Number " + ii + " parse has dobj.");
						writer.newLine();
						writer.write("Sentence : " + line);
						writer.newLine();
						
						System.out.println("Number " + ii + " parse has dobj.");
						System.out.println("Sentence : " + line);
		
						final_tree = t;
						final_tdl = tdl;
						break;
					}
				}
				if (final_tree != null)
					break;
			}
			if (ii == 5) {
				writer.append("Sentence : " + line);
				writer.newLine();
				writer.append("There are no dobj in 5 parses");
				writer.newLine();
				writer.append("===========================================================================");
				writer.newLine();
				/*
				 * System.out.println("Sentence : " + line);
				 * System.out.println("There are no dobj in 5 parses"); System.out.println(
				 * "==========================================================================="
				 * );
				 */
				continue;
			}

			//System.out.println("----------------------------------------");
			//System.out.println("Sentence : " + sent2 + "\n");
			
			Relation reln = new Relation(); // make relation class that store govIdx, devIdx, wordIdx, word
			// ex ) I eat an apple. 
			// nsubj (eat-2, I-1)
			// govIdx = 2, devIdx = 1
			// wordIdx = 0
			// word = "I"
			
			Vector<Relation> nsbjIdx = new Vector<Relation>();
			Vector<Relation> dobjIdx = new Vector<Relation>();
			Vector<Relation> compIdx = new Vector<Relation>();
			Vector<Relation> nmodIdx = new Vector<Relation>();
			int wordIdx = -1;

			for (int i = 0; i < final_tdl.size(); i++) {
				String extractElement = final_tdl.get(i).reln().toString();
				if (extractElement.equals("compound")) {
					reln = new Relation(final_tdl.get(i).gov().toCopyIndex(), final_tdl.get(i).dep().toCopyIndex(), i); // store govIdx, depIdx, wordIdx
					reln.setWord(final_tdl.get(i).dep().originalText().toLowerCase()); // store dependency word 
					compIdx.add(reln); // add relation object to compound vector
				}
				if (extractElement.equals("nsubj")) {
					reln = new Relation(final_tdl.get(i).gov().toCopyIndex(), final_tdl.get(i).dep().toCopyIndex(), i);
					reln.setWord(final_tdl.get(i).dep().originalText().toLowerCase());
					nsbjIdx.add(reln);
				}
				if (extractElement.equals("dobj")) {
					reln = new Relation(final_tdl.get(i).gov().toCopyIndex(), final_tdl.get(i).dep().toCopyIndex(), i);
					reln.setWord(final_tdl.get(i).dep().originalText().toLowerCase());
					dobjIdx.add(reln);
				}
				if (extractElement.contains("nmod")) {
					reln = new Relation(final_tdl.get(i).gov().toCopyIndex(), final_tdl.get(i).dep().toCopyIndex(), i);
					reln.setWord(final_tdl.get(i).dep().originalText().toLowerCase());
					nmodIdx.add(reln);
				}
			}
			
			for (int compNum = compIdx.size() - 1; compNum > -1; compNum--) {
				for (int dobjNum = 0; dobjNum < dobjIdx.size(); dobjNum++) {
					if (compIdx.get(compNum).govIdx.equals(dobjIdx.get(dobjNum).depIdx)) {
						dobjIdx.get(dobjNum).addBeforeWord(compIdx.get(compNum).word);
					}
				}
				for (int nsbjNum = 0; nsbjNum < nsbjIdx.size(); nsbjNum++) {
					if (compIdx.get(compNum).govIdx.equals(nsbjIdx.get(nsbjNum).depIdx)) {
						nsbjIdx.get(nsbjNum).addBeforeWord(compIdx.get(compNum).word);
					}
				}
			}
			for (int nmodNum = 0; nmodNum < nmodIdx.size(); nmodNum++) {
				String nmodText;
				String prep;
				for (int dobjNum = 0; dobjNum < dobjIdx.size(); dobjNum++) {
					if (nmodIdx.get(nmodNum).govIdx.equals(dobjIdx.get(dobjNum).depIdx)) { // compare govidx of nmod with depIdx of dobj
						wordIdx = nmodIdx.get(nmodNum).wordIdx;
						nmodText = final_tdl.get(wordIdx).reln().toString();
						prep = nmodText.substring(5);
						dobjIdx.get(dobjNum).addAfterWord(prep);
						dobjIdx.get(dobjNum).addAfterWord(nmodIdx.get(nmodNum).word);
					}
				}
				for (int nsbjNum = 0; nsbjNum < nsbjIdx.size(); nsbjNum++) {
					if (nmodIdx.get(nmodNum).govIdx.equals(nsbjIdx.get(nsbjNum).depIdx)) {
						wordIdx = nmodIdx.get(nmodNum).wordIdx;
						nmodText = final_tdl.get(wordIdx).reln().toString();
						prep = nmodText.substring(5);
						nsbjIdx.get(nsbjNum).addAfterWord(prep);
						nsbjIdx.get(nsbjNum).addAfterWord(nmodIdx.get(nmodNum).word);
					}
				}
			}
			
			//System.out.print("Subject : ");
			writer.append("Subject : ");
			for (int i = 0; i < nsbjIdx.size(); i++) {
				//System.out.print(nsbjIdx.get(i).word + " ");
				writer.append( " ( " + nsbjIdx.get(i).word + " ) ");
			}
			//System.out.print("\nDoject : ");
			writer.newLine();
			writer.append("Doject : ");
			for (int j = 0; j < dobjIdx.size(); j++) {
				//System.out.print("( " + dobjIdx.get(j).word + " ) ");
				writer.append("( " + dobjIdx.get(j).word + " ) ");
			}
			
			writer.newLine();
			
			for (int i = 0; i < nsbjIdx.size(); i++) 
			{
				writer.newLine();
				writer.append(nsbjIdx.get(i).word);
				writer.append(" / ");
				//System.out.print(nsbjIdx.get(i).word);
				//System.out.print(" / ");
				for (int j = 0; j < dobjIdx.size(); j++) {
					if (nsbjIdx.get(i).govIdx.equals(dobjIdx.get(j).govIdx)) {
						wordIdx = nsbjIdx.get(i).wordIdx;
						writer.append(final_tdl.get(wordIdx).gov().originalText().toLowerCase());
						writer.append(" / ");
						writer.append(dobjIdx.get(j).word);
						writer.newLine();
						/*
						System.out.print(final_tdl.get(wordIdx).gov().originalText().toLowerCase());
						System.out.print(" / ");
						System.out.print(dobjIdx.get(j).word);
						System.out.println();
						*/
					}
				}
			}
			
			writer.append("----------------------------------------");
			writer.newLine();
			
			// TreePrint tp = new TreePrint("penn,typedDependencies"); // penn -> seg tree ,
			// typedDependencies -> Dependecy in TreePrint function
			// System.out.println("printTree function \n");
			// tp.printTree(final_tree);
		}
		writer.close();
	}

	/**
	 * demoAPI demonstrates other ways of calling the parser with already tokenized
	 * text, or in some cases, raw text that needs to be tokenized as a single
	 * sentence. Output is handled with a TreePrint object. Note that the options
	 * used when creating the TreePrint can determine what results to print out.
	 * Once again, one can capture the output by passing a PrintWriter to
	 * TreePrint.printTree. This code is for English.
	 */

	public static void demoAPI(LexicalizedParser lp) {
		// This option shows parsing a list of correctly tokenized words
		// String[] sent = { "The", "machine", "checks", "how",
		// "much","money","has","been","deposited", "." };
		// List<CoreLabel> rawWords = SentenceUtils.toCoreLabelList(sent);

		List<ScoredObject<Tree>> parses;
		Tree final_tree = null;
		List<TypedDependency> final_tdl = null;

		//String sent2 = "The registrar inputs the name, address, and phone number of the applicant.";
		//String sent2 = "The registrar inputs the name, and phone number of the applicant and checks the documents.";
		//String sent2 = "ATM displays the amount which a user entered.";
		//String sent2 = "Print the value which user entered on the screen.";
		//String sent2 = "Don't notify me about this conflict";
		//String sent2 = "Cashier tells Customer the total, and asks for payment.";
		//String sent2 = "User has selected the items to be purchased.";
		//String sent2 = "The user will confirm that the order information is accurate.";
		//String sent2 = "The system will present the amount that the order will cost including applicable taxes and shipping charges.";
		//String sent2 = "The applicant hands a filled out copy of form UI13 University Application Form to the registrar. ";
		String sent2 = "ATM asks amount to withdraw.";

		TokenizerFactory<CoreLabel> tokenizerFactory = PTBTokenizer.factory(new CoreLabelTokenFactory(), "");
		Tokenizer<CoreLabel> tok = tokenizerFactory.getTokenizer(new StringReader(sent2));
		List<CoreLabel> rawWords2 = tok.tokenize();
		// System.out.println("rawWords:" + rawWords2);
		parses = lp.kparse(rawWords2);
		int ii = 0;
		for (ScoredObject<Tree> parse : parses) {
			ii++;
			Tree t = parse.object();
			TreebankLanguagePack tlp = lp.treebankLanguagePack(); // PennTreebankLanguagePack for English
			GrammaticalStructureFactory gsf = tlp.grammaticalStructureFactory();
			GrammaticalStructure gs = gsf.newGrammaticalStructure(t);
			List<TypedDependency> tdl = gs.typedDependenciesCCprocessed();
			for (int i = 0; i < tdl.size(); i++) {
				String extractElement = tdl.get(i).reln().toString();
				if (extractElement.equals("dobj")) {
					System.out.println("Number " + ii + " parse has dobj.");
					System.out.println("");
					final_tree = t;
					final_tdl = tdl;
					break;
				}
			}
			if (final_tree != null)
				break;
		}
		if (ii == 5) {
			System.out.println("There are no dobj in 5 parses");
			return;
		}

		System.out.println("Sentence : " + sent2 + "\n");

		TreePrint tp = new TreePrint("penn,typedDependencies"); // penn -> seg tree ,
		// typedDependencies -> Dependecy in TreePrint function
		// System.out.println("printTree function \n");
		tp.printTree(final_tree);
		Relation reln = new Relation();
		Vector<Relation> nsbjIdx = new Vector<Relation>();
		Vector<Relation> dobjIdx = new Vector<Relation>();
		Vector<Relation> compIdx = new Vector<Relation>();
		Vector<Relation> nmodIdx = new Vector<Relation>();
		int wordIdx = -1;

		for (int i = 0; i < final_tdl.size(); i++) {
			String extractElement = final_tdl.get(i).reln().toString();
			if (extractElement.equals("compound")) {
				reln = new Relation(final_tdl.get(i).gov().toCopyIndex(), final_tdl.get(i).dep().toCopyIndex(), i);
				reln.setWord(final_tdl.get(i).dep().originalText().toLowerCase());
				compIdx.add(reln);
			}
			if (extractElement.equals("nsubj")) {
				reln = new Relation(final_tdl.get(i).gov().toCopyIndex(), final_tdl.get(i).dep().toCopyIndex(), i);
				reln.setWord(final_tdl.get(i).dep().originalText().toLowerCase());
				nsbjIdx.add(reln);
			}
			if (extractElement.equals("dobj")) {
				reln = new Relation(final_tdl.get(i).gov().toCopyIndex(), final_tdl.get(i).dep().toCopyIndex(), i);
				reln.setWord(final_tdl.get(i).dep().originalText().toLowerCase());
				dobjIdx.add(reln);
			}
			if (extractElement.contains("nmod")) {
				reln = new Relation(final_tdl.get(i).gov().toCopyIndex(), final_tdl.get(i).dep().toCopyIndex(), i);
				reln.setWord(final_tdl.get(i).dep().originalText().toLowerCase());
				nmodIdx.add(reln);
			}
		}

		for (int compNum = compIdx.size() - 1; compNum > -1; compNum--) {
			for (int dobjNum = 0; dobjNum < dobjIdx.size(); dobjNum++) {
				if (compIdx.get(compNum).govIdx.equals(dobjIdx.get(dobjNum).depIdx)) {
					dobjIdx.get(dobjNum).addBeforeWord(compIdx.get(compNum).word);
				}
			}
			for (int nsbjNum = 0; nsbjNum < nsbjIdx.size(); nsbjNum++) {
				if (compIdx.get(compNum).govIdx.equals(nsbjIdx.get(nsbjNum).depIdx)) {
					nsbjIdx.get(nsbjNum).addBeforeWord(compIdx.get(compNum).word);
				}
			}
		}
		for (int nmodNum = 0; nmodNum < nmodIdx.size(); nmodNum++) {
			String nmodText;
			String prep;
			for (int dobjNum = 0; dobjNum < dobjIdx.size(); dobjNum++) {
				if (nmodIdx.get(nmodNum).govIdx.equals(dobjIdx.get(dobjNum).depIdx)) {
					wordIdx = nmodIdx.get(nmodNum).wordIdx;
					nmodText = final_tdl.get(wordIdx).reln().toString();
					prep = nmodText.substring(5);
					dobjIdx.get(dobjNum).addAfterWord(prep);
					dobjIdx.get(dobjNum).addAfterWord(nmodIdx.get(nmodNum).word);
				}
			}
			for (int nsbjNum = 0; nsbjNum < nsbjIdx.size(); nsbjNum++) {
				if (nmodIdx.get(nmodNum).govIdx.equals(nsbjIdx.get(nsbjNum).depIdx)) {
					wordIdx = nmodIdx.get(nmodNum).wordIdx;
					nmodText = final_tdl.get(wordIdx).reln().toString();
					prep = nmodText.substring(5);
					nsbjIdx.get(nsbjNum).addAfterWord(prep);
					nsbjIdx.get(nsbjNum).addAfterWord(nmodIdx.get(nmodNum).word);
				}
			}
		}
		System.out.print("Subject : ");
		for (int i = 0; i < nsbjIdx.size(); i++) {
			System.out.print(nsbjIdx.get(i).word + " ");
		}
		System.out.print("\nDoject : ");
		for (int j = 0; j < dobjIdx.size(); j++) {
			System.out.print("( " + dobjIdx.get(j).word + " ) ");
		}
		System.out.println("\n");

		for (int i = 0; i < nsbjIdx.size(); i++) 
		{
			System.out.print(nsbjIdx.get(i).word);
			System.out.print(" / ");
			for (int j = 0; j < dobjIdx.size(); j++) {
				if (nsbjIdx.get(i).govIdx.equals(dobjIdx.get(j).govIdx)) {
					wordIdx = nsbjIdx.get(i).wordIdx;
					System.out.print(final_tdl.get(wordIdx).gov().originalText().toLowerCase());
					System.out.print(" / ");
					System.out.print(dobjIdx.get(j).word);
					System.out.println();
				}
			}
		}

	}

	private ParserDemo() {
	} // static methods only
	
	

}
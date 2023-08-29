import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.tartarus.snowball.ext.PorterStemmer;

import java.io.*;
import java.nio.file.Path;

public class Main {
       final static String indexPath = "indexedFiles";
       final static String DocsPath = "inputFiles";
       final static String PDFDocsPath = "PDF_Files";
    public static void main(String[] args) throws IOException,ParseException {
        // Dealing with pdf files
        File[]PDFFiles = new File(PDFDocsPath).listFiles();
        for(File f : PDFFiles){
            PDDocument PdfDoc = Loader.loadPDF(f);
            String PdfContent = new PDFTextStripper().getText(PdfDoc);
            File newTxt = new File(DocsPath+'\\'+f.getName()+".txt");
            FileWriter fileWriter = new FileWriter(newTxt);
            fileWriter.write(PdfContent);
            fileWriter.close();
        }
        //getting and preparing all input files
        File[]files = new File(DocsPath).listFiles();
        Directory dir = FSDirectory.open(Path.of(indexPath).toFile());
        //Building Documents and inverted index
        EnglishAnalyzer analyzer = new EnglishAnalyzer(Version.LUCENE_42);
        //PorterStemmer analyzer = new PorterStemmer();
        IndexWriterConfig cfg = new IndexWriterConfig(Version.LUCENE_42,analyzer);
        IndexWriter writer = new IndexWriter(dir,cfg);
        for (File f:files) {
            if(f.isFile() && f.canRead() && !f.isHidden()){
                Document doc = new Document();
                try {
                    doc.add(new Field("FileName",
                            f.getName(), Field.Store.YES, Field.Index.NOT_ANALYZED));

                    doc.add(new Field("Content",
                            new FileReader(f)));

                    doc.add(new Field("FullPath",
                            f.getCanonicalPath(), Field.Store.YES, Field.Index.NOT_ANALYZED));

                } catch (FileNotFoundException e) {
                    throw new RuntimeException(e);
                }
                writer.addDocument(doc);
            }
        }
        System.out.println("# of docs = " + writer.numDocs());
        writer.close();

        //Search Part (Phrase & Term & Boolean Queries)
        String Query = "emphasized";
        search(indexPath,Query,"Content","FullPath",10);
    }
    public static void search(String path, String Query,String SearchField,String ReturnedField,int hits)throws IOException, ParseException {
        Directory dir = FSDirectory.open(Path.of(path).toFile());
        IndexReader reader = DirectoryReader.open(dir);
        Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_42);
        Query query = new QueryParser(Version.LUCENE_42,SearchField,analyzer).parse(Query);
        IndexSearcher searcher = new IndexSearcher(reader);
        TopDocs Hits = searcher.search(query,hits);
        for(ScoreDoc scoreDoc : Hits.scoreDocs){
            Document doc = searcher.doc(scoreDoc.doc);
            System.out.println(doc.get(ReturnedField));
        }
        reader.close();
    }
}
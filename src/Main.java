import org.apache.lucene.analysis.en.EnglishAnalyzer;
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
import javax.swing.*;
import java.awt.*;

import java.io.*;
import java.nio.file.Path;

public class Main {
       final static String indexPath = "indexedFiles";
       final static String DocsPath = "inputFiles";
       final static String PDFDocsPath = "PDF_Files";
       private static JTextField queryTextField;
       private static JTextArea resultsTextArea;
    public static void main(String[] args) throws IOException {
        // Create the GUI frame
        JFrame frame = new JFrame("Lucene Search");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());


        // Create the query panel
        JPanel queryPanel = new JPanel(new BorderLayout());
        JLabel queryLabel = new JLabel("Query:");
        queryTextField = new JTextField();
        JButton searchButton = new JButton("Search");


        // Create the results panel
        JPanel resultsPanel = new JPanel(new BorderLayout());
        JLabel resultsLabel = new JLabel("Results:");
        resultsTextArea = new JTextArea();
        resultsTextArea.setEditable(false);
        JScrollPane resultsScrollPane = new JScrollPane(resultsTextArea);
        resultsPanel.add(resultsLabel, BorderLayout.NORTH);
        resultsPanel.add(resultsScrollPane, BorderLayout.CENTER);


        // Dealing with pdf files
        File[]PDFFiles = new File(PDFDocsPath).listFiles();
        assert PDFFiles != null;
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
        EnglishAnalyzer analyzer = new EnglishAnalyzer (Version.LUCENE_42);
        //Building Documents and inverted index
        IndexWriterConfig cfg = new IndexWriterConfig(Version.LUCENE_42,analyzer);
        IndexWriter writer = new IndexWriter(dir,cfg);
        assert files != null;
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
       // System.out.println("#of docs = " + writer.numDocs());
        writer.close();
        searchButton.addActionListener(e -> {
            String query = queryTextField.getText();
            try {
                search(indexPath, query, "Content", "FullPath", 10);
            } catch (IOException | ParseException ex) {
                ex.printStackTrace();
            }
        });










        queryPanel.add(queryLabel, BorderLayout.WEST);
        queryPanel.add(queryTextField, BorderLayout.CENTER);
        queryPanel.add(searchButton, BorderLayout.EAST);
        // Add the panels to the frame
        frame.add(queryPanel, BorderLayout.NORTH);
        frame.add(resultsPanel, BorderLayout.CENTER);

        // Set frame properties
        frame.setSize(600, 400);
        frame.setVisible(true);

    }
    public static void search(String path, String Query,String SearchField,String ReturnedField,int hits)throws IOException, ParseException {
        Directory dir = FSDirectory.open(Path.of(path).toFile());
        IndexReader reader = DirectoryReader.open(dir);
        EnglishAnalyzer  analyzer = new EnglishAnalyzer (Version.LUCENE_42);
        Query query = new QueryParser(Version.LUCENE_42,SearchField,analyzer).parse(Query);
        IndexSearcher searcher = new IndexSearcher(reader);
        TopDocs Hits = searcher.search(query,hits);
        for(ScoreDoc scoreDoc : Hits.scoreDocs){
            Document doc = searcher.doc(scoreDoc.doc);
            resultsTextArea.append(doc.get(ReturnedField) + "\n");
        }
        reader.close();
    }
}
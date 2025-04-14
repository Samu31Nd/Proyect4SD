/*
 * Proyecto no.4
 * Sanchez Leyva Eduardo Samuel
 * Grupo 7CM2 Sistemas Distribuidos
 */

import java.io.File;
import java.util.List;
import java.util.HashMap;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

public class Server {
    public static final String ANSI_RED = "\033[31m";
    public static final String ANSI_BOLD = "\033[1m";
    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_YELLOW = "\u001B[33m";

    // collects all the searched words
    // word, repeatedSearch
    // book, <word, [paragraph, page]>
    public static HashMap<String, HashMap<String, Integer[]>> booksWordsSearched;
    public static GoogleCloudService cloudOperations;

    public static void initServer(){
        booksWordsSearched = new HashMap<>();
        cloudOperations = new GoogleCloudService();
        Logger.getLogger("org.apache.pdfbox.pdmodel.font.PDTrueTypeFont").setLevel(Level.SEVERE);
    }

    static String searchPatternInBook(String bookPath, String searchingWord) {
    StringBuilder result = new StringBuilder();
    HashMap<String, Integer[]> wordsSearched = booksWordsSearched.getOrDefault(bookPath, new HashMap<>());
    Integer bookIndex[] = wordsSearched.getOrDefault(searchingWord, new Integer[] { 0, 1 });

    if (bookIndex[1] == -1) {
        return "";
    }

    boolean found = false;

    File file = new File(bookPath);
    String bookName = bookPath.split("/", 2)[1];

    try (PDDocument documento = Loader.loadPDF(file)) {
        int totalPags = documento.getNumberOfPages();
        PDFTextStripper stripper = new PDFTextStripper();

        for (int pag = bookIndex[1]; pag <= totalPags; pag++) {
            stripper.setStartPage(pag);
            stripper.setEndPage(pag);

            String texto = stripper.getText(documento);
            String[] parrafos = texto.split("(?<=[\\.\\?!])\\r?\\n");

            for (int i = bookIndex[0]; i < parrafos.length; i++) {
                if (parrafos[i].toLowerCase().contains(searchingWord.toLowerCase())) {
                    result.append(ANSI_BOLD + "\nEn el texto " + getTitle(bookName) + " de "
                            + getAuthor(bookName) + " se encontró la oración:" + ANSI_RESET + "\n" +
                            "[Pagina " + pag + "]\n");

                    String parrafoPlano = parrafos[i].replace("\n", " ");
                    Pattern pattern = Pattern.compile("(?i)\\b" + Pattern.quote(searchingWord));
                    Matcher matcher = pattern.matcher(parrafoPlano);
                    int lastEnd = 0;

                    while (matcher.find()) {
                        result.append(parrafoPlano, lastEnd, matcher.start());
                        result.append(ANSI_YELLOW).append(parrafoPlano, matcher.start(), matcher.end()).append(ANSI_RESET);
                        lastEnd = matcher.end();
                    }
                    result.append(parrafoPlano.substring(lastEnd));

                    try {
                        result.append("\n" + cloudOperations.translate(parrafos[i]));
                    } catch (Exception e) {
                        result.append("\n" + ANSI_RED
                                + "Error: No se pudo traducir. Probablemente el servicio de traducción se encuentra inactivo actualmente.\n"
                                + "Intentar más tarde...." + ANSI_RESET);
                    }

                    bookIndex[1] = pag;
                    bookIndex[0] = i + 1;
                    found = true;
                    break;
                }
            }

            if (found)
                break;

            bookIndex[0] = 0;
        }
        documento.close();

    } catch (IOException e) {
        e.printStackTrace();
    }

    if (!found)
        bookIndex[1] = -1;

    wordsSearched.put(searchingWord, bookIndex);
    booksWordsSearched.put(bookPath, wordsSearched);

    return result.toString() + "\n";
}
    public static String getTitle(String bookName) {
        // Notar que también pudimos haber obtenido el nombre y autor del libro con
        // busqueda en el pdf, pero... no, no es eficiente
        return bookName.substring(0, bookName.length() - 4).split("-", 2)[0].replace("_", " ");
    }

    public static String getAuthor(String bookName) {
        return bookName.substring(0, bookName.length() - 4).split("-", 2)[1].replace("_", " ");
    }

    public static String searchWordInFiles(String wordToSearch) throws IOException, InterruptedException {
        StringBuilder result = new StringBuilder();
        List<BucketItem> bucketFiles = cloudOperations.listBucket();
        for (BucketItem book : bucketFiles) {
            cloudOperations.downloadBook(book);
            result.append(searchPatternInBook(book.name, wordToSearch));
        }
        return result.toString();
    }

    public static String searchWordWithPattern(String author, String title, String word) throws IOException, InterruptedException {
        StringBuilder result = new StringBuilder();
        List<BucketItem> bucketFiles = cloudOperations.listBucket();
        for (BucketItem book : bucketFiles) {
            String nameBook = getTitle(book.name.split("/",2)[1]);
            String authorBook = getAuthor(book.name.split("/",2)[1]);
            if(! (nameBook.toLowerCase().contains(title.toLowerCase())) ||
            !(authorBook.toLowerCase().contains(author.toLowerCase())) )
            continue;
            cloudOperations.downloadBook(book);
            result.append(searchPatternInBook(book.name, word));
        }
        return result.toString();
    }
    
    public static String getAllBooks() throws IOException, InterruptedException {
        StringBuilder result = new StringBuilder();
        List<BucketItem> bucketFiles = cloudOperations.listBucket();
        for (BucketItem book : bucketFiles) {
            String nameBook = getTitle(book.name.split("/",2)[1]);
            String authorBook = getAuthor(book.name.split("/",2)[1]);
            result.append("\t" + nameBook +  " - " + authorBook + "\n");
        }
        return result.toString();
    }
}

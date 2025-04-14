/*
 * Proyecto no.4
 * Sanchez Leyva Eduardo Samuel
 * Grupo 7CM2 Sistemas Distribuidos
 */

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;

public class WebServer {
    
    private static final String TASK_SEARCH_WORD_ALL_FILES  = "/search";
    private static final String TASK_SEARCH_WORD_FILE_PATTERN = "/searchWithFilter";
    private static final String TASK_LIST_BOOKS = "/list";

    private final int port;
    private HttpServer server;
    public static void main(String[] args) {
        int serverPort = 8080;
        if(args.length == 1)
            serverPort= Integer.parseInt(args[0]);
        Server.initServer();
        WebServer webServer = new WebServer(serverPort);
        webServer.startServer();
        System.out.println("Servidor escuchando en el puerto " + serverPort);
    }

    public WebServer(int port){
        this.port = port;
        
    }

    public void startServer(){
        try {
            this.server = HttpServer.create( new InetSocketAddress(port), 0 );
        } catch(IOException e){
            e.printStackTrace();
            return;
        }
        HttpContext searchAllFilesContext = server.createContext(TASK_SEARCH_WORD_ALL_FILES);
        HttpContext searchFilePatternContext = server.createContext(TASK_SEARCH_WORD_FILE_PATTERN);
        HttpContext listAllBooksContext = server.createContext(TASK_LIST_BOOKS);
        searchAllFilesContext.setHandler(this::handleSearchAllFiles);
        searchFilePatternContext.setHandler(this::handleSearchFilePattern);
        listAllBooksContext.setHandler(this::handleListBooks);
        server.setExecutor(Executors.newFixedThreadPool(4));
        server.start();
    }

    private void handleSearchAllFiles(HttpExchange exchange) throws IOException {
        if(!exchange.getRequestMethod().equalsIgnoreCase("post")){
            exchange.close();
            return;
        }
        
        byte []requestBytes = exchange.getRequestBody().readAllBytes();
        String wordToSearch = new String(requestBytes);

        String responseMessage = "";
        try {
            responseMessage = Server.searchWordInFiles(wordToSearch);
        } catch (IOException | InterruptedException e) {
            sendResponse(new String("An error happened!").getBytes(), exchange);
            e.printStackTrace();
        }
        sendResponse(responseMessage.getBytes(), exchange);
    }

    private void handleSearchFilePattern(HttpExchange exchange) throws IOException{
        if(!exchange.getRequestMethod().equalsIgnoreCase("post")){
            exchange.close();
            return;
        }
        byte []requestBytes = exchange.getRequestBody().readAllBytes();
        String requestString = new String(requestBytes);
        String []params = requestString.split(",");
        String word = params[params.length-1];
        String author = "", title = "";
        for(int i = 0; i < params.length-1; i++){
            if(params[i].contains("AUTHOR"))
                author = params[i].split("=")[1];
            else if(params[i].contains("TITLE")) 
                title = params[i].split("=")[1];
        }

        String responseMessage = "";
        try {
            responseMessage = Server.searchWordWithPattern(author, title, word);
        } catch (IOException | InterruptedException e) {
            sendResponse(new String("An error happened!").getBytes(), exchange);
            e.printStackTrace();
        }
        sendResponse(responseMessage.getBytes(), exchange);
    }

    private void handleListBooks(HttpExchange exchange) throws IOException{
        if(!exchange.getRequestMethod().equalsIgnoreCase("get")){
            exchange.close();
            return;
        }
        String response = "Libros disponibles:\n\n";
        try {
            response += Server.getAllBooks();
        } catch(IOException | InterruptedException e){
            e.printStackTrace();
        }
        sendResponse(response.getBytes(), exchange);
    }

    private void sendResponse(byte[] responseBytes, HttpExchange exchange) throws IOException {
        exchange.sendResponseHeaders(200, responseBytes.length);
        OutputStream outputStream = exchange.getResponseBody();
        outputStream.write(responseBytes);
        outputStream.flush();
        outputStream.close();
        exchange.close();
    }
}

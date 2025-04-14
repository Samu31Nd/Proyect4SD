/*
 * Proyecto no.4
 * Sanchez Leyva Eduardo Samuel
 * Grupo 7CM2 Sistemas Distribuidos
 */

import java.net.URI;
import java.util.Scanner;
import java.time.Duration;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpRequest.BodyPublishers;

public class Client {

    public static final String ANSI_RED = "\033[31m";
    public static final String ANSI_BOLD = "\033[1m";
    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    private static final String regex = "^(AUTHOR=([^=,\\n]+),)?(TITLE=([^=,\\n]+),)?([^=,\\n]+)$";

    private static final HttpClient httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(30))
            .build();

    public static void main(String[] args) throws IOException, InterruptedException {
        Pattern pattern = Pattern.compile(regex);

        System.out.print("\033[H\033[2J");
        System.out.flush();
        while (true) {
            String petitionServer = getSearchingWord();

            if (petitionServer.contains("LIST")) {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:8080/list"))
                        .GET()
                        .build();
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                System.out.println(response.body());
                continue;
            }

            String endpoint = "search";
            Matcher match = pattern.matcher(petitionServer);
            if (match.matches())
                endpoint = "searchWithFilter";
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8080/" + endpoint))
                    .POST(BodyPublishers.ofString(petitionServer))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println(response.body());
        }
    }

    static Scanner scan = new Scanner(System.in);

    public static String getSearchingWord() {
        String input = "";
        do {
            System.out.print(
                    "Ingresa HELP para mostrar la ayuda.\n" +
                            "Ingresa la palabra a buscar en los documentos: "
                            + ANSI_YELLOW);
            input = scan.nextLine();
            System.out.print(ANSI_RESET);

            if (input.equals("HELP")) {
                System.out.println(
                        "\nPresiona Ctrl+C para terminar el programa!\n" +
                                "Si deseas busqueda con filtro, intenta:\n\t" + ANSI_BOLD
                                + "AUTHOR=<Autor>,TITLE=<Titulo del libro>,<palabra a buscar>\n" + ANSI_RESET +
                                "Ingresa " + ANSI_BOLD + "LIST " + ANSI_RESET + "para listar los libros.");
                input = "";
            }
            if (input.equals(" ")) {
                input = "";
                System.out.println("Ingresa algo valido!");
            }
        } while (input.isBlank());
        return input;
    }

    static void setCloseOperations() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println(ANSI_GREEN + "\n\nPrograma terminado con exito." + ANSI_RESET);
        }));
    }
}

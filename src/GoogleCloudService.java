/*
 * Proyecto no.4
 * Sanchez Leyva Eduardo Samuel
 * Grupo 7CM2 Sistemas Distribuidos
 */

import java.io.File;
import java.net.URI;
import java.util.List;
import java.net.URLEncoder;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class GoogleCloudService {
    private static String TOKEN = "";
    private static String TRANSLATE_API_KEY = "";
    private static String TRANSLATE_URL = "https://translation.googleapis.com/language/translate/v2?target=es&key=";
    private static String BUCKET_ROUTE = "first-bvic/o?prefix=pdfFiles/";
    private HttpClient client;
    private Gson gson;

    private static String pathBooks = "";

    GoogleCloudService(){

        try (FileInputStream fileInputStream = new FileInputStream(".env")) {
                 Properties properties = new Properties();
                 properties.load(fileInputStream);
                 TOKEN = properties.getProperty("GOOGLE_TOKEN");
                 TRANSLATE_API_KEY = properties.getProperty("TRANSLATE_API_KEY");
                 TRANSLATE_URL += TRANSLATE_API_KEY;
                 System.out.println(TOKEN);
             } catch (IOException e) {
                 e.printStackTrace();
             }

        this.gson = new Gson();
        this.client = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .build();    
    }

    public List<BucketItem> listBucket() throws IOException, InterruptedException{
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("https://storage.googleapis.com/storage/v1/b/" + BUCKET_ROUTE))
            .setHeader("Authorization", "Bearer " + TOKEN)
            .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            String responseBody = response.body();

            BucketListResponse responseGson = gson.fromJson(responseBody, BucketListResponse.class);
            if(responseGson == null) return List.of();
            responseGson.items.remove(0);
            
            return responseGson.items;
    }

    public void downloadBook(BucketItem item) throws IOException, InterruptedException {

        if (pathBooks.isBlank()){
            pathBooks = item.name.split("/")[0];
            File dirBooks = new File(pathBooks);
            if(!dirBooks.exists()){
                if(!dirBooks.mkdirs()){
                    System.out.println("No se pudo crear el directorio!");
                    System.exit(1);
                }
            }
        }

        if(new File(item.name).exists())
            return;

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(item.mediaLink))
            .setHeader("Authorization", "Bearer " + TOKEN)
            .build();

        HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
        
        if(response.statusCode() == 200){
            try {
                InputStream inputStream = response.body();
                FileOutputStream bookOutput = new FileOutputStream(item.name);

                inputStream.transferTo(bookOutput);
                System.out.println(item.name + "descargado!");
            } catch (Exception e){
                e.printStackTrace();
                return;
            }
        } else {
            System.out.println("Error agarrando el libro: " + response.statusCode());
        }

    }

    public String getResponseJson(String url) throws IOException, InterruptedException  {
        
        HttpRequest request = HttpRequest.newBuilder()
            .GET()
            .uri(URI.create(url))
            .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        return response.body();
    }

    public String translate(String parr) throws IOException, InterruptedException  {
        String WORK_ADD = TRANSLATE_URL + "&q=" + URLEncoder.encode(parr, StandardCharsets.UTF_8);
        String responseBody = getResponseJson(WORK_ADD);

        JsonObject responseJson = JsonParser.parseString(responseBody).getAsJsonObject();
        JsonObject data = responseJson.getAsJsonObject("data");
        JsonArray translations = data.getAsJsonArray("translations");
        String translatedText =translations.get(0).getAsJsonObject().get("translatedText").getAsString();
        return translatedText;

    }
}

class BucketListResponse {
    List<BucketItem> items;
}

class BucketItem{
    String name;
    String mediaLink;
}
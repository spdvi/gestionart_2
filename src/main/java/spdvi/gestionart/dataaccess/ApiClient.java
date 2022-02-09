package spdvi.gestionart.dataaccess;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import spdvi.gestionart.Models.Municipi;
import spdvi.gestionart.Models.Usuari;

/**
 *
 * @author DevMike
 */
public class ApiClient {

    public String authenticate(Usuari user) throws IOException, InterruptedException, Exception {
//    {
//    "username":"jj@fmail.com",
//    "password":"12345"
//    }  
        String token = null;

        String body = "{\"username\":\"" + user.getEmail() + "\",\"password\":\""
                + user.getPassword() + "\"}";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/authenticate"))
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .header("Content-Type", "application/json")
                .build();
        HttpClient client = HttpClient.newHttpClient();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        
        String responseBody = response.body();
        int responseCode = response.statusCode();
        
        if (responseCode == 200) {
            // Convert responseBody to ArrayList<Municipi> - gson
            Gson gson = new Gson();
            JsonObject jsonObject = gson.fromJson(responseBody, JsonObject.class);
            token = jsonObject.get("token").getAsString();
        }
        else {
            throw new Exception("Response code: " + responseCode);
        }
        
        return token;
    }
    
    
    public ArrayList<Municipi> getMunicipis(String token) throws IOException, InterruptedException, Exception {
        ArrayList<Municipi> municipis = new ArrayList<>();
        
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/municipis"))
                .GET()
                .header("Authorization", "Bearer " + token)
                .build();
        
        HttpClient client = HttpClient.newHttpClient();       
        
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        
        String responseBody = response.body();
        int responseCode = response.statusCode();
        
        if (responseCode == 200) {
            // Convert responseBody to ArrayList<Municipi> - gson
            Gson gson = new Gson();
            JsonReader reader = new JsonReader(new StringReader(responseBody));
            municipis = gson.fromJson(reader, new TypeToken<ArrayList<Municipi>>() {}.getType());
        }
        else {
            throw new Exception("" + responseCode);
        }
        
        return municipis;
    }
    
    public Usuari createUser(Usuari user) throws URISyntaxException, IOException, InterruptedException {
        
        Gson gson = new Gson();
        String body = gson.toJson(user);
        
        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI("http://localhost:8080/register"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        
        HttpClient client = HttpClient.newHttpClient();
        
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        
        Usuari returnUser = null;
        if (response.statusCode() == 201) {
            String responseBody = response.body();
            JsonReader reader = new JsonReader(new StringReader(responseBody));
            returnUser = gson.fromJson(reader, Usuari.class);
        }
        
        return returnUser;
    }
    
}

package org.example.service;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class AiSuggestionService {
    // Păstrăm modelul stabil configurat
    private static final String API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.8-flash:generateContent";
    private final HttpClient httpClient;

    public AiSuggestionService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public String suggestCategory(String title, String artistName) throws Exception {
        String prompt = "Ești un expert în istoria artei. Baza ta de date necesită o categorie pentru o operă de artă. " +
                "Titlu: " + title + ", Artist: " + artistName + ". " +
                "Răspunde DOAR cu numele categoriei (ex: Pictură, Sculptură, Artă Digitală, Instalație). Fără punctuație sau alte explicații.";

        return callGemini(prompt).trim();
    }

    public String generateArtPieceDescription(String title, String artistName) throws Exception {
        String prompt = "Ești un expert în istoria artei, scrii pentru un muzeu modern. " +
                "Scrie o scurtă descriere captivantă (maxim 10 fraze) despre opera '" + title + "' creată de " + artistName + ". " +
                "Fără introduceri, răspunde doar cu descrierea.";

        return callGemini(prompt).trim();
    }

    public String suggestNationality(String artistName) throws Exception {
        String prompt = "Care este naționalitatea artistului/autorului \"" + artistName + "\"? Răspunde strict doar cu naționalitatea (de exemplu: Română, Franceză, Engleză), fără alte cuvinte.";
        return callGemini(prompt).trim();
    }

    public String suggestBiography(String artistName) throws Exception {
        String prompt = "Scrie o biografie scurtă de maximum 3-4 propoziții despre artistul/autorul \"" + artistName + "\".";
        return callGemini(prompt).trim();
    }

    private String callGemini(String promptText) throws Exception {
        String apiKey = System.getenv("GEMINI_API_KEY");
        if (apiKey == null || apiKey.trim().isEmpty()) {
            apiKey = System.getenv("OPENAI_API_KEY");
        }

        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new IllegalStateException("API Key-ul lipsesc! Setează variabila de mediu în IntelliJ.");
        }

        JSONObject part = new JSONObject();
        part.put("text", promptText);

        JSONArray parts = new JSONArray();
        parts.put(part);

        JSONObject content = new JSONObject();
        content.put("parts", parts);

        JSONArray contents = new JSONArray();
        contents.put(content);

        JSONObject requestBody = new JSONObject();
        requestBody.put("contents", contents);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL + "?key=" + apiKey))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Eroare de la serverul Gemini (Cod " + response.statusCode() + "): " + response.body());
        }

        JSONObject jsonResponse = new JSONObject(response.body());

        return jsonResponse.getJSONArray("candidates")
                .getJSONObject(0)
                .getJSONObject("content")
                .getJSONArray("parts")
                .getJSONObject(0)
                .getString("text");
    }
}
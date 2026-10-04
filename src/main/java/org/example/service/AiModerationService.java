package org.example.service;

import org.example.dao.ArtPieceDAO;
import org.example.dao.ArtistDAO;
import org.example.dao.CommentDAO;
import org.example.dao.ReviewDAO;
import org.example.model.ArtPiece;
import org.example.model.Artist;
import org.example.model.Comment;
import org.example.model.Review;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

public class AiModerationService implements ContentModerator {
    private final ReviewDAO reviewDao;
    private final CommentDAO commentDao;
    private final ArtPieceDAO artPieceDao;
    private final ArtistDAO artistDao;

    private static final String API_URL = "https://api.openai.com/v1/chat/completions";
    private static final String API_KEY = System.getenv("OPENAI_API_KEY");
    private final HttpClient httpClient;

    public AiModerationService(ReviewDAO reviewDao, CommentDAO commentDao, ArtPieceDAO artPieceDao, ArtistDAO artistDao) {
        this.reviewDao = reviewDao;
        this.commentDao = commentDao;
        this.artPieceDao = artPieceDao;
        this.artistDao = artistDao;
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build();
    }

    @Override
    public void moderateReview(Review review) {
        CompletableFuture.runAsync(() -> {
            try {
                String original = review.getContent();

                String systemPrompt = "Ești un moderator de conținut. Sarcina ta este să elimini discursul instigator la ură și limbajul extrem de ofensator din textul utilizatorului. Păstrează sensul original și limba. Dacă textul este deja curat, returnează-l exact cum a fost primit, fără alte comentarii sau ghilimele suplimentare.";

                String cleaned = callOpenAi(systemPrompt, original);

                if (!original.equals(cleaned) && !cleaned.isEmpty()) {
                    review.setContent(cleaned);
                    reviewDao.update(review);
                }
            } catch (Exception e) {
                System.err.println("Eroare Moderare Review: " + e.getMessage());
            }
        });
    }

    @Override
    public void moderateComment(Comment comment) {
        CompletableFuture.runAsync(() -> {
            try {
                String original = comment.getContent();

                String systemPrompt = "Ești un moderator de conținut. Sarcina ta este să elimini jignirile, sa faci textul politicaly corect. Păstrează ideea originală. Dacă e deja OK, returnează textul original fără alte adăugiri.";

                String cleaned = callOpenAi(systemPrompt, original);

                if (!original.equals(cleaned) && !cleaned.isEmpty()) {
                    comment.setContent(cleaned);
                    commentDao.update(comment);
                }
            } catch (Exception e) {
                System.err.println("Eroare Moderare Comentariu: " + e.getMessage());
            }
        });
    }

    @Override
    public void moderateArtPiece(ArtPiece artPiece) {
        CompletableFuture.runAsync(() -> {
            try {
                String originalDesc = artPiece.getDescription();

                String systemPrompt = "Ești un expert în istoria artei. Analizează descrierea primită pentru opera '" + artPiece.getTitle() + "' de '" + artPiece.getArtist().getName() + "'. Corectează orice eroare factuală majoră (anul greșit, context fals, inepții). Dacă este corectă factual, doar returnează descrierea inițială. Nu scrie introduceri de genul 'Iată textul corectat'.";

                String validatedDesc = callOpenAi(systemPrompt, originalDesc);

                if (!originalDesc.equals(validatedDesc) && !validatedDesc.isEmpty()) {
                    artPiece.setDescription(validatedDesc);
                    artPieceDao.update(artPiece);
                }
            } catch (Exception e) {
                System.err.println("Eroare Fact-Check Opera: " + e.getMessage());
            }
        });
    }

    @Override
    public void moderateArtist(Artist artist) {
        CompletableFuture.runAsync(() -> {
            try {
                String originalBio = artist.getBiography();

                String systemPrompt = "Ești un istoric de artă. Verifică biografia primită pentru artistul '" + artist.getName() + "'. Elimină invențiile sau neadevărurile evidente. Dacă este corectă, returnează textul original. Răspunde direct cu textul.";

                String validatedBio = callOpenAi(systemPrompt, originalBio);

                if (!originalBio.equals(validatedBio) && !validatedBio.isEmpty()) {
                    artist.setBiography(validatedBio);
                    artistDao.update(artist);
                }
            } catch (Exception e) {
                System.err.println("Eroare Fact-Check Artist: " + e.getMessage());
            }
        });
    }

    private String callOpenAi(String systemPrompt, String userText) throws Exception {
        if (API_KEY == null || API_KEY.trim().isEmpty()) {
            throw new IllegalStateException("API Key-ul lipsește! Setează OPENAI_API_KEY.");
        }

        JSONObject systemMessage = new JSONObject();
        systemMessage.put("role", "system");
        systemMessage.put("content", systemPrompt);

        JSONObject userMessage = new JSONObject();
        userMessage.put("role", "user");
        userMessage.put("content", userText);

        JSONArray messages = new JSONArray();
        messages.put(systemMessage);
        messages.put(userMessage);

        JSONObject requestBody = new JSONObject();
        requestBody.put("model", "gpt-3.5-turbo");
        requestBody.put("messages", messages);
        requestBody.put("temperature", 0.3);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + API_KEY)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Eroare server AI: " + response.body());
        }

        JSONObject jsonResponse = new JSONObject(response.body());
        String result = jsonResponse.getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content");

        return result.trim();
    }
}
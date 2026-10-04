package org.example;

import io.javalin.Javalin;
import org.example.connection.DatabaseConnection;
import org.example.dao.*;
import org.example.model.ArtPiece;
import org.example.model.Comment;
import org.example.model.Review;
import org.example.model.User;
import org.example.service.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Main {
    public static void main(String[] args) {
        DatabaseConnection.getSessionFactory();

        UserDAO userDAO = new UserDAO();
        EmailService emailService = new EmailService();
        UserService userService = new UserService(userDAO, emailService);

        ArtPieceDAO artPieceDAO = new ArtPieceDAO();
        ArtistDAO artistDAO = new ArtistDAO();
        CategoryDAO categoryDAO = new CategoryDAO();
        ArtPieceService artPieceService = new ArtPieceService(artPieceDAO, artistDAO, categoryDAO);

        ReviewDAO reviewDAO = new ReviewDAO();
        ReviewService reviewService = new ReviewService(reviewDAO);

        CommentDAO commentDAO = new CommentDAO();
        CommentService commentService = new CommentService(commentDAO);

        AiSuggestionService aiSuggestionService = new AiSuggestionService();
        AiModerationService aiModerationService = new AiModerationService(reviewDAO, commentDAO, artPieceDAO, artistDAO);

        int port = System.getenv("PORT") != null ? Integer.parseInt(System.getenv("PORT")) : 8080;

        Javalin app = Javalin.create(config -> {
            config.staticFiles.add("/public", io.javalin.http.staticfiles.Location.CLASSPATH);
        }).start(port);

        app.before(ctx -> {
            String path = ctx.path();
            boolean isPublic = path.equals("/") ||
                    path.equals("/login.html") ||
                    path.equals("/register.html") ||
                    path.equals("/reset-password.html") ||
                    path.equals("/api/login") ||
                    path.equals("/api/register") ||
                    path.equals("/api/forgot-password") ||
                    path.equals("/api/reset-password") ||
                    path.startsWith("/api/ai/");

            if (!isPublic && ctx.sessionAttribute("userId") == null) {
                if (path.startsWith("/api/")) {
                    ctx.status(401).result("Neautentificat");
                } else {
                    ctx.redirect("/login.html");
                }
            }
        });

        app.get("/", ctx -> ctx.redirect("/login.html"));

        app.post("/api/register", ctx -> {
            try {
                String username = ctx.formParam("username");
                String email = ctx.formParam("email");
                String password = ctx.formParam("password");
                userService.registerUser(username, email, password);
                ctx.status(201).result("Utilizator înregistrat cu succes!");
            } catch (InvalidDataException e) {
                ctx.status(400).result(e.getMessage());
            }
        });

        app.post("/api/login", ctx -> {
            try {
                String email = ctx.formParam("email");
                String password = ctx.formParam("password");
                User user = userService.loginUser(email, password);
                ctx.sessionAttribute("userId", user.getId());
                ctx.sessionAttribute("username", user.getUsername());
                ctx.status(200).result("Autentificare reușită!");
            } catch (InvalidDataException e) {
                ctx.status(401).result(e.getMessage());
            }
        });

        app.get("/api/art-pieces", ctx -> {
            ctx.json(artPieceDAO.findAll());
        });

        app.post("/api/art-pieces", ctx -> {
            try {
                Integer userId = ctx.sessionAttribute("userId");
                if (userId == null) {
                    ctx.status(401).result("Neautorizat!");
                    return;
                }

                User currentUser = userDAO.findById(userId);
                if (currentUser == null) {
                    ctx.status(401).result("Utilizatorul nu a fost găsit.");
                    return;
                }

                String title = ctx.formParam("title");
                String artistName = ctx.formParam("artist");
                String categoryName = ctx.formParam("category");
                String description = ctx.formParam("description");

                artPieceService.addNewArtPiece(title, artistName, description, categoryName, currentUser);
                ctx.status(201).result("Operă adăugată cu succes!");
            } catch (InvalidDataException e) {
                ctx.status(400).result(e.getMessage());
            }
        });

        app.post("/api/ai/generate-description", ctx -> {
            try {
                String title = ctx.formParam("title");
                String artist = ctx.formParam("artist");
                String description = aiSuggestionService.generateArtPieceDescription(title, artist);
                ctx.json(Map.of("description", description));
            } catch (Exception e) {
                e.printStackTrace();
                ctx.status(500).result("Eroare de la serverul Gemini: " + e.getMessage());
            }
        });

        app.post("/api/ai/suggest-category", ctx -> {
            try {
                String title = ctx.formParam("title");
                String artist = ctx.formParam("artist");
                String category = aiSuggestionService.suggestCategory(title, artist);
                ctx.json(Map.of("category", category));
            } catch (Exception e) {
                e.printStackTrace();
                ctx.status(500).result("Eroare de la serverul Gemini: " + e.getMessage());
            }
        });

        app.get("/api/current-user", ctx -> {
            Integer userId = ctx.sessionAttribute("userId");
            if (userId == null) {
                ctx.status(401).result("Neautentificat");
                return;
            }
            User user = userDAO.findById(userId);

            List<ArtPiece> userArtPieces = artPieceDAO.findAll().stream()
                    .filter(p -> p.getUser() != null && userId.equals(p.getUser().getId()))
                    .toList();

            List<Review> userReviews = reviewDAO.findByUserId(userId);

            Map<String, Object> data = new HashMap<>();
            data.put("username", user.getUsername() != null ? user.getUsername() : "Necunoscut");
            data.put("email", user.getEmail() != null ? user.getEmail() : "Fără email");
            data.put("artPieces", userArtPieces);
            data.put("reviews", userReviews);
            data.put("comments", List.of());

            ctx.json(data);
        });

        app.get("/api/art-pieces/{id}", ctx -> {
            try {
                int id = Integer.parseInt(ctx.pathParam("id"));
                ArtPiece artPiece = artPieceDAO.findById(id);
                if (artPiece != null) {
                    ctx.json(artPiece);
                } else {
                    ctx.status(404).result("Opera nu a fost găsită.");
                }
            } catch (Exception e) {
                ctx.status(400).result("ID invalid.");
            }
        });

        app.post("/api/art-pieces/{id}/reviews", ctx -> {
            try {
                Integer userId = ctx.sessionAttribute("userId");
                if (userId == null) {
                    ctx.status(401).result("Neautentificat");
                    return;
                }

                int artId = Integer.parseInt(ctx.pathParam("id"));
                String content = ctx.formParam("content");

                if (content == null || content.trim().isEmpty()) {
                    ctx.status(400).result("Recenzia nu poate fi goală.");
                    return;
                }

                User user = userDAO.findById(userId);
                ArtPiece artPiece = artPieceDAO.findById(artId);

                if (artPiece == null) {
                    ctx.status(404).result("Opera nu există.");
                    return;
                }

                Review newReview = reviewService.addReview(user, artPiece, content);
                aiModerationService.moderateReview(newReview);

                ctx.status(201).result("Recenzie adăugată cu succes!");
            } catch (Exception e) {
                ctx.status(400).result("Eroare la adăugarea recenziei: " + e.getMessage());
            }
        });

        app.get("/api/artists", ctx -> {
            ctx.json(artistDAO.findAll());
        });

        app.post("/api/ai/artist-details", ctx -> {
            try {
                String artistName = ctx.formParam("artistName");
                if (artistName == null || artistName.trim().isEmpty()) {
                    ctx.status(400).result("Numele artistului lipsește.");
                    return;
                }

                String nationality = aiSuggestionService.suggestNationality(artistName);
                String biography = aiSuggestionService.suggestBiography(artistName);

                Map<String, String> details = new HashMap<>();
                details.put("nationality", nationality);
                details.put("biography", biography);

                ctx.json(details);
            } catch (Exception e) {
                ctx.status(500).result("Eroare la generarea detaliilor prin AI: " + e.getMessage());
            }
        });

        app.get("/api/art-pieces/{id}/reviews", ctx -> {
            try {
                int artId = Integer.parseInt(ctx.pathParam("id"));
                ArtPiece artPiece = artPieceDAO.findById(artId);
                if (artPiece == null) {
                    ctx.status(404).result("Opera nu a fost găsită.");
                    return;
                }
                List<Review> reviews = reviewDAO.getReviewsForArtPiece(artPiece);
                ctx.json(reviews);
            } catch (Exception e) {
                e.printStackTrace();
                ctx.status(400).result("Eroare la preluarea recenziilor: " + e.getMessage());
            }
        });

        app.delete("/api/art-pieces/{id}", ctx -> {
            try {
                Integer userId = ctx.sessionAttribute("userId");
                if (userId == null) {
                    ctx.status(401).result("Neautentificat");
                    return;
                }

                int artId = Integer.parseInt(ctx.pathParam("id"));
                User currentUser = userDAO.findById(userId);
                ArtPiece artPiece = artPieceDAO.findById(artId);

                if (artPiece == null) {
                    ctx.status(404).result("Opera nu a fost găsită.");
                    return;
                }

                if (artPiece.getUser() != null && !userId.equals(artPiece.getUser().getId())) {
                    ctx.status(403).result("Nu ai permisiunea să ștergi această operă!");
                    return;
                }

                artPieceService.deleteArtPiece(artId, currentUser);
                ctx.status(200).result("Opera a fost ștersă cu succes!");
            } catch (InvalidDataException e) {
                ctx.status(400).result(e.getMessage());
            } catch (Exception e) {
                ctx.status(500).result("Eroare la ștergerea operei: " + e.getMessage());
            }
        });

        app.delete("/api/account", ctx -> {
            try {
                Integer userId = ctx.sessionAttribute("userId");
                if (userId == null) {
                    ctx.status(401).result("Neautentificat");
                    return;
                }
                String password = ctx.formParam("password");
                User user = userDAO.findById(userId);

                userService.deleteAccount(user, password);
                ctx.req().getSession().invalidate();
                ctx.status(200).result("Contul a fost șters cu succes!");
            } catch (InvalidDataException e) {
                ctx.status(400).result(e.getMessage());
            } catch (Exception e) {
                ctx.status(500).result("Eroare la ștergerea contului: " + e.getMessage());
            }
        });

        app.post("/api/forgot-password", ctx -> {
            try {
                String email = ctx.formParam("email");
                userService.initiatePasswordReset(email);
                ctx.status(200).result("Email-ul de resetare a fost trimis!");
            } catch (InvalidDataException e) {
                ctx.status(400).result(e.getMessage());
            } catch (Exception e) {
                ctx.status(500).result("Eroare la trimiterea email-ului: " + e.getMessage());
            }
        });

        app.post("/api/reset-password", ctx -> {
            try {
                String token = ctx.formParam("token");
                String newPassword = ctx.formParam("password");
                userService.resetPassword(token, newPassword);
                ctx.status(200).result("Parola a fost resetată cu succes!");
            } catch (InvalidDataException e) {
                ctx.status(400).result(e.getMessage());
            } catch (Exception e) {
                ctx.status(500).result("Eroare la resetarea parolei: " + e.getMessage());
            }
        });

        app.get("/api/reviews/{id}/comments", ctx -> {
            try {
                int reviewId = Integer.parseInt(ctx.pathParam("id"));
                Review review = reviewDAO.findById(reviewId);
                if (review == null) {
                    ctx.status(404).result("Recenzia nu există.");
                    return;
                }
                ctx.json(commentDAO.getCommentsForReview(review));
            } catch (Exception e) {
                ctx.status(400).result("Eroare la preluarea comentariilor.");
            }
        });

        app.post("/api/reviews/{id}/comments", ctx -> {
            try {
                Integer userId = ctx.sessionAttribute("userId");
                if (userId == null) {
                    ctx.status(401).result("Neautentificat");
                    return;
                }

                int reviewId = Integer.parseInt(ctx.pathParam("id"));
                String content = ctx.formParam("content");

                User user = userDAO.findById(userId);
                Review review = reviewDAO.findById(reviewId);

                if (review == null) {
                    ctx.status(404).result("Recenzia nu există.");
                    return;
                }

                Comment newComment = commentService.addComent(user, review, content);
                aiModerationService.moderateComment(newComment);

                ctx.status(201).result("Comentariu adăugat cu succes!");
            } catch (InvalidDataException e) {
                ctx.status(400).result(e.getMessage());
            } catch (Exception e) {
                ctx.status(500).result("Eroare: " + e.getMessage());
            }
        });

        app.post("/api/reviews/{id}/report", ctx -> {
            try {
                int reviewId = Integer.parseInt(ctx.pathParam("id"));
                reviewService.reportReview(reviewId);
                ctx.status(200).result("Recenzia a fost raportată.");
            } catch (Exception e) {
                ctx.status(500).result("Eroare la raportarea recenziei.");
            }
        });

        app.post("/api/comments/{id}/report", ctx -> {
            try {
                int commentId = Integer.parseInt(ctx.pathParam("id"));
                commentService.reportComment(commentId);
                ctx.status(200).result("Comentariul a fost raportat.");
            } catch (Exception e) {
                ctx.status(500).result("Eroare la raportarea comentariului.");
            }
        });
    }
}
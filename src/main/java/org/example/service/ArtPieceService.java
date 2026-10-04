package org.example.service;

import org.example.dao.ArtPieceDAO;
import org.example.dao.ArtistDAO;
import org.example.dao.CategoryDAO;
import org.example.model.ArtPiece;
import org.example.model.Artist;
import org.example.model.Category;
import org.example.model.User;

public class ArtPieceService {
    private final ArtPieceDAO artPieceDao;
    private final ArtistDAO artistDao;
    private final CategoryDAO categoryDao;
    private final AiSuggestionService aiSuggestionService;

    public ArtPieceService(ArtPieceDAO artPieceDao, ArtistDAO artistDao, CategoryDAO categoryDao) {
        this.artPieceDao = artPieceDao;
        this.artistDao = artistDao;
        this.categoryDao = categoryDao;
        this.aiSuggestionService = new AiSuggestionService();
    }

    public ArtPiece addNewArtPiece(String title, String artistName, String description, String categoryName, User creator) throws InvalidDataException {
        if (title == null || title.trim().isEmpty() || title.length() > 300 ||
                artistName == null || artistName.trim().isEmpty() || artistName.length() > 150 ||
                description == null || description.trim().isEmpty() ||
                categoryName == null || categoryName.trim().isEmpty() || categoryName.length() > 100) {
            throw new InvalidDataException("Datele introduse nu sunt valide sau depășesc limitele admise!");
        }

        String nationality = null;
        String biography = null;

        try {
            nationality = aiSuggestionService.suggestNationality(artistName.trim());
            biography = aiSuggestionService.suggestBiography(artistName.trim());
        } catch (Exception e) {
            System.err.println("Avertisment AI: " + e.getMessage());
        }

        Artist artist = artistDao.findByNameOrCreate(artistName.trim(), nationality, biography);
        Category category = categoryDao.findByNameOrCreate(categoryName.trim());

        ArtPiece newArtPiece = new ArtPiece(title.trim(), artist, description.trim(), category, creator);
        artPieceDao.save(newArtPiece);
        return newArtPiece;
    }

    public void deleteArtPiece(int artPieceId, User currentUser) throws InvalidDataException {
        ArtPiece artPiece = artPieceDao.findById(artPieceId);
        if (artPiece == null) {
            throw new InvalidDataException("Opera de artă căutată nu există!");
        }
        if (artPiece.getUser() != null && artPiece.getUser().getId() != currentUser.getId()) {
           throw new InvalidDataException("Nu ai permisiunea să ștergi această operă!");
        }

        artPieceDao.delete(artPiece);
    }
}
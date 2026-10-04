package org.example.service;

import org.example.model.ArtPiece;
import org.example.model.Artist;
import org.example.model.Comment;
import org.example.model.Review;

public interface ContentModerator {

    public void moderateReview(Review review);
    public void moderateComment(Comment comment);
    public void moderateArtPiece(ArtPiece artPiece);
    public void moderateArtist(Artist artist);

}

package org.example.service;

import org.example.dao.ReviewDAO;
import org.example.model.ArtPiece;
import org.example.model.Review;
import org.example.model.User;

public class ReviewService {
    private final ReviewDAO reviewDao;

    public ReviewService(ReviewDAO reviewDao) {
        this.reviewDao = reviewDao;
    }

    public Review addReview(User user, ArtPiece artPiece, String content) throws InvalidDataException{
          if(content == null || content.trim().isEmpty()){
              throw new InvalidDataException("Datele introduse nu sunt valide!");
          }

          Review newReview = new Review(user, artPiece, content);
          reviewDao.save(newReview);

          return newReview;
    }

    public void deleteReview(int reviewId) throws InvalidDataException {
        Review review = reviewDao.findById(reviewId);
        if (review == null) {
            throw new InvalidDataException("Review-ul căutat nu există!");
        }
        reviewDao.delete(review);
    }

    public void reportReview(int reviewId) {
        Review review = reviewDao.findById(reviewId);
        if (review != null) {
            review.setReportCount(review.getReportCount() + 1);
            if (review.getReportCount() >= 5) {
                reviewDao.delete(review);
            } else {
                reviewDao.update(review);
            }
        }
    }
}

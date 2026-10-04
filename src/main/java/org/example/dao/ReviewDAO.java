package org.example.dao;

import org.example.connection.DatabaseConnection;
import org.example.model.Review;
import org.hibernate.Session;
import org.hibernate.Transaction;

public class ReviewDAO extends AbstractDAO<Review> implements DeleteableDAO<Review>{

    public ReviewDAO()
    {
        super(Review.class);
    }


    @Override
    public void delete(Review obj) {
        try (Session session = DatabaseConnection.getSessionFactory().openSession()) {
            Transaction transaction = session.beginTransaction();
            session.createQuery("DELETE FROM Comment c WHERE c.review = :rev")
                    .setParameter("rev", obj)
                    .executeUpdate();
            session.remove(obj);
            transaction.commit();
        } catch (Exception e) {
            System.err.println("Eroare la ștergerea review-ului: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public java.util.List<Review> getReviewsForArtPiece(org.example.model.ArtPiece artPiece) {
        try (Session session = DatabaseConnection.getSessionFactory().openSession()) {
            return session.createQuery(
                            "FROM Review r WHERE r.artPiece.id = :artId AND r.isHidden = false ORDER BY r.date DESC", Review.class)
                    .setParameter("artId", artPiece.getId())
                    .getResultList();
        } catch (Exception e) {
            e.printStackTrace();
            return java.util.Collections.emptyList();
        }
    }

    public java.util.List<Review> findByUserId(int userId) {
        try (Session session = DatabaseConnection.getSessionFactory().openSession()) {
            return session.createQuery("FROM Review r WHERE r.user.id = :userId", Review.class)
                    .setParameter("userId", userId)
                    .getResultList();
        } catch (Exception e) {
            System.err.println("Eroare la căutarea recenziilor după userId: " + e.getMessage());
            return java.util.Collections.emptyList();
        }
    }
}

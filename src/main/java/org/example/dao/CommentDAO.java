package org.example.dao;

import org.example.connection.DatabaseConnection;
import org.example.model.Comment;
import org.hibernate.Session;
import org.hibernate.Transaction;

public class CommentDAO extends AbstractDAO<Comment> implements DeleteableDAO<Comment>{

    public CommentDAO()
    {
        super(Comment.class);
    }


    @Override
    public void delete(Comment obj) {
        try (Session session = DatabaseConnection.getSessionFactory().openSession()) {
            Transaction transaction = session.beginTransaction();
            session.remove(obj);
            transaction.commit();
        } catch (Exception e) {
            System.err.println("Eroare la ștergerea comentariului: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public java.util.List<Comment> getCommentsForReview(org.example.model.Review review) {
        try (Session session = DatabaseConnection.getSessionFactory().openSession()) {
            return session.createQuery(
                            "FROM Comment c WHERE c.review = :rev ORDER BY c.date ASC", Comment.class)
                    .setParameter("rev", review)
                    .getResultList();
        } catch (Exception e) {
            e.printStackTrace();
            return java.util.Collections.emptyList();
        }
    }

    public java.util.List<Comment> findByUserId(int userId) {
        try (Session session = DatabaseConnection.getSessionFactory().openSession()) {
            return session.createQuery("FROM Comment c WHERE c.user.id = :userId", Comment.class)
                    .setParameter("userId", userId)
                    .getResultList();
        } catch (Exception e) {
            System.err.println("Eroare la căutarea comentariilor după userId: " + e.getMessage());
            return java.util.Collections.emptyList();
        }
    }
}

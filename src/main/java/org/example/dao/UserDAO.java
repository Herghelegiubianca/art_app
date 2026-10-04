package org.example.dao;

import org.example.connection.DatabaseConnection;
import org.example.model.User;
import org.hibernate.Session;
import org.hibernate.Transaction;

public class UserDAO extends AbstractDAO<User> implements DeleteableDAO<User>{

    public UserDAO()
    {
        super(User.class);
    }


    @Override
    public void delete(User obj) {
        try (Session session = DatabaseConnection.getSessionFactory().openSession()) {
            Transaction transaction = session.beginTransaction();
            session.remove(obj);
            transaction.commit();
        } catch (Exception e) {
            System.err.println("Eroare la ștergerea utilizatorului: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public User findByEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return null;
        }

        String cleanedEmail = email.trim().toLowerCase();

        try (Session session = DatabaseConnection.getSessionFactory().openSession()) {
            return session.createQuery("FROM User WHERE LOWER(email) = :adresaEmail", User.class)
                    .setParameter("adresaEmail", cleanedEmail)
                    .uniqueResult();
        } catch (Exception e) {
            System.err.println("Eroare la căutarea după email: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    public User findByResetToken(String token) {
        try (Session session = DatabaseConnection.getSessionFactory().openSession()) {
            return session.createQuery("FROM User WHERE resetToken = :tokenCăutat", User.class)
                    .setParameter("tokenCăutat", token)
                    .uniqueResult();
        } catch (Exception e) {
            System.err.println("Eroare la căutarea după token-ul de resetare: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
}

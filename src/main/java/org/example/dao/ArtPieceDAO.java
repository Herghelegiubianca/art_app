package org.example.dao;

import org.example.connection.DatabaseConnection;
import org.example.model.ArtPiece;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.List;

public class ArtPieceDAO extends AbstractDAO<ArtPiece> implements DeleteableDAO<ArtPiece>{

    public ArtPieceDAO()
    {
        super(ArtPiece.class);
    }


    @Override
    public void delete(ArtPiece obj) {
        try (Session session = DatabaseConnection.getSessionFactory().openSession()) {
            Transaction transaction = session.beginTransaction();
            session.remove(obj);
            transaction.commit();
        } catch (Exception e) {
            System.err.println("Eroare la ștergerea piesei de arta: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public List<ArtPiece> findAll() {
        try (Session session = DatabaseConnection.getSessionFactory().openSession()) {
            return session.createQuery("FROM ArtPiece", ArtPiece.class).list();
        }
    }


}

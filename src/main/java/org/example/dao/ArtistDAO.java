package org.example.dao;

import org.example.connection.DatabaseConnection;
import org.example.model.Artist;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.List;

public class ArtistDAO extends AbstractDAO<Artist> {

    public ArtistDAO() {
        super(Artist.class);
    }

    public Artist findByNameOrCreate(String name, String nationality, String biography) {
        Transaction transaction = null;
        try (Session session = DatabaseConnection.getSessionFactory().openSession()) {
            Artist artist = session.createQuery("FROM Artist a WHERE LOWER(a.name) = LOWER(:name)", Artist.class)
                    .setParameter("name", name)
                    .uniqueResult();

            if (artist == null) {
                artist = new Artist(name, nationality, biography);
                transaction = session.beginTransaction();
                session.persist(artist);
                transaction.commit();
            } else {

                if ((artist.getNationality() == null || artist.getNationality().isEmpty()) && nationality != null) {
                    transaction = session.beginTransaction();
                    artist.setNationality(nationality);
                    if (biography != null) artist.setBiography(biography);
                    session.merge(artist);
                    transaction.commit();
                }
            }
            return artist;
        } catch (Exception e) {
            if (transaction != null && transaction.isActive()) {
                transaction.rollback();
            }
            System.err.println("Eroare la findByNameOrCreate pentru artist: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }


    public Artist findByNameOrCreate(String name) {
        return findByNameOrCreate(name, null, null);
    }

    public List<Artist> findAll() {
        try (Session session = DatabaseConnection.getSessionFactory().openSession()) {
            return session.createQuery("FROM Artist", Artist.class).list();
        } catch (Exception e) {
            System.err.println("Eroare la preluarea listei de artiști: " + e.getMessage());
            return java.util.Collections.emptyList();
        }
    }
}
package org.example.dao;

import org.example.connection.DatabaseConnection;
import org.hibernate.Session;
import org.hibernate.Transaction;

public class AbstractDAO<T> implements ReadableDAO<T>, SaveableDAO<T>, UpdatableDAO<T>{
    private final Class<T> entityClass;

    public AbstractDAO(Class<T> entityClass) {
        this.entityClass = entityClass;
    }

    @Override
    public void save(T entity) {
        try (Session session = DatabaseConnection.getSessionFactory().openSession()) {
            Transaction transaction = session.beginTransaction();
            session.persist(entity);
            transaction.commit();
        } catch (Exception e) {
            System.err.println("Eroare la salvarea entității: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public T findById(int id) {
        try (Session session = DatabaseConnection.getSessionFactory().openSession()) {
            return session.get(entityClass, id);
        } catch (Exception e) {
            System.err.println("Eroare la găsirea entității: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public void update(T entity) {
        try (Session session = DatabaseConnection.getSessionFactory().openSession()) {
            Transaction transaction = session.beginTransaction();
            session.merge(entity);
            transaction.commit();
        } catch (Exception e) {
            System.err.println("Eroare la actualizarea entității: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public java.util.List<T> getAll() {
        try (Session session = DatabaseConnection.getSessionFactory().openSession()) {
            return session.createQuery("FROM " + entityClass.getSimpleName(), entityClass).getResultList();
        } catch (Exception e) {
            System.err.println("Eroare la extragerea tuturor entităților: " + e.getMessage());
            e.printStackTrace();
            return java.util.Collections.emptyList();
        }
    }

}

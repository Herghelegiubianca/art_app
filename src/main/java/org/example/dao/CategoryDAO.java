package org.example.dao;

import org.example.connection.DatabaseConnection;
import org.example.model.Category;
import org.hibernate.Session;

public class CategoryDAO extends AbstractDAO<Category>{

    public CategoryDAO()
    {
        super(Category.class);
    }

    public Category findByName(String name) {
        try (Session session = DatabaseConnection.getSessionFactory().openSession()) {
            return session.createQuery("FROM Category WHERE lower(name) = :nume", Category.class)
                    .setParameter("nume", name.toLowerCase().trim())
                    .uniqueResult();
        } catch (Exception e) {
            System.err.println("Eroare la căutarea categoriei: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    public Category findByNameOrCreate(String name) {
        String cleanName = name.trim();

        Category existingCategory = findByName(cleanName);

        if (existingCategory != null) {
            return existingCategory;
        }

        Category newCategory = new Category(cleanName);
        this.save(newCategory);

        return newCategory;
    }
}

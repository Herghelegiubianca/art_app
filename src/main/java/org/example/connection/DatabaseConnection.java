package org.example.connection;

import org.example.model.Artist;
import org.example.model.ArtPiece;
import org.example.model.Category;
import org.example.model.Comment;
import org.example.model.Review;
import org.example.model.User;
import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;

import java.util.Properties;

public class DatabaseConnection {
    private static SessionFactory sessionFactory;

    public static SessionFactory getSessionFactory() {
        if (sessionFactory == null) {
            try {
                Configuration configuration = new Configuration();
                Properties settings = new Properties();
                settings.put(Environment.URL, System.getenv("DB_URL"));
                settings.put(Environment.USER, System.getenv("DB_USER"));
                settings.put(Environment.PASS, System.getenv("DB_PASSWORD"));
                settings.put(Environment.DRIVER, "org.postgresql.Driver");
                settings.put(Environment.DIALECT, "org.hibernate.dialect.PostgreSQLDialect");
                settings.put(Environment.SHOW_SQL, "true");

                settings.put(Environment.HBM2DDL_AUTO, "update");

                configuration.setProperties(settings);

                configuration.addAnnotatedClass(User.class);
                configuration.addAnnotatedClass(Artist.class);
                configuration.addAnnotatedClass(ArtPiece.class);
                configuration.addAnnotatedClass(Category.class);
                configuration.addAnnotatedClass(Review.class);
                configuration.addAnnotatedClass(Comment.class);

                org.hibernate.service.ServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder()
                        .applySettings(configuration.getProperties()).build();

                sessionFactory = configuration.buildSessionFactory(serviceRegistry);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return sessionFactory;
    }
}
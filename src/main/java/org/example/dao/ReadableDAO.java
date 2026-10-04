package org.example.dao;

public interface ReadableDAO<T> {
    public T findById(int id);
}

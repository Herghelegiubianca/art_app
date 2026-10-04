package org.example.dao;

public interface DeleteableDAO<T> {
    public void delete(T obj);
}

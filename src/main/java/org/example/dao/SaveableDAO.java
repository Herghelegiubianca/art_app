package org.example.dao;

public interface SaveableDAO<T>{
    public void save(T obj);
}

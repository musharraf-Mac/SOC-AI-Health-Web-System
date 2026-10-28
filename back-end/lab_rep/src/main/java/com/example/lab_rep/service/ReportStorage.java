package com.example.lab_rep.service;

public interface ReportStorage {
    void put(String path, byte[] content, String contentType);
    byte[] read(String path);
    void delete(String path);
}

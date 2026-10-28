package com.example.lab_rep.service;

import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class CloudReportStorage implements ReportStorage {
    private final String bucket;
    private volatile Storage client;

    public CloudReportStorage(@Value("${reports.storage.bucket:}") String bucket) {
        this.bucket = bucket;
    }

    private Storage client() {
        if (bucket.isBlank()) throw new IllegalStateException("GCS_BUCKET must be configured");
        if (client == null) {
            synchronized (this) {
                if (client == null) client = StorageOptions.getDefaultInstance().getService();
            }
        }
        return client;
    }

    public void put(String path, byte[] content, String contentType) {
        client().create(BlobInfo.newBuilder(bucket, path).setContentType(contentType).build(), content);
    }

    public byte[] read(String path) {
        return client().readAllBytes(BlobId.of(bucket, path));
    }

    public void delete(String path) {
        if (path != null) client().delete(BlobId.of(bucket, path));
    }
}

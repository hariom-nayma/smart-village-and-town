package com.smartvillage.common.storage;

import org.springframework.web.multipart.MultipartFile;

public interface StorageService {
    String store(MultipartFile file, String subDir);
    String store(byte[] content, String fileName, String contentType, String subDir);
    void delete(String relativePath);
    String resolveUrl(String relativePath);
}

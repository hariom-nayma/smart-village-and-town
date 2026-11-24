package com.smartvillage.common.storage;

import com.smartvillage.common.exception.BadRequestException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.UUID;

@Slf4j
@Service
public class LocalStorageService implements StorageService {

    private final Path rootDir;
    private final String baseUrl;

    public LocalStorageService(@Value("${app.storage.upload-dir:./uploads}") String uploadDir,
                               @Value("${app.storage.base-url:http://localhost:8080/uploads}") String baseUrl) {
        this.rootDir = Paths.get(uploadDir).toAbsolutePath().normalize();
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        try {
            Files.createDirectories(this.rootDir);
        } catch (IOException e) {
            log.error("Could not initialize local storage directory: {}", rootDir, e);
        }
    }

    @Override
    public String store(MultipartFile file, String subDir) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Cannot upload an empty file.");
        }
        try {
            String originalFilename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "upload.bin";
            String extension = "";
            int extIndex = originalFilename.lastIndexOf('.');
            if (extIndex > 0) {
                extension = originalFilename.substring(extIndex);
            }
            String newFilename = UUID.randomUUID() + extension;
            Path targetDir = subDir != null ? rootDir.resolve(subDir).normalize() : rootDir;
            Files.createDirectories(targetDir);
            Path targetFile = targetDir.resolve(newFilename);
            Files.write(targetFile, file.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            String relative = (subDir != null ? subDir + "/" : "") + newFilename;
            return relative.replace('\\', '/');
        } catch (IOException e) {
            log.error("Failed to store file: " + file.getOriginalFilename(), e);
            throw new BadRequestException("Could not store file: " + e.getMessage());
        }
    }

    @Override
    public String store(byte[] content, String fileName, String contentType, String subDir) {
        try {
            Path targetDir = subDir != null ? rootDir.resolve(subDir).normalize() : rootDir;
            Files.createDirectories(targetDir);
            Path targetFile = targetDir.resolve(fileName);
            Files.write(targetFile, content, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            String relative = (subDir != null ? subDir + "/" : "") + fileName;
            return relative.replace('\\', '/');
        } catch (IOException e) {
            log.error("Failed to store byte content as: " + fileName, e);
            throw new BadRequestException("Could not store content: " + e.getMessage());
        }
    }

    @Override
    public void delete(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) return;
        try {
            Path file = rootDir.resolve(relativePath).normalize();
            if (Files.exists(file) && file.startsWith(rootDir)) {
                Files.delete(file);
            }
        } catch (IOException e) {
            log.warn("Could not delete file at: {}", relativePath, e);
        }
    }

    @Override
    public String resolveUrl(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) return null;
        if (relativePath.startsWith("http://") || relativePath.startsWith("https://")) {
            return relativePath;
        }
        return baseUrl + "/" + (relativePath.startsWith("/") ? relativePath.substring(1) : relativePath);
    }
}

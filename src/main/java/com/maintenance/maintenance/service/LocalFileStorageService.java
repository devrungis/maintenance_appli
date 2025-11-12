package com.maintenance.maintenance.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class LocalFileStorageService {

    private final Path rootDir;

    public LocalFileStorageService(@Value("${media.storage.base-path:uploads}") String basePath) throws IOException {
        this.rootDir = Paths.get(basePath).toAbsolutePath().normalize();
        Files.createDirectories(this.rootDir);
    }

    public List<String> saveMachinePhotos(String entrepriseId, String machineId, MultipartFile[] files, int maxUploads) throws IOException {
        List<String> urls = new ArrayList<>();
        if (files == null || files.length == 0 || maxUploads <= 0) {
            return urls;
        }

        Path machineDir = rootDir.resolve(Paths.get("machines", entrepriseId, machineId)).normalize();
        Files.createDirectories(machineDir);

        for (MultipartFile file : files) {
            if (urls.size() >= maxUploads) {
                break;
            }
            if (file == null || file.isEmpty()) {
                continue;
            }

            String original = StringUtils.cleanPath(file.getOriginalFilename() != null ? file.getOriginalFilename() : "photo");
            String extension = "";
            int dotIndex = original.lastIndexOf('.');
            if (dotIndex >= 0) {
                extension = original.substring(dotIndex);
            }

            String filename = UUID.randomUUID() + extension;
            Path target = machineDir.resolve(filename).normalize();

            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
            }

            String relativePath = String.format("/media/machines/%s/%s/%s", entrepriseId, machineId, filename).replace('\\', '/');
            urls.add(relativePath);
        }

        return urls;
    }

    public void deletePhoto(String photoUrl) throws IOException {
        if (!StringUtils.hasText(photoUrl) || !photoUrl.startsWith("/media/")) {
            return;
        }

        String relativePart = photoUrl.substring("/media/".length());
        Path target = rootDir.resolve(relativePart).normalize();

        if (!target.startsWith(rootDir)) {
            throw new IOException("Chemin de fichier invalide: " + photoUrl);
        }

        Files.deleteIfExists(target);
    }
}

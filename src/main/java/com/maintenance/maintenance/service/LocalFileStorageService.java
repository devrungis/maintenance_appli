package com.maintenance.maintenance.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class LocalFileStorageService {

    private final Path rootDir;

    public LocalFileStorageService(@Value("${media.storage.base-path:uploads}") String basePath) throws IOException {
        Path configuredPath = Paths.get(basePath).toAbsolutePath().normalize();
        this.rootDir = configuredPath;
        Files.createDirectories(this.rootDir);
        migrateLegacyUploadsIfNeeded();
    }

    private void migrateLegacyUploadsIfNeeded() throws IOException {
        Path legacyDir = Paths.get("uploads").toAbsolutePath().normalize();
        if (Files.notExists(legacyDir) || legacyDir.equals(rootDir)) {
            return;
        }
        Files.walkFileTree(legacyDir, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Path relative = legacyDir.relativize(file);
                Path target = rootDir.resolve(relative).normalize();
                Files.createDirectories(target.getParent());
                if (Files.notExists(target)) {
                    Files.copy(file, target, StandardCopyOption.COPY_ATTRIBUTES);
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }

    public List<String> saveMachinePhotos(String entrepriseId, String machineId, MultipartFile[] files, int maxUploads) throws IOException {
        List<String> urls = new ArrayList<>();
        if (files == null || files.length == 0 || maxUploads <= 0) {
            return urls;
        }

        // Créer le dossier de manière robuste (comme pour les commentaires)
        Path imageDir = rootDir.resolve(Paths.get("image", entrepriseId, machineId)).normalize();
        
        // Vérifier que le chemin est sécurisé
        if (!imageDir.startsWith(rootDir)) {
            throw new IOException("Chemin de fichier invalide pour les photos de machine");
        }
        
        Files.createDirectories(imageDir);
        System.out.println("=== Dossier créé pour les photos: " + imageDir.toString() + " ===");

        for (MultipartFile file : files) {
            if (urls.size() >= maxUploads) {
                break;
            }
            if (file == null || file.isEmpty()) {
                continue;
            }

            try {
                String original = StringUtils.cleanPath(file.getOriginalFilename() != null ? file.getOriginalFilename() : "photo");
                String extension = "";
                int dotIndex = original.lastIndexOf('.');
                if (dotIndex >= 0) {
                    extension = original.substring(dotIndex);
                }

                String filename = UUID.randomUUID() + extension;
                Path target = imageDir.resolve(filename).normalize();
                
                // Vérifier à nouveau la sécurité du chemin
                if (!target.startsWith(imageDir)) {
                    System.err.println("=== Chemin de fichier invalide, photo ignorée: " + filename + " ===");
                    continue;
                }

                try (InputStream inputStream = file.getInputStream()) {
                    Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
                    System.out.println("=== Photo sauvegardée: " + target.toString() + " ===");
                }

                String relativePath = String.format("/media/image/%s/%s/%s", entrepriseId, machineId, filename).replace('\\', '/');
                urls.add(relativePath);
            } catch (Exception e) {
                System.err.println("=== Erreur lors de la sauvegarde d'une photo: " + e.getMessage() + " ===");
                e.printStackTrace();
                // Continuer avec les autres photos même si une échoue
            }
        }

        System.out.println("=== Total de photos sauvegardées: " + urls.size() + " sur " + maxUploads + " ===");
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
    
    public String saveCommentImage(String entrepriseId, String ticketId, MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            return null;
        }

        Path imageDir = rootDir.resolve(Paths.get("image", entrepriseId, "tickets", ticketId, "comments")).normalize();
        Files.createDirectories(imageDir);

        String original = StringUtils.cleanPath(file.getOriginalFilename() != null ? file.getOriginalFilename() : "photo");
        String extension = "";
        int dotIndex = original.lastIndexOf('.');
        if (dotIndex >= 0) {
            extension = original.substring(dotIndex);
        }

        String filename = UUID.randomUUID() + extension;
        Path target = imageDir.resolve(filename).normalize();

        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
        }

        return String.format("/media/image/%s/tickets/%s/comments/%s", entrepriseId, ticketId, filename).replace('\\', '/');
    }
}

package com.maintenance.maintenance.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class FirebaseConfig {

    private final ResourceLoader resourceLoader;

    @Value("${firebase.credentials.path:}")
    private String serviceAccountPath;

    @Value("${firebase.database.url:}")
    private String databaseUrl;

    public FirebaseConfig(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @Bean
    public FirebaseApp firebaseApp() throws IOException {
        if (FirebaseApp.getApps().isEmpty()) {
            FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(resolveCredentials())
                .setDatabaseUrl(databaseUrl)
                .build();

            return FirebaseApp.initializeApp(options);
        }
        return FirebaseApp.getInstance();
    }

    private GoogleCredentials resolveCredentials() throws IOException {
        IOException lastError = null;

        if (StringUtils.hasText(serviceAccountPath)) {
            Path path = Paths.get(serviceAccountPath);
            if (Files.exists(path)) {
                try (InputStream serviceAccount = Files.newInputStream(path)) {
                    return GoogleCredentials.fromStream(serviceAccount);
                }
            }

            Resource resource = resourceLoader.getResource(serviceAccountPath);
            if (resource.exists()) {
                try (InputStream serviceAccount = resource.getInputStream()) {
                    return GoogleCredentials.fromStream(serviceAccount);
                }
            }
        }

        try {
            return GoogleCredentials.getApplicationDefault();
        } catch (IOException ex) {
            lastError = ex;
        }

        throw new IOException(
            "Firebase credentials non configurées. Définissez la propriété 'firebase.credentials.path' " +
            "ou la variable d'environnement GOOGLE_APPLICATION_CREDENTIALS.",
            lastError
        );
    }

    @Bean
    public FirebaseAuth firebaseAuth(FirebaseApp firebaseApp) {
        return FirebaseAuth.getInstance(firebaseApp);
    }

    @Bean
    public FirebaseDatabase firebaseDatabase(FirebaseApp firebaseApp) {
        FirebaseDatabase database = FirebaseDatabase.getInstance(firebaseApp);
        database.setPersistenceEnabled(false);
        return database;
    }

    @Bean
    public DatabaseReference databaseReference(FirebaseDatabase firebaseDatabase) {
        return firebaseDatabase.getReference();
    }
}


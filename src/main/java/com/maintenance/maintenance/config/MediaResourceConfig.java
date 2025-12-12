package com.maintenance.maintenance.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class MediaResourceConfig implements WebMvcConfigurer {

    private final String mediaBasePath;

    public MediaResourceConfig(@Value("${media.storage.base-path:uploads}") String basePath) {
        this.mediaBasePath = Paths.get(basePath).toAbsolutePath().normalize().toString();
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path primaryPath = Path.of(mediaBasePath).toAbsolutePath().normalize();
        Path legacyPath = Path.of("uploads").toAbsolutePath().normalize();

        String primaryLocation = primaryPath.toUri().toString();
        String legacyLocation = legacyPath.toUri().toString();

        registry.addResourceHandler("/media/**")
            .addResourceLocations(primaryLocation, legacyLocation)
            .setCachePeriod(3600);
    }
}

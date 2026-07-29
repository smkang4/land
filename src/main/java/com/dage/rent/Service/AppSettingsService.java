package com.dage.rent.Service;

import com.dage.rent.DTO.AppSettingsDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class AppSettingsService {

    private static final String SETTINGS_FILE_NAME = "app-settings.json";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Path settingsPath;
    private final Object lock = new Object();

    public AppSettingsService(@Value("${file.upload-dir:./upload}") String uploadDir) {
        this.settingsPath = Paths.get(uploadDir).toAbsolutePath().normalize().resolve(SETTINGS_FILE_NAME);
    }

    public boolean isMainNoticeModalEnabled() {
        return load().isMainNoticeModalEnabled();
    }

    public void setMainNoticeModalEnabled(boolean enabled) {
        synchronized (lock) {
            AppSettingsDTO settings = loadUnlocked();
            settings.setMainNoticeModalEnabled(enabled);
            saveUnlocked(settings);
        }
    }

    public AppSettingsDTO load() {
        synchronized (lock) {
            return loadUnlocked();
        }
    }

    private AppSettingsDTO loadUnlocked() {
        try {
            if (!Files.exists(settingsPath)) {
                return new AppSettingsDTO();
            }
            AppSettingsDTO settings = objectMapper.readValue(settingsPath.toFile(), AppSettingsDTO.class);
            return settings != null ? settings : new AppSettingsDTO();
        } catch (IOException e) {
            System.err.println("앱 설정 JSON 읽기 실패: " + settingsPath + " - " + e.getMessage());
            return new AppSettingsDTO();
        }
    }

    private void saveUnlocked(AppSettingsDTO settings) {
        try {
            Path parent = settingsPath.getParent();
            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);
            }
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(settingsPath.toFile(), settings);
        } catch (IOException e) {
            throw new IllegalStateException("앱 설정 JSON 저장 실패: " + settingsPath, e);
        }
    }
}

package com.example.stt.domain.service;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class FileStorageService {

    public void saveFile(MultipartFile file, String path) throws IOException {
        if (!file.isEmpty()) {
            String fileName = StringUtils.cleanPath(file.getOriginalFilename());
            Path destinationPath = Paths.get(path + File.separator + fileName);
            Files.write(destinationPath, file.getBytes());
        } else {
            throw new IOException("File is empty!");
        }
    }

    public byte[] readFile(String path) throws IOException {
        Path filePath = Paths.get(path);
        return Files.readAllBytes(filePath);
    }
}

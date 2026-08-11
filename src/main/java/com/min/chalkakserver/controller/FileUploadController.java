package com.min.chalkakserver.controller;

import com.min.chalkakserver.service.FileUploadService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/uploads")
public class FileUploadController {

    private final FileUploadService fileUploadService;

    public FileUploadController(FileUploadService fileUploadService) {
        this.fileUploadService = fileUploadService;
    }

    /** 업로드 대상 디렉터리 화이트리스트 (임의 경로 주입 방지). */
    private static final Set<String> ALLOWED_TYPES = Set.of("reviews", "profiles");

    @PostMapping("/image")
    public ResponseEntity<Map<String, String>> uploadImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "type", required = false, defaultValue = "reviews") String type)
            throws IOException {

        if (!ALLOWED_TYPES.contains(type)) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "지원하지 않는 업로드 타입입니다."));
        }

        // Validate file
        if (file.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "파일이 비어있습니다."));
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "이미지 파일만 업로드 가능합니다."));
        }

        // Max 10MB
        if (file.getSize() > 10 * 1024 * 1024) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "파일 크기는 10MB를 초과할 수 없습니다."));
        }

        String imageUrl = fileUploadService.uploadFile(file, type);
        return ResponseEntity.ok(Map.of("imageUrl", imageUrl));
    }
}

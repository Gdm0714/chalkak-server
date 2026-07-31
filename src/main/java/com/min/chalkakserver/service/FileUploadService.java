package com.min.chalkakserver.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import net.coobird.thumbnailator.Thumbnails;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Service
public class FileUploadService {

    private static final Logger log = LoggerFactory.getLogger(FileUploadService.class);

    private final S3Client s3Client;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    @Value("${cloud.aws.region.static}")
    private String region;

    // MinIO 등 커스텀 엔드포인트. 있으면 path-style URL을 생성한다.
    @Value("${cloud.aws.s3.endpoint:}")
    private String endpoint;

    public FileUploadService(S3Client s3Client) {
        this.s3Client = s3Client;
    }

    public String uploadFile(MultipartFile file, String directory) throws IOException {
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String key = directory + "/" + UUID.randomUUID() + extension;

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(file.getContentType())
                .build();

        s3Client.putObject(putObjectRequest,
                RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

        return buildPublicUrl(key);
    }

    /**
     * 원본 이미지를 ~500x500 이내로 리사이즈한 썸네일(JPEG)을 S3/MinIO에 업로드한다.
     * 키 포맷: {directory}/{UUID}_thumb.jpg
     * 읽을 수 없는 포맷(HEIC 등) 등 어떤 예외가 발생하면 경고 로깅 후 null을 반환한다.
     * (호출부는 null이면 원본 이미지 URL로 폴백)
     */
    public String uploadThumbnail(MultipartFile file, String directory) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Thumbnails.of(file.getInputStream())
                    .size(500, 500)
                    .outputFormat("jpg")
                    .toOutputStream(baos);

            byte[] bytes = baos.toByteArray();
            String key = directory + "/" + UUID.randomUUID() + "_thumb.jpg";

            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType("image/jpeg")
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromBytes(bytes));

            return buildPublicUrl(key);
        } catch (Exception e) {
            log.warn("썸네일 생성/업로드 실패 (원본 URL로 폴백): directory={}, error={}", directory, e.getMessage());
            return null;
        }
    }

    /**
     * S3 key에 대한 public URL을 생성한다.
     * MinIO 등 커스텀 엔드포인트면 path-style URL, 아니면 실제 AWS virtual-hosted URL.
     */
    private String buildPublicUrl(String key) {
        if (endpoint != null && !endpoint.isBlank()) {
            String base = endpoint.endsWith("/") ? endpoint.substring(0, endpoint.length() - 1) : endpoint;
            return String.format("%s/%s/%s", base, bucket, key);
        }
        return String.format("https://%s.s3.%s.amazonaws.com/%s", bucket, region, key);
    }

    /**
     * S3에 저장된 객체를 public URL 기준으로 삭제한다.
     * URL 포맷: https://{bucket}.s3.{region}.amazonaws.com/{key}
     * null/blank/파싱 불가 시 no-op (경고 로깅).
     */
    public void deleteFile(String url) {
        if (url == null || url.isBlank()) {
            return;
        }

        try {
            URI uri = URI.create(url);
            String path = uri.getPath();
            if (path == null || path.isBlank() || "/".equals(path)) {
                log.warn("S3 삭제 스킵: URL에서 key를 추출할 수 없습니다. url={}", url);
                return;
            }

            // 선행 슬래시 제거 후 URL 디코딩하여 실제 S3 key 복원
            String key = path.startsWith("/") ? path.substring(1) : path;
            key = URLDecoder.decode(key, StandardCharsets.UTF_8);

            // path-style(MinIO) URL이면 앞의 "{bucket}/" 를 제거해 순수 key만 남긴다.
            String bucketPrefix = bucket + "/";
            if (key.startsWith(bucketPrefix)) {
                key = key.substring(bucketPrefix.length());
            }

            if (key.isBlank()) {
                log.warn("S3 삭제 스킵: 비어있는 key. url={}", url);
                return;
            }

            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build();

            s3Client.deleteObject(deleteObjectRequest);
        } catch (Exception e) {
            log.warn("S3 객체 삭제 실패 (무시하고 계속): url={}, error={}", url, e.getMessage());
        }
    }
}

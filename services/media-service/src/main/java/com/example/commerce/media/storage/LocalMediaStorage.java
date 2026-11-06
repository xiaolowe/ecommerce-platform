package com.example.commerce.media.storage;

import com.example.commerce.platform.common.error.BadRequestException;
import com.example.commerce.platform.common.error.NotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;

@Component
public class LocalMediaStorage {

    private static final long MAX_BYTES = 10L * 1024 * 1024;
    private static final long MAX_PIXELS = 50_000_000L;
    private static final Map<String, String> EXTENSIONS = Map.of(
            "image/jpeg", "jpg",
            "image/png", "png",
            "image/gif", "gif"
    );

    private final Path root;
    private final String publicBaseUrl;

    public LocalMediaStorage(@Value("${media.storage.local-root:./.data/media}") String root,
                             @Value("${media.public-base-url:/media}") String publicBaseUrl) {
        this.root = Path.of(root).toAbsolutePath().normalize();
        this.publicBaseUrl = publicBaseUrl.endsWith("/")
                ? publicBaseUrl.substring(0, publicBaseUrl.length() - 1)
                : publicBaseUrl;
    }

    public StoredImage store(MultipartFile file) {
        if (file.isEmpty()) {
            throw new BadRequestException("图片文件为空");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new BadRequestException("图片不能超过 10 MB");
        }

        String contentType = file.getContentType();
        String extension = EXTENSIONS.get(contentType);
        if (extension == null) {
            throw new BadRequestException("仅支持 JPG、PNG 和 GIF 图片");
        }

        try {
            byte[] bytes = file.getBytes();
            BufferedImage image;
            try (InputStream input = file.getInputStream()) {
                image = ImageIO.read(input);
            }
            if (image == null || image.getWidth() <= 0 || image.getHeight() <= 0) {
                throw new BadRequestException("无法识别图片内容");
            }
            if ((long) image.getWidth() * image.getHeight() > MAX_PIXELS) {
                throw new BadRequestException("图片像素尺寸过大");
            }

            LocalDate today = LocalDate.now();
            String objectKey = "images/%04d/%02d/%s.%s".formatted(
                    today.getYear(), today.getMonthValue(), UUID.randomUUID(), extension);
            Path target = resolve(objectKey);
            Files.createDirectories(target.getParent());
            Path temporary = Files.createTempFile(target.getParent(), "upload-", ".tmp");
            try {
                Files.write(temporary, bytes);
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } finally {
                Files.deleteIfExists(temporary);
            }

            return new StoredImage(
                    objectKey,
                    safeOriginalName(file.getOriginalFilename()),
                    contentType,
                    bytes.length,
                    image.getWidth(),
                    image.getHeight(),
                    sha256(bytes),
                    publicUrl(objectKey)
            );
        } catch (IOException exception) {
            throw new BadRequestException("图片保存失败");
        }
    }

    public Resource load(String objectKey) {
        Path path = resolve(objectKey);
        if (!Files.isRegularFile(path)) {
            throw new NotFoundException("图片不存在");
        }
        try {
            return new UrlResource(path.toUri());
        } catch (IOException exception) {
            throw new NotFoundException("图片不存在");
        }
    }

    public void delete(String objectKey) {
        try {
            Files.deleteIfExists(resolve(objectKey));
        } catch (IOException exception) {
            throw new BadRequestException("图片文件删除失败");
        }
    }

    public String publicUrl(String objectKey) {
        return publicBaseUrl + "/" + objectKey;
    }

    private Path resolve(String objectKey) {
        if (objectKey == null || objectKey.isBlank() || objectKey.startsWith("/")) {
            throw new BadRequestException("图片 key 无效");
        }
        Path resolved = root.resolve(objectKey).normalize();
        if (!resolved.startsWith(root)) {
            throw new BadRequestException("图片 key 无效");
        }
        return resolved;
    }

    private String safeOriginalName(String originalName) {
        if (originalName == null || originalName.isBlank()) {
            return "image";
        }
        String name = Path.of(originalName).getFileName().toString();
        return name.length() > 255 ? name.substring(name.length() - 255) : name;
    }

    private String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    public record StoredImage(
            String objectKey,
            String originalName,
            String contentType,
            long sizeBytes,
            int width,
            int height,
            String sha256,
            String url
    ) {
    }
}

package com.example.commerce.media;

import com.example.commerce.media.asset.MediaAsset;
import com.example.commerce.media.asset.MediaAssetService;
import com.example.commerce.media.storage.LocalMediaStorage;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;

import static com.example.commerce.media.MediaDtos.MediaAssetResponse;

@RestController
public class MediaController {

    private final MediaAssetService assetService;
    private final LocalMediaStorage storage;

    public MediaController(MediaAssetService assetService, LocalMediaStorage storage) {
        this.assetService = assetService;
        this.storage = storage;
    }

    @PostMapping(value = "/api/v1/media/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    MediaAssetResponse upload(@RequestParam("file") MultipartFile file) {
        return MediaAssetResponse.from(assetService.upload(file), storage);
    }

    @GetMapping("/api/v1/media/images")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    Page<MediaAssetResponse> list(@RequestParam(defaultValue = "0") int page,
                                  @RequestParam(defaultValue = "24") int size) {
        int safeSize = Math.min(Math.max(size, 1), 100);
        return assetService.list(PageRequest.of(Math.max(page, 0), safeSize))
                .map(asset -> MediaAssetResponse.from(asset, storage));
    }

    @DeleteMapping("/api/v1/media/images/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    void delete(@PathVariable String id) {
        assetService.delete(id);
    }

    @GetMapping("/media/**")
    ResponseEntity<Resource> serve(HttpServletRequest request) {
        String prefix = "/media/";
        String objectKey = request.getRequestURI().substring(prefix.length());
        MediaAsset asset = assetService.getByObjectKey(objectKey);
        Resource resource = storage.load(objectKey);
        MediaType contentType = MediaType.parseMediaType(asset.getContentType());

        return ResponseEntity.ok()
                .contentType(contentType)
                .cacheControl(CacheControl.maxAge(Duration.ofDays(30)).cachePublic())
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                .body(resource);
    }
}

package com.example.commerce.media;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "media.storage.local-root=target/test-media")
class MediaServiceApplicationTest {

    @Test
    void contextLoads() {
    }
}

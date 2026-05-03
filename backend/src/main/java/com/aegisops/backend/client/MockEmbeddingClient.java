package com.aegisops.backend.client;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

@Component
@ConditionalOnProperty(name = "aegisops.embedding.provider", havingValue = "mock", matchIfMissing = true)
public class MockEmbeddingClient implements EmbeddingClient {

    private final int vectorSize;

    public MockEmbeddingClient(@Value("${aegisops.qdrant.vector-size:384}") int vectorSize) {
        this.vectorSize = vectorSize;
    }

    @Override
    public List<Double> embed(String text) {
        byte[] digest = digest(text == null ? "" : text);
        List<Double> vector = new ArrayList<>(vectorSize);
        for (int index = 0; index < vectorSize; index++) {
            int value = digest[index % digest.length] & 0xff;
            vector.add((value / 127.5d) - 1.0d);
        }
        return vector;
    }

    private byte[] digest(String text) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 digest is unavailable", exception);
        }
    }
}

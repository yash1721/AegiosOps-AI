package com.aegisops.backend.client;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MockEmbeddingClientTest {

    @Test
    void mockEmbeddingIsDeterministicAndUsesConfiguredVectorSize() {
        MockEmbeddingClient client = new MockEmbeddingClient(384);

        assertThat(client.embed("checkout cpu restart"))
                .hasSize(384)
                .isEqualTo(client.embed("checkout cpu restart"));
    }
}

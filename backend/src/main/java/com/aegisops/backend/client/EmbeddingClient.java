package com.aegisops.backend.client;

import java.util.List;

public interface EmbeddingClient {

    List<Double> embed(String text);
}

package com.ckontur.edms.model;

import io.vavr.collection.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

@Getter
@RequiredArgsConstructor
public class Document {
    private final Long id;
    private final User author;
    private final DocumentType type;
    private final String path;
    private final Long size;
    private final LocalDateTime createdAt;
    private final List<SignRoute> signatureRoute;

    public boolean isSigned() {
        return signatureRoute.map(SignRoute::isSigned).reduceOption((a, b) -> a && b).getOrElse(false);
    }
}

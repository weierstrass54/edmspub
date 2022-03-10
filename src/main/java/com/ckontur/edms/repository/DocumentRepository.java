package com.ckontur.edms.repository;

import com.ckontur.edms.exception.InvalidDocumentException;
import com.ckontur.edms.model.Document;
import com.ckontur.edms.model.DocumentType;
import com.ckontur.edms.model.SignRoute;
import com.ckontur.edms.model.User;
import io.vavr.collection.List;
import io.vavr.control.Option;
import io.vavr.control.Try;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;

@Repository
@RequiredArgsConstructor
public class DocumentRepository {
    private final JdbcTemplate jdbcTemplate;
    private final SignatureRepository signatureRepository;
    private final UserRepository userRepository;

    public Option<Document> findById(Long id) {
        List<SignRoute> signatures = signatureRepository.findAllByDocumentId(id);
        return userRepository.findByDocumentId(id).flatMap(user -> Option.ofOptional(
            jdbcTemplate.query("SELECT * FROM documents WHERE id = ?", new DocumentMapper(user, signatures), id)
                .stream().findAny()
        ));
    }

    public Try<Document> create(User author, List<User> signers, DocumentType documentType, String path, Long size, LocalDateTime createdAt) {
        return Try.of(() -> {
            final String query = "INSERT INTO documents(author_id, type, path, size, created_at) VALUES (?, ?, ?, ?, ?) RETURNING id";
            return jdbcTemplate.queryForObject(
                query, Long.class, author.getId(), documentType.getValue(), path, size, Timestamp.valueOf(createdAt)
            );
        }).flatMap(documentId ->
            signatureRepository.create(documentId, signers.toArray())
                .map(signRoute -> new Document(documentId, author, documentType, path, size, createdAt, signRoute))
        );
    }

    public Try<Document> sign(Document document, User user, String notes) {
        return document.getSignatureRoute().find(sr -> !sr.isSigned())
            .filter(sr -> sr.getUser().getId().equals(user.getId()))
            .map(signRoute -> signatureRepository.sign(document.getId(), signRoute.getId(), notes, LocalDateTime.now())
                .map(signRoutes -> new Document(
                    document.getId(), document.getAuthor(), document.getType(), document.getPath(), document.getSize(), document.getCreatedAt(), signRoutes)
                )
            )
            .getOrElse(Try.failure(new InvalidDocumentException("Пользователь " + user.getId() + " не должен подписывать этот документ.")));
    }

    public Option<Try<Document>> deleteById(Long id) {
        return findById(id).map(document ->
            Try.of(() -> jdbcTemplate.update("DELETE FROM documents WHERE id = ?", id))
                .map(__ -> document)
        );
    }

    @RequiredArgsConstructor
    private static class DocumentMapper implements RowMapper<Document> {
        private final User author;
        private final List<SignRoute> signRoutes;

        @Override
        public Document mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new Document(
                rs.getLong("id"),
                author,
                DocumentType.of(rs.getString("type")),
                rs.getString("path"),
                rs.getLong("size"),
                rs.getTimestamp("created_at").toLocalDateTime(),
                signRoutes
            );
        }
    }
}

package com.ckontur.edms.repository;

import com.ckontur.edms.model.SignRoute;
import com.ckontur.edms.model.Signature;
import com.ckontur.edms.model.User;
import com.ckontur.edms.utils.RSAUtils;
import io.vavr.Tuple2;
import io.vavr.collection.Array;
import io.vavr.collection.List;
import io.vavr.collection.Map;
import io.vavr.collection.Stream;
import io.vavr.control.Try;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.function.Function;

@Repository
@RequiredArgsConstructor
public class SignatureRepository {
    private final JdbcTemplate jdbcTemplate;
    private final UserRepository userRepository;

    @Transactional
    public List<SignRoute> findAllByDocumentId(Long id) {
        Map<Long, User> users = userRepository.findAllRoutesByDocumentIdIndexexByRouteId(id);
        Map<Long, Signature> signatures = findAllSignaturesByDocumentIdIndexedByRouteId(id);
        return List.ofAll(
            jdbcTemplate.query(
                "SELECT id, user_id, ordinal FROM sign_routes WHERE document_id = ? ORDER BY ordinal",
                new SignRouteMapper(users, signatures), id
            )
        );
    }

    @Transactional
    public Try<List<SignRoute>> create(Long documentId, Array<User> users) {
        return Try.sequence(
            Stream.range(0, users.length()).map(i -> Try.of(() ->
                jdbcTemplate.update(
                    "INSERT INTO sign_routes(document_id, user_id, ordinal) VALUES (?, ?, ?)",
                    documentId, users.get(i).getId(), i
                )
            ))
        ).map(__ -> findAllByDocumentId(documentId));
    }

    @Transactional
    public Try<List<SignRoute>> sign(Long documentId, Long signRouteId, String notes, LocalDateTime signedAt) {
        return Try.of(() -> jdbcTemplate.update(
            "INSERT INTO signatures(sign_route_id, notes, signed_at) VALUES (?, ?, ?)", signRouteId, notes, Timestamp.valueOf(signedAt))
        )
        .map(__ -> findAllByDocumentId(documentId));
    }

    private Map<Long, Signature> findAllSignaturesByDocumentIdIndexedByRouteId(Long documentId) {
        final String query = "SELECT s.*, k.public_key " +
            "FROM signatures s " +
            "JOIN sign_routes sr ON s.sign_route_id = sr.id " +
            "JOIN keys k ON sr.user_id = k.user_id " +
            "WHERE sr.document_id = ?";
        return List.ofAll(jdbcTemplate.query(query, (rs, rowNum) -> {
            Signature signature = SignatureMapper.INSTANCE.mapRow(rs, rowNum);
            return new Tuple2<>(rs.getLong("sign_route_id"), signature);
        }, documentId)).groupBy(Tuple2::_1).mapValues(__ -> __.map(Tuple2::_2).get());
    }

    @RequiredArgsConstructor
    private static class SignRouteMapper implements RowMapper<SignRoute> {
        private final Map<Long, User> users;
        private final Map<Long, Signature> signatures;

        @Override
        public SignRoute mapRow(ResultSet rs, int rowNum) throws SQLException {
            Long id = rs.getLong("id");
            return new SignRoute(
                id,
                users.get(id).getOrNull(),
                rs.getInt("ordinal"),
                signatures.get(id).getOrNull()
            );
        }
    }

    private static class SignatureMapper implements RowMapper<Signature> {
        private static final SignatureMapper INSTANCE = new SignatureMapper();

        @Override
        public Signature mapRow(ResultSet rs, int rowNum) throws SQLException {
            return RSAUtils.publicKeyOf(rs.getString("public_key"))
                .flatMap(rsaPublicKey -> Try.of(() -> new Signature(
                    rs.getString("notes"),
                    rsaPublicKey,
                    rs.getTimestamp("signed_at").toLocalDateTime()
                )))
                .getOrElseThrow((Function<Throwable, SQLException>) SQLException::new);
        }
    }
}

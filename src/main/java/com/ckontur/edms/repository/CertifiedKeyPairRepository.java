package com.ckontur.edms.repository;

import com.ckontur.edms.model.CertifiedKeyPair;
import com.ckontur.edms.utils.RSAUtils;
import io.vavr.control.Option;
import io.vavr.control.Try;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.function.Function;

@Repository
@RequiredArgsConstructor
public class CertifiedKeyPairRepository {
    private final JdbcTemplate jdbcTemplate;

    public Option<CertifiedKeyPair> findByUserId(Long userId) {
        return Option.ofOptional(
            jdbcTemplate.query("SELECT * FROM keys WHERE user_id = ?", CertifiedKeyPairMapper.INSTANCE, userId)
                .stream().findAny()
        );
    }

    public Try<CertifiedKeyPair> create(Long userId, CertifiedKeyPair ckp) {
        return RSAUtils.toString(ckp.getX509Certificate())
            .flatMap(x509 -> Try.of(() ->
                jdbcTemplate.queryForObject(
                    "INSERT INTO keys(user_id, private_key, public_key, x509_certificate) VALUES (?, ?, ?, ?) RETURNING *",
                    CertifiedKeyPairMapper.INSTANCE,
                    userId, RSAUtils.toString(ckp.getPrivateKey()), RSAUtils.toString(ckp.getPublicKey()), x509
                )
            ));
    }

    public Option<Try<CertifiedKeyPair>> deleteByUserId(Long userId) {
        return findByUserId(userId).map(ckp ->
            Try.of(() -> jdbcTemplate.update("DELETE FROM keys WHERE user_id = ?", userId))
                .map(__ -> ckp)
        );
    }

    private static class CertifiedKeyPairMapper implements RowMapper<CertifiedKeyPair> {
        private static final CertifiedKeyPairMapper INSTANCE = new CertifiedKeyPairMapper();

        @Override
        public CertifiedKeyPair mapRow(ResultSet rs, int rowNum) throws SQLException {
            RSAPrivateKey privateKey = RSAUtils.privateKeyOf(rs.getString("private_key"))
                .getOrElseThrow((Function<Throwable, SQLException>) SQLException::new);
            RSAPublicKey publicKey = RSAUtils.publicKeyOf(rs.getString("public_key"))
                .getOrElseThrow((Function<Throwable, SQLException>) SQLException::new);
            X509Certificate x509Certificate = RSAUtils.x509Of(rs.getString("x509_certificate"))
                .getOrElseThrow((Function<Throwable, SQLException>) SQLException::new);
            return new CertifiedKeyPair(privateKey, publicKey, x509Certificate);
        }
    }
}

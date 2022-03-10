package com.ckontur.edms.repository;

import com.ckontur.edms.model.SignRouteTemplate;
import com.ckontur.edms.utils.SqlUtils;
import io.vavr.control.Option;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;

@Repository
@RequiredArgsConstructor
public class SignRouteTemplateRepository {
    private final JdbcTemplate jdbcTemplate;

    public Option<SignRouteTemplate> findById(Long id) {
        final String query = "SELECT srt.id, srt.name, ARRAY_AGG(srtu.user_id)::text[] AS user_ids " +
            "FROM sign_route_templates srt " +
            "JOIN sign_route_template_users srtu ON srtu.template_id = srt.id " +
            "WHERE srt.id = ? " +
            "GROUP BY srt.id";
        return Option.ofOptional(
            jdbcTemplate.query(query, SignRouteTemplateMapper.INSTANCE, id).stream().findAny()
        );
    }

    private static class SignRouteTemplateMapper implements RowMapper<SignRouteTemplate> {
        private static final SignRouteTemplateMapper INSTANCE = new SignRouteTemplateMapper();

        @Override
        public SignRouteTemplate mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new SignRouteTemplate(
                rs.getLong("id"),
                rs.getString("name"),
                SqlUtils.listOf(rs.getArray("user_ids"), Long::valueOf)
            );
        }
    }
}

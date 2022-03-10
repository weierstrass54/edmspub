package com.ckontur.edms.repository;

import com.ckontur.edms.model.Page;
import com.ckontur.edms.model.User;
import com.ckontur.edms.web.PageRequest;
import io.vavr.collection.List;
import io.vavr.collection.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
public class SearchUserRepository {
    private final NamedParameterJdbcTemplate parametrizedJdbcTemplate;

    @Transactional
    public Page<User> findAllBySearchString(String searchString, PageRequest pageRequest) {
        MapSqlParameterSource params = new MapSqlParameterSource(
            "search", "%" + searchString.toLowerCase() + "%"
        );
        Long total = parametrizedJdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM users WHERE " + whereExpression(), params, Long.class
        );
        List<User> users = List.ofAll(
            parametrizedJdbcTemplate.query(
                "SELECT * FROM users WHERE " + whereExpression() + pageExpression(pageRequest), params, UserRepository.UserMapper.INSTANCE
            )
        );
        return Page.of(users, pageRequest.getSize(), total);
    }

    private String whereExpression() {
        return Stream.of("login", "first_name", "middle_name", "last_name", "phone", "email")
            .map(field -> "LOWER(" + field + ") LIKE :search")
            .mkString(" OR ");
    }

    private String pageExpression(PageRequest pageRequest) {
        return String.format(" ORDER BY last_name %1$s, middle_name %1$s, first_name %1$s " +
            "OFFSET %2$d LIMIT %3$d", pageRequest.getDirection().name(), pageRequest.getOffset(), pageRequest.getSize());
    }
}

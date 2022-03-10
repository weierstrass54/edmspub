package com.ckontur.edms.repository;

import com.ckontur.edms.component.auth.PasswordGenerator;
import com.ckontur.edms.model.Permission;
import com.ckontur.edms.model.User;
import com.ckontur.edms.utils.SqlUtils;
import com.ckontur.edms.web.UserRequests;
import io.vavr.Tuple2;
import io.vavr.collection.HashSet;
import io.vavr.collection.List;
import io.vavr.collection.Map;
import io.vavr.control.Option;
import io.vavr.control.Try;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Repository;

import javax.annotation.PostConstruct;
import java.sql.ResultSet;
import java.sql.SQLException;

import static com.ckontur.edms.utils.SqlUtils.setOf;

@Slf4j
@Repository
@RequiredArgsConstructor
public class UserRepository {
    private final PasswordEncoder passwordEncoder;
    private final PasswordGenerator passwordGenerator;
    protected final NamedParameterJdbcTemplate parametrizedJdbcTemplate;

    public Option<User> findById(Long id) {
        return Option.ofOptional(
            parametrizedJdbcTemplate.getJdbcTemplate().query("SELECT * FROM users WHERE id = ?", UserMapper.INSTANCE, id)
                .stream().findAny()
        );
    }

    public Option<User> findByLogin(String login) {
        return Option.ofOptional(
            parametrizedJdbcTemplate.getJdbcTemplate().query("SELECT * FROM users WHERE login = ?", UserMapper.INSTANCE, login)
                .stream().findAny()
        );
    }

    public Option<User> findByDocumentId(Long id) {
        final String query = "SELECT u.* FROM users u WHERE u.id IN (SELECT d.author_id FROM documents d WHERE d.id = ?)";
        return Option.ofOptional(
            parametrizedJdbcTemplate.getJdbcTemplate().query(query, UserMapper.INSTANCE, id).stream().findAny()
        );
    }

    public List<User> findAllByIds(List<Long> userIds) {
        return List.ofAll(
            parametrizedJdbcTemplate.query(
                "SELECT * FROM users WHERE id IN (:ids)",
                new MapSqlParameterSource("ids", userIds),
                UserMapper.INSTANCE
            )
        );
    }

    public Map<Long, User> findAllRoutesByDocumentIdIndexexByRouteId(Long documentId) {
        final String query = "SELECT sr.id AS route_id, u.* " +
            "FROM sign_routes sr " +
            "JOIN users u ON sr.user_id = u.id " +
            "WHERE sr.document_id = ?";
        return List.ofAll(parametrizedJdbcTemplate.getJdbcTemplate().query(query, (rs, rowNum) -> {
            User user = UserMapper.INSTANCE.mapRow(rs, rowNum);
            return new Tuple2<>(rs.getLong("route_id"), user);
        }, documentId)).groupBy(Tuple2::_1).mapValues(__ -> __.map(Tuple2::_2).get());
    }

    public Try<User> create(UserRequests.CreateUser request) {
        final String encodedPassword = passwordEncoder.encode(request.getPassword());
        return Try.of(() -> parametrizedJdbcTemplate.getJdbcTemplate().queryForObject(
            "INSERT INTO users(login, password, first_name, middle_name, last_name, appointment, phone, email, permissions) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?::text[]) RETURNING *",
            UserMapper.INSTANCE,
            request.getLogin(), encodedPassword, request.getFirstName(), request.getMiddleName(), request.getLastName(),
                request.getAppointment(), request.getPhone(), request.getEmail(),
                    SqlUtils.array(request.getPermissions(), Permission::name)
        ));
    }

    public Try<Option<User>> updateById(Long id, UserRequests.UpdateUser request) {
        final String encodedPassword = Option.of(request.getPassword()).map(passwordEncoder::encode).getOrNull();
        return Try.of(() -> Option.ofOptional(parametrizedJdbcTemplate.getJdbcTemplate().query(
            "UPDATE users SET " +
                    "login = COALESCE(?, login), " +
                    "password = COALESCE(?, password), " +
                    "first_name = COALESCE(?, first_name), " +
                    "middle_name = COALESCE(?, middle_name), " +
                    "last_name = COALESCE(?, last_name), " +
                    "appointment = COALESCE(?, appointment), " +
                    "phone = COALESCE(?, phone), " +
                    "email = COALESCE(?, email), " +
                    "permissions = COALESCE(?::text[], permissions)" +
                "WHERE id = ? " +
                "RETURNING *",
            UserMapper.INSTANCE,
            request.getLogin(), encodedPassword, request.getFirstName(), request.getMiddleName(), request.getLastName(),
                request.getAppointment(), request.getPhone(), request.getEmail(),
                    SqlUtils.array(request.getPermissions(), Permission::name), id
        ).stream().findAny()));
    }

    public Try<Option<User>> deleteById(Long id) {
        return Try.of(() -> Option.ofOptional(
            parametrizedJdbcTemplate.getJdbcTemplate().query("DELETE FROM users WHERE id = ? RETURNING *", UserMapper.INSTANCE, id)
                .stream().findAny()
            )
        );
    }

    @PostConstruct
    private void init() {
        String password = passwordGenerator.generate();
        parametrizedJdbcTemplate.update(
            "INSERT INTO users(login, password, appointment, phone, email, permissions) VALUES " +
                    "(:login, :password, :appointment, :phone, :email, :permissions::text[]) " +
                    "ON CONFLICT (login) DO UPDATE SET password = :password",
            new MapSqlParameterSource("login", "admin")
                .addValue("password", passwordEncoder.encode(password))
                .addValue("appointment", "admin")
                .addValue("phone", "+70000000000")
                .addValue("email", "admin@edms.ru")
                .addValue("permissions", SqlUtils.array(HashSet.of(Permission.ADMIN), Permission::name))
        );
        log.info("Created ADMIN user {}:{}", "admin", password);
    }

    protected static class UserMapper implements RowMapper<User> {
        protected static final UserMapper INSTANCE = new UserMapper();

        @Override
        public User mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new User(
                rs.getLong("id"),
                rs.getString("login"),
                rs.getString("password"),
                rs.getString("first_name"),
                rs.getString("middle_name"),
                rs.getString("last_name"),
                rs.getString("appointment"),
                rs.getString("phone"),
                rs.getString("email"),
                setOf(rs.getArray("permissions"), Permission::of)
            );
        }
    }
}

package com.ckontur.edms.component;

import com.ckontur.edms.component.auth.PasswordGenerator;
import io.vavr.Tuple2;
import io.vavr.control.Option;
import io.vavr.control.Try;
import lombok.extern.slf4j.Slf4j;
import org.mockito.Mockito;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.lifecycle.Startable;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;

import java.net.URI;

import static org.mockito.BDDMockito.given;

@Slf4j
@AutoConfigureMockMvc
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(initializers = SpringBootPostgreSQLContainerTests.SpringBootTestInitializer.class)
public abstract class SpringBootPostgreSQLContainerTests extends AbstractTestNGSpringContextTests {
    private static PostgreSQLContainer<?> postgreSQLContainer;

    @BeforeSuite
    protected static void beforeSuite() throws Exception {
        postgreSQLContainer = Try.of(() -> {
            SpringConfig config = SpringConfig.of("application.yml");
            URI uri = URI.create(config.getSpring().getDataSource().getUrl().substring(5));
            return new Tuple2<>(config, uri);
        })
        .filter(t -> t._2.getScheme().equals("postgresql"))
        .map(t ->
            new PostgreSQLContainer<>("postgres:latest")
                .withUsername(t._1.getSpring().getDataSource().getUsername())
                .withPassword(t._1.getSpring().getDataSource().getPassword())
                .withExposedPorts(t._2.getPort())
                .withDatabaseName(t._2.getPath())
        )
        .peek(PostgreSQLContainer::start)
        .peek(__ -> log.info("PostgreSQL container has been started."))
        .getOrElseThrow(() -> new Exception("Database source is invalid."));
    }

    @AfterSuite
    protected static void afterSuite() {
        Option.of(postgreSQLContainer)
            .peek(Startable::close)
            .forEach(__ -> log.info("PostgreSQL container has been closed."));
    }

    static class SpringBootTestInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        @Override
        public void initialize(ConfigurableApplicationContext applicationContext) {
            TestPropertyValues.of("spring.datasource.url=" + postgreSQLContainer.getJdbcUrl())
                .applyTo(applicationContext.getEnvironment());

            PasswordGenerator passwordGenerator = Mockito.mock(PasswordGenerator.class);
            given(passwordGenerator.generate()).willReturn("admin123");
            applicationContext.getBeanFactory().registerSingleton(passwordGenerator.getClass().getCanonicalName(), passwordGenerator);
        }
    }
}

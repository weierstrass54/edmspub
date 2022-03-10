package com.ckontur.edms.component;

import io.vavr.Tuple2;
import io.vavr.collection.List;
import io.vavr.control.Try;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.HttpWaitStrategy;
import org.testcontainers.lifecycle.Startable;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;

import java.net.URI;
import java.time.Duration;
import java.util.Objects;

@Slf4j
@AutoConfigureMockMvc
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(initializers = SpringBootMinioPostgreSQLContainerTests.SpringBootTestInitializer.class)
public abstract class SpringBootMinioPostgreSQLContainerTests extends AbstractTestNGSpringContextTests {
    private static PostgreSQLContainer<?> postgreSQLContainer;
    private static GenericContainer<?> minioContainer;

    @BeforeSuite
    protected static void beforeSuite() throws Exception {
        startPostgreSQLContainer();
        startMinioContainer();
    }

    @AfterSuite
    protected static void afterSuite() {
        List.of(postgreSQLContainer, minioContainer)
            .filter(Objects::nonNull)
            .peek(Startable::close)
            .forEach(c -> log.info("{} has been closed.", c.getClass().getName()));
    }

    private static void startPostgreSQLContainer() throws Exception {
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
        .peek(c -> log.info("{} container has been started.", c.getContainerName()))
        .getOrElseThrow(() -> new Exception("Database source is invalid."));
    }

    private static void startMinioContainer() throws Exception {
        int port = 9000;
        minioContainer = Try.of(() -> MinioConfig.of("application.yml"))
            .map(mc ->
                new GenericContainer<>("minio/minio")
                    .withEnv("MINIO_ACCESS_KEY", mc.getEdms().getMinio().getAccessKey())
                    .withEnv("MINIO_SECRET_KEY", mc.getEdms().getMinio().getSecretKey())
                    .withCommand("server /data")
                    .withExposedPorts(port)
                    .waitingFor(
                        new HttpWaitStrategy()
                            .forPath("/minio/health/ready")
                            .forPort(port)
                            .withStartupTimeout(Duration.ofSeconds(10))
                    )
            )
            .peek(GenericContainer::start)
            .peek(c -> log.info("{} container has been started.", c.getContainerName()))
            .getOrElseThrow(() -> new Exception("Minio source is invalid."));
    }

    static class SpringBootTestInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        @Override
        public void initialize(ConfigurableApplicationContext applicationContext) {
            String minioEndpoint = String.format(
                "http://%s:%s", minioContainer.getContainerIpAddress(), minioContainer.getFirstMappedPort()
            );
            TestPropertyValues.of(
                "spring.datasource.url=" + postgreSQLContainer.getJdbcUrl(),
                "edms.minio.endpoint=" + minioEndpoint
            ).applyTo(applicationContext.getEnvironment());
        }
    }
}

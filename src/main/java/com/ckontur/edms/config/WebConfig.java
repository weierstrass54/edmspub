package com.ckontur.edms.config;

import com.ckontur.edms.component.web.StringToVavrSetLongConverter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.minio.MinioClient;
import io.vavr.jackson.datatype.VavrModule;
import org.apache.poi.xwpf.converter.pdf.PdfConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.*;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spi.service.contexts.SecurityContext;
import springfox.documentation.spring.web.plugins.Docket;

import java.util.Collections;
import java.util.List;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Value("${edms.minio.endpoint}")
    private String minioEndpoint;

    @Value("${edms.minio.accessKey}")
    private String minioAccessKey;

    @Value("${edms.minio.secretKey}")
    private String minioSecretKey;

    @Autowired
    private BuildProperties buildProperties;

    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverter(new StringToVavrSetLongConverter());
    }

    @Override
    public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
        configurer.defaultContentType(MediaType.APPLICATION_JSON);
    }

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.registerModule(new VavrModule());
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        return objectMapper;
    }

    @Bean
    public MinioClient minioClient() {
        return MinioClient.builder()
            .endpoint(minioEndpoint)
            .credentials(minioAccessKey, minioSecretKey)
            .build();
    }

    @Bean
    public PdfConverter pdfConverter() {
        return (PdfConverter) PdfConverter.getInstance();
    }

    @Bean
    public Docket swaggerApi() {
        Contact contact = new Contact("Цифровой контур", "https://c-kontur.com/", "main@c-kontur.com");
        return new Docket(DocumentationType.SWAGGER_2)
            .select()
            .apis(RequestHandlerSelectors.basePackage(buildProperties.getGroup()))
            .paths(PathSelectors.any())
            .build()
            .apiInfo(new ApiInfo(
                buildProperties.getName(), description(), buildProperties.getVersion(), null, contact,
                    null, null, Collections.emptyList()
            ))
            .securityContexts(List.of(SecurityContext.builder().securityReferences(securityReferences()).build()))
            .securitySchemes(List.of(new ApiKey("JWT", "Authorization", "header")));
    }

    private List<SecurityReference> securityReferences() {
        return List.of(new SecurityReference("JWT", new AuthorizationScope[]{
            new AuthorizationScope("global", "accessEverything")
        }));
    }

    public String description() {
        return "Двухфакторная аутентификация.";
    }

}

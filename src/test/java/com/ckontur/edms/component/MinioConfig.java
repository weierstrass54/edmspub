package com.ckontur.edms.component;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import lombok.Data;

import java.io.IOException;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class MinioConfig {
    private Edms edms;

    public static MinioConfig of(String classpathSource) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
        return objectMapper.readValue(
            MinioConfig.class.getClassLoader().getResource(classpathSource), MinioConfig.class
        );
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Edms {
        private Minio minio;

        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Minio {
            private String accessKey;
            private String secretKey;
        }
    }
}

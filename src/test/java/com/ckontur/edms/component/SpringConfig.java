package com.ckontur.edms.component;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import lombok.Data;

import java.io.IOException;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SpringConfig {
    private Spring spring;

    public static SpringConfig of(String classpathSource) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
        return objectMapper.readValue(
            SpringConfig.class.getClassLoader().getResource(classpathSource), SpringConfig.class
        );
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Spring {
        @JsonProperty("datasource")
        private DataSource dataSource;

        @Data
        public static class DataSource {
            private String url;
            private String username;
            private String password;
        }
    }
}

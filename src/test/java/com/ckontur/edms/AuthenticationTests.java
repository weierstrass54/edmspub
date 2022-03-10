package com.ckontur.edms;

import com.ckontur.edms.component.SpringBootPostgreSQLContainerTests;
import com.ckontur.edms.component.auth.VerifyCodeGenerator;
import com.ckontur.edms.web.AuthenticateRequest;
import com.ckontur.edms.web.ConfirmRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@Slf4j
public class AuthenticationTests extends SpringBootPostgreSQLContainerTests {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockBean
    @Autowired
    private VerifyCodeGenerator verifyCodeGenerator;

    @BeforeClass
    void beforeClass() {
        given(verifyCodeGenerator.generate()).willReturn("111111");
        jdbcTemplate.update("INSERT INTO users(login, password, appointment, phone, email, permissions) VALUES (?, ?, ?, ?, ?, ?::text[])",
            "test", passwordEncoder.encode("test"), "test", "+79998880000", "test@edms.ru", "{SIGN, VIEW}");
    }

    @AfterClass
    void afterClass() {
        jdbcTemplate.execute("TRUNCATE users CASCADE");
    }

    @Test
    void testAnonymousAccessDenied() throws Exception {
        mockMvc.perform(
            post("/auth/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new ConfirmRequest("555555")))
        ).andExpect(status().isForbidden());

        mockMvc.perform(
            get("/auth/user")
        ).andExpect(status().isForbidden());
    }

    @Test
    void testInvalidLoginAndPassword() throws Exception {
        mockMvc.perform(
            post("/auth/authenticate")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(new AuthenticateRequest("any", "any")))
        ).andExpect(status().isForbidden());
    }

    @Test
    void testInvalidLogin() throws Exception {
        mockMvc.perform(
            post("/auth/authenticate")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(new AuthenticateRequest("any", "test")))
        ).andExpect(status().isForbidden());
    }

    @Test
    void testInvalidPassword() throws Exception {
        mockMvc.perform(
            post("/auth/authenticate")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(new AuthenticateRequest("test", "any")))
        ).andExpect(status().isForbidden());
    }

    @Test
    void testSuccessfullyAuthenticate() throws Exception {
        mockMvc.perform(
            post("/auth/authenticate")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(new AuthenticateRequest("test", "test")))
        ).andExpect(status().isOk()).andDo(MockMvcResultHandlers.print());
    }

    @Test
    void testSuccessfullyAuthenticateWithoutConfirm() throws Exception {
        String token = mockMvc.perform(
            post("/auth/authenticate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new AuthenticateRequest("test", "test")))
        ).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        mockMvc.perform(
            get("/auth/user")
                .header("Authorization", "Bearer " + token)
        ).andExpect(status().isForbidden());
    }

    @Test
    void testSuccessfullyAuthenticateWrongConfirm() throws Exception {
        String token = mockMvc.perform(
            post("/auth/authenticate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new AuthenticateRequest("test", "test")))
        ).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        mockMvc.perform(
            post("/auth/verify")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new ConfirmRequest("000000")))
        ).andExpect(status().isForbidden());
    }

    @Test
    void testSuccessfullyAuthenticateCorrectConfirm() throws Exception {
        String verifyToken = mockMvc.perform(
            post("/auth/authenticate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new AuthenticateRequest("test", "test")))
        ).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        String accessToken = mockMvc.perform(
            post("/auth/verify")
                .header("Authorization", "Bearer " + verifyToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new ConfirmRequest("111111")))
        ).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        mockMvc.perform(
            get("/auth/user")
                .header("Authorization", "Bearer " + accessToken)
        ).andExpect(status().isOk()).andDo(MockMvcResultHandlers.print());
    }

}

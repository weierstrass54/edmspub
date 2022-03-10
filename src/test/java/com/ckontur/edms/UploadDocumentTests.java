package com.ckontur.edms;


import com.ckontur.edms.component.DummyEmailService;
import com.ckontur.edms.component.SpringBootMinioPostgreSQLContainerTests;
import com.ckontur.edms.component.auth.VerifyCodeGenerator;
import com.ckontur.edms.web.AuthenticateRequest;
import com.ckontur.edms.web.ConfirmRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
public class UploadDocumentTests extends SpringBootMinioPostgreSQLContainerTests {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private DummyEmailService dummyEmailService;

    @MockBean
    @Autowired
    private VerifyCodeGenerator verifyCodeGenerator;

    @MockBean
    @Autowired
    private DummyEmailService emailService;

    private String authUser1;
    private String authUser2;

    @BeforeClass
    void beforeClass() throws Exception {
        given(verifyCodeGenerator.generate()).willReturn("111111");
        jdbcTemplate.update("INSERT INTO users(login, password, appointment, phone, email, permissions) VALUES (?, ?, ?, ?, ?, ?::text[])",
            "test1", passwordEncoder.encode("test1"), "test1", "+79998880000", "test1@edms.ru", "{SIGN, VIEW, UPLOAD}");
        jdbcTemplate.update("INSERT INTO users(login, password, appointment, phone, email, permissions) VALUES (?, ?, ?, ?, ?, ?::text[])",
            "test2", passwordEncoder.encode("test2"), "test2", "+79998880000", "test2@edms.ru", "{SIGN, VIEW}");
        jdbcTemplate.update("INSERT INTO users(login, password, appointment, phone, email, permissions) VALUES (?, ?, ?, ?, ?, ?::text[])",
            "test3", passwordEncoder.encode("test3"), "test3", "+79998880000", "test3@edms.ru", "{SIGN, VIEW}");
        jdbcTemplate.update("INSERT INTO users(login, password, appointment, phone, email, permissions) VALUES (?, ?, ?, ?, ?, ?::text[])",
            "test4", passwordEncoder.encode("test4"), "test4", "+79998880000", "test4@edms.ru", "{VIEW}");

        jdbcTemplate.update("INSERT INTO sign_route_templates(name) VALUES (?)", "Валидный тестовый шаблон");
        jdbcTemplate.update("INSERT INTO sign_route_template_users(template_id, user_id) VALUES (?, ?), (?, ?)", 1, 3, 1, 4);

        jdbcTemplate.update("INSERT INTO sign_route_templates(name) VALUES (?)", "Невалидный тестовый шаблон");
        jdbcTemplate.update("INSERT INTO sign_route_template_users(template_id, user_id) VALUES (? ,?), (?, ?)", 2, 4, 2, 5);

        String vt1 = mockMvc.perform(
            post("/auth/authenticate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new AuthenticateRequest("test1", "test1")))
        ).andReturn().getResponse().getContentAsString();
        String vt2 = mockMvc.perform(
            post("/auth/authenticate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new AuthenticateRequest("test2", "test2")))
        ).andReturn().getResponse().getContentAsString();

        authUser1 = mockMvc.perform(
            post("/auth/verify")
                .header("Authorization", "Bearer " + vt1)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new ConfirmRequest("111111")))
        ).andReturn().getResponse().getContentAsString();
        authUser2 = mockMvc.perform(
            post("/auth/verify")
                .header("Authorization", "Bearer " + vt2)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new ConfirmRequest("111111")))
        ).andReturn().getResponse().getContentAsString();
    }

    @AfterClass
    void afterClass() {
        jdbcTemplate.execute("TRUNCATE users CASCADE");
        jdbcTemplate.execute("TRUNCATE sign_routes CASCADE");
        jdbcTemplate.execute("TRUNCATE sign_route_templates CASCADE");
    }

    @AfterMethod
    void afterMethod() {
        clearInvocations(emailService);
    }

    @Test
    public void testForbiddenUpload() throws Exception {
        mockMvc.perform(
            multipart("/docs/upload/signers")
                .file(new MockMultipartFile(
                    "files", "hello.docx",
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                    this.getClass().getClassLoader().getResourceAsStream("docs/hello.docx").readAllBytes())
                )
                .param("signers", "2")
                .header("Authorization", "Bearer " + authUser2)
        ).andExpect(status().isForbidden());
    }

    @Test
    public void testAbsentSignersUpload() throws Exception {
        mockMvc.perform(
            multipart("/docs/upload/signers")
                .file(new MockMultipartFile(
                    "files", "hello.docx",
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                    this.getClass().getClassLoader().getResourceAsStream("docs/hello.docx").readAllBytes())
                )
                .param("signers", "2, 4, 5")
                .header("Authorization", "Bearer " + authUser1)
        ).andExpect(status().isBadRequest());
    }

    @Test
    public void testInvalidDocumentType() throws Exception {
        mockMvc.perform(
            multipart("/docs/upload/signers")
                .file(new MockMultipartFile(
                    "files", "invalid.txt",
                    "plain/text",
                    this.getClass().getClassLoader().getResourceAsStream("docs/invalid.txt").readAllBytes())
                )
                .param("signers", "3")
                .header("Authorization", "Bearer " + authUser1)
        ).andExpect(status().isBadRequest());
    }

    @Test
    public void testInvalidSignerUpload() throws Exception {
        mockMvc.perform(
            multipart("/docs/upload/signers")
                .file(new MockMultipartFile(
                    "files", "hello.docx",
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                    this.getClass().getClassLoader().getResourceAsStream("docs/hello.docx").readAllBytes())
                )
                .param("signers", "3, 5")
                .header("Authorization", "Bearer " + authUser1)
        ).andExpect(status().isBadRequest()).andDo(MockMvcResultHandlers.print());
    }

    @Test
    public void testNotFoundSignTemplateUpload() throws Exception {
        mockMvc.perform(
            multipart("/docs/upload/template")
                .file(new MockMultipartFile(
                    "files", "hello.docx",
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                    this.getClass().getClassLoader().getResourceAsStream("docs/hello.docx").readAllBytes())
                )
                .param("sign_template", "2000")
                .header("Authorization", "Bearer " + authUser1)
        ).andExpect(status().isNotFound());
    }

    @Test
    public void testInvalidSignTemplateUpload() throws Exception {
        mockMvc.perform(
            multipart("/docs/upload/template")
                .file(new MockMultipartFile(
                    "files", "hello.docx",
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                    this.getClass().getClassLoader().getResourceAsStream("docs/hello.docx").readAllBytes())
                )
                .param("sign_template", "2")
                .header("Authorization", "Bearer " + authUser1)
        ).andExpect(status().isBadRequest()).andDo(MockMvcResultHandlers.print());
    }

    @Test
    public void testSuccessfulUpload() throws Exception {
        mockMvc.perform(
            multipart("/docs/upload/signers")
                .file(new MockMultipartFile(
                    "files", "hello.docx",
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                    this.getClass().getClassLoader().getResourceAsStream("docs/hello.docx").readAllBytes())
                )
                .param("signers", "3")
                .header("Authorization", "Bearer " + authUser1)
        ).andExpect(status().isOk()).andDo(MockMvcResultHandlers.print());

        verify(emailService, times(1)).sendMessage(anyString(), anyString(), anyString());

        mockMvc.perform(
            get("/docs/1")
                .header("Authorization", "Bearer " + authUser1)
        ).andExpect(status().isOk()).andDo(MockMvcResultHandlers.print());
    }

    @Test
    public void testSuccessfulTemplateUpload() throws Exception {
        mockMvc.perform(
            multipart("/docs/upload/template")
                .file(new MockMultipartFile(
                    "files", "hello.docx",
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                    this.getClass().getClassLoader().getResourceAsStream("docs/hello.docx").readAllBytes())
                )
                .param("sign_template", "1")
                .header("Authorization", "Bearer " + authUser1)
        ).andExpect(status().isOk()).andDo(MockMvcResultHandlers.print());

        verify(emailService, times(1)).sendMessage(anyString(), anyString(), anyString());

        mockMvc.perform(
            get("/docs/1")
                .header("Authorization", "Bearer " + authUser1)
        ).andExpect(status().isOk()).andDo(MockMvcResultHandlers.print());
    }

}

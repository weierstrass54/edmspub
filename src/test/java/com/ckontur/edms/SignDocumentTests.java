package com.ckontur.edms;

import com.ckontur.edms.component.DummyEmailService;
import com.ckontur.edms.component.SpringBootMinioPostgreSQLContainerTests;
import com.ckontur.edms.component.signature.OpenXMLResourceSigner;
import com.ckontur.edms.component.signature.PdfResourceSigner;
import com.ckontur.edms.web.AuthenticateRequest;
import com.ckontur.edms.web.ConfirmRequest;
import com.ckontur.edms.web.SignRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.ByteArrayResource;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.testng.Assert.assertTrue;

@Slf4j
public class SignDocumentTests extends SpringBootMinioPostgreSQLContainerTests {
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
    private DummyEmailService emailService;

    @Autowired
    private OpenXMLResourceSigner openXMLResourceSigner;

    @Autowired
    private PdfResourceSigner pdfResourceSigner;

    private String authUser1;
    private String authUser2;
    private String authUser3;
    private String authUser4;
    private String authUser5;

    @BeforeClass
    void beforeClass() throws Exception {
        jdbcTemplate.update("INSERT INTO users(login, password, appointment, phone, email, permissions) VALUES (?, ?, ?, ?, ?, ?::text[])",
            "test1", passwordEncoder.encode("test1"), "test1", "+79998880000", "test1@edms.ru", "{SIGN, VIEW, UPLOAD}");
        jdbcTemplate.update("INSERT INTO users(login, password, appointment, phone, email, permissions) VALUES (?, ?, ?, ?, ?, ?::text[])",
            "test2", passwordEncoder.encode("test2"), "test2", "+79998880000", "test2@edms.ru", "{SIGN, VIEW}");
        jdbcTemplate.update("INSERT INTO users(login, password, appointment, phone, email, permissions) VALUES (?, ?, ?, ?, ?, ?::text[])",
            "test3", passwordEncoder.encode("test3"), "test3", "+79998880000", "test3@edms.ru", "{SIGN, VIEW}");
        jdbcTemplate.update("INSERT INTO users(login, password, appointment, phone, email, permissions) VALUES (?, ?, ?, ?, ?, ?::text[])",
            "test4", passwordEncoder.encode("test4"), "test4", "+79998880000", "test4@edms.ru", "{SIGN, VIEW}");
        jdbcTemplate.update("INSERT INTO users(login, password, appointment, phone, email, permissions) VALUES (?, ?, ?, ?, ?, ?::text[])",
            "test5", passwordEncoder.encode("test5"), "test5", "+79998880000", "test5@edms.ru", "{VIEW}");

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
        String vt3 = mockMvc.perform(
            post("/auth/authenticate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new AuthenticateRequest("test3", "test3")))
        ).andReturn().getResponse().getContentAsString();
        String vt4 = mockMvc.perform(
            post("/auth/authenticate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new AuthenticateRequest("test4", "test4")))
        ).andReturn().getResponse().getContentAsString();
        String vt5 = mockMvc.perform(
            post("/auth/authenticate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new AuthenticateRequest("test5", "test5")))
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
        authUser3 = mockMvc.perform(
            post("/auth/verify")
                .header("Authorization", "Bearer " + vt3)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new ConfirmRequest("111111")))
        ).andReturn().getResponse().getContentAsString();
        authUser4 = mockMvc.perform(
            post("/auth/verify")
                .header("Authorization", "Bearer " + vt4)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new ConfirmRequest("111111")))
        ).andReturn().getResponse().getContentAsString();
        authUser5 = mockMvc.perform(
            post("/auth/verify")
                .header("Authorization", "Bearer " + vt5)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new ConfirmRequest("111111")))
        ).andReturn().getResponse().getContentAsString();

        mockMvc.perform(
            post("/user/eds")
                .header("Authorization", "Bearer " + authUser2)
        ).andExpect(status().isOk());
        mockMvc.perform(
            post("/user/eds")
                .header("Authorization", "Bearer " + authUser3)
        ).andExpect(status().isOk());

        mockMvc.perform(
            multipart("/docs/upload/signers")
                .file(new MockMultipartFile(
                    "files", "hello.docx",
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                    this.getClass().getClassLoader().getResourceAsStream("docs/hello.docx").readAllBytes()
                ))
                .param("signers", "3, 4")
                .header("Authorization", "Bearer " + authUser1)
        ).andExpect(status().isOk());
        mockMvc.perform(
            multipart("/docs/upload/signers")
                .file(new MockMultipartFile(
                    "files", "hello.docx",
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                    this.getClass().getClassLoader().getResourceAsStream("docs/hello.docx").readAllBytes()
                ))
                .param("signers", "5")
                .header("Authorization", "Bearer " + authUser1)
        ).andExpect(status().isOk());

        mockMvc.perform(
            multipart("/docs/upload/signers")
                .file(new MockMultipartFile(
                    "files", "hello.pdf",
                    "application/pdf",
                    this.getClass().getClassLoader().getResourceAsStream("docs/hello.pdf").readAllBytes()
                ))
                .param("signers", "3, 4")
                .header("Authorization", "Bearer " + authUser1)
        ).andExpect(status().isOk());
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
    public void testNoPermissionToSign() throws Exception {
        mockMvc.perform(
            post("/docs/1/sign")
                .header("Authorization", "Bearer " + authUser5)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new SignRequest("Утверждено.")))
        ).andExpect(status().isForbidden());
    }

    @Test
    public void testInvalidUserChainSign() throws Exception {
        mockMvc.perform(
            post("/docs/1/sign")
                .header("Authorization", "Bearer " + authUser3)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new SignRequest("Утверждено.")))
        ).andExpect(status().isForbidden());
    }

    @Test
    public void testAbsentUserEsignTest() throws Exception {
        mockMvc.perform(
            post("/docs/2/sign")
                .header("Authorization", "Bearer " + authUser4)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new SignRequest("Утверждено.")))
        ).andExpect(status().isForbidden());
    }

    @Test
    public void testSuccessfullySignOpenXMLDocument() throws Exception {
        mockMvc.perform(
            post("/docs/1/sign")
                .header("Authorization", "Bearer " + authUser2)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new SignRequest("Утверждено.")))
        ).andExpect(status().isOk()).andDo(MockMvcResultHandlers.print());

        mockMvc.perform(
            post("/docs/1/sign")
                .header("Authorization", "Bearer " + authUser3)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new SignRequest("Утверждено.")))
        ).andExpect(status().isOk())
        .andExpect(jsonPath("$.signed").value("true"));

        verify(emailService, times(1)).sendMessage(eq("test3@edms.ru"), anyString(), anyString());

        byte[] signedFile = mockMvc.perform(
            get("/docs/1")
                .header("Authorization", "Bearer " + authUser1)
        ).andReturn().getResponse().getContentAsByteArray();

        assertTrue(openXMLResourceSigner.validateSign(new ByteArrayResource(signedFile)));
    }

    @Test
    public void testSuccessfullySignPdfDocument() throws Exception {
        mockMvc.perform(
            post("/docs/3/sign")
                .header("Authorization", "Bearer " + authUser2)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new SignRequest("Утверждено.")))
        ).andExpect(status().isOk()).andDo(MockMvcResultHandlers.print());

        mockMvc.perform(
            post("/docs/3/sign")
                .header("Authorization", "Bearer " + authUser3)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new SignRequest("Утверждено.")))
        ).andExpect(status().isOk())
        .andExpect(jsonPath("$.signed").value("true"));

        verify(emailService, times(1)).sendMessage(eq("test3@edms.ru"), anyString(), anyString());

        byte[] signedFile = mockMvc.perform(
            get("/docs/3")
                .header("Authorization", "Bearer " + authUser1)
        ).andReturn().getResponse().getContentAsByteArray();

        assertTrue(pdfResourceSigner.validateSign(new ByteArrayResource(signedFile)));
    }
}

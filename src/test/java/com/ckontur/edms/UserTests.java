package com.ckontur.edms;

import com.ckontur.edms.component.SpringBootPostgreSQLContainerTests;
import com.ckontur.edms.component.auth.VerifyCodeGenerator;
import com.ckontur.edms.model.Permission;
import com.ckontur.edms.model.User;
import com.ckontur.edms.web.AuthenticateRequest;
import com.ckontur.edms.web.ConfirmRequest;
import com.ckontur.edms.web.UserRequests;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vavr.collection.HashSet;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.testng.Assert.assertEquals;

@Slf4j
public class UserTests extends SpringBootPostgreSQLContainerTests {
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

    private String accessToken;
    private String adminToken;

    @BeforeClass
    void beforeClass() throws Exception {
        given(verifyCodeGenerator.generate()).willReturn("111111");
        jdbcTemplate.update("INSERT INTO users(login, password, appointment, phone, email, permissions) VALUES (?, ?, ?, ?, ?, ?::text[])",
            "test1", passwordEncoder.encode("test1"), "test1", "+79998880000", "test1@edms.ru", "{SIGN, VIEW, UPLOAD}");
        jdbcTemplate.update("INSERT INTO users(login, password, appointment, phone, email, permissions) VALUES (?, ?, ?, ?, ?, ?::text[])",
            "test2", passwordEncoder.encode("test2"), "test2", "+79998880000", "test2@edms.ru", "{SIGN, VIEW}");

        String va1 = mockMvc.perform(
            post("/auth/authenticate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new AuthenticateRequest("admin", "admin123")))
        ).andReturn().getResponse().getContentAsString();
        String vt1 = mockMvc.perform(
            post("/auth/authenticate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new AuthenticateRequest("test1", "test1")))
        ).andReturn().getResponse().getContentAsString();

        accessToken = mockMvc.perform(
            post("/auth/verify")
                .header("Authorization", "Bearer " + vt1)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new ConfirmRequest("111111")))
        ).andReturn().getResponse().getContentAsString();
        adminToken = mockMvc.perform(
            post("/auth/verify")
                .header("Authorization", "Bearer " + va1)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new ConfirmRequest("111111")))
        ).andReturn().getResponse().getContentAsString();
    }

    @AfterClass
    void afterClass() {
        jdbcTemplate.execute("TRUNCATE users CASCADE");
    }

    @AfterTest
    void afterTest() {
        jdbcTemplate.execute("TRUNCATE keys");
    }

    @Test
    public void testNotFoundUsers() throws Exception {
        mockMvc.perform(
            get("/user/search")
                .header("Authorization", "Bearer " + accessToken)
                .queryParam("search", "abcdefg")
        ).andExpect(status().isOk())
        .andExpect(jsonPath("$.total").value(0));
    }

    @Test
    public void testFoundUsers() throws Exception {
        mockMvc.perform(
            get("/user/search")
                .header("Authorization", "Bearer " + accessToken)
                .queryParam("search", "test")
        ).andExpect(status().isOk())
        .andExpect(jsonPath("$.total").value(2));
    }

    @Test
    public void testGenerateKeyPair() throws Exception {
        mockMvc.perform(
            post("/user/eds")
                .header("Authorization", "Bearer " + accessToken)
        ).andExpect(status().isOk())
        .andExpect(jsonPath("$.privateKey").value("**********"))
        .andExpect(jsonPath("$.publicKey").isNotEmpty())
        .andExpect(jsonPath("$.x509Certificate").isNotEmpty())
        .andDo(MockMvcResultHandlers.print());
    }

    @Test
    public void testGetTheSameKeyPairAfterGenerate() throws Exception {
        String ckp = mockMvc.perform(
            post("/user/eds")
                .header("Authorization", "Bearer " + accessToken)
        ).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        String ckpRetry = mockMvc.perform(
            post("/user/eds")
                .header("Authorization", "Bearer " + accessToken)
        ).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        assertEquals(ckpRetry, ckp);
    }

    @Test
    public void testCreateUserForbidden() throws Exception {
        mockMvc.perform(
            post("/user")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new UserRequests.CreateUser(
                    "abc123", "abc123", "abc", "abc", "abc", "abc",
                        "+79001231234", "abc@edms.ru", HashSet.of(Permission.SIGN, Permission.VIEW)
                )))
        ).andExpect(status().isForbidden());
    }

    @Test
    public void testCreateUserInvalidLogin() throws Exception {
        mockMvc.perform(
            post("/user")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new UserRequests.CreateUser(
                    "ab", "abc123", "abc", "abc", "abc", "abc",
                    "+79001231234", "abc@edms.ru", HashSet.of(Permission.SIGN, Permission.VIEW)
            )))
        ).andExpect(status().isBadRequest());
    }

    @Test
    public void testCreateUserInvalidPassword() throws Exception {
        mockMvc.perform(
            post("/user")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new UserRequests.CreateUser(
                        "abc123", "abc", "abc", "abc", "abc", "abc",
                        "+79001231234", "abc@edms.ru", HashSet.of(Permission.SIGN, Permission.VIEW)
                )))
        ).andExpect(status().isBadRequest());
    }

    @Test
    public void testCreateUserInvalidPhone() throws Exception {
        mockMvc.perform(
            post("/user")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new UserRequests.CreateUser(
                    "abc123", "abc123", "abc", "abc", "abc", "abc",
                    "+79001", "abc@edms.ru", HashSet.of(Permission.SIGN, Permission.VIEW)
                )))
        ).andExpect(status().isBadRequest());
    }

    @Test
    public void testCreateUserInvalidEmail() throws Exception {
        mockMvc.perform(
            post("/user")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new UserRequests.CreateUser(
                    "abc123", "abc123", "abc", "abc", "abc", "abc",
                    "+79001231234", "abc", HashSet.of(Permission.SIGN, Permission.VIEW)
                )))
        ).andExpect(status().isBadRequest());
    }

    @Test
    public void testCreateUserSuccessfully() throws Exception {
        String response = mockMvc.perform(
            post("/user")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new UserRequests.CreateUser(
                    "abc123", "abc123", "abc", "abc", "abc", "abc",
                    "+79001231234", "abc@edms.ru", HashSet.of(Permission.SIGN, Permission.VIEW)
                )))
        ).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        Long id = objectMapper.readValue(response, User.class).getId();
        mockMvc.perform(
            get("/user/" + id)
                .header("Authorization", "Bearer " + adminToken)
        ).andExpect(status().isOk());
    }

    @Test
    public void testUpdateUserForbidden() throws Exception {
        String response = mockMvc.perform(
            post("/user")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new UserRequests.CreateUser(
                    "abc123456", "abc123456", "abc", "abc", "abc", "abc",
                    "+79001231234", "abc@edms.ru", HashSet.of(Permission.SIGN, Permission.VIEW)
                )))
        ).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        Long id = objectMapper.readValue(response, User.class).getId();

        mockMvc.perform(
            put("/user/" + id)
               .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new UserRequests.UpdateUser(
                    null, null, "updated", null, null, null, null, null, null
                )))
        ).andExpect(status().isForbidden());
    }

    @Test
    public void testUpdateUserInvalidLogin() throws Exception {
        String response = mockMvc.perform(
            post("/user")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new UserRequests.CreateUser(
                    "abc1234567", "abc1234567", "abc", "abc", "abc", "abc",
                    "+79001231234", "abc@edms.ru", HashSet.of(Permission.SIGN, Permission.VIEW)
                )))
        ).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        Long id = objectMapper.readValue(response, User.class).getId();

        mockMvc.perform(
            put("/user/" + id)
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new UserRequests.UpdateUser(
                    "ab", null, "updated", null, null, null, null, null, null
                )))
        ).andExpect(status().isBadRequest());
    }

    @Test
    public void testUpdateUserInvalidPassword() throws Exception {
        String response = mockMvc.perform(
            post("/user")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new UserRequests.CreateUser(
                    "abc12345678", "abc12345678", "abc", "abc", "abc", "abc",
                    "+79001231234", "abc@edms.ru", HashSet.of(Permission.SIGN, Permission.VIEW)
                )))
        ).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        Long id = objectMapper.readValue(response, User.class).getId();

        mockMvc.perform(
            put("/user/" + id)
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new UserRequests.UpdateUser(
                    null, "abc", "updated", null, null, null, null, null, null
                )))
        ).andExpect(status().isBadRequest());
    }

    @Test
    public void testUpdateUserNotFound() throws Exception {
        mockMvc.perform(
            put("/user/1000")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new UserRequests.UpdateUser(
                        null, null, "updated", null, null, null, null, null, null
                )))
        ).andExpect(status().isNotFound());
    }

    @Test
    public void testUpdateUserSuccessfully() throws Exception {
        String user = mockMvc.perform(
            post("/user")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new UserRequests.CreateUser(
                    "abc1234321", "abc1234321", "abc", "abc", "abc", "abc",
                    "+79001231234", "abc@edms.ru", HashSet.of(Permission.SIGN, Permission.VIEW)
                )))
        ).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        Long id = objectMapper.readValue(user, User.class).getId();

        String response = mockMvc.perform(
            put("/user/" + id)
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new UserRequests.UpdateUser(
                    null, null, "updated", null, null, null, null, null, null
                )))
        ).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        String name = objectMapper.readValue(response, User.class).getFirstName();
        assertEquals(name, "updated");

        String updatedUser = mockMvc.perform(
            get("/user/" + id)
                .header("Authorization", "Bearer " + adminToken)
        ).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        String updatedName = objectMapper.readValue(response, User.class).getFirstName();
        assertEquals(updatedName, "updated");
    }

    @Test
    public void testDeleteUserForbidden() throws Exception {
        String response = mockMvc.perform(
            post("/user")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new UserRequests.CreateUser(
                        "abc1234", "abc1234", "abc", "abc", "abc", "abc",
                        "+79001231234", "abc@edms.ru", HashSet.of(Permission.SIGN, Permission.VIEW)
                )))
        ).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        Long id = objectMapper.readValue(response, User.class).getId();

        mockMvc.perform(
            delete("/user/" + id)
                .header("Authorization", "Bearer " + accessToken)
        ).andExpect(status().isForbidden());
    }

    @Test
    public void testDeleteUserNotFound() throws Exception {
        mockMvc.perform(
            delete("/user/1000")
                .header("Authorization", "Bearer " + adminToken)
        ).andExpect(status().isNotFound());
    }

    @Test
    public void testDeleteUserSuccessfully() throws Exception {
        String response = mockMvc.perform(
            post("/user")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new UserRequests.CreateUser(
                    "abc12345", "abc12345", "abc", "abc", "abc", "abc",
                    "+79001231234", "abc@edms.ru", HashSet.of(Permission.SIGN, Permission.VIEW)
                )))
        ).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        Long id = objectMapper.readValue(response, User.class).getId();

        mockMvc.perform(
            delete("/user/" + id)
                .header("Authorization", "Bearer " + adminToken)
        ).andExpect(status().isOk());

        mockMvc.perform(
            get("/user/" + id)
                .header("Authorization", "Bearer " + adminToken)
        ).andExpect(status().isNotFound());
    }

}

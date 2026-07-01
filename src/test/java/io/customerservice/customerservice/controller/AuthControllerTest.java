package io.customerservice.customerservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.customerservice.customerservice.dto.request.AgreementRequest;

import io.customerservice.customerservice.dto.request.LoginRequest;
import io.customerservice.customerservice.dto.request.RefreshRequest;
import io.customerservice.customerservice.dto.request.RegisterRequest;
import io.customerservice.customerservice.dto.response.AuthResponse;
import io.customerservice.customerservice.dto.response.MessageResponse;
import io.customerservice.customerservice.entity.AgreementType;
import io.customerservice.customerservice.exception.EmailAlreadyExistsException;
import io.customerservice.customerservice.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class AuthControllerTest {

    @Autowired WebApplicationContext context;

    private final ObjectMapper objectMapper = new ObjectMapper();

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
    }

    @MockitoBean AuthService authService;

    private static final List<AgreementRequest> MANDATORY_AGREEMENTS = List.of(
            new AgreementRequest(AgreementType.TERMS_OF_SERVICE, 1, true),
            new AgreementRequest(AgreementType.PRIVACY_POLICY, 1, true),
            new AgreementRequest(AgreementType.LOYALTY_PROGRAM_TERMS, 1, true)
    );

    @Test
    void register_success_returns201() throws Exception {
        RegisterRequest req = new RegisterRequest(
                "new@example.com", "Password1!", "Test", "User", "+1234567890", MANDATORY_AGREEMENTS);
        when(authService.register(any())).thenReturn(new MessageResponse("Registration successful"));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Registration successful"));
    }

    @Test
    void register_missingEmail_returns400() throws Exception {
        String body = """
                {"password":"Password1!","firstName":"Test","lastName":"User","phone":"+1234567890","agreements":[]}
                """;
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_duplicateEmail_returns409() throws Exception {
        RegisterRequest req = new RegisterRequest(
                "taken@example.com", "Password1!", "Test", "User", "+1234567890", MANDATORY_AGREEMENTS);
        when(authService.register(any())).thenThrow(new EmailAlreadyExistsException());

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict());
    }

    @Test
    void login_success_returns200() throws Exception {
        when(authService.login(any())).thenReturn(new AuthResponse("access-token", "refresh-token"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("user@example.com", "Password1!"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token"));
    }

    @Test
    void login_badCredentials_returns401() throws Exception {
        when(authService.login(any())).thenThrow(new BadCredentialsException(""));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("user@example.com", "wrong"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refresh_success_returns200() throws Exception {
        when(authService.refresh(any())).thenReturn(new AuthResponse("new-access", "new-refresh"));

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshRequest("old-token"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new-access"));
    }

    @Test
    void logout_success_returns204() throws Exception {
        mockMvc.perform(delete("/api/v1/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshRequest("token"))))
                .andExpect(status().isNoContent());
    }
}

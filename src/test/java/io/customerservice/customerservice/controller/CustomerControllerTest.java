package io.customerservice.customerservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.customerservice.customerservice.dto.request.UpdateProfileRequest;
import io.customerservice.customerservice.dto.response.CustomerResponse;
import io.customerservice.customerservice.entity.Role;
import io.customerservice.customerservice.service.CustomerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class CustomerControllerTest {

    @Autowired WebApplicationContext context;

    private final ObjectMapper objectMapper = new ObjectMapper();

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
    }

    @MockitoBean CustomerService customerService;

    private static final CustomerResponse CUSTOMER_RESPONSE = new CustomerResponse(
            1L, "test@example.com", "Test", "User", "+1234567890",
            Role.CUSTOMER, true, OffsetDateTime.now(), OffsetDateTime.now()
    );

    @Test
    void getProfile_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/customers/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getProfile_authenticated_returns200() throws Exception {
        when(customerService.getProfile(1L)).thenReturn(CUSTOMER_RESPONSE);

        mockMvc.perform(get("/api/v1/customers/me")
                        .with(customerAuth(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("test@example.com"));
    }

    @Test
    void updateProfile_authenticated_returns200() throws Exception {
        UpdateProfileRequest req = new UpdateProfileRequest("New", "Name", "+9999999999");
        when(customerService.updateProfile(1L, req)).thenReturn(CUSTOMER_RESPONSE);

        mockMvc.perform(patch("/api/v1/customers/me")
                        .with(customerAuth(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test
    void deleteProfile_authenticated_returns204() throws Exception {
        mockMvc.perform(delete("/api/v1/customers/me")
                        .with(customerAuth(1L)))
                .andExpect(status().isNoContent());
    }

    private static org.springframework.test.web.servlet.request.RequestPostProcessor customerAuth(Long customerId) {
        return authentication(new UsernamePasswordAuthenticationToken(
                customerId, null, List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))));
    }
}

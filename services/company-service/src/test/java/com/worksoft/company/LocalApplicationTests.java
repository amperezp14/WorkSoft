package com.worksoft.company;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.context.ApplicationContext;
import org.springframework.test.web.servlet.MockMvc;
import javax.sql.DataSource;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class LocalApplicationTests {
    @Autowired ApplicationContext context;
    @Autowired MockMvc mvc;
    @Test void startsWithoutDatabaseSecretsOrIdentityProvider() throws Exception {
        assertThat(context.getBeansOfType(DataSource.class)).isEmpty();
        assertThat(context.getBeansOfType(JwtDecoder.class)).isEmpty();
        mvc.perform(get("/actuator/health")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("UP"));
        assertThat(context.getEnvironment().getProperty("server.address")).isEqualTo("127.0.0.1");
    }
    @Test void createsAndReplaysWithoutAuthentication() throws Exception {
        String body="{\"legalName\":\"Ejemplo local\",\"nit\":\"000001\",\"verificationDigit\":\"0\"}";
        var first=mvc.perform(post("/api/v1/companies").header("Idempotency-Key","local-test-key-0001")
            .contentType("application/json").content(body)).andExpect(status().isCreated()).andReturn();
        var second=mvc.perform(post("/api/v1/companies").header("Idempotency-Key","local-test-key-0001")
            .contentType("application/json").content(body)).andExpect(status().isCreated()).andReturn();
        assertThat(first.getResponse().getContentAsString()).isEqualTo(second.getResponse().getContentAsString());
        mvc.perform(get(first.getResponse().getHeader("Location"))).andExpect(status().isOk()).andExpect(jsonPath("$.nit").value("000001"));
    }
    @Test void listWorksWithoutToken() throws Exception {
        mvc.perform(get("/api/v1/companies")).andExpect(status().isOk()).andExpect(jsonPath("$.items").isArray());
    }
}

package com.orderhub;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;
import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@Transactional
class OrderApiTest {
    @Container static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");
    @DynamicPropertySource static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    private long create(String username) throws Exception {
        var response = mvc.perform(post("/api/orders").with(user(username).roles("USER"))
            .contentType("application/json").content("{\"lines\":[{\"productId\":1,\"quantity\":2},{\"productId\":2,\"quantity\":1}]}"))
            .andExpect(status().isCreated()).andExpect(header().string("Location", startsWith("/api/orders/")))
            .andExpect(jsonPath("$.status").value("NEW")).andExpect(jsonPath("$.total").value(597.90))
            .andExpect(jsonPath("$.lines", hasSize(2))).andReturn().getResponse().getContentAsString();
        return json.readTree(response).get("id").asLong();
    }
    @Test void fullOrderLifecycleAndFiltering() throws Exception {
        long id = create("user");
        mvc.perform(get("/api/orders/{id}", id).with(httpBasic("user", "user-local")))
            .andExpect(status().isOk()).andExpect(jsonPath("$.customerId").value(1));
        for (String next : new String[]{"PAID", "SHIPPED", "CANCELLED"}) {
            mvc.perform(patch("/api/orders/{id}/status", id).with(httpBasic("admin", "admin-local"))
                .contentType("application/json").content("{\"status\":\"" + next + "\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value(next));
        }
        mvc.perform(get("/api/orders?status=CANCELLED&page=0&size=1").with(user("user")))
            .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.content[0].id").value(id));
        mvc.perform(get("/api/orders?status=NEW").with(user("user")))
            .andExpect(status().isOk()).andExpect(jsonPath("$.content", hasSize(0)));
    }
    @Test void illegalTransitionReturnsProblemAndDoesNotMutateOrder() throws Exception {
        long id = create("user");
        mvc.perform(patch("/api/orders/{id}/status", id).with(user("admin").roles("ADMIN"))
            .contentType("application/json").content("{\"status\":\"SHIPPED\"}"))
            .andExpect(status().isConflict()).andExpect(content().contentTypeCompatibleWith("application/problem+json"));
        mvc.perform(get("/api/orders/{id}", id).with(user("user")))
            .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("NEW"));
    }
    @Test void authenticationAndRoleBoundaries() throws Exception {
        mvc.perform(get("/api/orders")).andExpect(status().isUnauthorized())
            .andExpect(content().contentTypeCompatibleWith("application/problem+json"));
        mvc.perform(get("/api/orders").with(httpBasic("user", "wrong"))).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/customers").with(httpBasic("user", "user-local"))).andExpect(status().isForbidden());
        mvc.perform(get("/api/customers").with(httpBasic("admin", "admin-local"))).andExpect(status().isOk());
        long id = create("user");
        mvc.perform(patch("/api/orders/{id}/status", id).with(user("user"))
            .contentType("application/json").content("{\"status\":\"PAID\"}"))
            .andExpect(status().isForbidden()).andExpect(content().contentTypeCompatibleWith("application/problem+json"));
        mvc.perform(post("/api/products").with(user("user")).contentType("application/json")
            .content("{\"sku\":\"TEST\",\"name\":\"Test\",\"price\":1.20}"))
            .andExpect(status().isForbidden());
    }
    @Test void usersCannotReadOtherCustomersOrders() throws Exception {
        long id = create("other");
        mvc.perform(get("/api/orders/{id}", id).with(user("user"))).andExpect(status().isNotFound());
        mvc.perform(get("/api/orders").with(user("user"))).andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(get("/api/orders/{id}", id).with(user("admin").roles("ADMIN"))).andExpect(status().isOk());
        mvc.perform(get("/api/orders").with(user("admin").roles("ADMIN"))).andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1));
    }
    @Test void validatesPayloadsAndQueryParameters() throws Exception {
        for (String payload : new String[]{"{}", "{\"lines\":[]}", "{\"lines\":[null]}",
            "{\"lines\":[{\"productId\":1,\"quantity\":0}]}",
            "{\"lines\":[{\"productId\":1,\"quantity\":1},{\"productId\":1,\"quantity\":2}]}"}) {
            mvc.perform(post("/api/orders").with(user("user")).contentType("application/json").content(payload))
                .andExpect(status().isBadRequest()).andExpect(content().contentTypeCompatibleWith("application/problem+json"));
        }
        for (String query : new String[]{"size=0", "size=101", "page=-1", "status=INVALID", "page=nope"}) {
            mvc.perform(get("/api/orders?" + query).with(user("user"))).andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"));
        }
        mvc.perform(post("/api/orders").with(user("user")).contentType("application/json")
            .content("{\"lines\":[{\"productId\":999999,\"quantity\":1}]}"))
            .andExpect(status().isNotFound());
    }
    @Test void catalogAndCustomerEndpoints() throws Exception {
        mvc.perform(get("/api/customers/me").with(user("user"))).andExpect(status().isOk()).andExpect(jsonPath("$.username").value("user"));
        mvc.perform(get("/api/products").with(user("user"))).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(2)));
        mvc.perform(post("/api/products").with(user("admin").roles("ADMIN")).contentType("application/json")
            .content("{\"sku\":\"TEST\",\"name\":\"Test product\",\"price\":12.34}"))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.price").value(12.34));
    }
    @Test void openApiAndHealthArePublic() throws Exception {
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
            .andExpect(jsonPath("$.paths['/api/orders']").exists())
            .andExpect(jsonPath("$.components.securitySchemes.basicAuth.scheme").value("basic"));
        mvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
        mvc.perform(get("/actuator/health")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("UP"));
    }
}

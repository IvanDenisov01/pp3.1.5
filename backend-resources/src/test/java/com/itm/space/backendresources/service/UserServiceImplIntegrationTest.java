package com.itm.space.backendresources.service;

import com.itm.space.backendresources.BaseIntegrationTest;
import com.itm.space.backendresources.api.request.UserRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class UserServiceImplIntegrationTest extends BaseIntegrationTest {

    private final Keycloak keycloak;
    private String createdUserId;

    @Autowired
    UserServiceImplIntegrationTest(Keycloak keycloak) {
        this.keycloak = keycloak;
    }

    @Test
    @WithMockUser(roles = "MODERATOR")
    @DisplayName("Создание пользователя")
    void createUser() throws Exception {
        UserRequest userRequest = new UserRequest(
                "testUser",
                "test@test.com",
                "password",
                "firstName",
                "lastName"
        );

        mvc.perform(requestWithContent(post("/api/users"), userRequest))
                .andExpect(status().isOk());

        createdUserId = findUserIdByUsername(userRequest.getUsername());
    }

    @Test
    @WithMockUser(roles = "MODERATOR")
    @DisplayName("Создание уже существующего пользователя")
    void createUserAlreadyExists() throws Exception {
        UserRequest userRequest = new UserRequest(
                "testUser",
                "test@test.com",
                "password",
                "firstName",
                "lastName"
        );

        mvc.perform(requestWithContent(post("/api/users"), userRequest))
                .andExpect(status().isOk());

        mvc.perform(requestWithContent(post("/api/users"), userRequest))
                .andDo(print())
                .andExpect(status().isConflict());

        createdUserId = findUserIdByUsername(userRequest.getUsername());
    }

    @Test
    @WithMockUser(roles = "MODERATOR")
    @DisplayName("Получение уже существующего пользователя")
    void getUserByIdExists() throws Exception {
        UUID userId = UUID.fromString("1539372b-ef5c-4981-99e6-cc3be3c946e5");

        mvc.perform(requestToJson(get("/api/users/{id}", userId)))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "MODERATOR")
    @DisplayName("Получение не существующего пользователя")
    void getUserByIdNotExists() throws Exception {
        UUID userId = UUID.randomUUID();

        mvc.perform(requestToJson(get("/api/users/{id}", userId)))
                .andExpect(status().is5xxServerError());
    }

    @AfterEach
    public void cleanUp() {
        if (createdUserId != null) {
            keycloak.realm("ITM").users().get(createdUserId).remove();
            createdUserId = null;
        }
    }

    private String findUserIdByUsername(String username) {
        return keycloak.realm("ITM")
                .users()
                .search(username)
                .get(0)
                .getId();
    }
}
package com.itm.space.backendresources.controller;

import com.itm.space.backendresources.BaseIntegrationTest;
import com.itm.space.backendresources.api.request.UserRequest;
import com.itm.space.backendresources.api.response.UserResponse;
import com.itm.space.backendresources.exception.BackendResourcesException;
import com.itm.space.backendresources.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class UserControllerIntegrationTest extends BaseIntegrationTest {

    @MockBean
    private UserService userService;

    @Test
    @WithMockUser(roles = {"MODERATOR"})
    @DisplayName("Добавление пользователя")
    void CreateUser() throws Exception {
        UserRequest userRequest = new UserRequest(
                "testuser",
                "test@test.com",
                "password",
                "firstName",
                "lastName"
        );

        mvc.perform(requestWithContent(post("/api/users"), userRequest))
                .andExpect(status().isOk());

        verify(userService, times(1)).createUser(any(UserRequest.class));
    }

    @Test
    @WithMockUser(roles = {"MODERATOR"})
    @DisplayName("Ошибка валидации при добавлении пользователя")
    void CreateUserValidationError() throws Exception {
        UserRequest invalidUserRequest = new UserRequest(
                "",
                "email",
                "pwd",
                "firstName",
                "lastName"
        );

        mvc.perform(requestWithContent(post("/api/users"), invalidUserRequest))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.username").value("Username should not be blank"))
                .andExpect(jsonPath("$.email").value("Email should be valid"))
                .andExpect(jsonPath("$.password").value("Password should be greater than 4 characters long"));
    }

    @Test
    @WithMockUser(roles = {"MODERATOR"})
    @DisplayName("Получение пользователя по id")
    void GetUserById() throws Exception {
        UUID userId = UUID.randomUUID();
        UserResponse userResponse = new UserResponse(
                "firstName",
                "lastName",
                "test@test.com",
                List.of("ROLE_USER"),
                List.of("GROUP_A", "GROUP_B")
        );

        when(userService.getUserById(userId)).thenReturn(userResponse);

        mvc.perform(get("/api/users/{id}", userId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("firstName"))
                .andExpect(jsonPath("$.email").value("test@test.com"));
    }

    @Test
    @WithMockUser(roles = {"MODERATOR"})
    @DisplayName("Ошибка: пользователь не найден")
    void GetUserByIdNotFound() throws Exception {
        UUID userId = UUID.randomUUID();

        when(userService.getUserById(userId))
                .thenThrow(new BackendResourcesException("User not found", HttpStatus.NOT_FOUND));

        mvc.perform(get("/api/users/{id}", userId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }


    @Test
    @WithMockUser(roles = {"MODERATOR"})
    @DisplayName("Ошибка: некорректный формат ID")
    void GetUserByInvalidIdFormat() throws Exception {
        mvc.perform(get("/api/users/{id}", "invalid-uuid")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = {"USER"})
    @DisplayName("Ошибка: недостаточно прав для доступа")
    void GetUserByIdAccessDenied() throws Exception {
        UUID userId = UUID.randomUUID();

        mvc.perform(get("/api/users/{id}", userId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "testuser", roles = {"MODERATOR"})
    @DisplayName("Проверка авторизованного пользователя")
    void shouldHello() throws Exception {
        mvc.perform(get("/api/users/hello")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value("testuser"));
    }
}

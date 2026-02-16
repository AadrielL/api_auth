package com.projeto.orcamento.demo.dto;

import com.projeto.orcamento.demo.model.User;
import com.projeto.orcamento.demo.model.UserRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

// Agrupando todos os contratos de dados em um único lugar para agilizar
public class AuthDTOs {

    public record AuthenticationDTO(@NotBlank String email, @NotBlank String password) {}

    public record LoginResponseDTO(String token) {}

    public record RegisterDTO(
            @NotBlank String nome,
            @NotBlank String email,
            @NotBlank String password,
            @NotNull UserRole role
    ) {}

    public record UserResponseDTO(String id, String nome, String email, String role, String tenantId) {
        public UserResponseDTO(User user) {
            this(user.getId(), user.getNome(), user.getEmail(), user.getRole().toString(), user.getTenantId());
        }
    }
}
package com.projeto.orcamento.demo.dto;

import com.projeto.orcamento.demo.model.User;
import jakarta.validation.constraints.NotBlank;

public class AuthDTOs {

    public record AuthenticationDTO(@NotBlank String email, @NotBlank String password) {}

    // ATUALIZADO: Agora envia tudo que o Angular precisa para o saveSession()
    public record LoginResponseDTO(
            String token,
            String role,
            String nome,
            String email
    ) {}

    public record RegisterDTO(
            @NotBlank String nome,
            @NotBlank String email,
            @NotBlank String password
    ) {}

    public record UserResponseDTO(String id, String nome, String email, String role, String tenantId) {
        public UserResponseDTO(User user) {
            this(user.getId(), user.getNome(), user.getEmail(), user.getRole().toString(), user.getTenantId());
        }
    }
}
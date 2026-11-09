package com.projeto.orcamento.demo.dto;

import com.projeto.orcamento.demo.model.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AuthDTOs {

    public record AuthenticationDTO(
            @NotBlank(message = "E-mail é obrigatório") 
            @Email(message = "Formato de e-mail inválido")
            String email, 
            
            @NotBlank(message = "Senha é obrigatória") 
            String password
    ) {}

    public record LoginResponseDTO(
            String token,
            String role,
            String nome,
            String email,
            String tipoPerfil,
            String tenantId
    ) {
        public LoginResponseDTO(String token, String role, String nome, String email) {
            this(token, role, nome, email, "ELETRICISTA", null);
        }
    }

    public record RegisterDTO(
            @NotBlank(message = "Nome é obrigatório") 
            String nome,

            @NotBlank(message = "E-mail é obrigatório") 
            @Email(message = "Formato de e-mail inválido")
            String email,

            @NotBlank(message = "Senha é obrigatória")
            @Size(min = 8, message = "A senha deve ter no mínimo 8 caracteres")
            String password,

            String tipoPerfil, // ELETRICISTA ou CLIENTE
            String eletricistaVinculadoId // Opcional: ID ou Tenant do parceiro escolhido
    ) {}

    public record UserResponseDTO(
            String id, 
            String nome, 
            String email, 
            String role, 
            String tenantId, 
            String planType,
            String tipoPerfil,
            String eletricistaVinculadoId
    ) {
        public UserResponseDTO(User user) {
            this(
                user.getId(), 
                user.getNome(), 
                user.getEmail(), 
                user.getRole().toString(), 
                user.getTenantId(), 
                user.getPlanType() != null ? user.getPlanType() : "FREE",
                user.getTipoPerfil() != null ? user.getTipoPerfil() : "ELETRICISTA",
                user.getEletricistaVinculadoId()
            );
        }
    }

    public record ParceiroDestaqueDTO(
            String id,
            String nome,
            String tenantId,
            String regiao,
            Double avaliacao,
            Integer totalAvaliacoes,
            java.util.List<String> especialidades,
            Boolean inicianteDestaque,
            String tempoResposta,
            Boolean seloNbr
    ) {}
}

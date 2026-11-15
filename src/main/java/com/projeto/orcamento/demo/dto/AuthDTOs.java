package com.projeto.orcamento.demo.dto;

import com.projeto.orcamento.demo.model.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AuthDTOs {

    public record VerificarCodigoDTO(
            @NotBlank(message = "E-mail é obrigatório")
            @Email(message = "Formato de e-mail inválido")
            String email,

            @NotBlank(message = "Código é obrigatório")
            @Size(min = 6, max = 6, message = "O código deve conter 6 dígitos")
            String codigo,

            String codigoSms
    ) {}

    public record ReenviarCodigoDTO(
            @NotBlank(message = "E-mail é obrigatório")
            @Email(message = "Formato de e-mail inválido")
            String email,

            String canal // EMAIL, SMS ou AMBOS
    ) {}

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
            String eletricistaVinculadoId, // Opcional: ID ou Tenant do parceiro escolhido

            // Ficha Cadastral & Geolocalização
            String cpf,
            String telefone,
            String cep,
            String estado,
            String cidade,
            String bairro
    ) {}

    public record UserResponseDTO(
            String id, 
            String nome, 
            String email, 
            String role, 
            String tenantId, 
            String planType,
            String tipoPerfil,
            String eletricistaVinculadoId,
            String cpf,
            String telefone,
            String cep,
            String estado,
            String cidade,
            String bairro,
            Boolean telefoneVerificado
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
                user.getEletricistaVinculadoId(),
                user.getCpf(),
                user.getTelefone(),
                user.getCep(),
                user.getEstado(),
                user.getCidade(),
                user.getBairro(),
                user.getTelefoneVerificado()
            );
        }
    }

    public record ParceiroDestaqueDTO(
            String id,
            String nome,
            String tenantId,
            String regiao,
            String estado,
            String cidade,
            String bairro,
            Double avaliacao,
            Integer totalAvaliacoes,
            java.util.List<String> especialidades,
            Boolean inicianteDestaque,
            String tempoResposta,
            Boolean seloNbr
    ) {}

    public record AvaliacaoProfissionalDTO(
            String profissionalId,
            String tenantId,
            String orcamentoId,
            Integer estrelas,
            String tag
    ) {}
}

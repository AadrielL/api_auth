package com.projeto.orcamento.demo.service;

import com.projeto.orcamento.demo.dto.AuthDTOs.RegisterDTO;
import com.projeto.orcamento.demo.model.User;
import com.projeto.orcamento.demo.model.UserRole;
import com.projeto.orcamento.demo.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.UUID; // Importante para o Tenant

@Service
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    @Autowired
    private UserRepository userRepository;

    public ResponseEntity register(RegisterDTO data) {
        logger.info("Tentativa de registro SaaS para o e-mail: {}", data.email());

        if (this.userRepository.findByEmail(data.email()) != null) {
            logger.warn("Falha no registro: E-mail {} já existe.", data.email());
            return ResponseEntity.badRequest().build();
        }

        try {
            String encryptedPassword = new BCryptPasswordEncoder().encode(data.password());

            // REGRA SAAS: Todo novo usuário entra como VISITANTE.
            // O upgrade para ADMIN (Plano Pago) será feito via confirmação de pagamento.
            User newUser = new User(
                    data.nome(),
                    data.email(),
                    encryptedPassword,
                    UserRole.VISITANTE,
                    UUID.randomUUID().toString() // Cada cliente tem seu próprio "espaço" (Tenant)
            );

            this.userRepository.save(newUser);
            logger.info("Usuário {} registrado como VISITANTE. Tenant: {}", data.email(), newUser.getTenantId());

            return ResponseEntity.ok().build();
        } catch (Exception e) {
            logger.error("Erro crítico no registro: ", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * MODO SIMULAÇÃO DE PAGAMENTO:
     * Use este método futuramente para integrar com Webhooks (Mercado Pago/Stripe)
     */
    public void upgradeUserToAdmin(String email) {
        User user = (User) userRepository.findByEmail(email);
        if (user != null) {
            user.setRole(UserRole.ADMIN);
            userRepository.save(user);
            logger.info("UPGRADE: Usuário {} agora é ADMIN (Plano Ativado)!", email);
        }
    }

    public User findByEmail(String email) {
        return (User) userRepository.findByEmail(email);
    }

    public java.util.List<com.projeto.orcamento.demo.dto.AuthDTOs.ParceiroDestaqueDTO> listarParceirosAntiPanelinha() {
        var usuarios = userRepository.findAll();
        if (usuarios.isEmpty()) {
            return java.util.List.of(
                new com.projeto.orcamento.demo.dto.AuthDTOs.ParceiroDestaqueDTO(
                    "elet-01", "Carlos Eduardo Siqueira", "tenant-carlos-104-sul",
                    "Plano Diretor Sul", 4.9, 8,
                    java.util.List.of("Instalações Residenciais", "Quadro QDF", "Norma NBR 5410"),
                    true, "< 15 min", true
                ),
                new com.projeto.orcamento.demo.dto.AuthDTOs.ParceiroDestaqueDTO(
                    "elet-02", "Marcos Vinicius Ribeiro", "tenant-marcos-taquaralto",
                    "Taquaralto e Região Sul", 4.8, 6,
                    java.util.List.of("Padrão Energisa", "Chuveiro & Aquecedores", "Rede Bifásica"),
                    true, "< 20 min", true
                )
            );
        }

        return usuarios.stream()
                .filter(u -> u.getNome() != null && !u.getNome().isBlank())
                .map(u -> new com.projeto.orcamento.demo.dto.AuthDTOs.ParceiroDestaqueDTO(
                        u.getId(),
                        u.getNome(),
                        u.getTenantId() != null ? u.getTenantId() : u.getEmail(),
                        "Plano Diretor e Região de Palmas",
                        4.8 + (Math.abs(u.getNome().hashCode() % 3) * 0.1),
                        Math.max(3, Math.abs(u.getNome().hashCode() % 15)),
                        java.util.List.of("Instalações Residenciais", "Norma NBR 5410", "Padrão Energisa"),
                        u.getDailyCount() == null || u.getDailyCount() < 10,
                        "< 15 min",
                        true
                ))
                .sorted((a, b) -> {
                    if (a.inicianteDestaque() && !b.inicianteDestaque()) return -1;
                    if (!a.inicianteDestaque() && b.inicianteDestaque()) return 1;
                    return Double.compare(b.avaliacao(), a.avaliacao());
                })
                .toList();
    }
}

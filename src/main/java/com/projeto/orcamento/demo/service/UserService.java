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
}
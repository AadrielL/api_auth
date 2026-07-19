package com.projeto.orcamento.demo.service;

import com.projeto.orcamento.demo.model.User;
import com.projeto.orcamento.demo.model.UserRole;
import com.projeto.orcamento.demo.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class SubscriptionService {

    private static final Logger logger = LoggerFactory.getLogger(SubscriptionService.class);

    @Autowired
    private UserRepository userRepository;

    /**
     * Promove o usuário para o plano Pro (ADMIN).
     * @param email E-mail do cliente vindo do gateway de pagamento.
     * @param amount Valor pago (para determinar se é diário ou mensal).
     */
    @Transactional
    public void activatePlan(String email, double amount) {
        User user = (User) userRepository.findByEmail(email);

        if (user != null) {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime currentExpiry = user.getSubscriptionExpiryDate();
            
            if (currentExpiry == null || currentExpiry.isBefore(now)) {
                currentExpiry = now;
            }

            if (amount == 7.00) {
                user.setSubscriptionExpiryDate(currentExpiry.plusDays(1));
                logger.info("Plano Diário ativado para {}", email);
            } else if (amount >= 89.00) {
                user.setSubscriptionExpiryDate(currentExpiry.plusDays(30));
                logger.info("Plano Mensal ativado para {}", email);
            } else {
                logger.warn("Valor de pagamento não reconhecido para plano: {}", amount);
                return;
            }

            user.setRole(UserRole.ADMIN);
            userRepository.save(user);

            logger.info("PLANO PRO ATIVADO: Usuário {} promovido com sucesso até {}!", email, user.getSubscriptionExpiryDate());
        } else {
            logger.error("FALHA AO ATIVAR PLANO: Usuário com e-mail {} não encontrado.", email);
        }
    }
}
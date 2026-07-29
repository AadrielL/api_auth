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
    public long getVagasVitaliciasRestantes() {
        long count = userRepository.countByPlanType("LIFETIME");
        return Math.max(0, 1000 - count);
    }

    @Transactional
    public void activatePlan(String email, double amount) {
        User user = (User) userRepository.findByEmail(email);

        if (user != null) {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime currentExpiry = user.getSubscriptionExpiryDate();
            
            if (currentExpiry == null || currentExpiry.isBefore(now)) {
                currentExpiry = now;
            }

            if (amount == 1.99) {
                user.setSubscriptionExpiryDate(currentExpiry.plusDays(7));
                user.setPlanType("WEEKLY");
                logger.info("Plano Semanal ativado para {}", email);
            } else if (amount == 9.99) {
                user.setSubscriptionExpiryDate(currentExpiry.plusDays(30));
                user.setPlanType("MONTHLY");
                logger.info("Plano Mensal ativado para {}", email);
            } else if (amount >= 199.00) {
                if (getVagasVitaliciasRestantes() > 0) {
                    user.setSubscriptionExpiryDate(currentExpiry.plusYears(100)); // Simula vitalício
                    user.setPlanType("LIFETIME");
                    logger.info("Plano Vitalício ativado para {}", email);
                } else {
                    logger.warn("Plano Vitalício esgotado para {}", email);
                    throw new RuntimeException("Vagas esgotadas para o Plano Vitalício.");
                }
            } else {
                logger.warn("Valor de pagamento não reconhecido para plano: {}", amount);
                return;
            }

            user.setRole(UserRole.ADMIN);
            userRepository.save(user);

            logger.info("PLANO PRO ATIVADO: Usuário {} promovido com sucesso até {}! Tipo: {}", email, user.getSubscriptionExpiryDate(), user.getPlanType());
        } else {
            logger.error("FALHA AO ATIVAR PLANO: Usuário com e-mail {} não encontrado.", email);
        }
    }
}
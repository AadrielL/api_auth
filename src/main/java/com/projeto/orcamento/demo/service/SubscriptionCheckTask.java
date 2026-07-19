package com.projeto.orcamento.demo.service;

import com.projeto.orcamento.demo.model.User;
import com.projeto.orcamento.demo.model.UserRole;
import com.projeto.orcamento.demo.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class SubscriptionCheckTask {

    private static final Logger logger = LoggerFactory.getLogger(SubscriptionCheckTask.class);

    @Autowired
    private UserRepository userRepository;

    // Roda a cada hora
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void checkExpiredSubscriptions() {
        logger.info("Executando verificação de assinaturas expiradas...");
        LocalDateTime now = LocalDateTime.now();
        List<User> users = userRepository.findAll(); 
        
        int count = 0;
        for (User user : users) {
            if (user.getRole() == UserRole.ADMIN && user.getSubscriptionExpiryDate() != null) {
                if (user.getSubscriptionExpiryDate().isBefore(now)) {
                    user.setRole(UserRole.VISITANTE);
                    userRepository.save(user);
                    count++;
                    logger.info("Assinatura do usuário {} expirou. Rebaixado para VISITANTE.", user.getEmail());
                }
            }
        }
        logger.info("Verificação concluída. {} usuários rebaixados.", count);
    }
}

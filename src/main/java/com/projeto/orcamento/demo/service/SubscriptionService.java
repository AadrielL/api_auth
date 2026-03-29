package com.projeto.orcamento.demo.service;

import com.projeto.orcamento.demo.model.User;
import com.projeto.orcamento.demo.model.UserRole;
import com.projeto.orcamento.demo.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SubscriptionService {

    private static final Logger logger = LoggerFactory.getLogger(SubscriptionService.class);

    @Autowired
    private UserRepository userRepository;

    /**
     * Promove o usuário para o plano Pro (ADMIN).
     * @param email E-mail do cliente vindo do gateway de pagamento.
     */
    @Transactional
    public void activateProPlan(String email) {
        User user = (User) userRepository.findByEmail(email);

        if (user != null) {
            // Se já for ADMIN, não precisa fazer nada
            if (user.getRole() == UserRole.ADMIN) {
                logger.info("Usuário {} já possui plano Pro ativo.", email);
                return;
            }

            user.setRole(UserRole.ADMIN);
            userRepository.save(user);

            logger.info("PLANO PRO ATIVADO: Usuário {} promovido com sucesso!", email);
        } else {
            logger.error("FALHA AO ATIVAR PLANO: Usuário com e-mail {} não encontrado.", email);
        }
    }
}
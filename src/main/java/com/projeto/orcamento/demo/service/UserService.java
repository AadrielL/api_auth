package com.projeto.orcamento.demo.service;

import com.projeto.orcamento.demo.dto.AuthDTOs.ParceiroDestaqueDTO;
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

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    @Autowired
    private UserRepository userRepository;

    public ResponseEntity register(RegisterDTO data) {
        logger.info("Tentativa de registro SaaS para o e-mail: {}", data.email());

        // 1. Validação de e-mail duplicado
        if (this.userRepository.findByEmail(data.email()) != null) {
            logger.warn("Falha no registro: E-mail {} já existe.", data.email());
            return ResponseEntity.badRequest().body("E-mail já cadastrado na plataforma.");
        }

        // 2. Validação estrita de formato de e-mail
        if (data.email() == null || !data.email().matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            return ResponseEntity.badRequest().body("Formato de e-mail inválido.");
        }

        // 3. Validação de senha forte (mínimo 8 caracteres, maiúscula, minúscula e número)
        if (data.password() == null || data.password().length() < 8 ||
                !data.password().matches("^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z]).{8,}$")) {
            return ResponseEntity.badRequest().body("A senha deve ter no mínimo 8 caracteres, incluindo letras maiúsculas, minúsculas e números.");
        }

        try {
            String encryptedPassword = new BCryptPasswordEncoder().encode(data.password());
            String tipoPerfil = (data.tipoPerfil() != null && !data.tipoPerfil().isBlank())
                    ? data.tipoPerfil().toUpperCase()
                    : "ELETRICISTA";

            String tenantId;
            String eletricistaVinculado = data.eletricistaVinculadoId();

            if ("CLIENTE".equals(tipoPerfil)) {
                // Fluxo do Cliente: Vincula ao Eletricista escolhido ou atribui automaticamente
                if (eletricistaVinculado != null && !eletricistaVinculado.isBlank()) {
                    var eletricistaOpt = userRepository.findById(eletricistaVinculado);
                    if (eletricistaOpt.isPresent()) {
                        tenantId = eletricistaOpt.get().getTenantId();
                    } else {
                        tenantId = UUID.randomUUID().toString();
                    }
                } else {
                    // Atribuição automática (Anti-Panelinha): Eletricista mais recente com avaliação >= 4.5
                    var eletricistasAtivos = userRepository.findAll().stream()
                            .filter(u -> !"CLIENTE".equals(u.getTipoPerfil()) && u.getRole() != UserRole.ADMIN)
                            .filter(u -> u.getAvaliacao() != null && u.getAvaliacao() >= 4.5)
                            .sorted(Comparator.comparing(User::getDataCadastro, Comparator.nullsLast(Comparator.reverseOrder())))
                            .toList();

                    if (!eletricistasAtivos.isEmpty()) {
                        User eleito = eletricistasAtivos.get(0);
                        tenantId = eleito.getTenantId();
                        eletricistaVinculado = eleito.getId();
                        logger.info("Anti-Panelinha: Cliente {} atribuído ao eletricista recente: {}", data.email(), eleito.getNome());
                    } else {
                        tenantId = UUID.randomUUID().toString();
                    }
                }
            } else {
                // Eletricista cria seu próprio espaço tenant
                tenantId = UUID.randomUUID().toString();
            }

            // REGRA SAAS: Todo novo usuário entra como VISITANTE e plano FREE
            // Upgrade para ADMIN / PRO ocorre somente via confirmação de pagamento
            User newUser = new User(
                    data.nome(),
                    data.email(),
                    encryptedPassword,
                    UserRole.VISITANTE,
                    tenantId,
                    tipoPerfil,
                    eletricistaVinculado
            );

            this.userRepository.save(newUser);
            logger.info("Usuário {} registrado como {} (VISITANTE/FREE). Tenant: {}", data.email(), tipoPerfil, tenantId);

            return ResponseEntity.ok().build();
        } catch (Exception e) {
            logger.error("Erro crítico no registro: ", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * MODO SIMULAÇÃO DE PAGAMENTO:
     * Usado por Webhooks (Mercado Pago/Stripe) após confirmação de pagamento
     */
    public void upgradeUserToAdmin(String email) {
        User user = (User) userRepository.findByEmail(email);
        if (user != null) {
            user.setRole(UserRole.ADMIN);
            user.setPlanType("PRO");
            userRepository.save(user);
            logger.info("UPGRADE: Usuário {} agora é ADMIN/PRO (Plano Ativado)!", email);
        }
    }

    public User findByEmail(String email) {
        return (User) userRepository.findByEmail(email);
    }

    /**
     * Lista apenas eletricistas cadastrados reais no banco.
     * Se não houver nenhum, retorna lista vazia (sem mock falso).
     */
    public List<ParceiroDestaqueDTO> listarParceirosAntiPanelinha() {
        var usuarios = userRepository.findAll();

        // Filtra apenas profissionais reais (não clientes e com nome válido)
        var eletricistas = usuarios.stream()
                .filter(u -> !"CLIENTE".equals(u.getTipoPerfil()))
                .filter(u -> u.getNome() != null && !u.getNome().isBlank())
                .toList();

        // Se não houver eletricistas reais cadastrados no banco, retorna lista vazia
        if (eletricistas.isEmpty()) {
            return List.of();
        }

        return eletricistas.stream()
                .map(u -> new ParceiroDestaqueDTO(
                        u.getId(),
                        u.getNome(),
                        u.getTenantId() != null ? u.getTenantId() : u.getEmail(),
                        "Palmas e Região",
                        u.getAvaliacao() != null ? u.getAvaliacao() : 4.8,
                        Math.max(1, Math.abs(u.getNome().hashCode() % 15)),
                        List.of("Instalações Residenciais", "Norma NBR 5410", "Padrão Energisa"),
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

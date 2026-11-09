package com.projeto.orcamento.demo.service;

import com.projeto.orcamento.demo.dto.AuthDTOs.*;
import com.projeto.orcamento.demo.infra.security.TokenService;
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
import java.util.Map;
import java.util.Random;
import java.util.UUID;

@Service
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private TokenService tokenService;

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

            // Gera código numérico de 6 dígitos para validação real do e-mail
            String codigo = String.format("%06d", new Random().nextInt(1000000));

            User newUser = new User(
                    data.nome(),
                    data.email(),
                    encryptedPassword,
                    UserRole.VISITANTE,
                    tenantId,
                    tipoPerfil,
                    eletricistaVinculado
            );
            newUser.setCodigoVerificacao(codigo);
            newUser.setCodigoExpiracao(LocalDateTime.now().plusMinutes(15));
            newUser.setEmailVerificado(false);

            this.userRepository.save(newUser);
            logger.info("Usuário {} registrado. Código de verificação gerado.", data.email());

            // Dispara envio do e-mail via serviço de mensageria
            emailService.enviarCodigoVerificacao(newUser.getEmail(), newUser.getNome(), codigo);

            return ResponseEntity.ok(Map.of(
                    "mensagem", "Cadastro realizado com sucesso! Enviamos um código de verificação para o seu e-mail.",
                    "email", newUser.getEmail(),
                    "emailVerificado", false
            ));
        } catch (Exception e) {
            logger.error("Erro crítico no registro: ", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Valida o código de 6 dígitos recebido por e-mail e ativa a conta
     */
    public ResponseEntity verificarCodigo(VerificarCodigoDTO data) {
        User user = (User) userRepository.findByEmail(data.email());
        if (user == null) {
            return ResponseEntity.status(404).body("Usuário não encontrado.");
        }

        if (Boolean.TRUE.equals(user.getEmailVerificado())) {
            var token = tokenService.generateToken(user);
            return ResponseEntity.ok(new LoginResponseDTO(
                    token,
                    user.getRole().toString(),
                    user.getNome(),
                    user.getEmail(),
                    user.getTipoPerfil() != null ? user.getTipoPerfil() : "ELETRICISTA",
                    user.getTenantId()
            ));
        }

        if (user.getCodigoExpiracao() == null || LocalDateTime.now().isAfter(user.getCodigoExpiracao())) {
            return ResponseEntity.badRequest().body("Código expirado. Por favor, solicite um novo código.");
        }

        if (!data.codigo().trim().equals(user.getCodigoVerificacao())) {
            return ResponseEntity.badRequest().body("Código de verificação incorreto.");
        }

        // Ativa a conta com sucesso
        user.setEmailVerificado(true);
        user.setCodigoVerificacao(null);
        user.setCodigoExpiracao(null);
        userRepository.save(user);

        logger.info("Conta ativada com sucesso para o e-mail: {}", user.getEmail());

        var token = tokenService.generateToken(user);
        return ResponseEntity.ok(new LoginResponseDTO(
                token,
                user.getRole().toString(),
                user.getNome(),
                user.getEmail(),
                user.getTipoPerfil() != null ? user.getTipoPerfil() : "ELETRICISTA",
                user.getTenantId()
        ));
    }

    /**
     * Reenvia um novo código de verificação para o e-mail
     */
    public ResponseEntity reenviarCodigo(ReenviarCodigoDTO data) {
        User user = (User) userRepository.findByEmail(data.email());
        if (user == null) {
            return ResponseEntity.status(404).body("Usuário não encontrado.");
        }

        if (Boolean.TRUE.equals(user.getEmailVerificado())) {
            return ResponseEntity.badRequest().body("Este e-mail já foi verificado anteriormente.");
        }

        String novoCodigo = String.format("%06d", new Random().nextInt(1000000));
        user.setCodigoVerificacao(novoCodigo);
        user.setCodigoExpiracao(LocalDateTime.now().plusMinutes(15));
        userRepository.save(user);

        emailService.enviarCodigoVerificacao(user.getEmail(), user.getNome(), novoCodigo);
        logger.info("Novo código de verificação reenviado para: {}", user.getEmail());

        return ResponseEntity.ok(Map.of("mensagem", "Novo código enviado com sucesso!"));
    }

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

    public List<ParceiroDestaqueDTO> listarParceirosAntiPanelinha() {
        var usuarios = userRepository.findAll();

        var eletricistas = usuarios.stream()
                .filter(u -> !"CLIENTE".equals(u.getTipoPerfil()))
                .filter(u -> u.getNome() != null && !u.getNome().isBlank())
                .toList();

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

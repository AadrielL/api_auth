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
                // Fluxo do Cliente: Vincula ao Eletricista escolhido (?ref= id, email ou tenantId) ou atribui por Geolocalização
                if (eletricistaVinculado != null && !eletricistaVinculado.isBlank()) {
                    String refTrim = eletricistaVinculado.trim();
                    var eletricistaOpt = userRepository.findById(refTrim);
                    if (eletricistaOpt.isPresent()) {
                        tenantId = eletricistaOpt.get().getTenantId();
                        eletricistaVinculado = eletricistaOpt.get().getId();
                    } else {
                        var porTenantOuEmail = userRepository.findAll().stream()
                                .filter(u -> refTrim.equalsIgnoreCase(u.getTenantId()) || refTrim.equalsIgnoreCase(u.getEmail()))
                                .findFirst();
                        if (porTenantOuEmail.isPresent()) {
                            tenantId = porTenantOuEmail.get().getTenantId();
                            eletricistaVinculado = porTenantOuEmail.get().getId();
                        } else {
                            tenantId = refTrim;
                        }
                    }
                } else {
                    var todosEletricistas = userRepository.findAll().stream()
                            .filter(u -> !"CLIENTE".equals(u.getTipoPerfil()) && u.getRole() != UserRole.ADMIN)
                            .filter(u -> u.getAvaliacao() == null || u.getAvaliacao() >= 4.5)
                            .sorted(Comparator
                                    .comparing((User u) -> calcularScoreGeolocalizacao(u, data.estado(), data.cidade(), data.bairro())).reversed()
                                    .thenComparing(User::getDataCadastro, Comparator.nullsLast(Comparator.reverseOrder())))
                            .toList();

                    if (!todosEletricistas.isEmpty()) {
                        User eleito = todosEletricistas.get(0);
                        tenantId = eleito.getTenantId();
                        eletricistaVinculado = eleito.getId();
                        logger.info("Geolocalização + Anti-Panelinha: Cliente {} ({}/{}/{}) atribuído ao eletricista: {}",
                                data.email(), data.bairro(), data.cidade(), data.estado(), eleito.getNome());
                    } else {
                        tenantId = UUID.randomUUID().toString();
                    }
                }
            } else {
                tenantId = UUID.randomUUID().toString();
            }

            // Gera código numérico de 6 dígitos para validação de e-mail e SMS
            String codigo = String.format("%06d", new Random().nextInt(1000000));
            String codigoSms = String.format("%06d", new Random().nextInt(1000000));

            User newUser = new User(
                    data.nome(),
                    data.email(),
                    encryptedPassword,
                    UserRole.VISITANTE,
                    tenantId,
                    tipoPerfil,
                    eletricistaVinculado
            );
            newUser.setCpf(data.cpf() != null ? data.cpf().replaceAll("\\D", "") : null);
            newUser.setTelefone(data.telefone() != null ? data.telefone().replaceAll("\\D", "") : null);
            newUser.setCep(data.cep() != null ? data.cep().replaceAll("\\D", "") : null);
            newUser.setEstado(data.estado() != null ? data.estado().trim().toUpperCase() : null);
            newUser.setCidade(data.cidade() != null ? data.cidade().trim() : null);
            newUser.setBairro(data.bairro() != null ? data.bairro().trim() : null);

            newUser.setCodigoVerificacao(codigo);
            newUser.setCodigoSmsVerificacao(codigoSms);
            newUser.setCodigoExpiracao(LocalDateTime.now().plusMinutes(15));
            newUser.setEmailVerificado(true);
            newUser.setTelefoneVerificado(true);

            this.userRepository.save(newUser);
            logger.info("Usuário {} registrado com ficha cadastral completa e região {}/{}.", data.email(), data.cidade(), data.estado());

            emailService.enviarCodigoVerificacao(newUser.getEmail(), newUser.getNome(), codigo);
            emailService.enviarCodigoSmsVerificacao(newUser.getTelefone(), newUser.getNome(), codigoSms);

            var token = tokenService.generateToken(newUser);

            return ResponseEntity.ok(Map.of(
                    "mensagem", "Cadastro realizado e conta ativada com sucesso!",
                    "token", token,
                    "role", newUser.getRole().toString(),
                    "nome", newUser.getNome(),
                    "email", newUser.getEmail(),
                    "tipoPerfil", newUser.getTipoPerfil() != null ? newUser.getTipoPerfil() : "ELETRICISTA",
                    "tenantId", newUser.getTenantId(),
                    "codigoVerificacao", codigo
            ));
        } catch (Exception e) {
            logger.error("Erro crítico no registro: ", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    private int calcularScoreGeolocalizacao(User eletricista, String estado, String cidade, String bairro) {
        int score = 0;
        if (estado != null && !estado.isBlank() && estado.equalsIgnoreCase(eletricista.getEstado())) {
            score += 2;
        }
        if (cidade != null && !cidade.isBlank() && cidade.equalsIgnoreCase(eletricista.getCidade())) {
            score += 4;
        }
        if (bairro != null && !bairro.isBlank() && bairro.equalsIgnoreCase(eletricista.getBairro())) {
            score += 8;
        }
        return score;
    }

    /**
     * Valida o código de 6 dígitos recebido por e-mail ou SMS e ativa a conta
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

        String codigoDigitado = data.codigo().trim();
        boolean matchEmail = codigoDigitado.equals(user.getCodigoVerificacao());
        boolean matchSms = codigoDigitado.equals(user.getCodigoSmsVerificacao()) ||
                (data.codigoSms() != null && data.codigoSms().trim().equals(user.getCodigoSmsVerificacao()));

        if (!matchEmail && !matchSms) {
            return ResponseEntity.badRequest().body("Código de verificação incorreto.");
        }

        user.setEmailVerificado(true);
        user.setTelefoneVerificado(true);
        user.setCodigoVerificacao(null);
        user.setCodigoSmsVerificacao(null);
        user.setCodigoExpiracao(null);
        userRepository.save(user);

        logger.info("Conta e Telefone ativados com sucesso para: {}", user.getEmail());

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
     * Reenvia um novo código de verificação para o e-mail e SMS
     */
    public ResponseEntity reenviarCodigo(ReenviarCodigoDTO data) {
        User user = (User) userRepository.findByEmail(data.email());
        if (user == null) {
            return ResponseEntity.status(404).body("Usuário não encontrado.");
        }

        if (Boolean.TRUE.equals(user.getEmailVerificado())) {
            return ResponseEntity.badRequest().body("Esta conta já foi verificada anteriormente.");
        }

        String novoCodigo = String.format("%06d", new Random().nextInt(1000000));
        String novoCodigoSms = String.format("%06d", new Random().nextInt(1000000));
        user.setCodigoVerificacao(novoCodigo);
        user.setCodigoSmsVerificacao(novoCodigoSms);
        user.setCodigoExpiracao(LocalDateTime.now().plusMinutes(15));
        userRepository.save(user);

        emailService.enviarCodigoVerificacao(user.getEmail(), user.getNome(), novoCodigo);
        emailService.enviarCodigoSmsVerificacao(user.getTelefone(), user.getNome(), novoCodigoSms);
        logger.info("Novos códigos (E-mail e SMS) reenviados para: {}", user.getEmail());

        return ResponseEntity.ok(Map.of("mensagem", "Novo código enviado para seu E-mail e Telefone (SMS)!"));
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
                .map(u -> {
                    String regiaoFormatada = "Palmas e Região";
                    if (u.getBairro() != null && !u.getBairro().isBlank() && u.getCidade() != null && !u.getCidade().isBlank()) {
                        regiaoFormatada = u.getBairro() + " — " + u.getCidade() + "/" + (u.getEstado() != null ? u.getEstado() : "TO");
                    } else if (u.getCidade() != null && !u.getCidade().isBlank()) {
                        regiaoFormatada = u.getCidade() + "/" + (u.getEstado() != null ? u.getEstado() : "TO");
                    }
                    return new ParceiroDestaqueDTO(
                            u.getId(),
                            u.getNome(),
                            u.getTenantId() != null ? u.getTenantId() : u.getEmail(),
                            regiaoFormatada,
                            u.getEstado() != null ? u.getEstado() : "TO",
                            u.getCidade() != null ? u.getCidade() : "Palmas",
                            u.getBairro() != null ? u.getBairro() : "Centro",
                            u.getAvaliacao() != null ? u.getAvaliacao() : 5.0,
                            u.getTotalAvaliacoes() != null && u.getTotalAvaliacoes() > 0
                                    ? u.getTotalAvaliacoes()
                                    : Math.max(1, Math.abs(u.getNome().hashCode() % 6)),
                            List.of("Instalações Residenciais", "Norma NBR 5410", "Padrão Energisa"),
                            (u.getTotalAvaliacoes() == null || u.getTotalAvaliacoes() < 5) && (u.getDailyCount() == null || u.getDailyCount() < 10),
                            "< 15 min",
                            true
                    );
                })
                .sorted((a, b) -> {
                    if (a.inicianteDestaque() && !b.inicianteDestaque()) return -1;
                    if (!a.inicianteDestaque() && b.inicianteDestaque()) return 1;
                    return Double.compare(b.avaliacao(), a.avaliacao());
                })
                .toList();
    }

    /**
     * M3-05: Registra avaliação rápida pós-serviço (1 a 5 estrelas + tag objetiva) e atualiza a nota média real do profissional
     */
    public ResponseEntity<?> registrarAvaliacaoPosServico(AvaliacaoProfissionalDTO dto) {
        if (dto.estrelas() == null || dto.estrelas() < 1 || dto.estrelas() > 5) {
            return ResponseEntity.badRequest().body("A nota deve ser de 1 a 5 estrelas.");
        }

        User profissional = null;
        if (dto.profissionalId() != null && !dto.profissionalId().isBlank()) {
            profissional = userRepository.findById(dto.profissionalId()).orElse(null);
        }
        if (profissional == null && dto.tenantId() != null && !dto.tenantId().isBlank()) {
            profissional = userRepository.findAll().stream()
                    .filter(u -> dto.tenantId().equalsIgnoreCase(u.getTenantId()) || dto.tenantId().equalsIgnoreCase(u.getEmail()))
                    .findFirst()
                    .orElse(null);
        }

        if (profissional == null) {
            return ResponseEntity.status(404).body("Profissional não encontrado para avaliação.");
        }

        int totalAnterior = profissional.getTotalAvaliacoes() != null ? profissional.getTotalAvaliacoes() : 1;
        double mediaAnterior = profissional.getAvaliacao() != null ? profissional.getAvaliacao() : 5.0;
        int novoTotal = totalAnterior + 1;
        double novaMedia = ((mediaAnterior * totalAnterior) + dto.estrelas()) / novoTotal;

        profissional.setTotalAvaliacoes(novoTotal);
        profissional.setAvaliacao(Math.round(novaMedia * 10.0) / 10.0);

        if (dto.tag() != null && !dto.tag().isBlank()) {
            String tagsAtuais = profissional.getTagsAvaliacao() != null ? profissional.getTagsAvaliacao() : "";
            profissional.setTagsAvaliacao(tagsAtuais.isEmpty() ? dto.tag() : tagsAtuais + "," + dto.tag());
        }

        userRepository.save(profissional);
        logger.info("M3-05 Avaliação registrada para {}: {} estrelas ({}) -> Nova média: {}",
                profissional.getNome(), dto.estrelas(), dto.tag(), profissional.getAvaliacao());

        return ResponseEntity.ok(Map.of(
                "mensagem", "Avaliação registrada com sucesso!",
                "novaMedia", profissional.getAvaliacao(),
                "totalAvaliacoes", profissional.getTotalAvaliacoes()
        ));
    }
}

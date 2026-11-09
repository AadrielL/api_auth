package com.projeto.orcamento.demo.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String remetente;

    public void enviarCodigoVerificacao(String destinatario, String nome, String codigo) {
        // Log de alta visibilidade no terminal (para desenvolvimento e testes imediatos)
        logger.info("\n==============================================================" +
                    "\n⚡ [AMORIS TECH] CÓDIGO DE ATIVAÇÃO DE CONTA" +
                    "\nDestinatário : {} ({})" +
                    "\nCódigo       : {}" +
                    "\nValidade     : 15 minutos" +
                    "\n==============================================================",
                    destinatario, nome, codigo);

        // Se houver servidor SMTP configurado no application.properties / .env, envia o e-mail real
        if (mailSender != null && remetente != null && !remetente.isBlank()) {
            try {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setFrom(remetente);
                message.setTo(destinatario);
                message.setSubject("Seu Código de Confirmação — AMORIS Tech");
                message.setText("Olá " + nome + ",\n\n" +
                        "Seu código de verificação para ativar sua conta na AMORIS Tech é:\n\n" +
                        "👉 " + codigo + "\n\n" +
                        "Este código expira em 15 minutos.\n\n" +
                        "Se você não solicitou este cadastro, desconsidere esta mensagem.");
                mailSender.send(message);
                logger.info("E-mail de verificação enviado com sucesso para: {}", destinatario);
            } catch (Exception e) {
                logger.warn("Não foi possível disparar o e-mail via SMTP (código registrado em log): {}", e.getMessage());
            }
        }
    }
}

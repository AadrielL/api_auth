package com.projeto.orcamento.demo.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String remetente;

    public void enviarCodigoVerificacao(String destinatario, String nome, String codigo) {
        logger.info("\n==============================================================" +
                    "\n⚡ [AMORIS TECH] CÓDIGO DE ATIVAÇÃO DE E-MAIL" +
                    "\nDestinatário : {} ({})" +
                    "\nCódigo E-mail: {}" +
                    "\nValidade     : 15 minutos" +
                    "\n==============================================================",
                    destinatario, nome, codigo);

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

    public void enviarCodigoSmsVerificacao(String telefone, String nome, String codigoSms) {
        if (telefone == null || telefone.isBlank()) return;
        logger.info("\n==============================================================" +
                    "\n📱 [AMORIS TECH] SMS / WHATSAPP DE VERIFICAÇÃO DE NÚMERO REAL" +
                    "\nTelefone     : {} ({})" +
                    "\nCódigo SMS   : {}" +
                    "\nValidade     : 15 minutos" +
                    "\n==============================================================",
                    telefone, nome, codigoSms);
    }
}

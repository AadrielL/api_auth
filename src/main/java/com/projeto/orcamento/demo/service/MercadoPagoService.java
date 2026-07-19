package com.projeto.orcamento.demo.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class MercadoPagoService {

    private static final Logger logger = LoggerFactory.getLogger(MercadoPagoService.class);
    private static final String MP_API_URL = "https://api.mercadopago.com/v1/payments";

    @Value("${mercadopago.access.token}")
    private String accessToken;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Processa um pagamento chamando a API do Mercado Pago.
     * Recebe os dados do Brick (token do cartão, método, valor, etc.)
     * e envia para o MP usando o Access Token do backend.
     */
    public Map<String, Object> processPayment(Map<String, Object> paymentData) {
        logger.info("Processando pagamento via Mercado Pago...");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(accessToken);
        // Header de idempotência para evitar cobranças duplicadas
        headers.set("X-Idempotency-Key", java.util.UUID.randomUUID().toString());

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(paymentData, headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    MP_API_URL,
                    HttpMethod.POST,
                    request,
                    Map.class
            );

            Map<String, Object> body = response.getBody();
            logger.info("Resposta do Mercado Pago - Status: {}, ID: {}",
                    body != null ? body.get("status") : "null",
                    body != null ? body.get("id") : "null");

            return body;
        } catch (Exception e) {
            logger.error("Erro ao chamar API do Mercado Pago: {}", e.getMessage());
            throw new RuntimeException("Falha ao processar pagamento: " + e.getMessage());
        }
    }
}

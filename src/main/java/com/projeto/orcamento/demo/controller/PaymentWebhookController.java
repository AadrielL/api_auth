package com.projeto.orcamento.demo.controller;

import com.projeto.orcamento.demo.service.MercadoPagoService;
import com.projeto.orcamento.demo.service.SubscriptionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/payments")
public class PaymentWebhookController {

    private static final Logger logger = LoggerFactory.getLogger(PaymentWebhookController.class);

    @Autowired
    private SubscriptionService subscriptionService;

    @Autowired
    private MercadoPagoService mercadoPagoService;

    /**
     * Endpoint chamado pelo FRONTEND (Brick do MP).
     * Recebe o token do cartão + dados do pagamento e processa via API do MP.
     */
    @PostMapping("/process")
    public ResponseEntity<Map<String, Object>> processPayment(@RequestBody Map<String, Object> paymentData) {
        logger.info("Recebido pedido de pagamento do frontend");

        try {
            // Chama a API do Mercado Pago com o Access Token
            Map<String, Object> mpResponse = mercadoPagoService.processPayment(paymentData);

            String status = (String) mpResponse.get("status");
            logger.info("Status do pagamento: {}", status);

            // Se o pagamento foi aprovado, ativa o plano do usuário
            if ("approved".equals(status)) {
                Map<String, Object> payer = (Map<String, Object>) mpResponse.get("payer");
                String email = payer != null ? (String) payer.get("email") : null;
                Number amount = (Number) mpResponse.get("transaction_amount");

                if (email != null && amount != null) {
                    subscriptionService.activatePlan(email, amount.doubleValue());
                }
            }

            // Retorna o resultado para o frontend
            Map<String, Object> response = new HashMap<>();
            response.put("status", status);
            response.put("id", mpResponse.get("id"));
            response.put("detail", mpResponse.get("status_detail"));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Erro ao processar pagamento: {}", e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("status", "error");
            error.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * Webhook chamado pelo MERCADO PAGO (notificações IPN).
     * Para confirmações assíncronas (PIX, boleto, etc.)
     */
    @PostMapping("/confirm")
    public ResponseEntity<Void> handleWebhook(@RequestBody Map<String, Object> payload) {
        logger.info("Webhook recebido do Mercado Pago: {}", payload);

        String email = (String) payload.get("payer_email");

        double amount = 0.0;
        if (payload.get("transaction_amount") != null) {
            amount = Double.parseDouble(payload.get("transaction_amount").toString());
        }

        subscriptionService.activatePlan(email, amount);

        return ResponseEntity.ok().build();
    }
}
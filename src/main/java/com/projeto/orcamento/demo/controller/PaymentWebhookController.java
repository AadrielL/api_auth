package com.projeto.orcamento.demo.controller;

import com.projeto.orcamento.demo.service.SubscriptionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/payments")
public class PaymentWebhookController {

    @Autowired
    private SubscriptionService subscriptionService;

    // Endpoint que o Mercado Pago/Stripe vai chamar
    @PostMapping("/confirm")
    public ResponseEntity<Void> handleWebhook(@RequestBody Map<String, Object> payload) {
        // Exemplo: Pegando o email do payload (varia de acordo com o gateway)
        String email = (String) payload.get("payer_email");

        subscriptionService.activateProPlan(email);

        return ResponseEntity.ok().build();
    }
}
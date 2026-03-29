package com.projeto.orcamento.demo.infra.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.projeto.orcamento.demo.model.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Service
public class TokenService {
    @Value("${api.security.token.secret}")
    private String secret;

    public String generateToken(User user){
        Algorithm algorithm = Algorithm.HMAC256(secret);
        return JWT.create()
                .withIssuer("auth-api")
                .withSubject(user.getEmail())
                .withClaim("role", user.getRole().toString())
                // PADRONIZADO: Sem underline para bater com as outras APIs
                .withClaim("tenantId", user.getTenantId())
                .withExpiresAt(genExpirationDate())
                .sign(algorithm);
    }

    public String validateToken(String token){
        try {
            Algorithm algorithm = Algorithm.HMAC256(secret);
            return JWT.require(algorithm)
                    .withIssuer("auth-api")
                    .build()
                    .verify(token)
                    .getSubject();
        } catch (Exception exception){
            return "";
        }
    }

    public String getTenantIdFromToken(String token) {
        // Busca o nome padronizado
        return JWT.decode(token).getClaim("tenantId").asString();
    }

    private Instant genExpirationDate(){
        // 8 Horas em UTC evita conflitos de fuso horário do sistema operacional
        return LocalDateTime.now().plusHours(8).toInstant(ZoneOffset.UTC);
    }
}
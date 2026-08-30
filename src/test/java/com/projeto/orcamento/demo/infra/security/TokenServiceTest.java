package com.projeto.orcamento.demo.infra.security;

import com.projeto.orcamento.demo.model.User;
import com.projeto.orcamento.demo.model.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class TokenServiceTest {

    private TokenService tokenService;

    @BeforeEach
    void setUp() {
        tokenService = new TokenService();
        ReflectionTestUtils.setField(tokenService, "secret", "my-test-super-secret-key-123456");
    }

    @Test
    @DisplayName("Deve gerar e validar token JWT com sucesso extraindo o subject")
    void deveGerarEValidarTokenComSucesso() {
        User user = new User();
        user.setEmail("eletricista@teste.com");
        user.setRole(UserRole.ADMIN);
        user.setTenantId("eletricista@teste.com");
        user.setPlanType("PRO");

        String token = tokenService.generateToken(user);

        assertNotNull(token);
        assertFalse(token.isEmpty());

        String subject = tokenService.validateToken(token);
        assertEquals("eletricista@teste.com", subject);
    }

    @Test
    @DisplayName("Deve extrair TenantId corretamente a partir do token")
    void deveExtrairTenantIdCorretamente() {
        User user = new User();
        user.setEmail("admin@mygo.com");
        user.setRole(UserRole.ADMIN);
        user.setTenantId("tenant-12345");
        user.setPlanType("LIFETIME");

        String token = tokenService.generateToken(user);
        String tenantId = tokenService.getTenantIdFromToken(token);

        assertEquals("tenant-12345", tenantId);
    }

    @Test
    @DisplayName("Deve retornar string vazia ao validar token inválido ou forjado")
    void deveRetornarVazioParaTokenInvalido() {
        String tokenInvalido = "token.jwt.falso123";
        String resultado = tokenService.validateToken(tokenInvalido);

        assertEquals("", resultado);
    }
}
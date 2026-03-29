package com.projeto.orcamento.demo.infra.security;

import com.projeto.orcamento.demo.config.TenantContext;
import com.projeto.orcamento.demo.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;

@Component
public class SecurityFilter extends OncePerRequestFilter {
    private static final Logger logger = LoggerFactory.getLogger(SecurityFilter.class);

    @Autowired
    private TokenService tokenService;

    @Autowired
    private UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // 1. IGNORA O FILTRO PARA ROTAS DE LOGIN E REGISTER
        String path = request.getRequestURI();
        if (path.contains("/auth/login") || path.contains("/auth/register")) {
            filterChain.doFilter(request, response);
            return;
        }

        var token = this.recoverToken(request);

        if (token != null) {
            var login = tokenService.validateToken(token);

            if (login != null && !login.isEmpty()) {
                var tenantId = tokenService.getTenantIdFromToken(token);
                UserDetails user = userRepository.findByEmail(login);

                if (user != null) {
                    logger.info("Auth Ativado: Usuário {} | Tenant: {}", login, tenantId);

                    // Seta o Tenant para operações de banco nesta thread
                    TenantContext.setTenantId(tenantId);

                    var authentication = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                } else {
                    logger.warn("Usuário logado no Token não existe no Banco: {}", login);
                }
            } else {
                logger.warn("Token inválido ou expirado recebido em: {}", path);
            }
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            // Limpa o contexto para evitar vazamento de dados entre requisições
            TenantContext.clear();
        }
    }

    private String recoverToken(HttpServletRequest request) {
        var authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) return null;
        return authHeader.substring(7);
    }
}
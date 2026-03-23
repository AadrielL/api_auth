package com.projeto.orcamento.demo.infra.security;

import com.projeto.orcamento.demo.config.TenantContext;
import com.projeto.orcamento.demo.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.slf4j.Logger; // IMPORTANTE
import org.slf4j.LoggerFactory; // IMPORTANTE
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

    @Autowired TokenService tokenService;
    @Autowired UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        var token = this.recoverToken(request);

        if (token != null) {
            var login = tokenService.validateToken(token);
            if (!login.isEmpty()) {
                var tenantId = tokenService.getTenantIdFromToken(token);
                UserDetails user = userRepository.findByEmail(login);

                if (user != null) {
                    // LOG DE ATIVAÇÃO: Aqui você descobre se o Auth está funcionando
                    logger.info("Auth Ativado: Usuário {} | Tenant: {}", login, tenantId);

                    TenantContext.setTenantId(tenantId);
                    var authentication = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } else {
                logger.warn("Token inválido recebido em: {}", request.getRequestURI());
            }
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            // Garante que o Tenant não "vaze" para outra requisição (Crucial em Multi-tenant)
            TenantContext.clear();
        }
    }

    private String recoverToken(HttpServletRequest request) {
        var authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) return null;
        return authHeader.substring(7);
    }
}
package com.projeto.orcamento.demo.controller;

import com.projeto.orcamento.demo.dto.AuthDTOs.*;
import com.projeto.orcamento.demo.infra.security.TokenService;
import com.projeto.orcamento.demo.model.User;
import com.projeto.orcamento.demo.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@CrossOrigin(originPatterns = {"http://localhost:*", "http://127.0.0.1:*", "https://*.comandosamoris.tech", "https://comandosamoris.tech"}, allowCredentials = "true")
@RequestMapping("auth")
public class AuthController {
    @Autowired private AuthenticationManager authenticationManager;
    @Autowired private UserService userService;
    @Autowired private TokenService tokenService;
    @Autowired private com.projeto.orcamento.demo.service.SubscriptionService subscriptionService;

    @PostMapping("/login")
    public ResponseEntity login(@RequestBody @Valid AuthenticationDTO data){
        var usernamePassword = new UsernamePasswordAuthenticationToken(data.email(), data.password());
        var auth = this.authenticationManager.authenticate(usernamePassword);

        // Recupera o usuário do banco para pegar o Nome, Role e Tenant
        var user = (User) auth.getPrincipal();
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

    @PostMapping("/register")
    public ResponseEntity register(@RequestBody @Valid RegisterDTO data){
        return userService.register(data);
    }

    @GetMapping("/me")
    public ResponseEntity getMe() {
        var user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return ResponseEntity.ok(new UserResponseDTO(user));
    }

    @GetMapping("/refresh")
    public ResponseEntity refresh() {
        var user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        var updatedUser = userService.findByEmail(user.getEmail());
        var token = tokenService.generateToken(updatedUser);
        return ResponseEntity.ok(new LoginResponseDTO(
                token,
                updatedUser.getRole().toString(),
                updatedUser.getNome(),
                updatedUser.getEmail(),
                updatedUser.getTipoPerfil() != null ? updatedUser.getTipoPerfil() : "ELETRICISTA",
                updatedUser.getTenantId()
        ));
    }

    @GetMapping("/vagas-vitalicias")
    public ResponseEntity<Long> getVagasVitalicias() {
        return ResponseEntity.ok(subscriptionService.getVagasVitaliciasRestantes());
    }

    @GetMapping("/parceiros-destaque")
    public ResponseEntity<java.util.List<ParceiroDestaqueDTO>> getParceirosDestaque() {
        return ResponseEntity.ok(userService.listarParceirosAntiPanelinha());
    }
}

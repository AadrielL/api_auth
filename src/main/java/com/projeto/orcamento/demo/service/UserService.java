package com.projeto.orcamento.demo.service;

import com.projeto.orcamento.demo.dto.AuthDTOs; // Importamos a classe que contém os records
import com.projeto.orcamento.demo.model.User;
import com.projeto.orcamento.demo.model.UserRole;
import com.projeto.orcamento.demo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class UserService {

    @Autowired
    private UserRepository repository;

    // Referenciamos como AuthDTOs.RegisterDTO
    public ResponseEntity register(AuthDTOs.RegisterDTO data) {
        if(this.repository.findByEmail(data.email()) != null) {
            return ResponseEntity.badRequest().body("E-mail já cadastrado");
        }

        String encryptedPassword = new BCryptPasswordEncoder().encode(data.password());

        // Lógica Multi-tenant: ADMIN gera UUID, VISITANTE usa 'public'
        String tenantId = (data.role() == UserRole.ADMIN) ? UUID.randomUUID().toString() : "public";

        User newUser = new User(data.nome(), data.email(), encryptedPassword, data.role(), tenantId);
        this.repository.save(newUser);

        return ResponseEntity.ok().build();
    }

    public boolean canUserCalculate(User user) {
        if (user.getRole() == UserRole.ADMIN) return true;

        // Lógica de limite para Visitante: 1 por dia
        user.incrementDailyCount();
        if (user.getDailyCount() > 1) return false;

        repository.save(user);
        return true;
    }
}
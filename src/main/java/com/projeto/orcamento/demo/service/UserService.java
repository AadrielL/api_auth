package com.projeto.orcamento.demo.service;

import com.projeto.orcamento.demo.dto.AuthDTOs.RegisterDTO;
import com.projeto.orcamento.demo.model.User;
import com.projeto.orcamento.demo.model.UserRole;
import com.projeto.orcamento.demo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    public ResponseEntity register(RegisterDTO data) {
        if (this.userRepository.findByEmail(data.email()) != null) {
            return ResponseEntity.badRequest().build();
        }

        String encryptedPassword = new BCryptPasswordEncoder().encode(data.password());

        // COPIE ESTA PARTE: Criando você como ADMIN direto
        User newUser = new User(
                data.nome(),
                data.email(),
                encryptedPassword,
                UserRole.ADMIN, // Força Admin para liberar as telas
                "default-tenant"
        );

        this.userRepository.save(newUser);

        return ResponseEntity.ok().build();
    }
}
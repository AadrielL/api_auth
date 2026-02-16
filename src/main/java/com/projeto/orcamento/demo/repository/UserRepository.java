package com.projeto.orcamento.demo.repository;

import com.projeto.orcamento.demo.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, String> {
    // O Spring Data JPA entende automaticamente que deve buscar pelo campo 'email'
    UserDetails findByEmail(String email);
}
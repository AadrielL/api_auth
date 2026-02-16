package com.projeto.orcamento.demo.model;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private String nome;

    @Column(unique = true)
    private String email;

    private String password;

    @Enumerated(EnumType.STRING)
    private UserRole role;

    private String tenantId;

    private LocalDate lastCalculationDate;
    private Integer dailyCount = 0;

    // Construtor personalizado para o Registro
    public User(String nome, String email, String password, UserRole role, String tenantId) {
        this.nome = nome;
        this.email = email;
        this.password = password;
        this.role = role;
        this.tenantId = tenantId;
    }

    public void incrementDailyCount() {
        LocalDate today = LocalDate.now();
        if (lastCalculationDate == null || !today.equals(lastCalculationDate)) {
            this.lastCalculationDate = today;
            this.dailyCount = 1;
        } else {
            this.dailyCount++;
        }
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if(this.role == UserRole.ADMIN) {
            return List.of(new SimpleGrantedAuthority("ROLE_ADMIN"), new SimpleGrantedAuthority("ROLE_USER"));
        }
        return List.of(new SimpleGrantedAuthority("ROLE_USER"));
    }

    @Override public String getUsername() { return email; }
    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return true; }
}
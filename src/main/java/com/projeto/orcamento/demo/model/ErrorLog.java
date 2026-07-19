package com.projeto.orcamento.demo.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "error_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ErrorLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDateTime timestamp;
    
    private String serviceName;
    
    private String endpoint;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Column(columnDefinition = "TEXT")
    private String stackTrace;

    public ErrorLog(String serviceName, String endpoint, String message, String stackTrace) {
        this.timestamp = LocalDateTime.now();
        this.serviceName = serviceName;
        this.endpoint = endpoint;
        this.message = message;
        this.stackTrace = stackTrace;
    }
}

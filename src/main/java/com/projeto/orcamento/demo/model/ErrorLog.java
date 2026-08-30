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
    
    private String errorCode;

    private Integer statusCode;

    private String endpoint;

    private String httpMethod;

    private String tenantId;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Column(columnDefinition = "TEXT")
    private String stackTrace;

    public ErrorLog(String serviceName, String errorCode, Integer statusCode, String endpoint, String httpMethod, String tenantId, String message, String stackTrace) {
        this.timestamp = LocalDateTime.now();
        this.serviceName = serviceName;
        this.errorCode = errorCode;
        this.statusCode = statusCode;
        this.endpoint = endpoint;
        this.httpMethod = httpMethod;
        this.tenantId = tenantId;
        this.message = message;
        this.stackTrace = stackTrace;
    }
}

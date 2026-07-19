package com.projeto.orcamento.demo.controller;

import com.projeto.orcamento.demo.model.ErrorLog;
import com.projeto.orcamento.demo.repository.ErrorLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @Autowired
    private ErrorLogRepository errorLogRepository;

    @Value("${spring.application.name:api_1}")
    private String serviceName;

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleAllExceptions(Exception ex, HttpServletRequest request) {
        // Extrair o stack trace
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        ex.printStackTrace(pw);
        String stackTrace = sw.toString();

        // Salvar o erro no banco de dados (Clean Architecture - Log Centralizado)
        try {
            ErrorLog errorLog = new ErrorLog(serviceName, request.getRequestURI(), ex.getMessage(), stackTrace);
            errorLogRepository.save(errorLog);
        } catch (Exception dbEx) {
            logger.error("Falha ao salvar erro no banco de dados!", dbEx);
        }

        // Construir resposta padronizada para o Frontend (Cyber/Pixel friendly API response)
        Map<String, Object> response = new HashMap<>();
        response.put("error", "Internal Server Error");
        response.put("message", "Ocorreu um problema no servidor. Nossa equipe técnica foi notificada.");
        response.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        
        logger.error("Erro capturado e salvo: {}", ex.getMessage());

        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}

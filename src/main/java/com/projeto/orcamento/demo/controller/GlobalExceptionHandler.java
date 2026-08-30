package com.projeto.orcamento.demo.controller;

import com.projeto.orcamento.demo.config.TenantContext;
import com.projeto.orcamento.demo.dto.StandardErrorResponse;
import com.projeto.orcamento.demo.model.ErrorLog;
import com.projeto.orcamento.demo.repository.ErrorLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String ORIGIN_SERVICE = "API_1";

    @Autowired
    private ErrorLogRepository errorLogRepository;

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<StandardErrorResponse> handleBadCredentials(BadCredentialsException ex, HttpServletRequest request) {
        String errorCode = "UNAUTHORIZED_API_1";
        int status = HttpStatus.UNAUTHORIZED.value();
        String message = "E-mail ou senha inválidos.";

        salvarLogSilencioso(errorCode, status, request, ex, message);

        StandardErrorResponse response = new StandardErrorResponse(
                LocalDateTime.now(),
                status,
                errorCode,
                message,
                ORIGIN_SERVICE,
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<StandardErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String errorCode = "VALIDATION_ERROR_API_1";
        int status = HttpStatus.BAD_REQUEST.value();
        String message = "Dados de formulário inválidos ou incompletos.";

        salvarLogSilencioso(errorCode, status, request, ex, message);

        StandardErrorResponse response = new StandardErrorResponse(
                LocalDateTime.now(),
                status,
                errorCode,
                message,
                ORIGIN_SERVICE,
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<StandardErrorResponse> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
        String errorCode = "BAD_REQUEST_API_1";
        int status = HttpStatus.BAD_REQUEST.value();

        salvarLogSilencioso(errorCode, status, request, ex, ex.getMessage());

        StandardErrorResponse response = new StandardErrorResponse(
                LocalDateTime.now(),
                status,
                errorCode,
                ex.getMessage(),
                ORIGIN_SERVICE,
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<StandardErrorResponse> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        String errorCode = "FORBIDDEN_API_1";
        int status = HttpStatus.FORBIDDEN.value();
        String message = "Acesso negado para este recurso.";

        salvarLogSilencioso(errorCode, status, request, ex, message);

        StandardErrorResponse response = new StandardErrorResponse(
                LocalDateTime.now(),
                status,
                errorCode,
                message,
                ORIGIN_SERVICE,
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<StandardErrorResponse> handleAllExceptions(Exception ex, HttpServletRequest request) {
        String errorCode = "INTERNAL_SERVER_ERROR_API_1";
        int status = HttpStatus.INTERNAL_SERVER_ERROR.value();
        String message = "Ocorreu um problema no servidor. Nossa equipe técnica foi notificada.";

        salvarLogSilencioso(errorCode, status, request, ex, ex.getMessage());
        logger.error("[API_1] Erro não tratado: {}", ex.getMessage(), ex);

        StandardErrorResponse response = new StandardErrorResponse(
                LocalDateTime.now(),
                status,
                errorCode,
                message,
                ORIGIN_SERVICE,
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    private void salvarLogSilencioso(String errorCode, int status, HttpServletRequest request, Exception ex, String message) {
        try {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            ex.printStackTrace(pw);
            String stackTrace = sw.toString();

            String tenantId = TenantContext.getCurrentTenant();
            if (tenantId == null) tenantId = "ANONYMOUS";

            ErrorLog log = new ErrorLog(
                    ORIGIN_SERVICE,
                    errorCode,
                    status,
                    request.getRequestURI(),
                    request.getMethod(),
                    tenantId,
                    message,
                    stackTrace
            );
            errorLogRepository.save(log);
        } catch (Exception dbEx) {
            logger.error("[API_1] Falha ao salvar log de erro no banco de dados: {}", dbEx.getMessage());
        }
    }
}

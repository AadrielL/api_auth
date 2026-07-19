package com.projeto.orcamento.demo.repository;

import com.projeto.orcamento.demo.model.ErrorLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ErrorLogRepository extends JpaRepository<ErrorLog, Long> {
}

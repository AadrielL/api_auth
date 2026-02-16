package com.projeto.orcamento.demo.model;

public enum UserRole {
    ADMIN("admin"),
    VISITANTE("visitante");

    private String role;
    UserRole(String role) { this.role = role; }
    public String getRole() { return role; }
}
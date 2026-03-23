package com.projeto.orcamento.demo.config;

public class TenantContext {

    // ThreadLocal garante que cada requisição de usuário tenha seu próprio ID isolado
    private static final ThreadLocal<String> currentTenant = new ThreadLocal<>();

    // O método que estava faltando e causando o erro "Cannot resolve"
    public static void setTenantId(String tenantId) {
        currentTenant.set(tenantId);
    }

    public static String getCurrentTenant() {
        return currentTenant.get();
    }

    public static void clear() {
        currentTenant.remove();
    }
}
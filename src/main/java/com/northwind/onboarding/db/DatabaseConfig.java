package com.northwind.onboarding.db;

import org.jdbi.v3.core.Jdbi;

public class DatabaseConfig {

    private static final String DEFAULT_URL = "jdbc:postgresql://localhost:5432/northwind";

    private DatabaseConfig() {}

    public static Jdbi createJdbi() {
        String url      = System.getenv().getOrDefault("DB_URL", DEFAULT_URL);
        String user     = requireEnv("DB_USER");
        String password = requireEnv("DB_PASSWORD");
        return Jdbi.create(url, user, password);
    }

    private static String requireEnv(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Required environment variable not set: " + name);
        }
        return value;
    }
}

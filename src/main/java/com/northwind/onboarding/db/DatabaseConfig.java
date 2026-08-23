package com.northwind.onboarding.db;

import org.jdbi.v3.core.Jdbi;

public class DatabaseConfig {

    private static final String DEFAULT_URL      = "jdbc:postgresql://localhost:5432/northwind";
    private static final String DEFAULT_USER     = "postgres";
    private static final String DEFAULT_PASSWORD = "postgres";

    private DatabaseConfig() {}

    public static Jdbi createJdbi() {
        String url      = System.getenv().getOrDefault("DB_URL",      DEFAULT_URL);
        String user     = System.getenv().getOrDefault("DB_USER",     DEFAULT_USER);
        String password = System.getenv().getOrDefault("DB_PASSWORD", DEFAULT_PASSWORD);
        return Jdbi.create(url, user, password);
    }
}

package com.northwind.onboarding.model;

/** Immutable value object representing a new onboarding application. */
public record OnboardingApplication(
        String customerId,
        String customerName,
        String email,
        String documentReference   // reference ID for docs in the Document Store
) {
    public OnboardingApplication {
        if (customerId == null || customerId.isBlank())
            throw new IllegalArgumentException("customerId must not be blank");
        if (email == null || !email.contains("@"))
            throw new IllegalArgumentException("invalid email: " + email);
    }
}

package com.northwind.onboarding.model;

/** Business status of an onboarding application. */
public enum ApplicationStatus {
    PENDING,        // just submitted, not yet validated
    VALIDATED,      // passed field/format checks
    KYC_IN_PROGRESS,
    KYC_PASSED,
    KYC_FAILED,
    UNDER_REVIEW,   // queued for human compliance review
    ESCALATED,      // review timer expired — escalated to senior reviewer
    APPROVED,       // account activated
    REJECTED        // terminal failure state
}

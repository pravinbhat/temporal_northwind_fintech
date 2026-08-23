package com.northwind.onboarding.model;

/** Audit events written to onboarding_audit at each step of the onboarding process. */
public enum AuditEvent {
    APPLICATION_SUBMITTED,
    APPLICATION_VALIDATED,
    DOCUMENTS_STORED,
    KYC_CHECK_STARTED,
    KYC_CHECK_PASSED,
    KYC_CHECK_FAILED,
    COMPLIANCE_REVIEW_QUEUED,
    REVIEW_TIMER_ESCALATED,
    REVIEW_DECISION_RECEIVED,
    ACCOUNT_ACTIVATED,
    APPLICATION_REJECTED
}

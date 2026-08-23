package com.northwind.onboarding.workflow;

import com.northwind.onboarding.db.OnboardingRepository;
import com.northwind.onboarding.model.ApplicationStatus;
import com.northwind.onboarding.model.AuditEvent;
import com.northwind.onboarding.model.OnboardingApplication;
import io.temporal.activity.Activity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Demo simulation notes:
//   - runKycCheck: fails on attempts 1 and 2 (simulated transient vendor errors), passes on attempt 3.
//     Set KYC_FORCE_FAIL=true to always return false (KYC rejection path).
//   - submitToDocumentStore / activateAccount / queueForComplianceReview: always succeed.
public class OnboardingActivitiesImpl implements OnboardingActivities {

    private static final Logger log = LoggerFactory.getLogger(OnboardingActivitiesImpl.class);

    private final OnboardingRepository repo;

    public OnboardingActivitiesImpl(OnboardingRepository repo) {
        this.repo = repo;
    }

    @Override
    public void validateAndPersistApplication(String workflowId, OnboardingApplication application) {
        int attempt = Activity.getExecutionContext().getInfo().getAttempt();
        log.info("[{}] Validating application for customer {} (attempt {})",
                workflowId, application.customerId(), attempt);
        repo.insertRequest(workflowId, application);
        repo.appendAudit(workflowId, AuditEvent.APPLICATION_SUBMITTED,
                "customer=" + application.customerId(), attempt);
        repo.updateStatusAndAudit(workflowId, ApplicationStatus.PENDING,
                AuditEvent.APPLICATION_VALIDATED, null, attempt);
    }

    @Override
    public void submitToDocumentStore(String workflowId, String documentReference) {
        int attempt = Activity.getExecutionContext().getInfo().getAttempt();
        log.info("[{}] Storing documents (ref={}, attempt {})", workflowId, documentReference, attempt);
        // Simulated: in production this calls the Document Store API
        simulateLatency(300);
        repo.appendAudit(workflowId, AuditEvent.DOCUMENTS_STORED,
                "ref=" + documentReference, attempt);
    }

    @Override
    public boolean runKycCheck(String workflowId, String customerId) {
        int attempt = Activity.getExecutionContext().getInfo().getAttempt();
        log.info("[{}] Running KYC check for {} (attempt {})", workflowId, customerId, attempt);

        repo.updateStatusAndAudit(workflowId, ApplicationStatus.KYC_IN_PROGRESS,
                AuditEvent.KYC_CHECK_STARTED, "attempt=" + attempt, attempt);

        Activity.getExecutionContext().heartbeat("kyc-in-progress-attempt-" + attempt);

        simulateLatency(500);

        boolean forceFail = "true".equalsIgnoreCase(System.getenv("KYC_FORCE_FAIL"));

        if (forceFail) {
            log.warn("[{}] KYC check FAILED (KYC_FORCE_FAIL=true)", workflowId);
            repo.updateStatusAndAudit(workflowId, ApplicationStatus.KYC_FAILED,
                    AuditEvent.KYC_CHECK_FAILED, "forced failure", attempt);
            return false;
        }

        if (attempt <= 2) {
            // Simulate transient vendor error on first two attempts
            log.warn("[{}] KYC vendor transient error on attempt {}",
                    workflowId, attempt);
            throw new RuntimeException("KYC vendor temporarily unavailable (attempt " + attempt + ")");
        }

        log.info("[{}] KYC check PASSED on attempt {}", workflowId, attempt);
        repo.updateStatusAndAudit(workflowId, ApplicationStatus.KYC_PASSED,
                AuditEvent.KYC_CHECK_PASSED, "attempt=" + attempt, attempt);
        return true;
    }

    @Override
    public void queueForComplianceReview(String workflowId, String customerId) {
        int attempt = Activity.getExecutionContext().getInfo().getAttempt();
        log.info("[{}] Queuing for compliance review (customer={}, attempt {})",
                workflowId, customerId, attempt);
        // Simulated: in production this creates a ticket in the review system
        simulateLatency(200);
        repo.updateStatusAndAudit(workflowId, ApplicationStatus.UNDER_REVIEW,
                AuditEvent.COMPLIANCE_REVIEW_QUEUED,
                "awaiting reviewer decision — 48h timer active", attempt);
    }

    @Override
    public void activateAccount(String workflowId, String customerId) {
        int attempt = Activity.getExecutionContext().getInfo().getAttempt();
        log.info("[{}] Activating account for customer {} (attempt {})",
                workflowId, customerId, attempt);
        // Simulated: in production this calls the Account Service API
        simulateLatency(300);
        repo.appendAudit(workflowId, AuditEvent.REVIEW_DECISION_RECEIVED,
                "decision=APPROVED", attempt);
        repo.updateStatusAndAudit(workflowId, ApplicationStatus.APPROVED,
                AuditEvent.ACCOUNT_ACTIVATED, "customer=" + customerId, attempt);
    }

    @Override
    public void rejectApplication(String workflowId, String reason) {
        int attempt = Activity.getExecutionContext().getInfo().getAttempt();
        log.warn("[{}] Rejecting application (attempt {}): {}", workflowId, attempt, reason);
        repo.updateStatusAndAudit(workflowId, ApplicationStatus.REJECTED,
                AuditEvent.APPLICATION_REJECTED, reason, attempt);
    }

    @Override
    public void rejectApplicationAfterReview(String workflowId) {
        int attempt = Activity.getExecutionContext().getInfo().getAttempt();
        log.warn("[{}] Rejecting application after compliance reviewer decision (attempt {})",
                workflowId, attempt);
        repo.appendAudit(workflowId, AuditEvent.REVIEW_DECISION_RECEIVED,
                "decision=REJECTED", attempt);
        repo.updateStatusAndAudit(workflowId, ApplicationStatus.REJECTED,
                AuditEvent.APPLICATION_REJECTED, "compliance review rejected", attempt);
    }

    @Override
    public void escalateReview(String workflowId) {
        int attempt = Activity.getExecutionContext().getInfo().getAttempt();
        log.warn("[{}] Compliance review timer expired — escalating to senior reviewer (attempt {})",
                workflowId, attempt);
        repo.updateStatusAndAudit(workflowId, ApplicationStatus.ESCALATED,
                AuditEvent.REVIEW_TIMER_ESCALATED,
                "48h timer expired — awaiting senior reviewer", attempt);
    }

    private static void simulateLatency(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

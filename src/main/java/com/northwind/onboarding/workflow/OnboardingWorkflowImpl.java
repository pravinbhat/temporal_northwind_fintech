package com.northwind.onboarding.workflow;

import com.northwind.onboarding.model.ApplicationStatus;
import com.northwind.onboarding.model.OnboardingApplication;
import com.northwind.onboarding.model.ReviewDecision;
import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.workflow.Workflow;
import org.slf4j.Logger;

import java.time.Duration;

public class OnboardingWorkflowImpl implements OnboardingWorkflow {

    private static final Logger log = Workflow.getLogger(OnboardingWorkflowImpl.class);

    // Production review timeout is 48 hours. For a quick escalation demo,
    // swap in Duration.ofSeconds(30) and restart the worker.
    private static final Duration REVIEW_TIMEOUT = Duration.ofHours(48);
    // private static final Duration REVIEW_TIMEOUT = Duration.ofSeconds(30); // ← uncomment for demo

    private ReviewDecision reviewDecision = null;
    private String currentStatus = ApplicationStatus.PENDING.name();

    private final OnboardingActivities activities = Workflow.newActivityStub(
            OnboardingActivities.class,
            ActivityOptions.newBuilder()
                    .setStartToCloseTimeout(Duration.ofSeconds(30))
                    .setRetryOptions(RetryOptions.newBuilder()
                            .setMaximumAttempts(3)
                            .setInitialInterval(Duration.ofSeconds(1))
                            .setBackoffCoefficient(2.0)
                            .build())
                    .build());

    private final OnboardingActivities kycActivities = Workflow.newActivityStub(
            OnboardingActivities.class,
            ActivityOptions.newBuilder()
                    .setStartToCloseTimeout(Duration.ofMinutes(2))
                    .setScheduleToCloseTimeout(Duration.ofHours(4))
                    .setHeartbeatTimeout(Duration.ofSeconds(30))
                    .setRetryOptions(RetryOptions.newBuilder()
                            .setMaximumAttempts(5)
                            .setInitialInterval(Duration.ofSeconds(2))
                            .setBackoffCoefficient(2.0)
                            .setMaximumInterval(Duration.ofSeconds(30))
                            .build())
                    .build());

    @Override
    public String startOnboarding(OnboardingApplication application) {
        String workflowId = Workflow.getInfo().getWorkflowId();
        log.info("Onboarding workflow started for customer {}", application.customerId());

        activities.validateAndPersistApplication(workflowId, application);
        currentStatus = ApplicationStatus.VALIDATED.name();

        activities.submitToDocumentStore(workflowId, application.documentReference());
        currentStatus = ApplicationStatus.KYC_IN_PROGRESS.name();
        boolean kycPassed = kycActivities.runKycCheck(workflowId, application.customerId());

        if (!kycPassed) {
            currentStatus = ApplicationStatus.KYC_FAILED.name();
            activities.rejectApplication(workflowId, "KYC check failed");
            return "REJECTED: KYC failed for customer " + application.customerId();
        }

        currentStatus = ApplicationStatus.KYC_PASSED.name();

        // Policy v2: re-verify documents after KYC passes
        int version = Workflow.getVersion("document-reverification-policy", Workflow.DEFAULT_VERSION, 2);
        if (version == 2) {
            activities.submitToDocumentStore(workflowId,
                    application.documentReference() + "-reverified");
        }

        currentStatus = ApplicationStatus.UNDER_REVIEW.name();
        activities.queueForComplianceReview(workflowId, application.customerId());

        log.info("Waiting for compliance review decision (timeout: {})", REVIEW_TIMEOUT);
        boolean decidedInTime = Workflow.await(REVIEW_TIMEOUT, () -> reviewDecision != null);

        if (!decidedInTime) {
            activities.escalateReview(workflowId);
            currentStatus = ApplicationStatus.ESCALATED.name();
            // Auto-reject if senior reviewer does not decide within 7 days.
            boolean seniorDecided = Workflow.await(Duration.ofDays(7), () -> reviewDecision != null);
            if (!seniorDecided) {
                currentStatus = ApplicationStatus.REJECTED.name();
                activities.rejectApplication(workflowId, "senior review timed out — auto-rejected");
                return "REJECTED: senior review timeout for customer " + application.customerId();
            }
        }

        if (reviewDecision == ReviewDecision.APPROVED) {
            currentStatus = ApplicationStatus.APPROVED.name();
            activities.activateAccount(workflowId, application.customerId());
            return "APPROVED: account activated for customer " + application.customerId();
        } else {
            currentStatus = ApplicationStatus.REJECTED.name();
            activities.rejectApplication(workflowId, "compliance review rejected");
            return "REJECTED: compliance review rejected for customer " + application.customerId();
        }
    }

    @Override
    public void reviewDecision(ReviewDecision decision) {
        log.info("Received review signal: {}", decision);
        this.reviewDecision = decision;
    }

    @Override
    public String getStatus() {
        return currentStatus;
    }
}

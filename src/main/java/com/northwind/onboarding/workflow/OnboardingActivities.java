package com.northwind.onboarding.workflow;

import com.northwind.onboarding.model.OnboardingApplication;
import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

/** Activities for the KYC onboarding process. */
@ActivityInterface
public interface OnboardingActivities {

    /** Validate input and write the initial row to onboarding_requests. */
    @ActivityMethod
    void validateAndPersistApplication(String workflowId, OnboardingApplication application);

    /** Store documents in the Document Store (simulated). */
    @ActivityMethod
    void submitToDocumentStore(String workflowId, String documentReference);

    /**
     * Call the KYC vendor API (simulated).
     * Returns true if KYC passed, false if failed.
     */
    @ActivityMethod
    boolean runKycCheck(String workflowId, String customerId);

    /** Write the compliance review task to the review queue (simulated). */
    @ActivityMethod
    void queueForComplianceReview(String workflowId, String customerId);

    /** Activate the account in the Account Service and update status. */
    @ActivityMethod
    void activateAccount(String workflowId, String customerId);

    /** Reject the application with the given reason (e.g. KYC failure, compliance review rejection). */
    @ActivityMethod
    void rejectApplication(String workflowId, String reason);

    /** Record that the 48-hour compliance review timer expired and escalate to a senior reviewer. */
    @ActivityMethod
    void escalateReview(String workflowId);
}

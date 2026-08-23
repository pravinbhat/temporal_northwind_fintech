package com.northwind.onboarding.workflow;

import com.northwind.onboarding.model.OnboardingApplication;
import com.northwind.onboarding.model.ReviewDecision;
import io.temporal.workflow.SignalMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

/**
 * Workflow interface for the KYC onboarding process.
 *
 * Lifecycle:
 *   ApplicationSubmitted → KYC Check → Compliance Review → Account Activated (or Rejected)
 */
@WorkflowInterface
public interface OnboardingWorkflow {

    String TASK_QUEUE = "kyc-onboarding";

    /** Runs the full onboarding process to a terminal state (APPROVED or REJECTED). */
    @WorkflowMethod
    String startOnboarding(OnboardingApplication application);

    /** Called by the compliance reviewer to approve or reject the application. */
    @SignalMethod
    void reviewDecision(ReviewDecision decision);
}

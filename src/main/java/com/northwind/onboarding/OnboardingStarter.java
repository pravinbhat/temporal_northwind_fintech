package com.northwind.onboarding;

import com.northwind.onboarding.model.OnboardingApplication;
import com.northwind.onboarding.workflow.OnboardingWorkflow;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.serviceclient.WorkflowServiceStubs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.UUID;

public class OnboardingStarter {

    private static final Logger log = LoggerFactory.getLogger(OnboardingStarter.class);

    public static void main(String[] args) {
        if (args.length < 3) {
            System.err.println("Usage: OnboardingStarter <customerId> <customerName> <email>");
            System.err.println("Example: OnboardingStarter CUST-12345 \"Alice Johnson\" alice@example.com");
            System.exit(1);
        }

        String customerId   = args[0];
        String customerName = args[1];
        String email        = args[2];

        String workflowId = "onboarding-" + customerId + "-" + UUID.randomUUID();

        OnboardingApplication application = new OnboardingApplication(
                customerId,
                customerName,
                email,
                "doc-ref-" + UUID.randomUUID()  // simulated document reference
        );

        log.info("Starting onboarding workflow for customer {} with workflow ID: {}",
                customerId, workflowId);

        WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();
        WorkflowClient client = WorkflowClient.newInstance(service);

        OnboardingWorkflow workflow = client.newWorkflowStub(
                OnboardingWorkflow.class,
                WorkflowOptions.newBuilder()
                        .setWorkflowId(workflowId)
                        .setTaskQueue(OnboardingWorkflow.TASK_QUEUE)
                        .setWorkflowExecutionTimeout(Duration.ofDays(30))
                        .build());

        WorkflowClient.start(workflow::startOnboarding, application);

        log.info("✓ Workflow started — ID: {}", workflowId);
        log.info("  Approve: curl -X POST http://localhost:8080/review/{}/approve", workflowId);
        log.info("  Reject:  curl -X POST http://localhost:8080/review/{}/reject", workflowId);

        service.shutdown();
    }
}

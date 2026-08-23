package com.northwind.onboarding;

import com.northwind.onboarding.db.DatabaseConfig;
import com.northwind.onboarding.db.OnboardingRepository;
import com.northwind.onboarding.workflow.OnboardingActivitiesImpl;
import com.northwind.onboarding.workflow.OnboardingWorkflow;
import com.northwind.onboarding.workflow.OnboardingWorkflowImpl;
import io.temporal.client.WorkflowClient;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import org.jdbi.v3.core.Jdbi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OnboardingWorker {

    private static final Logger log = LoggerFactory.getLogger(OnboardingWorker.class);

    public static void main(String[] args) {
        log.info("Starting Northwind KYC Onboarding Worker...");

        WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();
        WorkflowClient client = WorkflowClient.newInstance(service);

        Jdbi jdbi = DatabaseConfig.createJdbi();
        OnboardingRepository repo = new OnboardingRepository(jdbi);
        log.info("Database connection established");

        WorkerFactory factory = WorkerFactory.newInstance(client);
        Worker worker = factory.newWorker(OnboardingWorkflow.TASK_QUEUE);
        worker.registerWorkflowImplementationTypes(OnboardingWorkflowImpl.class);
        worker.registerActivitiesImplementations(new OnboardingActivitiesImpl(repo));

        log.info("Worker registered on task queue: {}", OnboardingWorkflow.TASK_QUEUE);

        factory.start();
        log.info("Worker started — polling for tasks. Press Ctrl+C to stop.");

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Shutting down worker...");
            factory.shutdown();
            service.shutdown();
        }));

        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

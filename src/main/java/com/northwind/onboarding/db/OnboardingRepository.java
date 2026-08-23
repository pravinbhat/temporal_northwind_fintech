package com.northwind.onboarding.db;

import com.northwind.onboarding.model.ApplicationStatus;
import com.northwind.onboarding.model.AuditEvent;
import com.northwind.onboarding.model.OnboardingApplication;
import org.jdbi.v3.core.Jdbi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OnboardingRepository {

    private static final Logger log = LoggerFactory.getLogger(OnboardingRepository.class);
    private final Jdbi jdbi;

    public OnboardingRepository(Jdbi jdbi) {
        this.jdbi = jdbi;
    }

    /** Insert the initial request row when an application is first received. */
    public void insertRequest(String workflowId, OnboardingApplication app) {
        jdbi.useHandle(h -> h.createUpdate("""
                INSERT INTO onboarding_requests
                    (workflow_id, customer_id, customer_name, email, status)
                VALUES
                    (:wid, :cid, :name, :email, :status)
                ON CONFLICT (workflow_id) DO NOTHING
                """)
                .bind("wid",    workflowId)
                .bind("cid",    app.customerId())
                .bind("name",   app.customerName())
                .bind("email",  app.email())
                .bind("status", ApplicationStatus.PENDING.name())
                .execute());
        log.info("[{}] inserted onboarding_request", workflowId);
    }

    public void appendAudit(String workflowId, AuditEvent event, String detail, int attempt) {
        jdbi.useHandle(h -> h.createUpdate("""
                INSERT INTO onboarding_audit (workflow_id, event, detail, activity_attempt)
                VALUES (:wid, :event, :detail, :attempt)
                ON CONFLICT ON CONSTRAINT uq_audit_workflow_event_detail_attempt DO NOTHING
                """)
                .bind("wid",     workflowId)
                .bind("event",   event.name())
                .bind("detail",  detail)
                .bind("attempt", attempt)
                .execute());
        log.info("[{}] audit ← {}{}", workflowId, event, detail != null ? ": " + detail : "");
    }

    /** Update the application status and append the corresponding audit event in a single transaction. */
    public void updateStatusAndAudit(String workflowId, ApplicationStatus status,
                                     AuditEvent event, String detail, int attempt) {
        jdbi.useTransaction(h -> {
            h.createUpdate("""
                    UPDATE onboarding_requests
                       SET status = :status, updated_at = now()
                     WHERE workflow_id = :wid
                    """)
                    .bind("status", status.name())
                    .bind("wid",    workflowId)
                    .execute();
            h.createUpdate("""
                    INSERT INTO onboarding_audit (workflow_id, event, detail, activity_attempt)
                    VALUES (:wid, :event, :detail, :attempt)
                    ON CONFLICT ON CONSTRAINT uq_audit_workflow_event_detail_attempt DO NOTHING
                    """)
                    .bind("wid",     workflowId)
                    .bind("event",   event.name())
                    .bind("detail",  detail)
                    .bind("attempt", attempt)
                    .execute();
        });
        log.info("[{}] status → {} | audit ← {}", workflowId, status, event);
    }
}

-- =============================================================================
-- Northwind KYC Onboarding — Database Schema
-- =============================================================================

-- One row per onboarding application; status is the current business state.
CREATE TABLE IF NOT EXISTS onboarding_requests (
    workflow_id     TEXT        PRIMARY KEY,
    customer_id     TEXT        NOT NULL,
    customer_name   TEXT        NOT NULL,
    email           TEXT        NOT NULL,
    status          TEXT        NOT NULL,   -- PENDING | KYC_IN_PROGRESS | KYC_PASSED | KYC_FAILED | UNDER_REVIEW | ESCALATED | APPROVED | REJECTED
    submitted_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Append-only audit log; one row per workflow event.
-- This is the business audit trail a regulator would query.
-- The unique constraint on (workflow_id, event, detail, activity_attempt) makes every activity
-- retry idempotent: if Temporal retries an activity that already wrote this row, the duplicate
-- INSERT is silently skipped.  Including `detail` allows the same event type to appear more
-- than once in a workflow with different details — e.g. DOCUMENTS_STORED fires twice when
-- policy v2 is active (initial store + re-verification).
CREATE TABLE IF NOT EXISTS onboarding_audit (
    id               BIGSERIAL   PRIMARY KEY,
    workflow_id      TEXT        NOT NULL,
    event            TEXT        NOT NULL,   -- see AuditEvent enum for all values
    detail           TEXT,                   -- optional free-text context
    activity_attempt INT         NOT NULL DEFAULT 1,
    occurred_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_audit_workflow_event_detail_attempt
        UNIQUE (workflow_id, event, detail, activity_attempt)
);

CREATE INDEX IF NOT EXISTS idx_audit_workflow_id ON onboarding_audit (workflow_id);

# Northwind KYC Onboarding — Temporal Prototype

A working demonstration of a Temporal-based fintech customer onboarding workflow.

---

## Project Structure

```
src/main/java/com/northwind/onboarding/
│
├── model/
│   ├── OnboardingApplication.java
│   ├── ApplicationStatus.java
│   ├── ReviewDecision.java
│   └── AuditEvent.java
│
├── db/
│   ├── DatabaseConfig.java
│   └── OnboardingRepository.java
│
├── workflow/
│   ├── OnboardingWorkflow.java
│   ├── OnboardingWorkflowImpl.java
│   ├── OnboardingActivities.java
│   └── OnboardingActivitiesImpl.java
│
├── OnboardingWorker.java
├── OnboardingStarter.java
└── ReviewServer.java

src/main/resources/
├── schema.sql
└── logback.xml
```

---

## Prerequisites

| Tool         | Version | Check                |
| -------------|---------|----------------------|
| Java         | 25+     | `java -version`      |
| Maven        | 3.9+    | `mvn -version`       |
| Temporal CLI | latest  | `temporal --version` |
| PostgreSQL   | 14+     | `psql --version`     |

### Install Temporal CLI

```bash
# macOS
brew install temporal

# Linux / manual
curl -sSf https://temporal.download/cli.sh | sh
```

---

## Setup

### 1. Start Temporal dev server

```bash
temporal server start-dev
```

### 2. Create the Postgres database and schema

```bash
createdb northwind
psql -U postgres -d northwind -f src/main/resources/schema.sql
```

Default connection settings (override with environment variables):

| Env var       | Default                                      |
|---------------|----------------------------------------------|
| `DB_URL`      | `jdbc:postgresql://localhost:5432/northwind` |
| `DB_USER`     | `postgres`                                   |
| `DB_PASSWORD` | `postgres`                                   |

---

## Running the Demo

Open **three terminal windows**.

### Terminal 1 — Start the Worker

```bash
mvn compile exec:java -Dexec.mainClass="com.northwind.onboarding.OnboardingWorker"
```

### Terminal 2 — Start the Review Server

```bash
mvn compile exec:java -Dexec.mainClass="com.northwind.onboarding.ReviewServer"
```

### Terminal 3 — Start an Onboarding Application

```bash
mvn compile exec:java -Dexec.mainClass="com.northwind.onboarding.OnboardingStarter" \
     -Dexec.args="CUST-001 'Alice Johnson' alice@example.com"
```

Once KYC passes and the workflow is waiting for compliance review, approve or reject:

```bash
curl -X POST http://localhost:8080/review/<workflowId>/approve
# or
curl -X POST http://localhost:8080/review/<workflowId>/reject
```

View workflow progress in the Temporal Web UI: `http://localhost:8233`

# Northwind KYC Onboarding — Temporal Prototype

A working demonstration of a Temporal-based fintech customer onboarding workflow.

📄 **[View slide deck (PDF)](docs/northwind-kyc-slides.pdf)**

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
| --------------| ---------| ----------------------|
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

View workflow progress in the Temporal Web UI: [localhost:8233](http://localhost:8233)

### 2. Create the Postgres database and schema

```bash
# Create the `northwind` DB in Postgres
createdb northwind

# macOS: Create the schema (using default user)
psql -d northwind -f src/main/resources/schema.sql

# Linux: Create the schema (using default user)
psql -U postgres -d northwind -f src/main/resources/schema.sql
```

Connection settings (configured via environment variables):

| Env var       | Required | Default                                      |
|---------------|----------|----------------------------------------------|
| `DB_URL`      | No       | `jdbc:postgresql://localhost:5432/northwind` |
| `DB_USER`     | **Yes**  | —                                            |
| `DB_PASSWORD` | **Yes**  | —                                            |

Export the required variables before starting any process:

```bash
export DB_USER=postgres
export DB_PASSWORD=postgres

# Optionally, if you want to FAIL KYC check
export KYC_FORCE_FAIL=true
```

### 3. Build the application

Build the project using Maven:

```bash
mvn clean package
```

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
     -Dexec.args="CUST-001 'Pravin Bhat' bhatman@gotham.com"
```

Once KYC passes and the workflow is waiting for compliance review, approve or reject:

```bash
curl -X POST http://localhost:8080/review/<workflowId>/approve
# or
curl -X POST http://localhost:8080/review/<workflowId>/reject
```

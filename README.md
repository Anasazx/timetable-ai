<div align="center">

# Timetable AI

**Extract a university timetable image into validated, recurring calendar events.**

Local AI inference through [LM Studio](https://lmstudio.ai/) meets a Spring Boot API that validates the result before calendar generation.

![License: MIT](https://img.shields.io/badge/license-MIT-green.svg)

</div>

## Overview

Timetable AI turns a timetable image into structured entries and, after validation and user confirmation, a recurring `.ics` calendar. The AI interprets the image; the backend owns validation, job state, and calendar syntax.

The project is currently backend-first. The `frontend/` directory is reserved for the client application and does not yet contain a checked-in frontend.

## Features

- Upload a timetable image with semester start and end dates
- Process extraction asynchronously as a persisted job
- Use a local vision-capable model through LM Studio
- Parse structured timetable entries from the model response
- Detect missing fields, invalid times, duplicates, and overlaps
- Review and confirm corrected timetable data
- Generate recurring weekly `.ics` events deterministically
- Keep image inference local instead of sending images to a hosted AI API

## Architecture

```text
Client
  |
  v
Spring Boot API ---> PostgreSQL
  |
  v
Extraction job service --async--> LM Studio / local vision model
  |
  v
Validation service
  |                 \
  v                  v
COMPLETED      NEEDS_CONFIRMATION ---> user correction ---> COMPLETED
  |
  v
iCalendar (.ics) generation
```

The model returns timetable data only. The backend creates `VEVENT`, `RRULE`, `UID`, `DTSTART`, and `DTEND` values so malformed calendar syntax cannot be produced by the model.

## Technology Stack

| Area | Technology |
| --- | --- |
| Backend | Java 17, Spring Boot 4.1.1, Spring Web MVC, Spring Data JPA |
| Persistence | PostgreSQL, Hibernate |
| AI | LM Studio with a local Qwen vision model |
| Build | Maven Wrapper |
| Calendar | iCalendar (`.ics`), recurring weekly events |

## Repository Layout

```text
.
|-- backend/timetable/
|   |-- pom.xml
|   |-- mvnw
|   `-- src/
|       |-- main/java/tn/rnu/isetmd/timetable/
|       |   |-- ai/          # LM Studio integration and response DTOs
|       |   |-- calendar/    # iCalendar generation
|       |   |-- config/      # Spring configuration
|       |   |-- job/         # Async extraction jobs and persistence
|       |   |-- storage/     # Uploaded image storage
|       |   |-- timetable/   # HTTP API and timetable domain code
|       |   `-- validation/  # Extracted data validation
|       `-- main/resources/
|           |-- application.properties
|           `-- prompts/
|-- frontend/                # Reserved for the frontend application
`-- samples/                 # Sample files
```

## Requirements

- Java 17
- PostgreSQL
- LM Studio with a vision-capable local model
- macOS, Linux, or Windows

Maven is not required globally because the repository includes the Maven Wrapper.

## Local Setup

### 1. Create the database

```sql
CREATE DATABASE timetable;
```

The default development configuration expects PostgreSQL on `localhost:5432` with database `timetable`. Set your credentials in `backend/timetable/src/main/resources/application.properties` or through your preferred Spring configuration mechanism.

### 2. Configure LM Studio

Start the LM Studio local server and load the model configured by the application. The defaults are:

```properties
ai.lm-studio.base-url=http://localhost:1234
ai.lm-studio.model=qwen3.5-9b
ai.lm-studio.temperature=0.1
```

The model name must match the model identifier exposed by your LM Studio server.

### 3. Start the backend

```bash
cd backend/timetable
./mvnw spring-boot:run
```

The API starts on `http://localhost:8080` by default.

### 4. Run tests

```bash
cd backend/timetable
./mvnw test
```

## API

### Start extraction

`POST /api/timetables/extract` accepts `multipart/form-data` with these fields:

| Field | Type | Description |
| --- | --- | --- |
| `image` | file | Timetable image |
| `semesterStart` | `YYYY-MM-DD` | First date of the semester |
| `semesterEnd` | `YYYY-MM-DD` | Last date of the semester |

```bash
curl -X POST http://localhost:8080/api/timetables/extract \
  -F "image=@timetable.jpg" \
  -F "semesterStart=2026-09-15" \
  -F "semesterEnd=2027-01-31"
```

The endpoint returns `202 Accepted` with a job identifier:

```json
{
  "jobId": "23d0a6db-8218-4f22-a35b-912ac3ab9c30",
  "status": "PROCESSING"
}
```

### Check job status

`GET /api/timetables/jobs/{jobId}` returns the current status and, when available, the extracted result and validation issues.

```bash
curl http://localhost:8080/api/timetables/jobs/23d0a6db-8218-4f22-a35b-912ac3ab9c30
```

Job statuses are:

| Status | Meaning |
| --- | --- |
| `PROCESSING` | The image is being processed asynchronously. |
| `NEEDS_CONFIRMATION` | The result contains issues that require review or correction. |
| `COMPLETED` | The timetable passed confirmation and is ready for calendar generation. |
| `FAILED` | Processing failed and an error message is available. |

### Confirm a timetable

`POST /api/timetables/jobs/{jobId}/confirm` accepts the corrected timetable as JSON and returns the updated job status.

```bash
curl -X POST \
  http://localhost:8080/api/timetables/jobs/23d0a6db-8218-4f22-a35b-912ac3ab9c30/confirm \
  -H "Content-Type: application/json" \
  -d @corrected-timetable.json
```

An extracted entry contains fields such as `day`, `start_time`, `end_time`, `subject`, `teacher`, `room`, `group`, `type`, and `uncertainties`. The complete request shape is defined by the DTOs under `backend/timetable/src/main/java/tn/rnu/isetmd/timetable/ai/dto/`.

## Configuration

The development defaults are stored in `backend/timetable/src/main/resources/application.properties`:

```properties
spring.application.name=timetable
server.port=8080

spring.datasource.url=jdbc:postgresql://localhost:5432/timetable
spring.datasource.username=postgres
spring.datasource.password=postgres
spring.jpa.hibernate.ddl-auto=update

ai.lm-studio.base-url=http://localhost:1234
ai.lm-studio.model=qwen3.5-9b
ai.lm-studio.temperature=0.1

spring.servlet.multipart.max-file-size=20MB
spring.servlet.multipart.max-request-size=20MB
```

Change the database password before using the application outside a local development environment. Do not commit real credentials or other secrets.

## Privacy and Data Flow

The intended setup keeps model inference on the local machine through LM Studio. Uploaded images and extraction results are handled by the local application and its configured PostgreSQL database. Review the storage and database configuration before using real personal data.

## Development Status

The backend contains the extraction job workflow, LM Studio integration, validation services, persistence, and timetable API. The frontend is not currently checked in, and deployment, authentication, migrations, and production monitoring should be treated as future work unless implemented separately.

## License

This project is licensed under the [MIT License](LICENSE).
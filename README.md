Timetable AI

An AI-powered university timetable extraction system that converts timetable images into structured calendar data and generates recurring .ics events.

The system uses a local vision-capable LLM through LM Studio to understand timetable images, while the backend handles validation, user confirmation, and deterministic calendar generation.

AI interprets. The backend validates. The user confirms. The backend generates the calendar.

⸻

Features

* Upload a university timetable image
* Extract timetable information using a local AI model
* Run AI locally through LM Studio
* Convert the image into structured timetable entries
* Validate extracted information
* Detect missing, invalid, suspicious, duplicated, or overlapping entries
* Allow the user to correct extracted data
* Require confirmation before accepting uncertain results
* Generate recurring .ics calendar events
* Support semester start and end dates
* No timetable images need to be sent to a third-party AI API
* Asynchronous AI processing
* Persist extraction jobs and results in PostgreSQL

⸻

Architecture

┌──────────────────────┐
│      Angular UI      │
└──────────┬───────────┘
           │
           │ HTTP
           ▼
┌──────────────────────┐
│   Spring Boot API    │
└──────────┬───────────┘
           │
           ▼
┌──────────────────────┐
│     ExtractionJob    │
│       Service        │
└──────────┬───────────┘
           │
           │ Async
           ▼
┌──────────────────────┐
│  ExtractionJobWorker │
└──────────┬───────────┘
           │
           ▼
┌──────────────────────┐
│      AiService       │
└──────────┬───────────┘
           │
           ▼
┌──────────────────────┐
│  LM Studio / Qwen    │
│       3.5 9B         │
└──────────┬───────────┘
           │
           │ Structured JSON
           ▼
┌──────────────────────┐
│ Timetable Validation │
│       Service        │
└──────────┬───────────┘
           │
     ┌─────┴──────┐
     │            │
     ▼            ▼
  COMPLETE   NEEDS_CONFIRMATION
                  │
                  ▼
             User reviews
                  │
                  ▼
               Confirm
                  │
                  ▼
             COMPLETED
                  │
                  ▼
       ┌───────────────────┐
       │ IcsCalendarService│
       └─────────┬─────────┘
                 │
                 ▼
              .ics file

⸻

Core Design Principle

The AI is not responsible for generating calendar files.

Its only responsibility is to interpret the timetable image and return structured data.

The backend is responsible for:

1. Validating the extracted data
2. Detecting inconsistencies
3. Asking the user for confirmation when necessary
4. Generating the final calendar deterministically

This separation prevents the AI from inventing calendar syntax or producing malformed calendar files.

⸻

Technology Stack

Backend

* Java 21
* Spring Boot 4
* Spring Web
* Spring Data JPA
* Hibernate
* PostgreSQL
* Jackson 3
* Lombok
* Maven

AI

* LM Studio
* Qwen 3.5 9B
* Local HTTP API
* Low temperature configuration for more deterministic extraction

Frontend

* Angular
* TypeScript

Calendar

* iCalendar (.ics)
* Recurring weekly events
* Africa/Tunis timezone

⸻

Project Structure

timetable-ai/
│
├── backend/
│   ├── src/
│   │   └── main/
│   │       ├── java/
│   │       │   └── tn/rnu/isetmd/timetable/
│   │       │       │
│   │       │       ├── ai/
│   │       │       │   ├── AiService.java
│   │       │       │   ├── LmStudioProperties.java
│   │       │       │   ├── LmStudioService.java
│   │       │       │   └── dto/
│   │       │       │       ├── AiTimetableResponse.java
│   │       │       │       ├── AiTimetableEntry.java
│   │       │       │       └── TimetableValidationIssue.java
│   │       │       │
│   │       │       ├── calendar/
│   │       │       │   └── IcsCalendarService.java
│   │       │       │
│   │       │       ├── common/
│   │       │       │   └── exception/
│   │       │       │       └── GlobalExceptionHandler.java
│   │       │       │
│   │       │       ├── config/
│   │       │       │   ├── AsyncConfig.java
│   │       │       │   └── SecurityConfig.java
│   │       │       │
│   │       │       ├── job/
│   │       │       │   ├── ExtractionJob.java
│   │       │       │   ├── ExtractionJobRepository.java
│   │       │       │   ├── ExtractionJobService.java
│   │       │       │   ├── ExtractionJobStatus.java
│   │       │       │   ├── ExtractionJobWorker.java
│   │       │       │   └── dto/
│   │       │       │
│   │       │       ├── storage/
│   │       │       │   └── ImageStorageService.java
│   │       │       │
│   │       │       ├── timetable/
│   │       │       │   ├── controller/
│   │       │       │   ├── dto/
│   │       │       │   ├── model/
│   │       │       │   └── service/
│   │       │       │
│   │       │       └── validation/
│   │       │           └── TimetableValidationService.java
│   │       │
│   │       └── resources/
│   │           └── application.properties
│   │
│   └── pom.xml
│
└── frontend/
    └── Angular application

⸻

How It Works

1. Upload timetable

The client sends a timetable image together with the semester dates.

POST /api/timetables/extract
Content-Type: multipart/form-data

Parameters:

image
semesterStart
semesterEnd

Example:

semesterStart = 2026-09-15
semesterEnd   = 2027-01-31

The API immediately creates an extraction job and returns its ID.

⸻

2. Asynchronous processing

AI extraction can take several seconds, especially when running a local vision model.

Therefore, the request does not wait for the entire AI operation.

The job initially has:

PROCESSING

The worker processes the image asynchronously using a dedicated executor.

Only one Qwen extraction job is processed at a time.

⸻

3. AI extraction

The timetable image is sent to LM Studio.

The configured model is:

qwen3.5-9b

LM Studio runs locally:

http://localhost:1234

The AI returns structured JSON.

Example:

{
  "entries": [
    {
      "day": "Lundi",
      "start_time": "10:10",
      "end_time": "11:40",
      "subject": "Intelligence Artificielle",
      "teacher": "Soumaya HAMROUN",
      "room": "L211",
      "group": null,
      "type": "cours",
      "uncertainties": []
    }
  ],
  "questions_for_user": []
}

⸻

Timetable Entry Model

Each extracted timetable entry contains:

Field	Description
day	Day of the week
start_time	Start time in HH:mm format
end_time	End time in HH:mm format
subject	Course/module name
teacher	Teacher name
room	Classroom
group	Optional student group
type	Course, workshop, project, etc.
uncertainties	Information the AI could not determine confidently

Example:

{
  "day": "Mardi",
  "start_time": "10:10",
  "end_time": "11:40",
  "subject": "Atelier intelligence artificielle",
  "teacher": "Soumaya HAMROUN",
  "room": "L214",
  "group": null,
  "type": "atelier",
  "uncertainties": []
}

⸻

Validation

AI output is never trusted blindly.

The backend validates every extracted entry.

Errors

Errors prevent the timetable from being automatically accepted.

Examples:

* Missing day
* Invalid day
* Missing subject
* Missing teacher
* Missing room
* Missing start time
* Missing end time
* Invalid time format
* Start time after end time
* Duplicate timetable entry

Example:

{
  "entryIndex": 4,
  "type": "MISSING_ROOM",
  "severity": "ERROR",
  "message": "The room is missing."
}

⸻

Warnings

Warnings indicate potentially suspicious data but do not necessarily prevent confirmation.

Examples:

* Suspicious subject name
* Unusually long class
* Overlapping classes

Example:

{
  "entryIndex": 5,
  "type": "OVERLAP",
  "severity": "WARNING",
  "message": "This entry overlaps with entry 2 on Mardi."
}

⸻

Job Lifecycle

A job can have one of four states:

PROCESSING
     │
     ├───────────────► FAILED
     │
     ▼
Validation
     │
     ├───────────────► COMPLETED
     │
     ▼
NEEDS_CONFIRMATION
     │
     │ User edits/confirms
     ▼
COMPLETED

PROCESSING

The image is currently being processed by the AI.

NEEDS_CONFIRMATION

The AI returned data containing validation issues that require the user to review.

COMPLETED

The timetable has been accepted and is ready for calendar generation.

FAILED

An unexpected error occurred during extraction or processing.

⸻

API

Extract timetable

POST /api/timetables/extract

Request

Content-Type: multipart/form-data

Fields:

image
semesterStart
semesterEnd

Example:

curl -X POST http://localhost:8080/api/timetables/extract \
  -F "image=@timetable.jpg" \
  -F "semesterStart=2026-09-15" \
  -F "semesterEnd=2027-01-31"

Response

{
  "jobId": "23d0a6db-8218-4f22-a35b-912ac3ab9c30",
  "status": "PROCESSING"
}

⸻

Get extraction status

GET /api/timetables/jobs/{jobId}

Example:

curl http://localhost:8080/api/timetables/jobs/23d0a6db-8218-4f22-a35b-912ac3ab9c30

Possible response:

{
  "jobId": "23d0a6db-8218-4f22-a35b-912ac3ab9c30",
  "status": "NEEDS_CONFIRMATION",
  "semesterStart": "2026-09-15",
  "semesterEnd": "2027-01-31",
  "result": {
    "entries": []
  },
  "validationIssues": [],
  "error": null
}

⸻

Confirm timetable

POST /api/timetables/jobs/{jobId}/confirm

The frontend sends the corrected timetable.

Example:

curl -X POST \
  http://localhost:8080/api/timetables/jobs/23d0a6db-8218-4f22-a35b-912ac3ab9c30/confirm \
  -H "Content-Type: application/json" \
  -d @corrected-timetable.json

If validation succeeds:

{
  "status": "COMPLETED"
}

⸻

Calendar Generation

Once a timetable is confirmed, the backend generates the .ics file.

Calendar generation is deterministic.

The AI does not generate:

* VEVENT
* RRULE
* UID
* DTSTART
* DTEND
* iCalendar syntax

The backend creates all of these.

Each timetable entry becomes a recurring weekly calendar event.

Conceptually:

Timetable Entry
       │
       ▼
Day + Start Time + End Time
       │
       ▼
Semester Start Date
       │
       ▼
Weekly Recurrence
       │
       ▼
Semester End Date
       │
       ▼
      .ics

Example calendar event structure:

BEGIN:VEVENT
UID:...
DTSTAMP:...
DTSTART:...
DTEND:...
SUMMARY:Intelligence Artificielle
DESCRIPTION:Teacher: Soumaya HAMROUN
LOCATION:L211
RRULE:FREQ=WEEKLY;UNTIL=...
END:VEVENT

⸻

Local Development

Requirements

Install:

* Java 21
* Maven
* PostgreSQL
* Node.js
* Angular CLI
* LM Studio

Verify Java:

java -version

Expected:

openjdk 21

Verify Maven:

./mvnw -version

Verify Node:

node -v

⸻

PostgreSQL Setup

Create the database:

CREATE DATABASE timetable;

Configure the credentials in:

backend/src/main/resources/application.properties

Example:

spring.datasource.url=jdbc:postgresql://localhost:5432/timetable
spring.datasource.username=postgres
spring.datasource.password=YOUR_PASSWORD

⸻

LM Studio Setup

Install LM Studio and download a vision-capable Qwen model.

Load:

qwen3.5-9b

Start the local server.

The application expects:

http://localhost:1234

Configuration:

ai.lm-studio.base-url=http://localhost:1234
ai.lm-studio.model=qwen3.5-9b
ai.lm-studio.temperature=0.1

The low temperature is intentional because timetable extraction should be as deterministic as possible.

⸻

Running the Backend

Navigate to the backend:

cd backend

Run:

./mvnw spring-boot:run

The API will be available at:

http://localhost:8080

⸻

Running the Frontend

Navigate to the frontend:

cd frontend

Install dependencies:

npm install

Run Angular:

npm start

The frontend will normally be available at:

http://localhost:4200

⸻

Configuration

Example application.properties:

spring.application.name=timetable
server.port=8080
# LM Studio
ai.lm-studio.base-url=http://localhost:1234
ai.lm-studio.model=qwen3.5-9b
ai.lm-studio.temperature=0.1
# Upload limits
spring.servlet.multipart.max-file-size=20MB
spring.servlet.multipart.max-request-size=20MB
# PostgreSQL
spring.datasource.url=jdbc:postgresql://localhost:5432/timetable
spring.datasource.username=postgres
spring.datasource.password=YOUR_PASSWORD
# Development
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true

Do not commit real database passwords or other secrets to Git.

⸻

Example Extraction

For a timetable containing:

Lundi
10:10 - 11:40
Intelligence Artificielle
Soumaya HAMROUN
L211

The AI produces:

{
  "day": "Lundi",
  "start_time": "10:10",
  "end_time": "11:40",
  "subject": "Intelligence Artificielle",
  "teacher": "Soumaya HAMROUN",
  "room": "L211",
  "group": null,
  "type": "cours",
  "uncertainties": []
}

The backend then validates it.

If valid, it can eventually become a recurring event:

Intelligence Artificielle
Every Monday
10:10 → 11:40
Room: L211
Teacher: Soumaya HAMROUN

The recurrence ends at the configured semester end date.

⸻

Error Handling

The application separates:

AI uncertainty

Handled by:

uncertainties
questions_for_user

Backend validation

Handled by:

TimetableValidationIssue

Processing failure

Handled by:

ExtractionJobStatus.FAILED

This distinction is important because an AI uncertainty is not necessarily a system error.

⸻

Privacy

The AI inference is designed to run locally through LM Studio.

The timetable image is processed by the local application and local model rather than being automatically sent to an external AI provider.

This makes the architecture suitable for timetable data that users may prefer to keep private.

⸻

Current Status

The following parts are implemented and tested:

* Timetable image upload
* Extraction job creation
* Asynchronous processing
* Local LM Studio integration
* Qwen timetable extraction
* Structured JSON parsing
* Object/array AI response handling
* Timetable validation
* Error detection
* Warning detection
* Duplicate detection
* Overlap detection
* User confirmation workflow
* Corrected timetable submission
* Job state management
* PostgreSQL persistence
* Complete frontend workflow
* Final .ics generation/download integration
* Authentication
* Production deployment
* Database migrations with Flyway
* Automated tests
* Production monitoring

⸻

Future Improvements

AI

* Improve extraction accuracy for low-quality images
* Support rotated timetable images
* Support multiple timetable layouts
* Better detection of groups
* Better uncertainty classification
* Support handwritten timetable information

Validation

* Detect more semantic inconsistencies
* Detect impossible room/group combinations
* Improve duplicate detection
* Distinguish legitimate parallel classes from accidental overlaps

Calendar

* Generate downloadable .ics
* Import directly into Google Calendar
* Support Apple Calendar
* Support Outlook
* Add calendar colors/categories
* Add notifications

Backend

* Add authentication
* Add user-specific timetables
* Add Flyway migrations
* Add automated tests
* Add rate limiting
* Add production logging
* Add image cleanup policies

Frontend

* Drag-and-drop image upload
* Extraction progress indicator
* Interactive timetable editor
* Validation issue highlighting
* Calendar preview
* One-click .ics download

⸻

Development Philosophy

This project intentionally avoids putting too much responsibility on the AI.

The architecture follows:

AI
↓
Interpretation
Backend
↓
Validation + Business Rules
User
↓
Confirmation
Backend
↓
Deterministic Calendar Generation

This makes the system more predictable, testable, and maintainable than allowing an LLM to directly generate calendar files.

⸻

License

This project is currently intended as an academic/personal project.

License information can be added here when the project is published.
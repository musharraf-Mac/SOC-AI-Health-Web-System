# Meridian Clinic — Microservices Health System

A distributed, microservices-based clinic management system built for the Service-Oriented Computing coursework. Patients can register, book appointments, track BMI, view prescriptions, and receive AI-assisted explanations of lab reports. Doctors manage their patient queue and write prescriptions. Lab staff upload reports for OCR + AI explanation.

---

## Architecture

```
                         ┌─────────────────────┐
                         │   Next.js Client     │
                         │   (localhost:3000)   │
                         └──────────┬───────────┘
                                    │
                         ┌──────────▼───────────┐
                         │    API Gateway        │
                         │   (localhost:8087)    │
                         │  JWT validation, CORS, │
                         │  rate limiting, routing│
                         └──────────┬───────────┘
        ┌───────────┬───────────────┼───────────────┬───────────┐
        ▼            ▼               ▼               ▼           ▼
   Auth Service  Appointment    Prescription/    BMI Service  Lab Report
   (8081)        Service (8082) Doctor Service   (8084)       Service (8083)
                                 (8086)
        │            │               │               │           │
        └────────────┴───────────────┴───────────────┴───────────┘
                    Each service → its own MongoDB Atlas database
```

Every request from the client goes through the Gateway. The Gateway validates the JWT and forwards requests to the correct microservice, attaching each service's individual API key. Services never call each other's databases directly.

---

## Services

| Service | Port | Owner | Responsibility |
|---|---|---|---|
| Client (Next.js) | 3000 | Musharraf-mac | Unified web interface |
| API Gateway | 8087 | musharraf-mac | Routing, JWT validation, CORS, rate limiting |
| Auth Service | 8081 | musharraf-mac | Registration, login, JWT issuance, profile, logout, account deletion |
| Appointment Service (doc_booking) | 8082 | Abisha | Booking, token numbers, doctor availability |
| Lab Report Service | 8083 | Niroshini2002 | Report upload, OCR extraction, AI explanation |
| BMI Service | 8084 | Deva-0907 | Public + authenticated BMI calculation, history |
| Doctor / Prescription Service | 8086 | Theebapriya | Doctor queue, prescriptions |
| Redis | 6379 | — | Backing store for Gateway rate limiting |

---

## Tech Stack

- **Backend:** Spring Boot (Java), Spring Cloud Gateway, Spring Security, Spring Data MongoDB
- **Frontend:** Next.js (App Router, TypeScript)
- **Database:** MongoDB Atlas — one database per service, shared cluster
- **Auth:** JWT (HS256), shared secret across services for independent verification
- **AI/OCR:** Tesseract (Tess4J) for text extraction, hosted LLM API for plain-language report explanations
- **Containerization:** Docker, Docker Compose

---

## Prerequisites

- Docker Desktop
- A MongoDB Atlas cluster with network access configured
- Node.js 20+ (only needed for local frontend development outside Docker)
- Java 17+ / Java 26 for the Gateway (only needed for local backend development outside Docker)

---

## Environment Variables

Create a `.env` file in the project root (same folder as `docker-compose.yml`):

```env
JWT_SECRET=your-shared-secret-min-32-characters

MONGODB_URI=mongodb+srv://<user>:<password>@cluster0.mongodb.net/soc_health_db

# One API key per service — generate a random 32+ char string for each
AUTH_API_KEY=
APPOINTMENT_API_KEY=
LAB_API_KEY=
BMI_API_KEY=
DOCTOR_API_KEY=
```

`.env` is excluded from version control via `.gitignore` — never commit real secrets.

---

## Running the System

Clone the repository, add your `.env` file at the root, then:

```bash
docker compose up --build
```

First build takes several minutes (6 Java images + 1 Node image). Subsequent runs are faster with cached layers.

Once running:
- Client: [http://localhost:3000](http://localhost:3000)
- Gateway: [http://localhost:8087](http://localhost:8087)

To stop:
```bash
docker compose down
```

To view logs for a specific service:
```bash
docker compose logs -f auth-service
```

---

## API Key Header Format

Every microservice requires an API key on all requests except Swagger UI, health checks, and error pages:

```
X-API-KEY: <service-specific-key>
```

In normal operation, the client never sends this directly — the Gateway attaches the correct key per service after validating the caller's JWT. The header is only needed when testing a service directly (bypassing the Gateway) in Postman.

| Service | Key |
|---|---|
| Auth Service | see `.env` → `AUTH_API_KEY` |
| Appointment Service | see `.env` → `APPOINTMENT_API_KEY` |
| Lab Report Service | see `.env` → `LAB_API_KEY` |
| BMI Service | see `.env` → `BMI_API_KEY` |
| Doctor Service | see `.env` → `DOCTOR_API_KEY` |

---

## Authentication

1. `POST /api/auth/register` — create an account (role: `PATIENT`, `DOCTOR`, or `LAB_ASSISTANT`)
2. `POST /api/auth/login` — returns a JWT (`token`) and `role`
3. Include the token on all subsequent requests: `Authorization: Bearer <token>`

Test credentials (development only):
```
Patient:  patient@test.com / password123
Doctor:   doctor@test.com  / password123
```

---

## Swagger / API Documentation

Each microservice exposes interactive OpenAPI docs once running:

| Service | Swagger UI URL |
|---|---|
| Auth Service | http://localhost:8081/swagger-ui/index.html |
| Appointment Service | http://localhost:8082/swagger-ui/index.html |
| Lab Report Service | http://localhost:8083/swagger-ui/index.html |
| BMI Service | http://localhost:8084/swagger-ui/index.html |
| Doctor Service | http://localhost:8086/swagger-ui/index.html |

Click **Authorize** on each page and paste that service's API key to enable "Try it out" on protected endpoints.

---

## Repository Structure

```
.
├── docker-compose.yml
├── .env                      # not committed
├── README.md
├── front-end/
│   └── health_client/        # Next.js client
└── back-end/
    ├── apigateway/
    ├── auth/
    ├── doc_booking/           # Appointment Service
    ├── lab_rep/               # Lab Report Service
    ├── bmi/                   # BMI Service
    └── doctor_page/           # Doctor / Prescription Service
```

Each backend folder is an independent Spring Boot Maven project with its own `Dockerfile`, `pom.xml`, and MongoDB database.

---

## Individual Contribution Matrix

| Student | Component | Key Contributions |
|---|---|---|
| [Name] | Client + API Gateway + Auth Service | Next.js client, Gateway routing/JWT/CORS/rate limiting, Auth Service |
| [Name] | Appointment Service | Booking logic, token numbering, doctor queue |
| [Name] | Doctor / Prescription Service | Prescription CRUD, doctor dashboard endpoints |
| [Name] | BMI Service | BMI calculation, history tracking, weight-goal suggestions |
| [Name] | Lab Report Service | OCR pipeline, AI explanation integration |

---

## Known Limitations

- OCR accuracy depends on scan quality; low-resolution or handwritten reports may need manual correction.
- AI-generated report explanations are general information only, not a diagnostic opinion.
- Logout is client-side token removal; JWTs remain technically valid until expiry (1 hour) even after logout.

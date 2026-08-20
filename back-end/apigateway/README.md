## API Key Header Format

Each microservice requires an API Key on every request (except public endpoints and Swagger UI):

Header: `X-API-KEY: <service-specific-key>`

| Service | Key (dev/test) |
|---|---|
| Auth Service | a-random-shared-key-for-this-service-abc123 |
| Appointment Service | [teammate's key] |
| Prescription Service | [teammate's key] |
| BMI Service | [teammate's key] |
| Lab Report Service | [teammate's key] |

In production, clients never call services directly — the API Gateway injects the correct key per service after validating the user's JWT/OAuth2 token.
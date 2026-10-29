# ft/lab-ollama: local inference diagnostics

This branch calls only local Ollama. Gemini, Groq and OpenRouter configuration is commented out; their API keys and old AI_PROVIDER_ORDER values cannot enable cloud inference in this implementation. OLLAMA_ENABLED is ignored: Ollama is always attempted. Cloud Tasks is disabled in application.yaml so a local upload is processed by this backend. Google Cloud Storage still stores the report PDF and explanation; GCS credentials and MongoDB configuration are still required for an end-to-end upload.

## Local configuration

```
AI_PROVIDER_ORDER=ollama
OLLAMA_BASE_URL=http://localhost:11434
OLLAMA_MODEL=qwen3.5:4b
OLLAMA_TIMEOUT_SECONDS=180
```

Start Ollama and check `ollama list`. The exact installed model name must match OLLAMA_MODEL. Restart the Java backend after configuration changes. For Docker Desktop, override OLLAMA_BASE_URL with http://host.docker.internal:11434; localhost inside a container does not refer to Windows. Docker Desktop must be running and Ollama must accept connections from the container network. Do not deploy this debugging branch to Cloud Run expecting to reach your laptop's localhost.

## Diagnose failures

Logs show the endpoint, model, timeout and elapsed time, never the report text, response bodies or raw exception details. A failed analysis retains the original PDF and saves a diagnostic in analysisError for the staff detail page. No explanation object is written on inference failure.

- OLLAMA_CONNECTION: address unreachable, server not running, DNS/network/container access problem.
- OLLAMA_NOT_FOUND: HTTP 404; check the base address and `ollama pull qwen3.5:4b`.
- OLLAMA_TIMEOUT: generation did not finish within the limit; inspect `ollama ps`, GPU/CPU memory and Ollama server logs. Cold loading or a long report may need more time.
- OLLAMA_REQUEST: HTTP 400; check model support and the Ollama version.
- OLLAMA_HTTP: server HTTP error; check available memory and server logs.
- OLLAMA_EMPTY / OLLAMA_INCOMPLETE / OLLAMA_RESPONSE: no usable, completed answer was received.
- OLLAMA_CONFIG: invalid address or timeout.

Use the staff report detail Retry explanation button after fixing the issue. Already completed reports are not reanalyzed by retry; upload a synthetic sample to test a fresh request. If a browser/Next.js proxy or API gateway times out first on a long request, check backend logs before uploading again; the backend may still be processing.

## Verify the Java client

Run from back-end/lab_rep (PowerShell):

```
.\mvnw.cmd -B '-Dtest=AiAnalysisServiceTest,LabReportServiceTest,ReportAccessTest' test
```

The real-model test is opt-in: append `'-Dollama.live=true'` while local Ollama is running. It sends only a synthetic sample, bypasses MongoDB/GCS, and exercises the actual application AI client. It does not verify a full authenticated PDF upload.

The previous multi-provider/deployment guide is retained below for reference; it does not describe the active inference behavior of this branch.

---

# Lab reports: Cloud Storage and AI fallback

Changes are on ft/lab. MongoDB keeps report metadata; new PDF and explanation bodies go to a private Google Cloud Storage bucket:

```
reports/<report-id>/blood-test.pdf
explanations/<report-id>/blood-test-explanation.json
```

The original name is sanitized, and a unique report ID prevents overwrites. Explanations record the original name, ID, provider, model and generation time. Existing JSON API fields remain available: analysisResult is loaded from storage for display, not stored in MongoDB for new reports.

## Required setup

1. Create a private Cloud Storage bucket near the Cloud Run service. Enable uniform bucket-level access and public access prevention. Do not grant allUsers access.
2. Set GCS_BUCKET to its bucket name (without gs://). Grant the lab-report Cloud Run service account Storage Object User on this bucket. Use the same JWT_SECRET as auth and the gateway, plus the existing API_KEY and MONGODB_URI.
3. Cloud Run uses its attached service account through Application Default Credentials. Do not upload a JSON service account key into your image. For local Java development run `gcloud auth application-default login`. Grant the local identity bucket access too.
4. For Docker Compose set GCP_ADC_FILE to the absolute path of your local ADC file; it is mounted read-only. On Windows it is normally under APPDATA/gcloud/application_default_credentials.json. The bind mount deliberately fails if the file does not exist. Never commit that file.
5. Keep LAB_REPORT_SERVICE on the gateway set to the lab-report Cloud Run HTTPS URL.

## Model order

Default: ollama -> gemini -> groq -> openrouter. Set AI_PROVIDER_ORDER to reorder or limit providers (comma separated). A missing cloud API key skips that provider. Each failed HTTP request, timeout, malformed/empty response advances to the next provider. Exhausting all providers marks the explanation FAILED and retains the PDF. Provider exception details are not returned or logged, to avoid exposing report text or keys.

| Provider | Default model | Configuration |
|---|---|---|
| Local Ollama | qwen3.5:4b | OLLAMA_MODEL, OLLAMA_BASE_URL, OLLAMA_ENABLED, OLLAMA_TIMEOUT_SECONDS |
| Gemini | gemini-2.5-flash | GEMINI_API_KEY, GEMINI_MODEL |
| Groq | llama-3.3-70b-versatile | GROQ_API_KEY, GROQ_MODEL |
| OpenRouter | deepseek/deepseek-chat-v3.1 | OPENROUTER_API_KEY, OPENROUTER_MODEL |

Run `ollama pull qwen3.5:4b`, then keep Ollama running. Native Java defaults to http://localhost:11434; Compose defaults to http://host.docker.internal:11434. Ollama must accept connections from Docker for this to work. Do not expose an unauthenticated Ollama server to the internet.

Cloud Run localhost is its own container, so your laptop's Ollama will not be discovered there. Set OLLAMA_ENABLED=false for cloud deployment unless you supply a securely reachable Ollama endpoint. Cloud fallback requires valid provider credentials and quota; models are configurable when provider availability changes. Cloud models may require billing. Report text is sent to each configured provider attempted; configure only providers you intend to use for that data.

## Durable processing on Cloud Run

For production enable Cloud Tasks and create a queue in the service region. Configure a bounded retry policy (for example 5 attempts with 30-second minimum backoff) and limit queue concurrency to suit model quotas.

Set:

```
REPORT_TASKS_QUEUE=projects/PROJECT/locations/REGION/queues/lab-report-analysis
REPORT_SERVICE_URL=https://YOUR-LAB-SERVICE.run.app
REPORT_TASKS_SERVICE_ACCOUNT=task-invoker@PROJECT.iam.gserviceaccount.com
```

Use the exact canonical service URL without a trailing slash. Grant the lab service account Cloud Tasks Enqueuer on the queue and Service Account User on the task invoker service account (to attach it). Grant the task invoker Cloud Run Invoker on the lab service. Ensure the Cloud Tasks service agent has its required token-generation permissions and the service ingress permits the task request. Use a private lab service where possible; the gateway also needs its existing Cloud Run invocation authentication/network setup.

Tasks call `/internal/reports/{id}/process`, outside the public gateway's report routes. The existing API key filter checks their API key; Cloud Run also validates task OIDC tokens when IAM authentication is required. Never route /internal/** through the public gateway.

With a queue configured, upload returns PENDING; the worker claims a MongoDB processing lease, saves the explanation and completes the record. Duplicate deliveries do not overwrite active processing. A lease expires after six minutes so task retries can recover interrupted processing. A failed worker returns 503 for the queue retry policy. If enqueue fails, the saved report is marked FAILED for a manual retry. Without REPORT_TASKS_QUEUE, processing is synchronous for local testing (60-second Ollama timeout; 45 seconds for each cloud provider). Configure a Cloud Run request timeout of at least 300 seconds for worker dispatch. Refresh the frontend report view to see completion; the page does not poll automatically.

## API and access

All report endpoints require a login JWT and the gateway-supplied API key. LAB_STAFF can upload/list/retry/delete/migrate. PATIENT can read only reports matching their token email. Do not give patients direct bucket credentials. Existing registration permits role selection; staff account provisioning should be restricted before using real patient data.

- POST /api/reports/upload: multipart patientName, patientEmail, reportType, file.
- GET /api/reports/view: staff list.
- GET /api/reports/{id}: metadata and explanation, accepts legacy file IDs too.
- GET /api/reports/{id}/file: authenticated PDF response.
- GET /api/reports/patient/{email}: patient-owned list.
- POST /api/reports/{id}/retry: retry a failed/pending explanation.
- DELETE /api/reports/delete/{id}: delete both objects and metadata; failed storage deletion retains the metadata for retry.
- POST /api/reports/{id}/migrate: copy a legacy GridFS report and successful analysis into GCS, read back and compare bytes, then update metadata. Existing GridFS objects are retained as a backup. Run this explicitly for each legacy report; migration has not been executed automatically. Review verification/backups before removing legacy copies.

Only readable PDFs containing selectable text are accepted, up to 20 MB and 60000 extracted characters for analysis. Scans need a real OCR integration; they return a clear validation error. AI output is presented as an explanation, not a diagnosis.

## Verification

From back-end/lab_rep:

```
./mvnw -B -Dtest=AiAnalysisServiceTest,LabReportServiceTest,ReportAccessTest test
```

From health_client: `npx tsc --noEmit`.

Before deployment, test with a non-sensitive sample: upload through gateway; verify both GCS paths and Mongo metadata; open PDF as its patient; confirm another patient gets 403; stop Ollama and confirm cloud fallback; simulate missing/exhausted keys and retry; verify Cloud Tasks delivery and failure retries. Live bucket permissions, cloud quota, task IAM and local Ollama require environment-specific verification.

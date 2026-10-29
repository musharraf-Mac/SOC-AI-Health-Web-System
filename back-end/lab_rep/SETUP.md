# Current backend: verified explanations and ordered model fallback

Active order: **ollama -> groq1 -> openrouter -> groq2**. Gemini is not implemented or called. The older deployment guide below is historical reference; its previous model lists do not apply to this configuration.

| Slot | Default model | Credentials |
|---|---|---|
| ollama | qwen3.5:4b | None for local Ollama |
| groq1 | openai/gpt-oss-120b | GROQ1_API_KEY, falling back to GROQ_API_KEY |
| openrouter | google/gemma-4-31b-it:free | OPENROUTER_API_KEY |
| groq2 | qwen/qwen3.8-27b | GROQ2_API_KEY, falling back to GROQ_API_KEY |

Groq1 and Groq2 are two model attempts on Groq, not two independent quota pools. They can share one Groq API key; account-level exhaustion can affect both. Missing cloud keys skip that slot. HTTP errors, quota errors, connection failures, timeouts, incomplete generation and unusable extractions move to the next slot. If every slot fails, the PDF remains saved, status is FAILED, and analysisError contains safe diagnostics. No explanation is saved as successful on total failure. API keys and raw provider responses are never logged.

## Configuration

```
AI_PROVIDER_ORDER=ollama,groq1,openrouter,groq2
OLLAMA_ENABLED=true
OLLAMA_BASE_URL=http://localhost:11434
OLLAMA_MODEL=qwen3.5:4b
OLLAMA_TIMEOUT_SECONDS=180
GROQ_API_KEY=your-groq-key
OPENROUTER_API_KEY=your-openrouter-key
```

Optional GROQ1_MODEL, GROQ2_MODEL, OPENROUTER_MODEL override the defaults above. Optional GROQ1_API_KEY and GROQ2_API_KEY override the shared key. Cloud timeouts default to 45 seconds, configurable with GROQ1_TIMEOUT_SECONDS, GROQ2_TIMEOUT_SECONDS and OPENROUTER_TIMEOUT_SECONDS. Models require provider access and quota. Model availability is not guaranteed by local tests.

For native Java use localhost. In Docker Desktop set OLLAMA_BASE_URL=http://host.docker.internal:11434. An explicit localhost value in your .env overrides Compose's host.docker.internal default. On Cloud Run, localhost cannot reach your laptop; disable Ollama or provide a securely reachable endpoint. Current queue configuration remains disabled for local processing; enabling production Cloud Tasks is a separate deployment choice. The maximum default model wait is roughly 315 seconds across all four attempts, excluding storage; configure gateway/request timeouts accordingly or enable durable processing before production use.

## How explanation accuracy changed

The model extracts structured measurements rather than writing clinical conclusions. Each entry must include its name, value, unit, printed numeric reference interval, and an exact contiguous quotation from the extracted PDF. Java checks the evidence and compares numbers with BigDecimal; the patient summary is rendered by backend code. Thus MCV 80 against 81-101 is below range, and MCHC 37.5 against 31.5-34.5 is above range. Models cannot substitute generic medical ranges or insert claims that normal results rule out disease. Internal document-security instructions are not patient-facing text and do not imply that the report is medically unreliable.

Ambiguous, qualitative, age/sex-dependent or unverifiable entries are omitted with a limitation notice. At least one verifiable numeric interval is required. Extraction can still miss or misassociate results: this is not a clinical validation or a complete report interpretation. Scanned PDFs still require OCR. Review original PDF evidence before clinical use.

New uploads get the new explanation. Existing COMPLETED explanations are not rewritten automatically and the retry endpoint returns those unchanged. Test with a new synthetic PDF; do not delete real patient reports just to regenerate an explanation.

## Tests

From back-end/lab_rep in PowerShell:

```
.\mvnw.cmd -B '-Dtest=AiAnalysisServiceTest,LabExplanationFormatterTest,LabReportServiceTest,ReportAccessTest' test
```

Append `'-Dollama.live=true'` to enable a live synthetic two-measurement extraction through the actual local Java/Ollama client. It bypasses MongoDB/GCS and does not verify authenticated uploads or live cloud calls. Unit tests simulate cloud transport/errors; cloud providers are not called with patient data during verification.

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

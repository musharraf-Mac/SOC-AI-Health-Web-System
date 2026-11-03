# Credential exposure review — 3 November 2026

## Confirmed findings

**Rotate the MongoDB credentials and shared JWT signing secret.** Cleaning today's files does not revoke credentials already present in Git history or logs.

1. The local JWT secret matched the value committed in `back-end/auth/.env.example`. The example now leaves the value blank and instructs the developer to generate a private random value. Earlier commits still contain the old value.
2. Historical versions of `back-end/doctor_page/src/main/java/com/example/doctor_page/application.properties` and `back-end/doctor_page/src/main/resources/application.yaml` contain MongoDB URIs matching local environment configuration. Other older booking, BMI and lab-service config blobs also contain credential-bearing MongoDB URIs. Secret values are deliberately omitted here.
3. Auth `/me` logged the complete Authorization header and a token prefix. Auth startup/configuration and BMI configuration logged MongoDB connection strings. Those logs were removed; auth token-parser logging no longer includes raw parser exception messages that may contain token material.
4. Backend `.dockerignore` files had their `.env` exclusion commented out. The current backend Dockerfiles copy selected source files, not the entire context, but the credentials still did not belong in build contexts. All service/frontend Docker contexts now exclude environment files, private keys, and common Google credential filenames.

The first local read-only check also found HTTP 200 from `/api/doctor/patients` without Authorization through the running gateway. Only the status was inspected. The updated gateway and doctor-service ownership checks must be deployed together; frontend filtering alone does not protect data returned by an old backend.

## Changes made

- Removed credential/bearer-token logs and the committed example signing value.
- Extended `.gitignore` for environment variants and common private-key/service-account files, preserving example templates.
- Excluded those files from all seven Docker build contexts.
- Reduced the appointment MongoDB driver log level from DEBUG to WARN.
- Added authenticated, current-account checks around doctor records and prescription mutations; protected booking/doctor gateway routes.
- Preserved actual local secret values and kept them out of tool output, application code and this report. The missing local booking secret was copied from the existing auth configuration only as a configuration repair, not a replacement credential.

## Required follow-up

1. In MongoDB, rotate/reset the exposed database user's password (or provision a replacement restricted database user). Update `MONGODB_URI` wherever it is deployed and in ignored local configuration, then revoke the old credentials. Coordinate the rollout to avoid leaving services unable to connect.
2. Generate a new cryptographically random JWT signing secret of at least 32 bytes. Set the same new value in auth, the gateway, and every service that validates JWTs. Restart/redeploy them together. Previously issued tokens should no longer validate; users must log in again. Do not use the public example value or paste the new value into Git/chat.
3. Deploy the logging fixes and review access to existing local/cloud logs and stored build artifacts that could contain the old values. Restrict/remove old copies according to your retention requirements.
4. After revocation, decide whether to clean repository history with your collaborators. History cleanup involves rewriting commits and coordinating other clones/forks; it was not performed here and is not a substitute for rotation.

## Scope and limits

The review checked tracked working-tree text, relevant authentication/configuration code, ignore rules, and 652 reachable Git blobs below 1 MB using credential patterns and exact comparisons against known local secrets. It found 12 historical candidate blobs, including confirmed local-value matches. After cleanup, no known local secret matched tracked/unignored source files or the 20 generated production browser JavaScript files. Only paths, blob identifiers and finding categories were printed, never secret values. The URI example in the current README was checked and is a placeholder, not a live credential.

This is not a complete penetration test or a guarantee that all secrets have been found. Unfetched remote refs, forks, provider logs, deployed environments and arbitrarily named external credential files were not audited. Other legacy endpoints outside doctor scheduling/prescriptions were not comprehensively authorization-tested.

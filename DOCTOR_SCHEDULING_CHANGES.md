# Doctor scheduling and record access — 3 November 2026

## Why the previous page showed only a retry button

The previous availability component replaced the form with a retry button whenever its GET request failed. It also discarded the HTTP status, so authentication failures and an old/missing endpoint looked the same.

During this review, `back-end/doc_booking/.env` lacked `JWT_SECRET`. An unauthenticated request to the running local gateway's `/api/doctor/patients` returned HTTP 200. The source changes were therefore not sufficient evidence that the running services had been updated. No patient response body was inspected. The exact error behind the screenshot cannot be established from the screenshot alone.

The new form stays visible on failure, displays a status-specific error, and disables editing/saving until the existing schedule loads. A response from an old appointment backend is explicitly rejected instead of being mistaken for an empty schedule.

## What doctors can do

1. Enter their specialization.
2. Select any weekdays, a starting date, start/end time, and appointment duration (30 minutes by default).
3. Choose **Repeat every week** or **Only once**.
4. Click **Add time range**, then **Save availability** to publish it. Edit/remove controls change existing rules. Form changes must first be added or updated before they can be saved.
5. Review the next 14 calendar days, including today. Every row shows the date, weekday and appointment start times in Asia/Colombo.
6. Use **Skip day** to exclude one date, then save. **Restore day** removes that exclusion. Other weeks keep the same schedule.

For a one-time rule, each selected weekday occurs once in the seven days starting from the chosen start date. A weekly rule continues indefinitely until removed. The end time is when the last appointment finishes: 09:00–10:00 with 30-minute appointments produces 09:00 and 09:30 starts. Ranges must contain whole appointments and overlapping rules are rejected, including overlaps beyond the visible window.

Skipping a day or removing a rule changes future availability; it does not cancel existing bookings. Confirmed appointments remain in both dashboards. The doctor's preview shows working times; patients see only open, unbooked times.

## How persistence and recurrence work

No new MongoDB collection is introduced:

| Existing collection | Purpose |
| --- | --- |
| `users` | Registered account identity and role. Passwords are not mapped into the booking/doctor services. |
| `doctor` | Specialization, embedded `availabilityRules`, and `excludedDates`. |
| `bookings` | Patient appointments, including the selected duration for new bookings. |
| `prescriptions` | Existing prescription storage, now checked against the doctor's own booking. |

`AvailabilitySchedule` generates only today through today + 13 days whenever availability is read. Weekly rules are not copied indefinitely, and no scheduled job is needed. The server enforces the same window when accepting a booking. Previously published individual slots are retained in `oneOffSlots` when a profile is next saved; the response's `availableSlots`, `availableDates` and `availableTimes` stay compatible with the booking UI.

Old individual slots/bookings have no duration field, so overlap checks treat them as 30 minutes. This is a compatibility assumption; review unusually long historical appointments before publishing an overlapping new schedule. Historical sample doctors/bookings are not deleted. The directory starts from registered DOCTOR accounts and excludes unregistered sample doctors.

New bookings use authenticated patient identity and registered doctor identity, rather than names/emails supplied by the browser. A deterministic MongoDB `_id` and `insert` prevent competing requests for the same doctor/date/start time from overwriting each other. Existing appointments also block overlapping intervals after a schedule change. Tests cover ordinary conflicting claims; live multi-instance concurrency during simultaneous schedule edits has not been integration-tested.

## Doctor-specific records

- The doctor service verifies the JWT, then loads the current registered user by its subject. Queries use that account's email and require the current DOCTOR role, so an old email/role claim cannot select another doctor's records.
- Appointment lists and token lookups query only that doctor's bookings.
- Prescription history requires a patient booked with that doctor and returns prescriptions from that doctor only.
- Prescription creation requires the selected appointment token. Doctor/patient identity is derived from that booking, and a submitted prescription ID cannot overwrite an existing record.
- Prescription deletion now requires the owning doctor. Anonymous and other-doctor requests are rejected.
- Responses use `Cache-Control: no-store`. The frontend also filters by the authenticated doctor's email and clears previous prescription data during patient changes, including when requests fail or arrive out of order.
- The gateway now requires a valid bearer token for `/api/doc_booking/**` and `/api/doctor/**`; the services still perform the role/ownership checks themselves.

The cream, teal and coral theme, header controls, and appointment/prescription cards remain. The schedule form uses the same styling; smaller screens stack the cards without horizontal overflow. Date labels now include weekdays in both dashboards. Patient names and appointment text remain black.

## Local configuration changes

Only ignored local environment files were changed:

- Added the existing auth-service `JWT_SECRET` to `back-end/doc_booking/.env` without printing it.
- Added the auth-service `DATABASE` to `back-end/doctor_page/.env`. Its URI already selected the same database/host; making the database explicit avoids differing defaults.

These are configuration repairs, **not credential rotation**. See `SECURITY_REVIEW.md`: the shared credentials have historical exposure and need replacement.

The appointment service now honors `${PORT:8082}`; the doctor service's fallback port is corrected to 8086 rather than the lab service's 8083.

## What to restart or deploy

Source changes do not update already-running JARs or Cloud Run revisions. Rebuild/restart `doc_booking`, `doctor_page`, and `apigateway`, and rebuild the frontend. Rebuild/redeploy `auth` and `bmi` too to remove credential logging. The lab-service application code is unchanged; its Docker ignore rules now exclude secrets.

All backend services must use the intended shared `MONGODB_URI`, `DATABASE`, `JWT_SECRET` and service `API_KEY`. Keep the gateway's `APPOINTMENT_SERVICE` and `DOCTOR_SERVICE` pointed at the new revisions. `BOOKING_TIME_ZONE` must remain `Asia/Colombo` to match the displayed timezone. The frontend's `NEXT_PUBLIC_API_URL` must point at this gateway and is set at build time; never put a service API key or JWT secret in a `NEXT_PUBLIC_` variable.

For local Docker Compose, from `back-end`:

```powershell
docker compose up -d --build appointment-service doctor-service api-gateway auth-service bmi-service
```

For services run in an IDE, restart those Spring Boot applications after rebuilding. A `.env` edit is not reliably picked up by an already-running process. Restart the frontend development server if its API URL changes. For Cloud Run/Netlify, deploy the backend revisions and frontend build; local `.env` changes do not update cloud configuration.

Rotate the exposed database credentials and JWT secret as described in the security review, then sign in again. Verify in order: anonymous doctor-list request is rejected; a doctor can load/save a schedule; a patient sees the matching dates/times; a booking appears for its doctor; a second doctor cannot retrieve it. These live account checks are still needed after deployment.

## Verification performed

- 19 appointment-service tests: recurrence, one-time rules, rolling window, exclusions, timezone boundaries, validation, HTTP schedule payloads, booking identity, duration conflicts, and cancellation ownership.
- 5 doctor-service tests: own records, revoked/deleted accounts, token/history access, prescription creation and deletion ownership.
- 1 gateway test covering protected appointment/doctor paths with missing, invalid and valid tokens.
- TypeScript and ESLint checks for the changed frontend files; successful production Next.js build.
- Browser checks with mocked APIs: weekly and one-time rules, saving/reloading exclusions, patient booking, mixed-doctor responses, failed/old-service states, black text, and desktop/mobile rendering.
- Auth and BMI compilation after logging cleanup.

Tests use mock repositories/API data. They do not verify live MongoDB writes or cloud configuration, and no live patient data was changed. The first production-build attempt was blocked from fetching the existing Google Fonts; the permitted network-enabled rerun succeeded.

The work is uncommitted on the existing `develop` checkout. No push, live deployment, secret rotation, or Git history rewrite was performed.

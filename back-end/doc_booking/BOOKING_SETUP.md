# Registered doctors and appointment booking

This flow uses existing MongoDB collections only:

- `users`: read the registered account name, email and role. No passwords are mapped into the booking service.
- `doctor`: save specialization, embedded `availabilityRules`, and `excludedDates`. Future `availableSlots` are generated for a rolling 14-day window. Legacy individual slots are preserved in `oneOffSlots`; availableDates/availableTimes are maintained for compatibility.
- `bookings`: save appointments. The doctor service reads this same collection.

## Deployment

Redeploy the appointment service (`doc_booking`), doctor service (`doctor_page`), gateway and frontend together. Auth and BMI also need redeploying for credential-logging cleanup. Configure the services to use the same existing MongoDB database: `MONGODB_URI` and `DATABASE`. The booking service previously hardcoded soc_health_db; it now honors DATABASE. The doctor service honors DATABASE, falling back to the database in its URI when it is absent. See `../../DOCTOR_SCHEDULING_CHANGES.md` and `../../SECURITY_REVIEW.md` for the complete change explanation and required credential rotation.

Set the same `JWT_SECRET` as the auth service in both services. The gateway already forwards /api/doc_booking/** and /api/doctor/** with X-API-KEY. Browsers send the user's bearer token through the gateway.

Booking times use Asia/Colombo. Keep `BOOKING_TIME_ZONE=Asia/Colombo` to match the dashboard's stated timezone. The server rejects past/invalid dates, unavailable slots, unregistered doctors, and requests without the correct account role.

## Use

1. Register or sign in with a DOCTOR account.
2. In My availability, enter specialization, select weekdays and a time range/duration, choose weekly repeat or only once, Add time range, then Save availability. A one-time rule applies to each selected weekday once in the seven days starting from the chosen date. Skip/restore individual dates in the 14-day preview and save to publish those changes.
3. In the patient dashboard, open Book Doctor or choose Refresh availability. Only registered doctors with future, unbooked published slots appear.
4. Pick a date, then one of that date's times. The backend derives the patient identity from the signed-in account, ignoring patient identity supplied in the body.
5. The appointment appears in My Appointments and in the selected doctor's My appointments list. Use Refresh on the doctor dashboard to see new bookings.

GET /api/doc_booking/doctor/me/availability and PUT to the same URL manage only the signed-in doctor's profile. The schedule payload is:

```json
{
  "specialization": "General practitioner",
  "availabilityRules": [{
    "id": "unique-rule-id",
    "startDate": "2026-11-03",
    "daysOfWeek": ["MONDAY", "WEDNESDAY"],
    "startTime": "09:00",
    "endTime": "12:00",
    "slotMinutes": 30,
    "repeatWeekly": true
  }],
  "excludedDates": ["2026-11-09"]
}
```

The prior availableSlots-only request is accepted for older clients during rollout. The new editor requires the updated backend response, including availabilityRules, excludedDates and windowStart/windowEnd. Repeats are generated on reads and do not need a cron job.

The existing /api/doctor/patients endpoint and token lookup now require a DOCTOR bearer token and query by that doctor's email. Patients can list/cancel only their own bookings.

One patient can reserve each slot. New bookings use a deterministic slot-based _id and MongoDB insert, so simultaneous inserts cannot overwrite each other. A precheck also respects existing bookings with older IDs. Cancellation removes the existing booking, freeing the slot if the doctor still publishes it.

Old sample doctor records and historical bookings are not deleted. Unregistered sample doctors are excluded from the directory, and the sample insertion script is disabled. Registered doctors must publish availability using the new editor. Existing appointments remain even if a doctor removes their availability.

Registered means an account with the DOCTOR role in users; this change does not add professional-credential verification.

## Verification

Run `./mvnw -Dtest=BookingFlowTest,AvailabilityScheduleTest test` in doc_booking, `./mvnw -Dtest=DoctorAppointmentsTest test` in doctor_page, and `./mvnw -Dtest=BookingGatewayAuthTest test` in apigateway (use mvnw.cmd on Windows). Tests mock repositories; they do not connect to or modify the live database. Browser checks likewise use sample API responses.

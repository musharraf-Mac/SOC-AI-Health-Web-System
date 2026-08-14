use health_db;

db.doctors.insertMany([
    {
        name: "Dr. Sarah Smith",
        email: "dr.smith@hospital.com",
        specialization: "Cardiologist",
        availableDates: ["2026-08-20", "2026-08-21", "2026-08-22"],
        availableTimes: ["09:00", "10:00", "11:00", "14:00", "15:00"]
    },
    {
        name: "Dr. John Davis",
        email: "dr.davis@hospital.com",
        specialization: "Dermatologist",
        availableDates: ["2026-08-20", "2026-08-22", "2026-08-23"],
        availableTimes: ["09:30", "10:30", "13:00", "14:30"]
    },
    {
        name: "Dr. Emily Wilson",
        email: "dr.wilson@hospital.com",
        specialization: "Pediatrician",
        availableDates: ["2026-08-21", "2026-08-22", "2026-08-24"],
        availableTimes: ["08:00", "09:00", "10:00", "11:00"]
    }
]);

print("✅ Doctors inserted successfully!");

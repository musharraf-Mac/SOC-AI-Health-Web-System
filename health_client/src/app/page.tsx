"use client";

import { useEffect, useRef, useState } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";

export default function HomePage() {
  const router = useRouter();
  const pathRef = useRef<SVGPathElement>(null);
  const [drawn, setDrawn] = useState(false);

  useEffect(() => {
    if (pathRef.current) {
      const length = pathRef.current.getTotalLength();
      pathRef.current.style.strokeDasharray = `${length}`;
      pathRef.current.style.strokeDashoffset = `${length}`;
      requestAnimationFrame(() => setDrawn(true));
    }
  }, []);

  return (
    <div
      style={{
        width: "100%",
        overflowX: "hidden",
        minHeight: "100vh",
        background: "#F6F3EC",
        color: "#1B1B18",
        fontFamily:
          "'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif",
      }}
    >
      {/* NAV */}
      <header
        className="home-header"
        style={{
          display: "flex",
          alignItems: "center",
          justifyContent: "space-between",
          borderBottom: "1px solid #DCD6C8",
          position: "sticky",
          top: 0,
          background: "rgba(246,243,236,0.9)",
          backdropFilter: "blur(6px)",
          zIndex: 10,
        }}
      >
        <div
          style={{
            fontFamily: "'Fraunces', Georgia, serif",
            fontSize: "20px",
            fontWeight: 600,
            letterSpacing: "-0.01em",
            color: "#0B3D3A",
          }}
        >
          <Link href="/" className="text-inherit decoration-none">
         Meridian&nbsp;Clinic
         </Link>
        </div>

        <nav className="home-nav" style={{ display: "flex", alignItems: "center", gap: "8px" }}>
          <NavButton onClick={() => router.push("/bmi")}>BMI Check</NavButton>
          <NavButton onClick={() => router.push("/patient/dashboard")}>
            Book Appointment
          </NavButton>
          <span
            style={{
              width: "1px",
              height: "22px",
              background: "#DCD6C8",
              margin: "0 4px",
            }}
          />
          <button
            onClick={() => router.push("/login")}
            style={{
              padding: "9px 18px",
              fontSize: "14px",
              fontWeight: 500,
              color: "#0B3D3A",
              background: "transparent",
              border: "1px solid #0B3D3A",
              borderRadius: "6px",
              cursor: "pointer",
              transition: "all 0.15s ease",
            }}
            onMouseEnter={(e) => {
              e.currentTarget.style.background = "#0B3D3A";
              e.currentTarget.style.color = "#F6F3EC";
            }}
            onMouseLeave={(e) => {
              e.currentTarget.style.background = "transparent";
              e.currentTarget.style.color = "#0B3D3A";
            }}
          >
            Log in
          </button>
          <button
            onClick={() => router.push("/register")}
            style={{
              padding: "9px 18px",
              fontSize: "14px",
              fontWeight: 600,
              color: "#F6F3EC",
              background: "#FF8A5B",
              border: "1px solid #FF8A5B",
              borderRadius: "6px",
              cursor: "pointer",
              transition: "opacity 0.15s ease",
            }}
            onMouseEnter={(e) => (e.currentTarget.style.opacity = "0.88")}
            onMouseLeave={(e) => (e.currentTarget.style.opacity = "1")}
          >
            Register
          </button>
        </nav>
      </header>

      {/* HERO */}
      <section
        className="home-hero"
        style={{
          maxWidth: "880px",
          margin: "0 auto",
          padding: "96px 32px 48px",
          textAlign: "center",
        }}
      >
        <p
          style={{
            fontSize: "13px",
            letterSpacing: "0.12em",
            textTransform: "uppercase",
            color: "#0B3D3A",
            opacity: 0.7,
            marginBottom: "18px",
            fontWeight: 600,
          }}
        >
          Care, coordinated
        </p>
        <h1 className="home-title"
          style={{
            fontFamily: "'Fraunces', Georgia, serif",
            fontSize: "clamp(36px, 5.5vw, 58px)",
            fontWeight: 500,
            lineHeight: 1.08,
            letterSpacing: "-0.02em",
            color: "#0B3D3A",
            margin: "0 0 20px",
          }}
        >
          One record. One line to your doctor.
          <br />
          No paperwork in between.
        </h1>
        <p
          style={{
            fontSize: "17px",
            lineHeight: 1.6,
            color: "#4A463E",
            maxWidth: "560px",
            margin: "0 auto 36px",
          }}
        >
          Book a doctor, track your health, and get your lab results
          explained in plain language — all from one place.
        </p>

        <div
          style={{
            display: "flex",
            gap: "14px",
            justifyContent: "center",
            flexWrap: "wrap",
          }}
        >
          <button
            onClick={() => router.push("/patient/dashboard")}
            style={{
              padding: "14px 28px",
              fontSize: "15px",
              fontWeight: 600,
              color: "#F6F3EC",
              background: "#FF8A5B",
              border: "none",
              borderRadius: "8px",
              cursor: "pointer",
              transition: "transform 0.15s ease, opacity 0.15s ease",
            }}
            onMouseEnter={(e) => {
              e.currentTarget.style.transform = "translateY(-1px)";
            }}
            onMouseLeave={(e) => {
              e.currentTarget.style.transform = "translateY(0)";
            }}
          >
            Book an appointment
          </button>
          <button
            onClick={() => router.push("/bmi")}
            style={{
              padding: "14px 28px",
              fontSize: "15px",
              fontWeight: 600,
              color: "#0B3D3A",
              background: "transparent",
              border: "1.5px solid #0B3D3A",
              borderRadius: "8px",
              cursor: "pointer",
            }}
          >
            Check your BMI
          </button>
        </div>
      </section>

      {/* HEARTBEAT DIVIDER — signature element */}
      <div
        className="home-heartbeat"
        style={{
          maxWidth: "880px",
          margin: "40px auto 0",
          padding: "0 16px",
        }}
      >
        <svg
          viewBox="0 0 800 60"
          width="100%"
          height="60"
          style={{ display: "block" }}
        >
          <path
            ref={pathRef}
            d="M0,30 L200,30 L230,30 L250,8 L275,52 L300,30 L330,30 L345,18 L360,30 L800,30"
            fill="none"
            stroke="#FF8A5B"
            strokeWidth="2"
            strokeLinecap="round"
            strokeLinejoin="round"
            style={{
              transition: drawn ? "stroke-dashoffset 1.4s ease-out" : "none",
              strokeDashoffset: drawn ? 0 : undefined,
            }}
          />
        </svg>
      </div>

      {/* FEATURE ROW */}
      <section
        className="home-features"
        style={{
          maxWidth: "980px",
          margin: "0 auto",
          padding: "24px 16px 72px",
          display: "grid",
          gridTemplateColumns: "repeat(auto-fit, minmax(240px, 1fr))",
          gap: "28px",
        }}
      >
        <Feature
          eyebrow="For patients"
          title="Book in under a minute"
          body="Pick a doctor, get a token number and an expected time — no phone calls."
        />
        <Feature
          eyebrow="For patients"
          title="Understand your results"
          body="Lab reports come with a plain-language explanation, not just numbers."
        />
        <Feature
          eyebrow="For everyone"
          title="Know where you stand"
          body="Check your BMI and get a simple, personalized target — no account needed."
        />
      </section>

      {/* FOOTER */}
      <footer
        style={{
          borderTop: "1px solid #DCD6C8",
          padding: "24px 32px",
          textAlign: "center",
          fontSize: "13px",
          color: "#8A8577",
        }}
      >
        Meridian Clinic — a Service-Oriented Computing project.
      </footer>
    </div>
  );
}

function NavButton({
  children,
  onClick,
}: {
  children: React.ReactNode;
  onClick: () => void;
}) {
  return (
    <button
      onClick={onClick}
      style={{
        padding: "9px 14px",
        fontSize: "14px",
        fontWeight: 500,
        color: "#1B1B18",
        background: "transparent",
        border: "none",
        cursor: "pointer",
        borderRadius: "6px",
        transition: "background 0.15s ease",
      }}
      onMouseEnter={(e) => (e.currentTarget.style.background = "#EBE6D9")}
      onMouseLeave={(e) => (e.currentTarget.style.background = "transparent")}
    >
      {children}
    </button>
  );
}

function Feature({
  eyebrow,
  title,
  body,
}: {
  eyebrow: string;
  title: string;
  body: string;
}) {
  return (
    <div
      style={{
        padding: "24px",
        background: "#FFFFFF",
        border: "1px solid #DCD6C8",
        borderRadius: "10px",
      }}
    >
      <p
        style={{
          fontSize: "11px",
          letterSpacing: "0.1em",
          textTransform: "uppercase",
          color: "#FF8A5B",
          fontWeight: 700,
          marginBottom: "10px",
        }}
      >
        {eyebrow}
      </p>
      <h3
        style={{
          fontFamily: "'Fraunces', Georgia, serif",
          fontSize: "19px",
          fontWeight: 500,
          color: "#0B3D3A",
          margin: "0 0 8px",
        }}
      >
        {title}
      </h3>
      <p style={{ fontSize: "14.5px", lineHeight: 1.55, color: "#4A463E", margin: 0 }}>
        {body}
      </p>
    </div>
  );
}

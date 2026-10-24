import type { Metadata } from "next";
import { Geist, Geist_Mono } from "next/font/google";
import "./globals.css";

const geistSans = Geist({
  variable: "--font-geist-sans",
  subsets: ["latin"],
});

const geistMono = Geist_Mono({
  variable: "--font-geist-mono",
  subsets: ["latin"],
});

// Custom viewport meta tag for mobile responsiveness
export const viewport = {
  width: "device-width",
  initialScale: 1,
  maximumScale: 1,
  userScalable: "no",
};

export const metadata: Metadata = {
  metadataBase: new URL("https://soc-health-web-system.onrender.com"),
  title: "Meridian Clinic | Your Healthcare Hub",
  description: "Book appointments, track your health, and get lab reports explained - all from one place.",
  keywords: ["healthcare", "appointments", "lab reports", "BMI calculator", "telemedicine"],
  icons: {
    icon: [
      { url: '/favicon.ico', sizes: 'any' },
      { url: '/favicon.png', sizes: '192x192', type: 'image/png' },
      { url: '/favicon-32x32.png', sizes: '32x32', type: 'image/png' },
    ],
    apple: [
      { url: '/apple-touch-icon.png', sizes: '192x192' },
    ],
  },
  manifest: '/manifest.json',
};

export default function RootLayout({ children }: LayoutProps<"/">) {
  return (
    <html
      lang="en"
      className={`${geistSans.variable} ${geistMono.variable} h-full antialiased`}
    >
      {/* Viewport meta tag for mobile responsiveness */}
      <meta name="viewport" content={`width=device-width, initial-scale=${viewport.initialScale}, maximum-scale=${viewport.maximumScale}, user-scalable=${viewport.userScalable == "no" ? "no" : "yes"}`} />
      <body className="min-h-full flex flex-col w-full">{children}</body>
    </html>
  );
}

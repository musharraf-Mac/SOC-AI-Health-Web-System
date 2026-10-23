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

export const metadata: Metadata = {
  title: "Meridian Clinic | Your Healthcare Hub",
  description: "Book appointments, track your health, and get lab reports explained - all from one place.",
  icons: {
    icon: [
      { url: '/favicon.ico', sizes: 'any' }, // auto-scales for browser tab
      { url: '/favicon.png', sizes: '192x192', type: 'image/png' }, // Apple Touch Icon, Android Chrome
      { url: '/favicon-32x32.png', sizes: '32x32', type: 'image/png' }, // standard icon
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
      <body className="min-h-full flex flex-col">{children}</body>
    </html>
  );
}

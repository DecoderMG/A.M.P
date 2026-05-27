import type { Metadata } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: "AMP - Activity Music Player",
  description: "Premium Energetic Workout Music Companion",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="en">
      <body className="antialiased">
        {children}
      </body>
    </html>
  );
}

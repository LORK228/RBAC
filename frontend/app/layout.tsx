import type { ReactNode } from "react";
import { AuthProvider } from "@/components/AuthProvider";
import NavBar from "@/components/NavBar";
import "./globals.css";

export const metadata = {
  title: "Taxi RBAC",
  description: "Role-based taxi ride management system",
};

export default function RootLayout({ children }: { children: ReactNode }) {
  return (
    <html lang="en">
      <body>
        <AuthProvider>
          <NavBar />
          {children}
        </AuthProvider>
      </body>
    </html>
  );
}

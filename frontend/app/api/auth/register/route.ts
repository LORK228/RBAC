import { NextResponse } from "next/server";
import crypto from "crypto";
import { createUser, findUserByEmail } from "@/lib/db";
import { signToken } from "@/lib/jwt";

function hashPassword(password: string): string {
  return crypto.createHash("sha256").update(password).digest("hex");
}

export async function POST(request: Request) {
  try {
    const { email, password, name, phone, role, licenseNumber } = await request.json();

    if (!email || !password || !name || !role) {
      return NextResponse.json({ error: "All fields are required" }, { status: 400 });
    }

    if (!phone) {
      return NextResponse.json({ error: "Phone is required" }, { status: 400 });
    }

    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
      return NextResponse.json({ error: "Invalid email format" }, { status: 400 });
    }

    if (password.length < 6) {
      return NextResponse.json({ error: "Password must be at least 6 characters" }, { status: 400 });
    }

    if (role !== "passenger" && role !== "driver") {
      return NextResponse.json({ error: "Role must be 'passenger' or 'driver'" }, { status: 400 });
    }

    if (findUserByEmail(email)) {
      return NextResponse.json({ error: "Email already registered" }, { status: 409 });
    }

    const userServiceUrl = process.env.NEXT_PUBLIC_USER_SERVICE_URL || "http://localhost:8081";
    let javaUserId: number;

    if (role === "passenger") {
      const res = await fetch(`${userServiceUrl}/passengers`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ name, email, phone }),
      });
      if (!res.ok) {
        const text = await res.text();
        return NextResponse.json({ error: `Failed to create passenger: ${text}` }, { status: 502 });
      }
      const passenger = await res.json();
      javaUserId = passenger.id;
    } else {
      if (!licenseNumber) {
        return NextResponse.json({ error: "License number is required for drivers" }, { status: 400 });
      }
      const res = await fetch(`${userServiceUrl}/drivers`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ name, email, phone, licenseNumber }),
      });
      if (!res.ok) {
        const text = await res.text();
        return NextResponse.json({ error: `Failed to create driver: ${text}` }, { status: 502 });
      }
      const driver = await res.json();
      javaUserId = driver.id;
    }

    const passwordHash = hashPassword(password);
    const user = createUser(email, name, role, javaUserId);
    (user as any).passwordHash = passwordHash;

    const token = signToken({ userId: user.id, email: user.email, role: user.role });

    return NextResponse.json({
      token,
      user: { id: user.id, email: user.email, name: user.name, role: user.role },
    });
  } catch (error) {
    console.error("Register error:", error);
    return NextResponse.json({ error: "Internal server error" }, { status: 500 });
  }
}

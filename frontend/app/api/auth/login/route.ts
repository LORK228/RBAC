import { NextResponse } from "next/server";
import crypto from "crypto";
import { signToken } from "@/lib/jwt";
import { loginUserAuth } from "@/lib/user-service";

function hashPassword(password: string): string {
  return crypto.createHash("sha256").update(password).digest("hex");
}

export async function POST(request: Request) {
  try {
    const { email, password } = await request.json();

    if (!email || !password) {
      return NextResponse.json({ error: "Email and password are required" }, { status: 400 });
    }

    const passwordHash = hashPassword(password);
    const user = await loginUserAuth(email, passwordHash);

    const token = signToken({ userId: user.id, email: user.email, role: user.role });

    return NextResponse.json({
      token,
      user: { id: user.id, email: user.email, name: user.name, role: user.role },
    });
  } catch (error) {
    console.error("Login error:", error);
    return NextResponse.json({ error: "Invalid email or password" }, { status: 401 });
  }
}

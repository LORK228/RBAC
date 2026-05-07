import jwt from "jsonwebtoken";
import type { AuthPayload } from "./types";

const JWT_SECRET = process.env.JWT_SECRET || "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";

export function signToken(payload: AuthPayload): string {
  return jwt.sign(
    { subject: payload.email, role: payload.role === "driver" ? "DRIVER" : "USER", userId: payload.userId },
    JWT_SECRET,
    { expiresIn: "24h" }
  );
}

export function verifyToken(token: string): AuthPayload | null {
  try {
    const decoded = jwt.verify(token, JWT_SECRET) as {
      subject: string;
      role: string;
      userId: number;
    };
    const role = decoded.role === "DRIVER" ? ("driver" as const) : ("passenger" as const);
    return { email: decoded.subject, role, userId: decoded.userId };
  } catch {
    return null;
  }
}

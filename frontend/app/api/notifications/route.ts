import { NextRequest, NextResponse } from "next/server";
import { verifyToken } from "@/lib/jwt";
import { findUserById } from "@/lib/db";

const NOTIFICATION_SERVICE = process.env.NEXT_PUBLIC_NOTIFICATION_SERVICE_URL || "http://localhost:8083";

function getUserId(request: NextRequest): number | null {
  const authHeader = request.headers.get("authorization");
  if (!authHeader?.startsWith("Bearer ")) return null;
  const payload = verifyToken(authHeader.slice(7));
  return payload?.userId ?? null;
}

export async function GET(request: NextRequest) {
  const authHeader = request.headers.get("authorization");
  if (!authHeader?.startsWith("Bearer ")) {
    return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
  }

  const userId = getUserId(request);
  if (!userId) {
    return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
  }

  const user = findUserById(userId);
  if (!user) {
    return NextResponse.json({ error: "User not found" }, { status: 404 });
  }

  const recipientType = user.role === "driver" ? "DRIVER" : "PASSENGER";

  try {
    const res = await fetch(
      `${NOTIFICATION_SERVICE}/notifications?recipient_type=${recipientType}&recipient_id=${user.javaUserId}`,
      {
        headers: {
          Authorization: authHeader,
        },
      }
    );

    if (!res.ok) {
      return NextResponse.json({ notifications: [] });
    }

    const notifications = await res.json();
    return NextResponse.json({ notifications });
  } catch {
    return NextResponse.json({ notifications: [] });
  }
}

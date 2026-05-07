import { NextRequest, NextResponse } from "next/server";
import { verifyToken } from "@/lib/jwt";
import { createRide, findRidesByUserId, findPendingRides, findRidesByDriverId, findUserById } from "@/lib/db";

function getUserId(request: NextRequest): number | null {
  const authHeader = request.headers.get("authorization");
  if (!authHeader?.startsWith("Bearer ")) return null;
  const payload = verifyToken(authHeader.slice(7));
  return payload?.userId ?? null;
}

function unauthorized() {
  return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
}

export async function POST(request: NextRequest) {
  const userId = getUserId(request);
  if (!userId) return unauthorized();

  const user = findUserById(userId);
  if (!user || user.role !== "passenger") {
    return NextResponse.json({ error: "Only passengers can create rides" }, { status: 403 });
  }

  try {
    const { origin, destination, distanceKm } = await request.json();
    if (!origin?.trim() || !destination?.trim()) {
      return NextResponse.json({ error: "Origin and destination are required" }, { status: 400 });
    }

    const distance = distanceKm != null ? parseFloat(distanceKm) : null;
    const ride = createRide(user.id, user.javaUserId, user.name, origin.trim(), destination.trim(), distance);

    return NextResponse.json({ ride }, { status: 201 });
  } catch {
    return NextResponse.json({ error: "Invalid request body" }, { status: 400 });
  }
}

export async function GET(request: NextRequest) {
  const userId = getUserId(request);
  if (!userId) return unauthorized();

  const user = findUserById(userId);
  if (!user) return unauthorized();

  const { searchParams } = new URL(request.url);
  const filter = searchParams.get("filter");

  if (user.role === "driver") {
    if (filter === "pending") {
      const rides = findPendingRides();
      return NextResponse.json({ rides });
    }
    if (filter === "accepted") {
      const rides = findRidesByDriverId(user.javaUserId).filter(
        (r) => r.status === "accepted" || r.status === "in_progress"
      );
      return NextResponse.json({ rides });
    }
    if (filter === "history") {
      const rides = findRidesByDriverId(user.javaUserId);
      return NextResponse.json({ rides });
    }
    const rides = findRidesByDriverId(user.javaUserId);
    return NextResponse.json({ rides });
  }

  const rides = findRidesByUserId(user.id);
  if (filter) {
    return NextResponse.json({ rides: rides.filter((r) => r.status === filter) });
  }
  return NextResponse.json({ rides });
}

import { NextRequest, NextResponse } from "next/server";
import { verifyToken } from "@/lib/jwt";
import { createRide, findRidesByUserId, findPendingRides, findRidesByDriverId } from "@/lib/db";
import { getUserAuthById } from "@/lib/user-service";

const TRIP_SERVICE = process.env.NEXT_PUBLIC_TRIP_SERVICE_URL || "http://localhost:8082";

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

  let user;
  try {
    user = await getUserAuthById(userId);
  } catch {
    return NextResponse.json({ error: "User not found" }, { status: 404 });
  }
  if (user.role !== "passenger") {
    return NextResponse.json({ error: "Only passengers can create rides" }, { status: 403 });
  }

  try {
    const { origin, destination, distanceKm } = await request.json();
    if (!origin?.trim() || !destination?.trim()) {
      return NextResponse.json({ error: "Origin and destination are required" }, { status: 400 });
    }

    const distance = distanceKm != null ? parseFloat(distanceKm) : null;

    const token = request.headers.get("authorization")?.slice(7);
    const tripRes = await fetch(`${TRIP_SERVICE}/trips`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        Authorization: `Bearer ${token}`,
      },
      body: JSON.stringify({
        passengerId: user.javaUserId,
        origin: origin.trim(),
        destination: destination.trim(),
        distanceKm: distance,
      }),
    });

    if (!tripRes.ok) {
      const text = await tripRes.text();
      return NextResponse.json({ error: `Trip creation failed: ${text}` }, { status: 502 });
    }

    const trip = await tripRes.json();
    const ride = createRide(user.id, user.javaUserId, user.name, origin.trim(), destination.trim(), distance, trip.id, trip.price != null ? Number(trip.price) : null);

    return NextResponse.json({ ride }, { status: 201 });
  } catch {
    return NextResponse.json({ error: "Invalid request body" }, { status: 400 });
  }
}

export async function GET(request: NextRequest) {
  const userId = getUserId(request);
  if (!userId) return unauthorized();

  let user;
  try {
    user = await getUserAuthById(userId);
  } catch {
    return NextResponse.json({ error: "User not found" }, { status: 404 });
  }

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

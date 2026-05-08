import { NextRequest, NextResponse } from "next/server";
import { verifyToken } from "@/lib/jwt";
import { findRideById, updateRide } from "@/lib/db";
import { getUserAuthById } from "@/lib/user-service";

const TRIP_SERVICE = process.env.NEXT_PUBLIC_TRIP_SERVICE_URL || "http://localhost:8082";
const USER_SERVICE = process.env.NEXT_PUBLIC_USER_SERVICE_URL || "http://localhost:8081";

function getUserIdAndRole(request: NextRequest): { userId: number; role: string } | null {
  const authHeader = request.headers.get("authorization");
  if (!authHeader?.startsWith("Bearer ")) return null;
  const payload = verifyToken(authHeader.slice(7));
  return payload ? { userId: payload.userId, role: payload.role } : null;
}

function unauthorized() {
  return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
}

export async function PATCH(request: NextRequest, { params }: { params: Promise<{ id: string }> }) {
  const auth = getUserIdAndRole(request);
  if (!auth) return unauthorized();

  const { id } = await params;
  const rideId = parseInt(id, 10);
  if (isNaN(rideId)) {
    return NextResponse.json({ error: "Invalid ride ID" }, { status: 400 });
  }

  const ride = findRideById(rideId);
  if (!ride) {
    return NextResponse.json({ error: "Ride not found" }, { status: 404 });
  }

  try {
    const body = await request.json();
    const { action, status } = body;

    if (action === "accept") {
      if (auth.role !== "driver") {
        return NextResponse.json({ error: "Only drivers can accept rides" }, { status: 403 });
      }
      if (ride.status !== "pending") {
        return NextResponse.json({ error: "Ride is no longer available" }, { status: 409 });
      }

      let driver;
      try {
        driver = await getUserAuthById(auth.userId);
      } catch {
        return NextResponse.json({ error: "Driver not found" }, { status: 404 });
      }

      await fetch(`${USER_SERVICE}/drivers/${driver.javaUserId}/status`, {
        method: "PATCH",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${request.headers.get("authorization")?.slice(7)}`,
        },
        body: JSON.stringify({ status: "AVAILABLE" }),
      });

      const tripPayload = {
        passengerId: ride.passengerId,
        origin: ride.origin,
        destination: ride.destination,
        distanceKm: ride.distanceKm ?? 5.0,
      };

      const tripRes = await fetch(`${TRIP_SERVICE}/trips`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${request.headers.get("authorization")?.slice(7)}`,
        },
        body: JSON.stringify(tripPayload),
      });

      if (!tripRes.ok) {
        const text = await tripRes.text();
        return NextResponse.json({ error: `Trip creation failed: ${text}` }, { status: 502 });
      }

      const trip = await tripRes.json();
      const updated = updateRide(rideId, {
        status: "accepted",
        driverId: driver.javaUserId,
        driverName: driver.name,
        tripId: trip.id,
        price: trip.price != null ? Number(trip.price) : null,
      });

      return NextResponse.json({ ride: updated });
    }

    if (action === "rating") {
      if (!ride.tripId) {
        return NextResponse.json({ error: "No trip associated with this ride" }, { status: 400 });
      }
      if (ride.status !== "completed") {
        return NextResponse.json({ error: "Can only rate completed rides" }, { status: 409 });
      }

      const rating = body.rating;
      if (!Number.isInteger(rating) || rating < 1 || rating > 5) {
        return NextResponse.json({ error: "Rating must be an integer between 1 and 5" }, { status: 400 });
      }

      const tripRes = await fetch(`${TRIP_SERVICE}/trips/${ride.tripId}/rating`, {
        method: "PATCH",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${request.headers.get("authorization")?.slice(7)}`,
        },
        body: JSON.stringify({ rating }),
      });

      if (!tripRes.ok) {
        const text = await tripRes.text();
        return NextResponse.json({ error: `Rating failed: ${text}` }, { status: 502 });
      }

      const updated = updateRide(rideId, { rating });
      return NextResponse.json({ ride: updated });
    }

    if (action === "status") {
      if (auth.role !== "driver") {
        return NextResponse.json({ error: "Only drivers can update ride status" }, { status: 403 });
      }
      if (!ride.tripId) {
        return NextResponse.json({ error: "No trip associated with this ride" }, { status: 400 });
      }

      const validStatuses = ["accepted", "in_progress", "completed"];
      if (!status || !validStatuses.includes(status)) {
        return NextResponse.json({ error: "Invalid status" }, { status: 400 });
      }

      const tripStatusMap: Record<string, string> = {
        accepted: "ACCEPTED",
        in_progress: "STARTED",
        completed: "COMPLETED",
      };

      const tripRes = await fetch(`${TRIP_SERVICE}/trips/${ride.tripId}/status`, {
        method: "PATCH",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${request.headers.get("authorization")?.slice(7)}`,
        },
        body: JSON.stringify({ status: tripStatusMap[status] }),
      });

      if (!tripRes.ok) {
        if (tripRes.status === 503) {
          const updated = updateRide(rideId, { status: status as any });
          return NextResponse.json({
            ride: updated,
            warning: "Trip status updated locally; downstream notification services unavailable.",
          });
        }
        const text = await tripRes.text();
        return NextResponse.json({ error: `Status update failed: ${text}` }, { status: 502 });
      }

      const updated = updateRide(rideId, { status: status as any });
      return NextResponse.json({ ride: updated });
    }

    return NextResponse.json({ error: "Invalid action" }, { status: 400 });
  } catch {
    return NextResponse.json({ error: "Invalid request body" }, { status: 400 });
  }
}

export async function DELETE(request: NextRequest, { params }: { params: Promise<{ id: string }> }) {
  const auth = getUserIdAndRole(request);
  if (!auth) return unauthorized();

  const { id } = await params;
  const rideId = parseInt(id, 10);
  if (isNaN(rideId)) {
    return NextResponse.json({ error: "Invalid ride ID" }, { status: 400 });
  }

  const ride = findRideById(rideId);
  if (!ride) {
    return NextResponse.json({ error: "Ride not found" }, { status: 404 });
  }

  if (ride.userId !== auth.userId) {
    return NextResponse.json({ error: "Not your ride" }, { status: 403 });
  }

  if (ride.status !== "pending") {
    return NextResponse.json({ error: "Can only cancel pending rides" }, { status: 409 });
  }

  const updated = updateRide(rideId, { status: "cancelled" });
  return NextResponse.json({ ride: updated });
}

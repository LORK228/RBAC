import type { RideRequest } from "./types";

interface Store {
  rides: RideRequest[];
  nextRideId: number;
}

const g = globalThis as { __store?: Store };

if (!g.__store) {
  g.__store = { rides: [], nextRideId: 1 };
}

const store: Store = g.__store;

export function createRide(
  userId: number,
  passengerId: number,
  passengerName: string,
  origin: string,
  destination: string,
  distanceKm: number | null = null
): RideRequest {
  const ride: RideRequest = {
    id: store.nextRideId++,
    userId,
    passengerId,
    passengerName,
    origin,
    destination,
    distanceKm,
    status: "pending",
    driverId: null,
    driverName: null,
    tripId: null,
    price: null,
    rating: null,
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString(),
  };
  store.rides.push(ride);
  return ride;
}

export function findRideById(id: number): RideRequest | undefined {
  return store.rides.find((r) => r.id === id);
}

export function findRidesByUserId(userId: number): RideRequest[] {
  return store.rides.filter((r) => r.userId === userId);
}

export function findPendingRides(): RideRequest[] {
  return store.rides.filter((r) => r.status === "pending");
}

export function findRidesByDriverId(driverId: number): RideRequest[] {
  return store.rides.filter((r) => r.driverId === driverId);
}

export function updateRide(
  id: number,
  updates: Partial<RideRequest>
): RideRequest | undefined {
  const ride = store.rides.find((r) => r.id === id);
  if (!ride) return undefined;
  Object.assign(ride, updates, { updatedAt: new Date().toISOString() });
  return ride;
}

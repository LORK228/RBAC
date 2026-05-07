import type { User, RideRequest } from "./types";

interface Store {
  users: (User & { passwordHash?: string })[];
  rides: RideRequest[];
  nextUserId: number;
  nextRideId: number;
}

const g = globalThis as { __store?: Store };

if (!g.__store) {
  g.__store = { users: [], rides: [], nextUserId: 1, nextRideId: 1 };
}

const store: Store = g.__store;

export function getStore(): Store {
  return store;
}

export function findUserByEmail(email: string): User | undefined {
  return store.users.find((u) => u.email === email);
}

export function findUserById(id: number): User | undefined {
  return store.users.find((u) => u.id === id);
}

export function findUserByJavaUserId(javaUserId: number): User | undefined {
  return store.users.find((u) => u.javaUserId === javaUserId);
}

export function createUser(email: string, name: string, role: "passenger" | "driver", javaUserId: number): User {
  const user: User = {
    id: store.nextUserId++,
    email,
    name,
    role,
    javaUserId,
    createdAt: new Date().toISOString(),
  };
  store.users.push(user);
  return user;
}

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

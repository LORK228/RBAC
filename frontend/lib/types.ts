export type Role = "passenger" | "driver";

export interface User {
  id: number;
  email: string;
  name: string;
  role: Role;
  javaUserId: number;
  createdAt: string;
}

export interface AuthPayload {
  userId: number;
  email: string;
  role: Role;
}

export interface RideRequest {
  id: number;
  userId: number;
  passengerId: number;
  passengerName: string;
  origin: string;
  destination: string;
  status: "pending" | "accepted" | "in_progress" | "completed" | "cancelled";
  driverId: number | null;
  driverName: string | null;
  tripId: number | null;
  distanceKm: number | null;
  price: number | null;
  rating: number | null;
  createdAt: string;
  updatedAt: string;
}

export interface NotificationItem {
  id: number;
  tripId: number;
  recipientType: string;
  recipientId: number;
  message: string;
  status: string;
  createdAt: string;
}

 "use client";

import { useState, useEffect, useCallback, useRef, FormEvent } from "react";
import { useRouter } from "next/navigation";
import { useAuth } from "@/components/AuthProvider";

const TRIP_SERVICE_URL = process.env.NEXT_PUBLIC_TRIP_SERVICE_URL || "http://localhost:8082";

interface Ride {
  id: number;
  origin: string;
  destination: string;
  status: string;
  driverName: string | null;
  price: number | null;
  rating: number | null;
  tripId: number | null;
  createdAt: string;
  updatedAt: string;
}

interface NotificationItem {
  id: number;
  tripId: number;
  recipientType: string;
  recipientId: number;
  message: string;
  status: string;
  createdAt: string;
}

export default function UserDashboard() {
  const { user, token, loading: authLoading } = useAuth();
  const router = useRouter();
  const [origin, setOrigin] = useState("");
  const [destination, setDestination] = useState("");
  const [distance, setDistance] = useState("");
  const [rides, setRides] = useState<Ride[]>([]);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");
  const [notifications, setNotifications] = useState<NotificationItem[]>([]);
  const [notifLoading, setNotifLoading] = useState(true);
  const wsRef = useRef<WebSocket | null>(null);

  const fetchRides = useCallback(async () => {
    if (!token) return;
    try {
      const res = await fetch("/api/rides", {
        headers: { Authorization: `Bearer ${token}` },
      });
      if (res.ok) {
        const data = await res.json();
        setRides(data.rides);
      }
    } catch {
      setError("Failed to fetch rides");
    } finally {
      setLoading(false);
    }
  }, [token]);

  const fetchNotifications = useCallback(async () => {
    if (!token) return;
    try {
      const res = await fetch("/api/notifications", {
        headers: { Authorization: `Bearer ${token}` },
      });
      if (res.ok) {
        const data = await res.json();
        setNotifications(data.notifications || []);
      }
    } catch {
    } finally {
      setNotifLoading(false);
    }
  }, [token]);

  useEffect(() => {
    if (!authLoading && (!user || user.role !== "passenger")) {
      router.replace("/login");
      return;
    }
    if (token) {
      fetchRides();
      fetchNotifications();
    }
  }, [user, token, authLoading, router, fetchRides, fetchNotifications]);

  const prevActiveKeyRef = useRef("");

  useEffect(() => {
    const activeKey = rides
      .filter((r) => r.tripId && r.status !== "completed" && r.status !== "cancelled")
      .map((r) => `${r.tripId}:${r.status}`)
      .sort()
      .join(",");

    if (activeKey === prevActiveKeyRef.current) return;
    prevActiveKeyRef.current = activeKey;

    wsRef.current?.close();
    wsRef.current = null;

    const activeTripIds = activeKey
      ? activeKey.split(",").map((s) => parseInt(s.split(":")[0], 10))
      : [];

    if (activeTripIds.length === 0) return;

    const tripId = activeTripIds[0];
    const wsUrl = TRIP_SERVICE_URL.replace(/^http/, "ws") + `/ws/trips/${tripId}`;

    try {
      const ws = new WebSocket(wsUrl);
      ws.onmessage = (event) => {
        try {
          const msg = JSON.parse(event.data);
          if (msg.type === "STATUS_CHANGE") {
            fetchRides();
            fetchNotifications();
          }
        } catch {
        }
      };
      ws.onerror = () => {};
      ws.onclose = () => {
        if (wsRef.current === ws) {
          wsRef.current = null;
        }
      };
      wsRef.current = ws;
    } catch {
    }
  }, [rides, fetchRides, fetchNotifications]);

  useEffect(() => {
    const interval = setInterval(() => {
      fetchRides();
      fetchNotifications();
    }, 10000);
    return () => clearInterval(interval);
  }, [fetchRides, fetchNotifications]);

  async function handleCreateRide(e: FormEvent) {
    e.preventDefault();
    setError("");
    setSuccess("");

    if (!origin.trim() || !destination.trim()) {
      setError("Both pickup and drop-off locations are required");
      return;
    }

    setSubmitting(true);
    try {
      const res = await fetch("/api/rides", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify({ origin: origin.trim(), destination: destination.trim(), distanceKm: distance || null }),
      });

      const data = await res.json();
      if (!res.ok) {
        setError(data.error || "Failed to create ride");
        return;
      }

      setSuccess("Ride request created! Waiting for a driver to accept.");
      setOrigin("");
      setDestination("");
      setDistance("");
      fetchRides();
    } catch {
      setError("Network error");
    } finally {
      setSubmitting(false);
    }
  }

  async function cancelRide(rideId: number) {
    if (!token) return;
    setError("");
    setSuccess("");

    try {
      const res = await fetch(`/api/rides/${rideId}`, {
        method: "DELETE",
        headers: { Authorization: `Bearer ${token}` },
      });

      const data = await res.json();
      if (!res.ok) {
        setError(data.error || "Failed to cancel ride");
        return;
      }

      setSuccess("Ride cancelled successfully");
      fetchRides();
    } catch {
      setError("Network error");
    }
  }

  async function handleRating(rideId: number, rating: number) {
    if (!token) return;
    setError("");
    setSuccess("");

    try {
      const res = await fetch(`/api/rides/${rideId}`, {
        method: "PATCH",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify({ action: "rating", rating }),
      });

      const data = await res.json();
      if (!res.ok) {
        setError(data.error || "Failed to submit rating");
        return;
      }

      setSuccess(`You rated this ride ${rating} star${rating > 1 ? "s" : ""}!`);
      fetchRides();
    } catch {
      setError("Network error");
    }
  }

  function formatPrice(price: number | null): string {
    if (price === null || price === undefined) return "";
    return `${Number(price).toFixed(2)} RUB`;
  }

  function renderStars(rideId: number, currentRating: number | null) {
    return (
      <div className="star-rating">
        {[1, 2, 3, 4, 5].map((star) => (
          <button
            key={star}
            type="button"
            className={`star-btn ${currentRating !== null && star <= currentRating ? "star-active" : ""}`}
            onClick={() => handleRating(rideId, star)}
            disabled={currentRating !== null}
            title={`${star} star${star > 1 ? "s" : ""}`}
          >
            {star <= (currentRating || 0) ? "\u2605" : "\u2606"}
          </button>
        ))}
      </div>
    );
  }

  const activeRides = rides.filter((r) => r.status !== "cancelled" && r.status !== "completed");
  const pastRides = rides.filter((r) => r.status === "completed" || r.status === "cancelled");

  if (authLoading) {
    return <div className="loading">Loading dashboard...</div>;
  }

  return (
    <div className="dashboard-page">
      <div className="container">
        <h2>User Dashboard</h2>
        <p className="subtitle">Request a ride and track your trips</p>

        {error && <div className="alert alert-error">{error}</div>}
        {success && <div className="alert alert-success">{success}</div>}

        <div className="dashboard-grid">
          <div className="card">
            <div className="card-header">Request a Ride</div>
            <div className="card-body">
              <form onSubmit={handleCreateRide}>
                <div className="form-group">
                  <label htmlFor="origin">Pickup Location</label>
                  <input
                    id="origin"
                    type="text"
                    placeholder="e.g. 123 Main St"
                    value={origin}
                    onChange={(e) => setOrigin(e.target.value)}
                    required
                  />
                </div>
                <div className="form-group">
                  <label htmlFor="destination">Drop-off Location</label>
                  <input
                    id="destination"
                    type="text"
                    placeholder="e.g. Airport Terminal 1"
                    value={destination}
                    onChange={(e) => setDestination(e.target.value)}
                    required
                  />
                </div>
                <div className="form-group">
                  <label htmlFor="distance">Distance (km)</label>
                  <input
                    id="distance"
                    type="number"
                    step="0.1"
                    min="0.1"
                    placeholder="e.g. 12.5"
                    value={distance}
                    onChange={(e) => setDistance(e.target.value)}
                  />
                </div>
                <div className="form-actions">
                  <button type="submit" className="btn btn-primary" style={{ flex: 1 }} disabled={submitting}>
                    {submitting ? "Requesting..." : "Request Ride"}
                  </button>
                </div>
              </form>
            </div>
          </div>

          <div className="card">
            <div className="card-header">Active Rides</div>
            <div className="card-body">
              {loading ? (
                <div className="loading">Loading rides...</div>
              ) : activeRides.length === 0 ? (
                <div className="empty-state">
                  <h3>No active rides</h3>
                  <p>Request a ride to get started</p>
                </div>
              ) : (
                activeRides.map((ride) => {
                  const canCancel = ride.status === "pending";
                  return (
                    <div key={ride.id} className="ride-card">
                      <div className="ride-card-header">
                        <span className={`status-badge status-${ride.status}`}>
                          {ride.status === "in_progress" ? "In Progress" : ride.status.charAt(0).toUpperCase() + ride.status.slice(1)}
                        </span>
                        {ride.price !== null && (
                          <span className="ride-price">{formatPrice(ride.price)}</span>
                        )}
                      </div>
                      <div className="ride-card-route">
                        {ride.origin} <span className="arrow">&rarr;</span> {ride.destination}
                      </div>
                      <div className="ride-card-meta">
                        {ride.driverName && <span>Driver: {ride.driverName}</span>}
                        {ride.price !== null && <span>Price: {formatPrice(ride.price)}</span>}
                        <span>{new Date(ride.createdAt).toLocaleString()}</span>
                      </div>
                      {canCancel && (
                        <div className="ride-card-actions">
                          <button className="btn btn-sm btn-danger" onClick={() => cancelRide(ride.id)}>
                            Cancel Ride
                          </button>
                        </div>
                      )}
                    </div>
                  );
                })
              )}
            </div>
          </div>
        </div>

        {pastRides.length > 0 && (
          <div className="card" style={{ marginTop: 20 }}>
            <div className="card-header">Ride History</div>
            <div className="card-body">
              {pastRides.map((ride) => (
                <div key={ride.id} className="ride-card">
                  <div className="ride-card-header">
                    <span className={`status-badge status-${ride.status}`}>
                      {ride.status === "completed" ? "Completed" : "Cancelled"}
                    </span>
                    {ride.price !== null && (
                      <span className="ride-price">{formatPrice(ride.price)}</span>
                    )}
                  </div>
                  <div className="ride-card-route">
                    {ride.origin} <span className="arrow">&rarr;</span> {ride.destination}
                  </div>
                  <div className="ride-card-meta">
                    {ride.driverName && <span>Driver: {ride.driverName}</span>}
                    {ride.price !== null && <span>Price: {formatPrice(ride.price)}</span>}
                    <span>{new Date(ride.createdAt).toLocaleString()}</span>
                  </div>
                  {ride.status === "completed" && (
                    <div className="ride-card-rating">
                      <span className="rating-label">Your rating:</span>
                      {renderStars(ride.id, ride.rating)}
                    </div>
                  )}
                </div>
              ))}
            </div>
          </div>
        )}

        <div className="card" style={{ marginTop: 20 }}>
          <div className="card-header">
            Notifications
            {!notifLoading && notifications.length > 0 && (
              <span className="notif-count">{notifications.length}</span>
            )}
          </div>
          <div className="card-body">
            {notifLoading ? (
              <div className="loading">Loading notifications...</div>
            ) : notifications.length === 0 ? (
              <div className="empty-state">
                <h3>No notifications</h3>
                <p>You will see notifications about your rides here</p>
              </div>
            ) : (
              notifications.map((notif) => (
                <div key={notif.id} className="notification-item">
                  <div className="notification-message">{notif.message}</div>
                  <div className="notification-meta">
                    <span className={`notif-status notif-status-${notif.status.toLowerCase()}`}>
                      {notif.status}
                    </span>
                    <span>{new Date(notif.createdAt).toLocaleString()}</span>
                  </div>
                </div>
              ))
            )}
          </div>
        </div>
      </div>
    </div>
  );
}

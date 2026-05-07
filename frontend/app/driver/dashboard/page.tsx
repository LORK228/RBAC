"use client";

import { useState, useEffect, useCallback } from "react";
import { useRouter } from "next/navigation";
import { useAuth } from "@/components/AuthProvider";

interface Ride {
  id: number;
  passengerName: string;
  origin: string;
  destination: string;
  status: string;
  createdAt: string;
  driverName: string | null;
}

export default function DriverDashboard() {
  const { user, token, loading: authLoading } = useAuth();
  const router = useRouter();
  const [availableRides, setAvailableRides] = useState<Ride[]>([]);
  const [acceptedRides, setAcceptedRides] = useState<Ride[]>([]);
  const [historyRides, setHistoryRides] = useState<Ride[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  const fetchRides = useCallback(async () => {
    if (!token) return;
    setError("");

    try {
      const [pendingRes, acceptedRes, historyRes] = await Promise.all([
        fetch("/api/rides?filter=pending", {
          headers: { Authorization: `Bearer ${token}` },
        }),
        fetch("/api/rides?filter=accepted", {
          headers: { Authorization: `Bearer ${token}` },
        }),
        fetch("/api/rides?filter=history", {
          headers: { Authorization: `Bearer ${token}` },
        }),
      ]);

      if (pendingRes.ok) {
        const data = await pendingRes.json();
        setAvailableRides(data.rides);
      }
      if (acceptedRes.ok) {
        const data = await acceptedRes.json();
        setAcceptedRides(data.rides);
      }
      if (historyRes.ok) {
        const data = await historyRes.json();
        setHistoryRides(data.rides.filter((r: Ride) => r.status === "completed" || r.status === "cancelled"));
      }
    } catch {
      setError("Failed to fetch rides");
    } finally {
      setLoading(false);
    }
  }, [token]);

  useEffect(() => {
    if (!authLoading && (!user || user.role !== "driver")) {
      router.replace("/login");
      return;
    }
    if (token) {
      fetchRides();
    }
  }, [user, token, authLoading, router, fetchRides]);

  async function acceptRide(rideId: number) {
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
        body: JSON.stringify({ action: "accept" }),
      });

      const data = await res.json();
      if (!res.ok) {
        setError(data.error || "Failed to accept ride");
        return;
      }

      setSuccess("Ride accepted successfully!");
      fetchRides();
    } catch {
      setError("Network error");
    }
  }

  async function updateStatus(rideId: number, status: string) {
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
        body: JSON.stringify({ action: "status", status }),
      });

      const data = await res.json();
      if (!res.ok) {
        setError(data.error || "Failed to update status");
        return;
      }

      setSuccess(`Ride marked as ${status.replace("_", " ")}!`);
      fetchRides();
    } catch {
      setError("Network error");
    }
  }

  if (authLoading || loading) {
    return <div className="loading">Loading dashboard...</div>;
  }

  return (
    <div className="dashboard-page">
      <div className="container">
        <h2>Driver Dashboard</h2>
        <p className="subtitle">Manage ride requests and your active trips</p>

        {error && <div className="alert alert-error">{error}</div>}
        {success && <div className="alert alert-success">{success}</div>}

        <div className="dashboard-grid">
          <div className="card">
            <div className="card-header">Available Ride Requests</div>
            <div className="card-body">
              {availableRides.length === 0 ? (
                <div className="empty-state">
                  <h3>No ride requests</h3>
                  <p>New ride requests from passengers will appear here</p>
                </div>
              ) : (
                availableRides.map((ride) => (
                  <div key={ride.id} className="ride-card">
                    <div className="ride-card-header">
                      <span className="status-badge status-pending">Available</span>
                    </div>
                    <div className="ride-card-route">
                      {ride.origin} <span className="arrow">&rarr;</span> {ride.destination}
                    </div>
                    <div className="ride-card-meta">
                      <span>Passenger: {ride.passengerName}</span>
                      <span>{new Date(ride.createdAt).toLocaleString()}</span>
                    </div>
                    <div className="ride-card-actions">
                      <button className="btn btn-sm btn-success" onClick={() => acceptRide(ride.id)}>
                        Accept Ride
                      </button>
                    </div>
                  </div>
                ))
              )}
            </div>
          </div>

          <div className="card">
            <div className="card-header">Active Rides</div>
            <div className="card-body">
              {acceptedRides.length === 0 ? (
                <div className="empty-state">
                  <h3>No active rides</h3>
                  <p>Rides you accept will appear here</p>
                </div>
              ) : (
                acceptedRides.map((ride) => (
                  <div key={ride.id} className="ride-card">
                    <div className="ride-card-header">
                      <span className={`status-badge status-${ride.status}`}>{ride.status.replace("_", " ")}</span>
                    </div>
                    <div className="ride-card-route">
                      {ride.origin} <span className="arrow">&rarr;</span> {ride.destination}
                    </div>
                    <div className="ride-card-meta">
                      <span>Passenger: {ride.passengerName}</span>
                      <span>{new Date(ride.createdAt).toLocaleString()}</span>
                    </div>
                    <div className="ride-card-actions">
                      {ride.status === "accepted" && (
                        <button className="btn btn-sm btn-warning" onClick={() => updateStatus(ride.id, "in_progress")}>
                          Start Ride
                        </button>
                      )}
                      {ride.status === "in_progress" && (
                        <button className="btn btn-sm btn-success" onClick={() => updateStatus(ride.id, "completed")}>
                          Complete Ride
                        </button>
                      )}
                    </div>
                  </div>
                ))
              )}
            </div>
          </div>
        </div>

        {historyRides.length > 0 && (
          <div className="card" style={{ marginTop: 20 }}>
            <div className="card-header">Ride History</div>
            <div className="card-body">
              {historyRides.map((ride) => (
                <div key={ride.id} className="ride-card">
                  <div className="ride-card-header">
                    <span className={`status-badge status-${ride.status}`}>{ride.status}</span>
                  </div>
                  <div className="ride-card-route">
                    {ride.origin} <span className="arrow">&rarr;</span> {ride.destination}
                  </div>
                  <div className="ride-card-meta">
                    <span>Passenger: {ride.passengerName}</span>
                    <span>{new Date(ride.createdAt).toLocaleString()}</span>
                  </div>
                </div>
              ))}
            </div>
          </div>
        )}
      </div>
    </div>
  );
}

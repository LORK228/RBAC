"use client";

import Link from "next/link";
import { useAuth } from "./AuthProvider";

export default function NavBar() {
  const { user, logout } = useAuth();

  return (
    <nav className="navbar">
      <div className="navbar-inner">
        <Link href="/" className="navbar-brand">
          Taxi RBAC
        </Link>
        <div className="navbar-links">
          {user ? (
            <>
              <span className="navbar-user">
                {user.name}
                <span className={`role-badge role-${user.role}`}>
                  {user.role === "driver" ? "Driver" : "User"}
                </span>
              </span>
              <Link
                href={user.role === "driver" ? "/driver/dashboard" : "/user/dashboard"}
                className="btn btn-sm btn-primary"
              >
                Dashboard
              </Link>
              <button onClick={logout} className="btn btn-sm btn-danger">
                Logout
              </button>
            </>
          ) : (
            <>
              <Link href="/login" className="btn btn-sm">
                Login
              </Link>
              <Link href="/register" className="btn btn-sm btn-primary">
                Register
              </Link>
            </>
          )}
        </div>
      </div>
    </nav>
  );
}

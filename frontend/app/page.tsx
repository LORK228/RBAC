"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { useAuth } from "@/components/AuthProvider";

export default function Home() {
  const { user, loading } = useAuth();
  const router = useRouter();

  useEffect(() => {
    if (loading) return;
    if (!user) {
      router.replace("/login");
    } else if (user.role === "driver") {
      router.replace("/driver/dashboard");
    } else {
      router.replace("/user/dashboard");
    }
  }, [user, loading, router]);

  return <div className="loading">Loading...</div>;
}

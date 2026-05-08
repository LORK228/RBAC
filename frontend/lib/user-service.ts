const USER_SERVICE_URL = process.env.NEXT_PUBLIC_USER_SERVICE_URL || "http://localhost:8081";

export interface UserAuthInfo {
  id: number;
  email: string;
  name: string;
  role: "passenger" | "driver";
  javaUserId: number;
}

export async function registerUserAuth(email: string, passwordHash: string, name: string, role: string, javaUserId: number): Promise<UserAuthInfo> {
  const res = await fetch(`${USER_SERVICE_URL}/auth/register`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ email, passwordHash, name, role, javaUserId }),
  });
  if (!res.ok) {
    const text = await res.text();
    throw new Error(text);
  }
  return res.json();
}

export async function loginUserAuth(email: string, passwordHash: string): Promise<UserAuthInfo> {
  const res = await fetch(`${USER_SERVICE_URL}/auth/login`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ email, passwordHash }),
  });
  if (!res.ok) {
    const text = await res.text();
    throw new Error(text);
  }
  return res.json();
}

export async function getUserAuthById(id: number): Promise<UserAuthInfo> {
  const res = await fetch(`${USER_SERVICE_URL}/auth/users/${id}`);
  if (!res.ok) {
    const text = await res.text();
    throw new Error(text);
  }
  return res.json();
}

export async function checkEmailExists(email: string): Promise<boolean> {
  const res = await fetch(`${USER_SERVICE_URL}/auth/users/by-email/${encodeURIComponent(email)}`);
  return res.ok;
}

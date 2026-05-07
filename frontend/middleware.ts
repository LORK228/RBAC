import { NextResponse } from "next/server";
import type { NextRequest } from "next/server";

const publicPaths = ["/login", "/register"];
const authApiPaths = ["/api/auth/login", "/api/auth/register"];

export function middleware(request: NextRequest) {
  const { pathname } = request.nextUrl;

  if (publicPaths.some((p) => pathname.startsWith(p))) {
    return NextResponse.next();
  }

  if (pathname.startsWith("/_next") || pathname.startsWith("/favicon") || pathname === "/") {
    return NextResponse.next();
  }

  if (authApiPaths.some((p) => pathname.startsWith(p))) {
    return NextResponse.next();
  }

  const token =
    request.cookies.get("token")?.value ||
    request.headers.get("authorization")?.replace("Bearer ", "") ||
    "";

  if (!token) {
    return NextResponse.redirect(new URL("/login", request.url));
  }

  let role = "";
  try {
    const base64Url = token.split(".")[1];
    const base64 = base64Url.replace(/-/g, "+").replace(/_/g, "/");
    const payload = JSON.parse(atob(base64));
    role = payload.role || "";
  } catch {
    return NextResponse.redirect(new URL("/login", request.url));
  }

  if (pathname.startsWith("/driver/") && role !== "DRIVER") {
    return NextResponse.redirect(new URL("/login", request.url));
  }

  if (pathname.startsWith("/user/") && role !== "USER") {
    return NextResponse.redirect(new URL("/login", request.url));
  }

  return NextResponse.next();
}

export const config = {
  matcher: ["/((?!_next/static|_next/image|favicon.ico|api/auth).*)"],
};

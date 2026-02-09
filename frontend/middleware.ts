import type { NextRequest } from "next/server";
import { NextResponse } from "next/server";

// Use a BACKEND_URL that resolves from inside the frontend container.
// Recommended in prod: https://team26-borsibaar.ddns.net (goes through nginx)
// Fallback: docker network name for backend.
const backendUrl =
  process.env.BACKEND_URL || "http://borsibaar-backend:8080";

type UserPayload = {
  needsOnboarding: boolean;
  // include other fields if you use them elsewhere
};

async function fetchUser(req: NextRequest): Promise<UserPayload | null> {
  try {
    const cookie = req.headers.get("cookie") || "";

    const res = await fetch(`${backendUrl}/api/account`, {
      headers: {
        cookie,
        // Force Spring Security to treat this as an API/AJAX call
        // so it returns 401 instead of redirecting to OAuth.
        accept: "application/json",
        "x-requested-with": "XMLHttpRequest",
      },
      cache: "no-store",
      // Never follow redirects in middleware; treat them as "not logged in"
      redirect: "manual",
    });

    // Unauthenticated (expected)
    if (res.status === 401) return null;

    // If backend tries to redirect to OAuth, treat as not logged in
    if (res.status >= 300 && res.status < 400) return null;

    if (!res.ok) return null;

    const ct = res.headers.get("content-type") || "";
    if (!ct.includes("application/json")) return null;

    return (await res.json()) as UserPayload;
  } catch {
    return null;
  }
}

export async function middleware(req: NextRequest) {
  const { pathname } = req.nextUrl;

  const user = await fetchUser(req);

  const protectedPaths = ["/dashboard", "/onboarding", "/pos"];
  const isProtected = protectedPaths.some((p) => pathname.startsWith(p));

  // If visiting protected pages without a user, go to /login
  if (isProtected && !user) {
    return NextResponse.redirect(new URL("/login", req.url));
  }

  // /login redirects if authenticated
  if (pathname.startsWith("/login")) {
    if (user) {
      return NextResponse.redirect(
        new URL(user.needsOnboarding ? "/onboarding" : "/dashboard", req.url)
      );
    }
    return NextResponse.next();
  }

  // If user exists, enforce onboarding flow
  if (
    user &&
    (pathname.startsWith("/dashboard") || pathname.startsWith("/pos")) &&
    user.needsOnboarding
  ) {
    return NextResponse.redirect(new URL("/onboarding", req.url));
  }

  if (user && pathname.startsWith("/onboarding") && user.needsOnboarding === false) {
    return NextResponse.redirect(new URL("/dashboard", req.url));
  }

  return NextResponse.next();
}

export const config = {
  matcher: [
    "/login",
    "/dashboard/:path*",
    "/onboarding/:path*",
    "/pos/:path*",
    "/inventory/:path*",
    "/client/:path*",
  ],
};

export const dynamic = "force-dynamic";

export default function LoginPage() {
  return (
    <div className="flex flex-col min-h-screen items-center justify-center content-center gap-4">
      <h1 className="text-2xl font-bold text-center">Login</h1>

      {/*
        Use a RELATIVE URL so OAuth always goes through the current domain.
        Nginx will proxy /oauth2/* to the backend.
        This avoids hardcoded localhost and env-based breakage.
      */}
      <a href="/oauth2/authorization/google">
        <button
          type="button"
          className="text-white bg-gradient-to-r from-blue-500 via-blue-600 to-blue-700 hover:bg-gradient-to-br focus:ring-4 focus:outline-none focus:ring-blue-300 dark:focus:ring-blue-800 font-medium rounded-lg text-sm px-5 py-2.5 text-center me-2 mb-2"
        >
          Login with Google
        </button>
      </a>
    </div>
  );
}

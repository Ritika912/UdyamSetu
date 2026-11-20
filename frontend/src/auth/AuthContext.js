import { createContext, useContext } from "react";

/** Authentication context value: { status, username, sessionError, login, logout }. Provided by AuthProvider. */
export const AuthContext = createContext(null);

/** Returns the authentication context. Must be used inside AuthProvider. */
export function useAuth() {
  const value = useContext(AuthContext);
  if (!value) {
    throw new Error("useAuth must be used inside AuthProvider.");
  }
  return value;
}

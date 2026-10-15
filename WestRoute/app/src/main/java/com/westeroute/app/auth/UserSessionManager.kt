package com.westeroute.app.auth

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.westeroute.app.model.AuthProvider
import com.westeroute.app.model.CitizenAuthSession
import com.westeroute.app.model.CitizenUser
import com.westeroute.app.model.UserRole
import com.westeroute.app.navigation.Screen

private const val TAG = "UserSessionManager"

/**
 * Manages persistent user sessions across app process deaths, restarts, and device reboots.
 * Guarantees that once logged in, the user stays logged in until they explicitly choose to log out.
 */
object UserSessionManager {

    private const val PREFS_NAME = "westeroute_user_session"
    private const val KEY_IS_LOGGED_IN = "is_logged_in"
    private const val KEY_USER_ROLE = "user_role"
    private const val KEY_USER_NAME = "user_name"
    private const val KEY_USER_EMAIL = "user_email"
    private const val KEY_AUTH_PROVIDER = "auth_provider"
    private const val KEY_WORKER_ID = "worker_id"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Checks if a user is currently logged in, checking persistent preferences and Firebase Auth.
     */
    fun isLoggedIn(context: Context): Boolean {
        val prefs = getPrefs(context)
        val isLocallyLoggedIn = prefs.getBoolean(KEY_IS_LOGGED_IN, false)
        if (isLocallyLoggedIn) {
            return true
        }

        // Direct fallback: check Firebase Auth user
        val firebaseUser = FirebaseAuthManager.getCurrentUser()
        if (firebaseUser != null) {
            // Self-heal local session preferences
            saveCitizenSession(context, firebaseUser)
            return true
        }

        return false
    }

    /**
     * Returns the active user role (CITIZEN or WORKER), or null if not logged in.
     */
    fun getUserRole(context: Context): UserRole? {
        val prefs = getPrefs(context)
        val roleStr = prefs.getString(KEY_USER_ROLE, null)
        if (roleStr != null) {
            try {
                return UserRole.valueOf(roleStr)
            } catch (e: Exception) {
                Log.w(TAG, "Unknown role in session preferences: $roleStr", e)
            }
        }

        // Fallback: If Firebase user exists, treat as CITIZEN
        if (FirebaseAuthManager.getCurrentUser() != null) {
            return UserRole.CITIZEN
        }

        return null
    }

    /**
     * Saves a Citizen session persistently to SharedPreferences and updates [CitizenAuthSession].
     */
    fun saveCitizenSession(context: Context, user: CitizenUser) {
        try {
            getPrefs(context).edit()
                .putBoolean(KEY_IS_LOGGED_IN, true)
                .putString(KEY_USER_ROLE, UserRole.CITIZEN.name)
                .putString(KEY_USER_NAME, user.name)
                .putString(KEY_USER_EMAIL, user.email)
                .putString(KEY_AUTH_PROVIDER, user.authProvider.name)
                .apply()

            CitizenAuthSession.setSession(user)
            Log.d(TAG, "Citizen session saved persistently for ${user.email}")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving citizen session", e)
        }
    }

    /**
     * Saves a Worker session persistently to SharedPreferences.
     */
    fun saveWorkerSession(
        context: Context,
        workerId: String,
        workerName: String,
        workerEmail: String = ""
    ) {
        try {
            getPrefs(context).edit()
                .putBoolean(KEY_IS_LOGGED_IN, true)
                .putString(KEY_USER_ROLE, UserRole.WORKER.name)
                .putString(KEY_WORKER_ID, workerId)
                .putString(KEY_USER_NAME, workerName)
                .putString(KEY_USER_EMAIL, workerEmail)
                .apply()
            Log.d(TAG, "Worker session saved persistently for ID: $workerId, Email: $workerEmail")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving worker session", e)
        }
    }


    /**
     * Restores the Citizen user session from live Firebase or local storage into [CitizenAuthSession].
     */
    fun restoreSessionIfAvailable(context: Context): CitizenUser? {
        // Priority 1: Check live Firebase user token
        val liveFirebaseUser = FirebaseAuthManager.getCurrentUser()
        if (liveFirebaseUser != null) {
            saveCitizenSession(context, liveFirebaseUser)
            return liveFirebaseUser
        }

        // Priority 2: Saved Citizen data in SharedPreferences
        val prefs = getPrefs(context)
        val isLoggedIn = prefs.getBoolean(KEY_IS_LOGGED_IN, false)
        val role = prefs.getString(KEY_USER_ROLE, null)

        if (isLoggedIn && role == UserRole.CITIZEN.name) {
            val name = prefs.getString(KEY_USER_NAME, "Citizen") ?: "Citizen"
            val email = prefs.getString(KEY_USER_EMAIL, "") ?: ""
            val providerStr = prefs.getString(KEY_AUTH_PROVIDER, AuthProvider.EMAIL.name)
            val provider = try {
                AuthProvider.valueOf(providerStr ?: AuthProvider.EMAIL.name)
            } catch (e: Exception) {
                AuthProvider.EMAIL
            }

            val restoredUser = CitizenUser(
                name = name,
                email = email,
                authProvider = provider,
                avatarInitial = name.firstOrNull()?.uppercase() ?: "C"
            )
            CitizenAuthSession.setSession(restoredUser)
            return restoredUser
        }

        return null
    }

    /**
     * Clears all session data across preferences and Firebase Auth on logout.
     */
    fun clearSession(context: Context) {
        try {
            getPrefs(context).edit().clear().apply()
            FirebaseAuthManager.signOut()
            CitizenAuthSession.signOut()
            Log.d(TAG, "User session cleared")
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing user session", e)
        }
    }

    /**
     * Resolves the start destination for AppNavigation on cold start.
     * If user is already authenticated, directly directs to their dashboard.
     */
    fun getInitialRoute(context: Context): String {
        restoreSessionIfAvailable(context)

        val role = getUserRole(context)
        val loggedIn = isLoggedIn(context)

        Log.d(TAG, "Resolving initial route: loggedIn=$loggedIn, role=$role")

        return if (loggedIn && role != null) {
            when (role) {
                UserRole.CITIZEN -> Screen.CitizenDashboard.route
                UserRole.WORKER -> Screen.WorkerDashboard.route
            }
        } else {
            Screen.RoleSelection.route
        }
    }

    /**
     * Returns saved worker credentials/details.
     */
    fun getSavedWorkerDetails(context: Context): Pair<String, String> {
        val prefs = getPrefs(context)
        val id = prefs.getString(KEY_WORKER_ID, "") ?: ""
        val name = prefs.getString(KEY_USER_NAME, "Worker") ?: "Worker"
        return Pair(id, name)
    }

    /**
     * Returns saved worker email, if any.
     */
    fun getSavedWorkerEmail(context: Context): String {
        return getPrefs(context).getString(KEY_USER_EMAIL, "") ?: ""
    }
}


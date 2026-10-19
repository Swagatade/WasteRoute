package com.wasteroute.app.auth

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.wasteroute.app.model.AuthProvider
import com.wasteroute.app.model.CitizenAuthSession
import com.wasteroute.app.model.CitizenUser
import com.wasteroute.app.model.UserRole
import com.wasteroute.app.navigation.Screen

private const val TAG = "UserSessionManager"

/**
 * Manages persistent user sessions across app process deaths, restarts, and device reboots.
 * Guarantees that once logged in, the user stays logged in until they explicitly choose to log out.
 */
object UserSessionManager {

    private const val PREFS_NAME = "wasteroute_user_session"
    private const val KEY_IS_LOGGED_IN = "is_logged_in"
    private const val KEY_USER_ROLE = "user_role"
    private const val KEY_USER_NAME = "user_name"
    private const val KEY_USER_EMAIL = "user_email"
    private const val KEY_AUTH_PROVIDER = "auth_provider"
    private const val KEY_WORKER_ID = "worker_id"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /** Workers are Firebase users too (provisioned under @worker.in): they must never be treated as citizens. */
    private fun isWorkerEmail(email: String?): Boolean =
        email?.trim()?.endsWith(FirebaseAuthManager.REQUIRED_WORKER_DOMAIN, ignoreCase = true) == true

    /** Rebuilds a saved worker session from the live Firebase account (worker id = the part before the @). */
    private fun healWorkerSession(context: Context, email: String) {
        val prefs = getPrefs(context)
        val savedId = prefs.getString(KEY_WORKER_ID, "").orEmpty()
        val id = savedId.ifBlank { email.substringBefore("@").trim().uppercase() }
        val name = prefs.getString(KEY_USER_NAME, null)?.takeIf { it.isNotBlank() } ?: "Worker $id"
        saveWorkerSession(context, id, name, email)
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
            // Self-heal local session preferences (as a worker or a citizen, whichever the account is)
            if (isWorkerEmail(firebaseUser.email)) healWorkerSession(context, firebaseUser.email)
            else saveCitizenSession(context, firebaseUser)
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

        // Fallback: a live Firebase account is a WORKER when it is an @worker.in address, otherwise a CITIZEN
        val live = FirebaseAuthManager.getCurrentUser()
        if (live != null) {
            return if (isWorkerEmail(live.email)) UserRole.WORKER else UserRole.CITIZEN
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
        // A worker session is never converted into a citizen one (this overwrote the saved WORKER role on the
        // second app launch and opened the citizen side for a signed-in worker).
        val savedRole = getPrefs(context).getString(KEY_USER_ROLE, null)
        val liveFirebaseUser = FirebaseAuthManager.getCurrentUser()
        if (savedRole == UserRole.WORKER.name || isWorkerEmail(liveFirebaseUser?.email)) {
            if (liveFirebaseUser != null && isWorkerEmail(liveFirebaseUser.email)) {
                healWorkerSession(context, liveFirebaseUser.email)
            }
            return null
        }

        // Priority 1: Check live Firebase user token
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


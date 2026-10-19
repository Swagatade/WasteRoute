package com.wasteroute.app.auth

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.gms.tasks.Task
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.wasteroute.app.BuildConfig
import com.wasteroute.app.model.AuthProvider
import com.wasteroute.app.model.CitizenAuthSession
import com.wasteroute.app.model.CitizenUser
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException


private const val TAG = "FirebaseAuthManager"

/**
 * Handles real Firebase Authentication for Citizen users, including:
 * - Google Sign-In via Android Credential Manager
 * - Email & Password Sign-In
 * - Email & Password User Registration
 * - Session state synchronization with [CitizenAuthSession]
 */
object FirebaseAuthManager {

    /**
     * Checks if Firebase has been initialized in the application.
     */
    fun isFirebaseInitialized(context: Context): Boolean {
        return try {
            FirebaseApp.getApps(context).isNotEmpty()
        } catch (e: Exception) {
            Log.e(TAG, "Error checking Firebase initialization", e)
            false
        }
    }

    /**
     * Fallback Web Client ID injected securely from .env via BuildConfig
     * in case resource extraction is delayed or missing.
     */
    private val FALLBACK_WEB_CLIENT_ID: String
        get() = BuildConfig.GOOGLE_OAUTH_CLIENT_ID

    /**
     * Retrieves the Firebase Web Client ID for OAuth Google Sign-In.
     * The Google Services Gradle plugin extracts this automatically from google-services.json
     * and generates the `default_web_client_id` string resource.
     */
    fun getWebClientId(context: Context): String {
        val resId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
        val extractedId = if (resId != 0) {
            val id = context.getString(resId)
            if (id.isNotBlank() && !id.contains("placeholder")) id else null
        } else {
            null
        }
        return extractedId ?: FALLBACK_WEB_CLIENT_ID
    }

    /**
     * Initiates real Google Sign-In using the Android Credential Manager and exchanges
     * the Google ID Token with Firebase Authentication.
     */
    suspend fun signInWithGoogle(context: Context): Result<CitizenUser> {
        return try {
            val webClientId = getWebClientId(context)

            val credentialManager = CredentialManager.create(context)
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(webClientId)
                .setAutoSelectEnabled(false)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            Log.d(TAG, "Prompting Google Credential Manager...")
            val response = credentialManager.getCredential(request = request, context = context)
            val credential = response.credential

            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val idToken = googleIdTokenCredential.idToken
                Log.d(TAG, "Obtained Google ID Token. Authenticating with Firebase...")

                val authCredential: AuthCredential = GoogleAuthProvider.getCredential(idToken, null)
                val authResult = FirebaseAuth.getInstance().signInWithCredential(authCredential).await()
                val firebaseUser = authResult.user
                    ?: return Result.failure(IllegalStateException("Firebase User is null after sign in"))

                val citizenUser = mapFirebaseUser(firebaseUser, AuthProvider.GOOGLE)
                CitizenAuthSession.setSession(citizenUser)
                UserSessionManager.saveCitizenSession(context, citizenUser)
                Result.success(citizenUser)
            } else {
                Result.failure(IllegalStateException("Unexpected credential format received: ${credential.type}"))
            }
        } catch (e: GetCredentialCancellationException) {
            Log.w(TAG, "Google Sign-In was cancelled by user")
            Result.failure(UserCancelledException("Sign-in cancelled"))
        } catch (e: NoCredentialException) {
            Log.w(TAG, "No Google accounts available for this app", e)
            Result.failure(
                NoGoogleAccountException(
                    "Google account not recognized for this app. If your Gmail is already on this device, the SHA-1 fingerprint must be added in Firebase Console. You can also sign in below with email."
                )
            )
        } catch (e: GetCredentialException) {
            Log.e(TAG, "Credential Manager error: ${e.message}", e)
            Result.failure(IllegalStateException(e.message ?: "Failed to retrieve Google credentials"))
        } catch (e: Exception) {
            Log.e(TAG, "Firebase Google sign-in failed", e)
            Result.failure(mapFirebaseException(e))
        }
    }

    /**
     * Opens Android System Settings or Add Account screen so the user can add a Google account.
     */
    fun openAddAccountSettings(context: Context) {
        try {
            val intent = android.content.Intent(android.provider.Settings.ACTION_ADD_ACCOUNT).apply {
                putExtra(android.provider.Settings.EXTRA_ACCOUNT_TYPES, arrayOf("com.google"))
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            try {
                val syncIntent = android.content.Intent(android.provider.Settings.ACTION_SYNC_SETTINGS).apply {
                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(syncIntent)
            } catch (e2: Exception) {
                val settingsIntent = android.content.Intent(android.provider.Settings.ACTION_SETTINGS).apply {
                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(settingsIntent)
            }
        }
    }

    /**
     * Signs in with Email and Password using Firebase Auth.
     */
    suspend fun signInWithEmail(email: String, password: String, context: Context? = null): Result<CitizenUser> {
        return try {
            val authResult = FirebaseAuth.getInstance()
                .signInWithEmailAndPassword(email.trim(), password)
                .await()
            val firebaseUser = authResult.user
                ?: return Result.failure(IllegalStateException("Authentication succeeded but user profile is null"))

            val citizenUser = mapFirebaseUser(firebaseUser, AuthProvider.EMAIL)
            CitizenAuthSession.setSession(citizenUser)
            context?.let { UserSessionManager.saveCitizenSession(it, citizenUser) }
            Result.success(citizenUser)
        } catch (e: Exception) {
            Log.e(TAG, "Firebase Email sign-in failed", e)
            Result.failure(mapFirebaseException(e))
        }
    }

    /**
     * Registers a new Citizen account with Email and Password in Firebase Auth.
     */
    suspend fun registerWithEmail(
        email: String,
        password: String,
        fullName: String,
        context: Context? = null
    ): Result<CitizenUser> {
        return try {
            val authResult = FirebaseAuth.getInstance()
                .createUserWithEmailAndPassword(email.trim(), password)
                .await()
            val firebaseUser = authResult.user
                ?: return Result.failure(IllegalStateException("Account creation succeeded but user profile is null"))

            // Update user's display name
            if (fullName.isNotBlank()) {
                val profileUpdates = UserProfileChangeRequest.Builder()
                    .setDisplayName(fullName.trim())
                    .build()
                firebaseUser.updateProfile(profileUpdates).await()
            }

            val citizenUser = mapFirebaseUser(firebaseUser, AuthProvider.EMAIL, customName = fullName.ifBlank { null })
            CitizenAuthSession.setSession(citizenUser)
            context?.let { UserSessionManager.saveCitizenSession(it, citizenUser) }
            Result.success(citizenUser)
        } catch (e: Exception) {
            Log.e(TAG, "Firebase registration failed", e)
            Result.failure(mapFirebaseException(e))
        }
    }

    /**
     * Signs out the current Firebase user, clears [CitizenAuthSession], and wipes persistent storage.
     */
    fun signOut(context: Context? = null) {
        try {
            FirebaseAuth.getInstance().signOut()
        } catch (e: Exception) {
            Log.e(TAG, "Error during Firebase sign out", e)
        }
        CitizenAuthSession.signOut()
        context?.let {
            try {
                UserSessionManager.clearSession(it)
            } catch (e: Exception) {
                Log.e(TAG, "Error clearing session on sign out", e)
            }
        }
    }

    /**
     * Returns the currently signed-in Firebase user mapped to [CitizenUser], or null if not authenticated.
     */
    fun getCurrentUser(): CitizenUser? {
        val firebaseUser = try {
            FirebaseAuth.getInstance().currentUser
        } catch (e: Exception) {
            null
        } ?: return null

        val isGoogle = firebaseUser.providerData.any { it.providerId == "google.com" }
        return mapFirebaseUser(
            user = firebaseUser,
            provider = if (isGoogle) AuthProvider.GOOGLE else AuthProvider.EMAIL
        )
    }

    private fun mapFirebaseUser(
        user: FirebaseUser,
        provider: AuthProvider,
        customName: String? = null
    ): CitizenUser {
        val resolvedName = customName
            ?: user.displayName?.ifBlank { null }
            ?: user.email?.substringBefore("@")
                ?.replace(".", " ")
                ?.split(" ")
                ?.joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
            ?: "Citizen"

        return CitizenUser(
            name = resolvedName,
            email = user.email ?: "",
            authProvider = provider,
            avatarInitial = resolvedName.firstOrNull()?.uppercase() ?: "C"
        )
    }

    const val REQUIRED_WORKER_DOMAIN = "@worker.in"

    /**
     * Authenticates a municipal sanitation worker with Firebase Authentication.
     * Strictly verifies that ONLY registered accounts under the official "@worker.in"
     * domain are permitted to access the Worker portal.
     *
     * @param workerId Municipal identification code (e.g., "WRK-106" or "WR-W-204")
     * @param email Worker's registered municipal email ending with "@worker.in"
     * @param password Password set by the supervisor during registration
     * @param context Application context for session persistence
     * @return Result containing Pair of (WorkerID, WorkerDisplayName)
     */
    suspend fun signInWorker(
        workerId: String,
        email: String,
        password: String,
        context: Context
    ): Result<Pair<String, String>> {
        val cleanEmail = email.trim().lowercase()
        val cleanWorkerId = workerId.trim().uppercase().ifBlank {
            cleanEmail.substringBefore("@").uppercase()
        }

        // Domain validation
        if (!cleanEmail.endsWith(REQUIRED_WORKER_DOMAIN) || cleanEmail.length <= REQUIRED_WORKER_DOMAIN.length) {
            val err = "Invalid Worker Email: Worker credentials must be registered under '$REQUIRED_WORKER_DOMAIN' (e.g. ${cleanWorkerId.lowercase()}$REQUIRED_WORKER_DOMAIN)"
            return Result.failure(IllegalArgumentException(err))
        }

        if (password.length < 6) {
            return Result.failure(IllegalArgumentException("Password must be at least 6 characters."))
        }

        return try {
            val authResult = FirebaseAuth.getInstance()
                .signInWithEmailAndPassword(cleanEmail, password)
                .await()

            val firebaseUser = authResult.user
                ?: return Result.failure(IllegalStateException("Authentication succeeded but worker profile is null"))

            // Enforce domain check on authenticated Firebase user
            val verifiedEmail = firebaseUser.email?.lowercase() ?: ""
            if (!verifiedEmail.endsWith(REQUIRED_WORKER_DOMAIN)) {
                // If a non-worker user tries to log in (e.g. citizen), reject and log out immediately
                FirebaseAuth.getInstance().signOut()
                return Result.failure(
                    SecurityException(
                        "Access Denied: The account $verifiedEmail is not a registered municipal worker. Worker logins require an official '$REQUIRED_WORKER_DOMAIN' address provisioned by your supervisor."
                    )
                )
            }

            // The worker ID comes from the account, never from what was typed: a typo (for example the letter O
            // for a zero) must not create a second worker record. Prefer the record registered for this email.
            val emailKey = verifiedEmail.substringBefore("@").uppercase()
            val registeredIds = runCatching {
                com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    .collection("workers").whereEqualTo("email", verifiedEmail).get().await()
                    .documents.map { it.id }
            }.getOrDefault(emptyList())
            val canonicalWorkerId = registeredIds.firstOrNull { it.equals(emailKey, ignoreCase = true) }
                ?: registeredIds.firstOrNull()
                ?: emailKey
            if (!canonicalWorkerId.equals(cleanWorkerId, ignoreCase = true)) {
                Log.w(TAG, "Typed worker ID $cleanWorkerId differs from account ID $canonicalWorkerId; using the account ID.")
            }

            val resolvedName = firebaseUser.displayName?.ifBlank { null }
                ?: canonicalWorkerId.let { "Worker $it" }

            // Save persistent worker session
            UserSessionManager.saveWorkerSession(
                context = context,
                workerId = canonicalWorkerId,
                workerName = resolvedName,
                workerEmail = cleanEmail
            )

            // Self-heal: guarantee the Supervisor portal can list this worker
            com.wasteroute.app.firebase.CollectionRepository.ensureWorkerRecord(
                workerId = canonicalWorkerId,
                displayName = resolvedName.substringBefore(" (").trim(),
                email = verifiedEmail
            )

            Log.d(TAG, "Worker successfully authenticated via Firebase: $canonicalWorkerId ($cleanEmail)")
            Result.success(Pair(canonicalWorkerId, resolvedName))
        } catch (e: Exception) {
            Log.e(TAG, "Worker Firebase sign-in failed for $cleanEmail", e)
            val mapped = when (e) {
                is FirebaseAuthInvalidUserException,
                is FirebaseAuthInvalidCredentialsException -> {
                    // Firebase gives the same error for a wrong password and for an unknown account, so look the
                    // email up in the worker registry to tell the worker which one it really is.
                    val registered = runCatching {
                        !com.google.firebase.firestore.FirebaseFirestore.getInstance()
                            .collection("workers").whereEqualTo("email", cleanEmail).limit(1).get().await().isEmpty
                    }.getOrDefault(false)
                    Exception(
                        if (registered) "This worker is registered, but the password is incorrect. Check the password or ask your supervisor to reset it."
                        else "No worker is registered with this email. Ask your supervisor to register you in the Supervisor Portal."
                    )
                }
                is FirebaseAuthWeakPasswordException -> Exception("Password is too weak. Please use at least 6 characters.")
                is FirebaseAuthException -> Exception(e.localizedMessage ?: "Worker authentication error (${e.errorCode})")
                else -> Exception(e.localizedMessage ?: "Worker authentication failed. Please check your internet connection.")
            }
            Result.failure(mapped)
        }
    }

    private fun mapFirebaseException(e: Exception): Exception {
        return when (e) {
            is FirebaseAuthInvalidUserException -> Exception("No account found with this email. Please register first.")
            is FirebaseAuthWeakPasswordException -> Exception("Password is too weak. Please use at least 6 characters.")
            is FirebaseAuthInvalidCredentialsException -> Exception("Invalid credentials. Please check your email and password.")
            is FirebaseAuthUserCollisionException -> Exception("An account already exists with this email address.")
            is FirebaseAuthException -> Exception(e.localizedMessage ?: "Authentication error (${e.errorCode})")
            else -> Exception(e.localizedMessage ?: "Authentication failed. Please check your internet connection.")
        }
    }
}

class UserCancelledException(message: String) : Exception(message)
class NoGoogleAccountException(message: String) : Exception(message)

/**
 * Await extension for Google Play Services Task<T> with coroutine cancellation support.
 */
suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { cont ->
    addOnSuccessListener { result ->
        if (cont.isActive) cont.resume(result)
    }
    addOnFailureListener { exception ->
        if (cont.isActive) cont.resumeWithException(exception)
    }
    addOnCanceledListener {
        if (cont.isActive) cont.cancel()
    }
}

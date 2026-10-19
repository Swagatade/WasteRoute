package com.westeroute.app.model

/**
 * Supported authentication providers for Citizen users.
 */
enum class AuthProvider {
    EMAIL,
    GOOGLE
}

/**
 * Data model representing an authenticated Citizen user.
 */
data class CitizenUser(
    val name: String,
    val email: String,
    val authProvider: AuthProvider,
    val avatarInitial: String = name.firstOrNull()?.uppercase() ?: "C"
)

/**
 * Lightweight in-memory session manager for Citizen user authentication.
 * Stores the authenticated Firebase user session.
 */
object CitizenAuthSession {
    var currentUser: CitizenUser? = null
        private set

    fun setSession(user: CitizenUser) {
        currentUser = user
    }

    fun signOut() {
        currentUser = null
    }
}

package com.westeroute.app.model

/**
 * User roles available in WesteRoute.
 */
enum class UserRole(
    val title: String,
    val subtitle: String,
    val description: String,
    val actionText: String,
    val badgeText: String
) {
    CITIZEN(
        title = "Citizen",
        subtitle = "Community & Cleanliness",
        description = "Report city cleanliness issues, track complaints and stay connected with your local services.",
        actionText = "Continue as Citizen",
        badgeText = "Citizen Access"
    ),
    WORKER(
        title = "Worker",
        subtitle = "Field Operations & Routing",
        description = "Access assigned collection tasks, routes and daily work updates.",
        actionText = "Continue as Worker",
        badgeText = "Worker Access"
    )
}

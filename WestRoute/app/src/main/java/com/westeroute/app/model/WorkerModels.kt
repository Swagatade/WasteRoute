package com.westeroute.app.model

enum class TaskStatus(val displayName: String) {
    PENDING("Pending"),
    ASSIGNED("Assigned"),
    IN_PROGRESS("In Progress"),
    COMPLETED("Completed")
}

enum class TaskPriority(val displayName: String) {
    LOW("Low"),
    MEDIUM("Medium"),
    HIGH("High"),
    URGENT("Urgent")
}

data class WorkerTask(
    val id: String,
    val title: String,
    val location: String,
    val distance: String,
    val priority: TaskPriority,
    val status: TaskStatus,
    val assignedTime: String,
    val estimatedTravel: String = "4 min",
    val description: String = "Clear solid waste collection point and verify surrounding area cleanliness."
)

data class WorkerProfile(
    val id: String = "",
    val name: String = "Worker",
    val role: String = "Waste Collection Worker",
    val workZone: String = "SRMIST – Kattankulathur",
    val shiftTiming: String = "08:00 AM — 04:00 PM",
    val startedAt: String = "08:03 AM",
    val completedTasksToday: Int = 3,
    val totalTasksToday: Int = 8,
    val householdCollectionsDone: Int = 24,
    val householdCollectionsTotal: Int = 36,
    val routeDistanceKm: Double = 12.6
)

object MockWorkerData {
    val sampleTasks = listOf(
        WorkerTask(
            id = "#WR-2048",
            title = "Roadside Garbage",
            location = "SRMIST Main Gate, Kattankulathur",
            distance = "450 m",
            priority = TaskPriority.HIGH,
            status = TaskStatus.IN_PROGRESS,
            assignedTime = "12:05 PM",
            estimatedTravel = "3 min"
        ),
        WorkerTask(
            id = "#WR-2050",
            title = "Overflowing Public Bin",
            location = "SRM Tech Park",
            distance = "850 m",
            priority = TaskPriority.MEDIUM,
            status = TaskStatus.ASSIGNED,
            assignedTime = "12:20 PM",
            estimatedTravel = "5 min"
        ),
        WorkerTask(
            id = "#WR-2053",
            title = "Garbage Near Hostel",
            location = "SRM Hostel Zone",
            distance = "1.1 km",
            priority = TaskPriority.HIGH,
            status = TaskStatus.ASSIGNED,
            assignedTime = "12:34 PM",
            estimatedTravel = "7 min"
        ),
        WorkerTask(
            id = "#WR-2055",
            title = "Roadside Waste",
            location = "Potheri Main Road",
            distance = "1.5 km",
            priority = TaskPriority.URGENT,
            status = TaskStatus.PENDING,
            assignedTime = "12:50 PM",
            estimatedTravel = "10 min"
        ),
        WorkerTask(
            id = "#WR-2057",
            title = "Overflowing Community Bin",
            location = "SRM Hospital Area",
            distance = "1.2 km",
            priority = TaskPriority.MEDIUM,
            status = TaskStatus.ASSIGNED,
            assignedTime = "01:05 PM",
            estimatedTravel = "8 min"
        )
    )
}

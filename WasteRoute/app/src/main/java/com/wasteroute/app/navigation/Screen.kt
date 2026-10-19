package com.westeroute.app.navigation

sealed class Screen(val route: String) {
    object RoleSelection : Screen("role_selection")
    object CitizenLogin : Screen("citizen_login")
    object CitizenRegister : Screen("citizen_register")
    object WorkerLogin : Screen("worker_login")
    object ForgotPassword : Screen("forgot_password/{role}") {
        fun createRoute(role: String) = "forgot_password/$role"
    }
    object CitizenDashboard : Screen("citizen_dashboard")
    object WorkerDashboard : Screen("worker_dashboard")
    object WorkerTasks : Screen("worker_tasks")
    object WorkerTaskDetail : Screen("worker_task_detail/{taskId}") {
        fun createRoute(taskId: String) = "worker_task_detail/$taskId"
    }
    object WorkerRoute : Screen("worker_route")
    object WorkerQr : Screen("worker_qr")
    object WorkerNotifications : Screen("worker_notifications")
    object WorkerProfile : Screen("worker_profile")
}


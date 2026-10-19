package com.westeroute.app.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.westeroute.app.auth.UserSessionManager
import com.westeroute.app.model.UserRole
import com.westeroute.app.screens.CitizenLoginScreen
import com.westeroute.app.screens.CitizenRegisterScreen
import com.westeroute.app.screens.ForgotPasswordScreen
import com.westeroute.app.screens.RoleSelectionScreen
import com.westeroute.app.screens.WorkerLoginScreen

@Suppress("FunctionName", "ModifierParameterPosition")
@Composable
fun AppNavigation(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    val context = LocalContext.current

    // Dynamically resolve start destination: if user is already logged in, jump straight to their dashboard
    val startDestination = remember {
        UserSessionManager.getInitialRoute(context)
    }

    val onSignOutAction: () -> Unit = {
        com.westeroute.app.firebase.CollectionRepository.stopListening()
        com.westeroute.app.notify.WorkerAlertService.stop(context)
        UserSessionManager.clearSession(context)
        navController.navigate(Screen.RoleSelection.route) {
            popUpTo(0) { inclusive = true }
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        // Step 1: Role Selection Screen
        composable(
            route = Screen.RoleSelection.route,
            enterTransition = {
                fadeIn(animationSpec = tween(250))
            },
            exitTransition = {
                slideIntoExit(AnimatedContentTransitionScope.SlideDirection.Left) + fadeOut(animationSpec = tween(250))
            },
            popEnterTransition = {
                slideIntoEntry(AnimatedContentTransitionScope.SlideDirection.Right) + fadeIn(animationSpec = tween(250))
            },
            popExitTransition = {
                fadeOut(animationSpec = tween(250))
            }
        ) {
            RoleSelectionScreen(
                onSelectRole = { role ->
                    when (role) {
                        UserRole.CITIZEN -> navController.navigate(Screen.CitizenLogin.route)
                        UserRole.WORKER -> navController.navigate(Screen.WorkerLogin.route)
                    }
                }
            )
        }

        // Step 2: Citizen Login Screen
        composable(
            route = Screen.CitizenLogin.route,
            enterTransition = {
                slideIntoEntry(AnimatedContentTransitionScope.SlideDirection.Left) + fadeIn(animationSpec = tween(250))
            },
            exitTransition = {
                slideIntoExit(AnimatedContentTransitionScope.SlideDirection.Left) + fadeOut(animationSpec = tween(250))
            },
            popEnterTransition = {
                slideIntoEntry(AnimatedContentTransitionScope.SlideDirection.Right) + fadeIn(animationSpec = tween(250))
            },
            popExitTransition = {
                slideIntoExit(AnimatedContentTransitionScope.SlideDirection.Right) + fadeOut(animationSpec = tween(250))
            }
        ) {
            CitizenLoginScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToRegister = {
                    navController.navigate(Screen.CitizenRegister.route)
                },
                onNavigateToForgotPassword = {
                    navController.navigate(Screen.ForgotPassword.createRoute("citizen"))
                },
                onSignInSuccess = {
                    navController.navigate(Screen.CitizenDashboard.route) {
                        popUpTo(Screen.RoleSelection.route) { inclusive = true }
                    }
                }
            )
        }

        // Step 2B: Citizen Register Screen
        composable(
            route = Screen.CitizenRegister.route,
            enterTransition = {
                slideIntoEntry(AnimatedContentTransitionScope.SlideDirection.Left) + fadeIn(animationSpec = tween(250))
            },
            exitTransition = {
                slideIntoExit(AnimatedContentTransitionScope.SlideDirection.Left) + fadeOut(animationSpec = tween(250))
            },
            popEnterTransition = {
                slideIntoEntry(AnimatedContentTransitionScope.SlideDirection.Right) + fadeIn(animationSpec = tween(250))
            },
            popExitTransition = {
                slideIntoExit(AnimatedContentTransitionScope.SlideDirection.Right) + fadeOut(animationSpec = tween(250))
            }
        ) {
            CitizenRegisterScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToLogin = {
                    navController.popBackStack()
                },
                onRegisterSuccess = {
                    navController.navigate(Screen.CitizenDashboard.route) {
                        popUpTo(Screen.RoleSelection.route) { inclusive = true }
                    }
                }
            )
        }

        // Step 2: Worker Login Screen
        composable(
            route = Screen.WorkerLogin.route,
            enterTransition = {
                slideIntoEntry(AnimatedContentTransitionScope.SlideDirection.Left) + fadeIn(animationSpec = tween(250))
            },
            exitTransition = {
                slideIntoExit(AnimatedContentTransitionScope.SlideDirection.Left) + fadeOut(animationSpec = tween(250))
            },
            popEnterTransition = {
                slideIntoEntry(AnimatedContentTransitionScope.SlideDirection.Right) + fadeIn(animationSpec = tween(250))
            },
            popExitTransition = {
                slideIntoExit(AnimatedContentTransitionScope.SlideDirection.Right) + fadeOut(animationSpec = tween(250))
            }
        ) {
            WorkerLoginScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToForgotPassword = {
                    navController.navigate(Screen.ForgotPassword.createRoute("worker"))
                },
                onSignInSuccess = {
                    navController.navigate(Screen.WorkerDashboard.route) {
                        popUpTo(Screen.RoleSelection.route) { inclusive = true }
                    }
                }
            )
        }

        // Step 3: Forgot Password Screen
        composable(
            route = Screen.ForgotPassword.route,
            arguments = listOf(
                navArgument("role") {
                    type = NavType.StringType
                    defaultValue = "citizen"
                }
            ),
            enterTransition = {
                slideIntoEntry(AnimatedContentTransitionScope.SlideDirection.Left) + fadeIn(animationSpec = tween(250))
            },
            exitTransition = {
                slideIntoExit(AnimatedContentTransitionScope.SlideDirection.Left) + fadeOut(animationSpec = tween(250))
            },
            popEnterTransition = {
                slideIntoEntry(AnimatedContentTransitionScope.SlideDirection.Right) + fadeIn(animationSpec = tween(250))
            },
            popExitTransition = {
                slideIntoExit(AnimatedContentTransitionScope.SlideDirection.Right) + fadeOut(animationSpec = tween(250))
            }
        ) { backStackEntry ->
            val roleArg = backStackEntry.arguments?.getString("role")?.lowercase() ?: "citizen"
            val userRole = if (roleArg == "worker") UserRole.WORKER else UserRole.CITIZEN

            ForgotPasswordScreen(
                userRole = userRole,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onResetSuccess = {
                    // Return back to the login screen
                    navController.popBackStack()
                }
            )
        }

        // Step 4: Citizen Dashboard Screen
        composable(
            route = Screen.CitizenDashboard.route,
            enterTransition = {
                slideIntoEntry(AnimatedContentTransitionScope.SlideDirection.Left) + fadeIn(animationSpec = tween(300))
            },
            exitTransition = {
                slideIntoExit(AnimatedContentTransitionScope.SlideDirection.Left) + fadeOut(animationSpec = tween(300))
            },
            popEnterTransition = {
                slideIntoEntry(AnimatedContentTransitionScope.SlideDirection.Right) + fadeIn(animationSpec = tween(300))
            },
            popExitTransition = {
                slideIntoExit(AnimatedContentTransitionScope.SlideDirection.Right) + fadeOut(animationSpec = tween(300))
            }
        ) {
            val citizenName = com.westeroute.app.model.CitizenAuthSession.currentUser?.name ?: "Citizen"
            com.westeroute.app.screens.CitizenDashboardScreen(
                userName = citizenName,
                location = "SRM KTR, Kattankulathur",
                onNavigateToReportWaste = {
                    // Placeholder for future ReportWasteScreen navigation
                },
                onNavigateToComplaints = {},
                onNavigateToMap = {},
                onNavigateToProfile = {},
                onNavigateToNotifications = {},
                onScanQr = {},
                onSignOut = onSignOutAction
            )
        }

        // Step 5: Worker Dashboard Screen
        composable(
            route = Screen.WorkerDashboard.route,
            enterTransition = {
                slideIntoEntry(AnimatedContentTransitionScope.SlideDirection.Left) + fadeIn(animationSpec = tween(300))
            },
            exitTransition = {
                slideIntoExit(AnimatedContentTransitionScope.SlideDirection.Left) + fadeOut(animationSpec = tween(300))
            },
            popEnterTransition = {
                slideIntoEntry(AnimatedContentTransitionScope.SlideDirection.Right) + fadeIn(animationSpec = tween(300))
            },
            popExitTransition = {
                slideIntoExit(AnimatedContentTransitionScope.SlideDirection.Right) + fadeOut(animationSpec = tween(300))
            }
        ) {
            val (savedWorkerId, savedWorkerName) = remember {
                UserSessionManager.getSavedWorkerDetails(context)
            }
            androidx.compose.runtime.LaunchedEffect(savedWorkerId) {
                if (savedWorkerId.isNotBlank()) {
                    com.westeroute.app.firebase.CollectionRepository.ensureWorkerRecord(
                        savedWorkerId,
                        savedWorkerName.substringBefore(" (").trim(),
                        UserSessionManager.getSavedWorkerEmail(context)
                    )
                    com.westeroute.app.firebase.CollectionRepository.listenToWorkerSchedule(savedWorkerId)
                }
            }
            val workerProfile = remember(savedWorkerId, savedWorkerName) {
                com.westeroute.app.model.WorkerProfile(
                    id = savedWorkerId,
                    name = savedWorkerName
                )
            }

            com.westeroute.app.screens.WorkerDashboardScreen(
                profile = workerProfile,
                onNavigateToQr = {
                    navController.navigate(Screen.WorkerQr.route)
                },
                onNavigateToTaskDetail = { taskId ->
                    // Task detail action
                },
                onNavigateToTasks = {},
                onNavigateToRoute = {
                    navController.navigate(Screen.WorkerRoute.route)
                },
                onNavigateToNotifications = {},
                onNavigateToProfile = {},
                onSignOut = onSignOutAction
            )
        }

        // Step 6: Full Screen Worker QR
        composable(
            route = Screen.WorkerQr.route,
            enterTransition = {
                slideIntoEntry(AnimatedContentTransitionScope.SlideDirection.Up) + fadeIn(animationSpec = tween(280))
            },
            exitTransition = {
                slideIntoExit(AnimatedContentTransitionScope.SlideDirection.Down) + fadeOut(animationSpec = tween(280))
            },
            popEnterTransition = {
                slideIntoEntry(AnimatedContentTransitionScope.SlideDirection.Down) + fadeIn(animationSpec = tween(280))
            },
            popExitTransition = {
                slideIntoExit(AnimatedContentTransitionScope.SlideDirection.Down) + fadeOut(animationSpec = tween(280))
            }
        ) {
            val (qrWorkerId, qrWorkerName) = remember {
                UserSessionManager.getSavedWorkerDetails(context)
            }
            com.westeroute.app.screens.WorkerQrScreen(
                profile = com.westeroute.app.model.WorkerProfile(id = qrWorkerId, name = qrWorkerName),
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // Step 7: Worker timetable + department map
        composable(
            route = Screen.WorkerRoute.route,
            enterTransition = {
                slideIntoEntry(AnimatedContentTransitionScope.SlideDirection.Up) + fadeIn(animationSpec = tween(280))
            },
            popExitTransition = {
                slideIntoExit(AnimatedContentTransitionScope.SlideDirection.Down) + fadeOut(animationSpec = tween(280))
            }
        ) {
            val (routeWorkerId, routeWorkerName) = remember {
                UserSessionManager.getSavedWorkerDetails(context)
            }
            com.westeroute.app.screens.WorkerTimetableScreen(
                workerId = routeWorkerId,
                workerName = routeWorkerName,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}

private fun AnimatedContentTransitionScope<*>.slideIntoEntry(
    direction: AnimatedContentTransitionScope.SlideDirection
) = slideIntoContainer(
    towards = direction,
    animationSpec = tween(280)
)

private fun AnimatedContentTransitionScope<*>.slideIntoExit(
    direction: AnimatedContentTransitionScope.SlideDirection
) = slideOutOfContainer(
    towards = direction,
    animationSpec = tween(280)
)

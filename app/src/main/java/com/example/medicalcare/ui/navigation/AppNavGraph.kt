package com.example.medicalcare.ui.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.medicalcare.firebase.FirebaseModule
import com.example.medicalcare.ui.viewmodel.HomeViewModel

// Import screens
import com.example.medicalcare.ui.screen.HomeScreen
import com.example.medicalcare.ui.screen.LoginScreen
import com.example.medicalcare.ui.screen.SignupScreen
import com.example.medicalcare.ui.screen.WelcomeScreen
import com.example.medicalcare.ui.screen.DoctorDiscoveryScreen
import com.example.medicalcare.ui.screen.ChatScreen
import com.example.medicalcare.ui.screen.AppointmentScreen
import com.example.medicalcare.ui.screen.SettingsScreen
import com.example.medicalcare.ui.screen.DoctorProfileScreen
import com.example.medicalcare.ui.screen.ChatThreadScreen
import com.example.medicalcare.ui.screen.UserManagementScreen

@Composable
fun AppNavGraph(
    navController: NavHostController,
    startRoute: String
) {
    // homeViewModel to pass down
    val homeVM: HomeViewModel = viewModel()

    NavHost(navController = navController, startDestination = startRoute) {

        composable(Routes.Welcome.route) {
            WelcomeScreen(
                onLogin = { navController.navigate(Routes.Login.route) },
                onSignup = { navController.navigate(Routes.Signup.route) }
            )
        }

        composable(Routes.Login.route) {
            LoginScreen(
                onSuccess = {
                    navController.navigate(Routes.Home.route) {
                        popUpTo(Routes.Welcome.route) { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.Signup.route) {
            SignupScreen(
                onSuccess = {
                    navController.navigate(Routes.Home.route) {
                        popUpTo(Routes.Welcome.route) { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.Home.route) {
            HomeScreen(
                homeVM = homeVM,
                onLogout = {
                    FirebaseModule.auth.signOut()
                    navController.navigate(Routes.Welcome.route) {
                        popUpTo(Routes.Home.route) { inclusive = true }
                    }
                },
                onNavigate = { route ->
                    navController.navigate(route) {
                        // Avoid building a huge stack when selecting nav items
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }

        composable(Routes.DoctorDiscovery.route) {
            DoctorDiscoveryScreen(
                homeVM = homeVM,
                onLogout = {
                    FirebaseModule.auth.signOut()
                    navController.navigate(Routes.Welcome.route) {
                        popUpTo(Routes.Home.route) { inclusive = true }
                    }
                },
                onNavigate = { route ->
                    navController.navigate(route) {
                        // Avoid building a huge stack when selecting nav items
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }

        composable(Routes.Chat.route) {
            ChatScreen(
                homeVM = homeVM,
                onLogout = {
                    FirebaseModule.auth.signOut()
                    navController.navigate(Routes.Welcome.route) {
                        popUpTo(Routes.Home.route) { inclusive = true }
                    }
                },
                onNavigate = { route ->
                    navController.navigate(route) {
                        // Avoid building a huge stack when selecting nav items
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }

        composable(Routes.Appointment.route) {
            AppointmentScreen(
                homeVM = homeVM,
                onLogout = {
                    FirebaseModule.auth.signOut()
                    navController.navigate(Routes.Welcome.route) {
                        popUpTo(Routes.Home.route) { inclusive = true }
                    }
                },
                onNavigate = { route ->
                    navController.navigate(route) {
                        // Avoid building a huge stack when selecting nav items
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }

        composable(Routes.Settings.route) {
            SettingsScreen(
                homeVM = homeVM,
                onLogout = {
                    FirebaseModule.auth.signOut()
                    navController.navigate(Routes.Welcome.route) {
                        popUpTo(Routes.Home.route) { inclusive = true }
                    }
                },
                onNavigate = { route ->
                    navController.navigate(route) {
                        // Avoid building a huge stack when selecting nav items
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }

        composable(Routes.UserManagement.route) {
            UserManagementScreen(
                homeVM = homeVM,
                onLogout = {
                    FirebaseModule.auth.signOut()
                    navController.navigate(Routes.Welcome.route) {
                        popUpTo(Routes.Home.route) { inclusive = true }
                    }
                },
                onNavigate = { route ->
                    navController.navigate(route) {
                        // Avoid building a huge stack when selecting nav items
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }

        composable(Routes.DoctorProfile.route) { backStackEntry ->
            val uid = backStackEntry.arguments?.getString("doctorUid") ?: return@composable
            DoctorProfileScreen(
                doctorUid = uid,
                homeVM = homeVM,
                onLogout = { FirebaseModule.auth.signOut()
                    navController.navigate(Routes.Welcome.route) {
                        popUpTo(Routes.Home.route) { inclusive = true }
                    }
                },
                onNavigate = { route ->
                    navController.navigate(route) {
                        // Avoid building a huge stack when selecting nav items
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }

        composable(Routes.ChatThread.route) { backStackEntry ->
            val otherUid = backStackEntry.arguments?.getString("otherUid") ?: return@composable

            ChatThreadScreen(
                otherUid = otherUid,
                homeVM = homeVM,
                onLogout = {
                    FirebaseModule.auth.signOut()
                    navController.navigate(Routes.Welcome.route) {
                        popUpTo(Routes.Home.route) { inclusive = true }
                    }
                },
                onNavigate = { route ->
                    navController.navigate(route) {
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }
    }
}
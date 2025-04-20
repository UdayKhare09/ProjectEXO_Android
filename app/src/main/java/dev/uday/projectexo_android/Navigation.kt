package dev.uday.projectexo_android

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dev.uday.projectexo_android.net.ClientSocket
import dev.uday.projectexo_android.ui.Chat
import dev.uday.projectexo_android.ui.FeatureScreen
import dev.uday.projectexo_android.ui.LoginScreen

object NavigationRoutes {
    const val LOGIN = "login"
    const val FEATURE = "feature"
    const val CHAT = "chat"
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val context = LocalContext.current

    // Monitor network changes
    NetworkMonitor(context)

    LaunchedEffect(navController) {
        ClientSocket.setNavController(navController)
    }

    NavHost(
        navController = navController,
        startDestination = NavigationRoutes.LOGIN,
    ) {
        composable(NavigationRoutes.LOGIN) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(NavigationRoutes.FEATURE) {
                        popUpTo(NavigationRoutes.LOGIN) { inclusive = true }
                    }
                }
            )
        }

        composable(NavigationRoutes.FEATURE) {
            FeatureScreen(
                onContinueToChat = {
                    navController.navigate(NavigationRoutes.CHAT)
                },
                onLogout = {
                    ClientSocket.disconnect()
                    navController.navigate(NavigationRoutes.LOGIN) {
                        popUpTo(NavigationRoutes.LOGIN) { inclusive = true }
                    }
                }
            )
        }

        composable(NavigationRoutes.CHAT) {
            val navActions = remember(navController) {
                NavigationActions(navController)
            }

            Chat.ChatScreen(
                onLogout = {
                    ClientSocket.disconnect()
                    navActions.navigateToLogin()
                },
                onBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}

class NavigationActions(private val navController: NavHostController) {
    fun navigateToLogin() {
        navController.navigate(NavigationRoutes.LOGIN) {
            popUpTo(NavigationRoutes.LOGIN) { inclusive = true }
        }
    }
}


@Composable
fun NetworkMonitor(context: Context = LocalContext.current) {
    // For Android 7.0 (N) and above
    val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    DisposableEffect(Unit) {
        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onLost(network: Network) {
                super.onLost(network)
                // Connection lost, disconnect the socket
                ClientSocket.disconnectDueToNetworkChange()
            }

            override fun onUnavailable() {
                super.onUnavailable()
                // Network unavailable, disconnect the socket
                ClientSocket.disconnectDueToNetworkChange()
            }
        }

        // Register callback for all network types
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        connectivityManager.registerNetworkCallback(networkRequest, networkCallback)

        onDispose {
            connectivityManager.unregisterNetworkCallback(networkCallback)
        }
    }
}
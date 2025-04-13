import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dev.uday.projectexo_android.net.ClientSocket
import ui.Chat
import ui.FeatureScreen
import ui.LoginScreen

object NavigationRoutes {
    const val LOGIN = "login"
    const val FEATURE = "feature"
    const val CHAT = "chat"
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = NavigationRoutes.LOGIN
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
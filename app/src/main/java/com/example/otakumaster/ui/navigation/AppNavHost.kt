import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.otakumaster.ui.navigation.AppRoute
import com.example.otakumaster.ui.screens.HomeScreen
import com.example.otakumaster.ui.screens.NewFeatureScreen
import com.example.otakumaster.ui.screens.ProfileScreen

@Composable
fun AppNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = AppRoute.Home.route,
        modifier = modifier
    ) {
        composable(AppRoute.Home.route) {
            HomeScreen()
        }
        composable(AppRoute.NewFeature.route) {
            NewFeatureScreen()
        }
        composable(AppRoute.Profile.route) {
            ProfileScreen()
        }
    }
}

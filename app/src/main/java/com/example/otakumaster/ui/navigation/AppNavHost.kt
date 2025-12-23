import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.otakumaster.ui.navigation.AppRoute
import com.example.otakumaster.ui.screens.home.HomeScreen
import com.example.otakumaster.ui.screens.NewFeatureScreen
import com.example.otakumaster.ui.screens.ProfileScreen
import com.example.otakumaster.ui.screens.add.AddAnimeScreen
import com.example.otakumaster.ui.screens.add.AddSeriesScreen
import com.example.otakumaster.ui.screens.detail.AnimeDetailScreen
import com.example.otakumaster.ui.screens.detail.SeriesDetailScreen

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
            HomeScreen(navController = navController)
        }
        composable(AppRoute.NewFeature.route) {
            NewFeatureScreen()
        }
        composable(AppRoute.Profile.route) {
            ProfileScreen()
        }

        composable(AppRoute.AddAnime.route) {
            AddAnimeScreen(navController=navController)
        }
        composable(AppRoute.AddSeries.route) {
            AddSeriesScreen(navController=navController)
        }
        composable(
            route = AppRoute.SeriesDetail.route,
            arguments = listOf(navArgument("seriesId") { type = NavType.StringType })
        ) { backStackEntry ->
            val seriesId = backStackEntry.arguments?.getString("seriesId")!!
            SeriesDetailScreen(
                navController = navController,
                seriesId = seriesId
            )
        }
        composable(
            route = AppRoute.AnimeDetail.route,
            arguments = listOf(navArgument("animeId") { type = NavType.StringType })
        ) { backStackEntry ->
            val animeId = backStackEntry.arguments?.getString("animeId")!!
            AnimeDetailScreen(navController = navController, animeId = animeId)
        }

    }
}

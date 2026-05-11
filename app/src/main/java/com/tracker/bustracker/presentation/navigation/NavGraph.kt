package com.tracker.bustracker.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.tracker.bustracker.presentation.journeyresults.JourneyResultsScreen
import com.tracker.bustracker.presentation.search.SearchScreen
import com.tracker.bustracker.presentation.tracking.TrackingScreen
import com.tracker.bustracker.presentation.tracking.TrackingViewModel
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

object Routes {
    const val SEARCH = "search"
    const val JOURNEY_RESULTS = "journey_results/{from}/{to}"
    const val TRACKING = "tracking/{lineId}"

    fun journeyResults(from: String, to: String) = "journey_results/$from/$to"
    fun tracking(lineId: String) = "tracking/$lineId"
}

@Composable
fun BusTrackerNavHost() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.SEARCH) {
        composable(Routes.SEARCH) {
            SearchScreen(
                onJourneyReady = { from, to ->
                    navController.navigate(Routes.journeyResults(from, to))
                }
            )
        }

        composable(
            route = Routes.JOURNEY_RESULTS,
            arguments = listOf(
                navArgument("from") { type = NavType.StringType },
                navArgument("to") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val from = backStackEntry.arguments?.getString("from") ?: return@composable
            val to = backStackEntry.arguments?.getString("to") ?: return@composable

            JourneyResultsScreen(
                from = from,
                to = to,
                onLegSelected = { lineId ->
                    navController.navigate(Routes.tracking(lineId))
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Routes.TRACKING,
            arguments = listOf(
                navArgument("lineId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val lineId = backStackEntry.arguments?.getString("lineId") ?: return@composable
            val viewModel: TrackingViewModel = koinViewModel { parametersOf(lineId) }

            TrackingScreen(
                lineId = lineId,
                onBack = { navController.popBackStack() },
                viewModel = viewModel
            )
        }
    }
}

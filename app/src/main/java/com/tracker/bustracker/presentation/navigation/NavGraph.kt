package com.tracker.bustracker.presentation.navigation

import android.net.Uri
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
    const val JOURNEY_RESULTS = "journey_results/{from}/{to}/{fromName}/{toName}"
    const val TRACKING = "tracking/{lineId}"

    fun journeyResults(from: String, to: String, fromName: String, toName: String) =
        "journey_results/$from/$to/${Uri.encode(fromName)}/${Uri.encode(toName)}"

    fun tracking(lineId: String) = "tracking/$lineId"
}

@Composable
fun BusTrackerNavHost() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.SEARCH) {
        composable(Routes.SEARCH) {
            SearchScreen(
                onJourneyReady = { from, to, fromName, toName ->
                    navController.navigate(Routes.journeyResults(from, to, fromName, toName))
                }
            )
        }

        composable(
            route = Routes.JOURNEY_RESULTS,
            arguments = listOf(
                navArgument("from") { type = NavType.StringType },
                navArgument("to") { type = NavType.StringType },
                navArgument("fromName") { type = NavType.StringType },
                navArgument("toName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val from = backStackEntry.arguments?.getString("from") ?: return@composable
            val to = backStackEntry.arguments?.getString("to") ?: return@composable
            val fromName = backStackEntry.arguments?.getString("fromName") ?: ""
            val toName = backStackEntry.arguments?.getString("toName") ?: ""

            JourneyResultsScreen(
                from = from,
                to = to,
                fromName = fromName,
                toName = toName,
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
                onBack = { navController.popBackStack() },
                viewModel = viewModel
            )
        }
    }
}

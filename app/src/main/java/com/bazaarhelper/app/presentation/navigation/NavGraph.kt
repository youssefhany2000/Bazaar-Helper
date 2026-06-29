package com.bazaarhelper.app.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.bazaarhelper.app.presentation.screens.add.AddEditRecordScreen
import com.bazaarhelper.app.presentation.screens.home.HomeScreen
import com.bazaarhelper.app.presentation.screens.monthly.MonthlyReportScreen
import com.bazaarhelper.app.presentation.screens.records.DailyRecordsScreen

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object AddEdit : Screen("add_edit?recordId={recordId}") {
        fun createRoute(recordId: Long? = null) =
            if (recordId != null) "add_edit?recordId=$recordId" else "add_edit"
    }
    object DailyRecords : Screen("daily_records")
    object MonthlyReport : Screen("monthly_report")
}

@Composable
fun BazaarNavGraph(navController: NavHostController) {
    NavHost(navController = navController, startDestination = Screen.Home.route) {

        composable(Screen.Home.route) {
            HomeScreen(
                onAddClick = { navController.navigate(Screen.AddEdit.createRoute()) },
                onReportsClick = { navController.navigate(Screen.DailyRecords.route) }
            )
        }

        composable(
            route = Screen.AddEdit.route,
            arguments = listOf(navArgument("recordId") {
                type = NavType.LongType
                defaultValue = -1L
            })
        ) {
            AddEditRecordScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.DailyRecords.route) {
            DailyRecordsScreen(
                onBack = { navController.popBackStack() },
                onEditRecord = { id -> navController.navigate(Screen.AddEdit.createRoute(id)) },
                onMonthlyReport = { navController.navigate(Screen.MonthlyReport.route) }
            )
        }

        composable(Screen.MonthlyReport.route) {
            MonthlyReportScreen(onBack = { navController.popBackStack() })
        }
    }
}

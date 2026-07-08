package com.axveer.lancar.ui

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.axveer.lancar.ui.drill.DrillScreen
import com.axveer.lancar.ui.home.HomeScreen
import com.axveer.lancar.ui.results.ResultsScreen
import com.axveer.lancar.ui.theme.LancarTheme

@Composable
fun App(appModule: AppModule) {
    LancarTheme {
        val nav = rememberNavController()
        NavHost(navController = nav, startDestination = Home) {
            composable<Home> {
                HomeScreen(appModule) { moduleId -> nav.navigate(Drill(moduleId)) }
            }
            composable<Drill> { entry ->
                val args = entry.toRoute<Drill>()
                DrillScreen(appModule, args.moduleId,
                    onFinish = { correct, total, mastered ->
                        nav.navigate(Results(args.moduleId, correct, total, mastered)) {
                            popUpTo(Home)
                        }
                    },
                    onBack = { nav.popBackStack() })
            }
            composable<Results> { entry ->
                val r = entry.toRoute<Results>()
                ResultsScreen(r,
                    onAgain = { nav.navigate(Drill(r.moduleId)) { popUpTo(Home) } },
                    onHome = { nav.popBackStack(Home, inclusive = false) })
            }
        }
    }
}

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

// TODO(Task 9): replace with Onboarding/Main tab shell once those screens exist.
@Composable
fun App(appModule: AppModule) {
    LancarTheme {
        val nav = rememberNavController()
        NavHost(navController = nav, startDestination = Onboarding) {
            composable<Onboarding> {
                HomeScreen(appModule) { moduleId -> nav.navigate(Drill(moduleId)) }
            }
            composable<Drill> { entry ->
                val args = entry.toRoute<Drill>()
                DrillScreen(appModule, args.moduleId,
                    onFinish = { correct, total, mastered ->
                        nav.navigate(Results(args.moduleId, correct, total, mastered)) {
                            popUpTo<Onboarding>()
                        }
                    },
                    onBack = { nav.popBackStack() })
            }
            composable<Results> { entry ->
                val r = entry.toRoute<Results>()
                ResultsScreen(r,
                    onAgain = { nav.navigate(Drill(r.moduleId)) { popUpTo<Onboarding>() } },
                    onHome = { nav.popBackStack<Onboarding>(inclusive = false) })
            }
        }
    }
}

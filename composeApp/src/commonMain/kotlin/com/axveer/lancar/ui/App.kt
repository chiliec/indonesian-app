package com.axveer.lancar.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.axveer.lancar.ui.drill.DrillScreen
import com.axveer.lancar.ui.main.MainScaffold
import com.axveer.lancar.ui.onboarding.OnboardingScreen
import com.axveer.lancar.ui.results.ResultsScreen
import com.axveer.lancar.ui.theme.LancarTheme

@Composable
fun App(appModule: AppModule) {
    val accent by appModule.accent.collectAsState()
    LancarTheme(accent = accent) {
        val nav = rememberNavController()
        NavHost(
            navController = nav,
            startDestination = startDestination(appModule.settings.onboardingSeen()),
        ) {
            composable<Onboarding> {
                OnboardingScreen(appModule) {
                    nav.navigate(Main) { popUpTo(Onboarding) { inclusive = true } }
                }
            }
            composable<Main> {
                MainScaffold(
                    appModule = appModule,
                    onOpenModule = { moduleId -> nav.navigate(Drill(moduleId)) },
                    onReplayOnboarding = {
                        nav.navigate(Onboarding) { popUpTo<Main> { inclusive = true } }
                    },
                )
            }
            composable<Drill> { entry ->
                val args = entry.toRoute<Drill>()
                DrillScreen(appModule, args.moduleId,
                    onFinish = { correct, total, mastered ->
                        nav.navigate(Results(args.moduleId, correct, total, mastered)) {
                            popUpTo(Main)
                        }
                    },
                    onBack = { nav.popBackStack() })
            }
            composable<Results> { entry ->
                val r = entry.toRoute<Results>()
                ResultsScreen(r,
                    onAgain = { nav.navigate(Drill(r.moduleId)) { popUpTo(Main) } },
                    onHome = { nav.popBackStack(Main, inclusive = false) })
            }
        }
    }
}

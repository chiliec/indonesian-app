package cx.viz.lancar.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import cx.viz.lancar.platform.Analytics
import cx.viz.lancar.ui.drill.DrillScreen
import cx.viz.lancar.ui.review.ReviewScreen
import cx.viz.lancar.ui.kartu.CardDeckScreen
import cx.viz.lancar.ui.main.MainScaffold
import cx.viz.lancar.ui.onboarding.OnboardingScreen
import cx.viz.lancar.ui.results.ResultsScreen
import cx.viz.lancar.ui.theme.LancarTheme

@Composable
fun App(appModule: AppModule) {
    val accent by appModule.accent.collectAsState()
    LancarTheme(accent = accent) {
        val nav = rememberNavController()
        // Typed routes render as "cx.viz.lancar.ui.Drill/{moduleId}" → "drill"; Main's tabs report themselves.
        val entry by nav.currentBackStackEntryAsState()
        LaunchedEffect(entry?.destination?.route) {
            entry?.destination?.route?.substringAfterLast('.')?.substringBefore('/')?.lowercase()
                ?.takeIf { it != "main" }?.let(Analytics::screen)
        }
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
                    onOpenDeck = { moduleId -> nav.navigate(Cards(moduleId)) },
                    onOpenReview = { nav.navigate(Review) },
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
            composable<Cards> { entry ->
                val args = entry.toRoute<Cards>()
                CardDeckScreen(appModule, args.moduleId, onBack = { nav.popBackStack() })
            }
            composable<Review> {
                ReviewScreen(
                    appModule,
                    onFinish = { nav.popBackStack(Main, inclusive = false) },
                    onBack = { nav.popBackStack() },
                )
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

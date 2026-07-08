package com.axveer.lancar.ui

import kotlin.test.Test
import kotlin.test.assertEquals

class StartDestinationTest {
    @Test fun unseenGoesToOnboarding() {
        assertEquals(Onboarding, startDestination(onboardingSeen = false))
    }

    @Test fun seenGoesToMain() {
        assertEquals(Main, startDestination(onboardingSeen = true))
    }
}

package cx.viz.lancar.ui.theme

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AccentTest {
    @Test fun knownNameResolves() {
        assertEquals(Accent.GREEN, Accent.fromName("GREEN"))
        assertEquals(Accent.BLUE, Accent.fromName("BLUE"))
    }

    @Test fun unknownOrNullDefaultsToTerracotta() {
        assertEquals(Accent.TERRACOTTA, Accent.fromName(null))
        assertEquals(Accent.TERRACOTTA, Accent.fromName("nope"))
    }

    @Test fun darkenMovesTowardBlackWithoutChangingAlpha() {
        val d = darken(LancarTerracotta)
        assertTrue(d.red < LancarTerracotta.red)
        assertTrue(d.green < LancarTerracotta.green)
        assertTrue(d.blue < LancarTerracotta.blue)
        assertEquals(LancarTerracotta.alpha, d.alpha)
    }
}

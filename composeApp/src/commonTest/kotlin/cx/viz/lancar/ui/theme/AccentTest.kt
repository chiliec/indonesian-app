package cx.viz.lancar.ui.theme

import kotlin.test.Test
import kotlin.test.assertEquals

class AccentTest {
    @Test fun knownNameResolves() {
        assertEquals(Accent.GREEN, Accent.fromName("GREEN"))
        assertEquals(Accent.BLUE, Accent.fromName("BLUE"))
    }

    @Test fun unknownOrNullDefaultsToTerracotta() {
        assertEquals(Accent.TERRACOTTA, Accent.fromName(null))
        assertEquals(Accent.TERRACOTTA, Accent.fromName("nope"))
    }
}

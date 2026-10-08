package medicapp.server.infrastructure.db.requests.repository

import kotlin.test.Test
import kotlin.test.assertEquals

class ContainsPatternTest {

    @Test
    fun `un texte simple est encadre par des jokers`() {
        val p = containsPattern("DOLIPRANE")

        assertEquals("%DOLIPRANE%", p.pattern)
        assertEquals('\\', p.escapeChar)
    }

    @Test
    fun `les jokers saisis par l utilisateur sont traites litteralement`() {
        assertEquals("%\\%%", containsPattern("%").pattern)
        assertEquals("%\\_%", containsPattern("_").pattern)
        assertEquals("%50\\% de \\_x%", containsPattern("50% de _x").pattern)
    }

    @Test
    fun `l antislash est lui aussi echappe`() {
        assertEquals("%a\\\\b%", containsPattern("a\\b").pattern)
    }
}

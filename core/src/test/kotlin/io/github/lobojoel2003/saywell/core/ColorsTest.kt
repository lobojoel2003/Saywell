package io.github.lobojoel2003.saywell.core

import io.github.lobojoel2003.saywell.core.model.Colors
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ColorsTest {
    @Test
    fun `reads the colour formats AAC apps write`() {
        assertEquals("#FF0000", Colors.normalize("rgb(255, 0, 0)"))
        assertEquals("#AABBCC", Colors.normalize("#abc"))
        assertEquals("#12AB34", Colors.normalize("#12ab34"))
        assertEquals("#12AB34", Colors.normalize("#12AB34FF"))
        assertEquals("#FFFFFF", Colors.normalize("white"))
        assertEquals("#FF8080", Colors.normalize("rgba(255, 0, 0, 0.5)"))
        assertEquals("#FF0000", Colors.normalize("rgb(100%, 0%, 0%)"))
        assertNull(Colors.normalize("transparent"))
        assertNull(Colors.normalize("not a colour"))
        assertNull(Colors.normalize(null))
    }

    @Test
    fun `writes the form the spec uses`() {
        assertEquals("rgb(255, 245, 157)", Colors.toCss(Colors.PEOPLE))
    }

    @Test
    fun `picks readable text`() {
        assertTrue(Colors.prefersDarkText("#FFF59D"))
        assertFalse(Colors.prefersDarkText("#202020"))
    }
}

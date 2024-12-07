package text

import kotlin.test.Test
import kotlin.test.assertEquals

class PrettyTest {
    @Test
    fun pretty() {
        assertEquals("", prettyString {})

        assertEquals("10", prettyString {
            item { append("10") }
        })

        assertEquals("id: 10", prettyString {
            field("id") { item { append("10") } }
        })

        assertEquals("vector\n  x: 10\n  y: 20", prettyString {
            struct("vector") {
                field("x") { item { append("10") } }
                field("y") { item { append("20") } }
            }
        })
    }
}
package kotlin.test.tests

import kotlin.test.*

@Ignore
class Ignored {
    @Test
    fun foo() {}
}

class Failed {
    @Test
    fun bar() {
        try {
            baz()
        } catch(e: Exception) {
            throw Exception("Bar", e)
        }
    }

    fun baz() {
        throw Exception("Baz")
    }
}

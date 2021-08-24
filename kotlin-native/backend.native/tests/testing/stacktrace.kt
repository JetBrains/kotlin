package kotlin.test.tests

import kotlin.native.internal.*
import kotlin.test.*

@Ignore
class Ignored {
    @Test
    fun foo() {}
}

class Failed {
    @Test
    fun bar() {
        val frame = runtimeGetCurrentFrame()
        try {
            baz()
        } catch(e: Exception) {
            assertTrue(runtimeCurrentFrameIsEqual(frame))
            throw Exception("Bar", e)
        }
    }

    fun baz() {
        throw Exception("Baz")
    }
}

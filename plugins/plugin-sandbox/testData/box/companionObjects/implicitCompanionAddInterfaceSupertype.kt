// WITH_STDLIB

import kotlin.test.assertEquals
import org.jetbrains.kotlin.plugin.sandbox.WithCompanion

interface Base {
    fun value(): String = "OK"
}

@WithCompanion(Base::class)
class Owner {
}

fun box(): String {
    assertEquals("OK", Owner.value())
    assertEquals("OK", Owner.Companion.value())
    return "OK"
}

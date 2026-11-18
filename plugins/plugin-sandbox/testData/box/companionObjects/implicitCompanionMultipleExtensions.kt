// WITH_STDLIB

import kotlin.test.assertEquals
import org.jetbrains.kotlin.plugin.sandbox.WithCompanion
import org.jetbrains.kotlin.plugin.sandbox.CompanionWithFoo

interface Base {
    fun value(): String = "OK"
}

@WithCompanion(Base::class)
@CompanionWithFoo
class Owner {
}

fun box(): String {
    assertEquals("OK", Owner.value())
    assertEquals("OK", Owner.Companion.value())
    assertEquals(10, Owner.foo())
    return "OK"
}

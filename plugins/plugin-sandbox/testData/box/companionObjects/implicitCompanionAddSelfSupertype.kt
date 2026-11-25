// WITH_STDLIB

import kotlin.test.assertEquals
import org.jetbrains.kotlin.plugin.sandbox.WithCompanion

@WithCompanion(Owner::class)
interface Owner {
    fun value(): String = "OK"
}

fun box(): String {
    assertEquals("OK", Owner.value())
    assertEquals("OK", Owner.Companion.value())
    return "OK"
}

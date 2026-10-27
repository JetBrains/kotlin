// WITH_STDLIB

package generatedCompanion

import org.jetbrains.kotlin.plugin.sandbox.AddToCompanion
import kotlin.test.assertFails
import kotlin.test.assertEquals

sealed interface Base {
    object Known : Base
}

@AddToCompanion("generatedCompanion/Base")
class Owner

// The compiler does not know that the companion is an inheritor of `Base`, so `when` is exhaustive without it.
fun describe(base: Base): String = when (base) {
    Base.Known -> "Known"
}

fun box(): String {
    val failure = assertFails { describe(Owner.Companion) }
    assertEquals("NoWhenBranchMatchedException", failure::class.simpleName)
    return "OK"
}

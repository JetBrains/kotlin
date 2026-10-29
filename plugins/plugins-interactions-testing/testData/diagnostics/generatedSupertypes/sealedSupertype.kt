// WITH_STDLIB

package generatedCompanion

import org.jetbrains.kotlin.plugin.sandbox.AddToCompanion
import kotlin.test.assertFails
import kotlin.test.assertEquals

sealed interface Base {
    object Known : Base
}

@AddToCompanion("generatedCompanion/Base")
class Owner {
    companion object
}

fun describe(base: Base): String = when (base) {
    Base.Known -> "Known"
}

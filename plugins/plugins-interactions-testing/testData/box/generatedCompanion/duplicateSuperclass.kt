// WITH_STDLIB

package generatedCompanion

import org.jetbrains.kotlin.plugin.sandbox.AddToCompanion
import org.jetbrains.kotlin.plugin.sandbox.AlsoAddToCompanion

open class Base {
    fun value(): String = "OK"
}

// Both extensions add the same superclass, but they get deduplicated
@AddToCompanion("generatedCompanion/Base")
@AlsoAddToCompanion("generatedCompanion/Base")
class Owner

fun box(): String {
    return Owner.Companion.value()
}

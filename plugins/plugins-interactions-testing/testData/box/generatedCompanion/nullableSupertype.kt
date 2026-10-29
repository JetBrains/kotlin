// IGNORE_PHASE_VERIFICATION: the exception leaves declarations unresolved
// EXPECT_EXCEPTION: ANY
// WITH_STDLIB

package generatedCompanion

import org.jetbrains.kotlin.plugin.sandbox.AddToCompanion

interface Base {
    fun value(): String = "OK"
}

@AddToCompanion("generatedCompanion/Base?")
class Owner

fun box(): String {
    val base: Base = Owner.Companion
    return base.value()
}

// IGNORE_PHASE_VERIFICATION: the exception leaves declarations unresolved
// EXPECT_EXCEPTION: ANY
// WITH_STDLIB

package generatedCompanion

import org.jetbrains.kotlin.plugin.sandbox.AddToCompanion

interface Provider<T>

@AddToCompanion("generatedCompanion/Provider<*>")
class Owner

fun box(): String {
    val companion: Any = Owner.Companion
    return if (companion is Provider<*>) "OK" else "FAIL"
}

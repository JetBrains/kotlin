// WITH_STDLIB

package generatedCompanion

import org.jetbrains.kotlin.plugin.sandbox.AddToCompanion

@AddToCompanion("kotlin/Any")
class Owner

fun box(): String {
    val companion: Any = Owner.Companion
    return if (companion.equals(companion)) "OK" else "FAIL"
}

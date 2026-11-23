// TARGET_BACKEND: JVM
// WITH_STDLIB
// FULL_JDK

package generatedCompanion

import org.jetbrains.kotlin.plugin.sandbox.AddToCompanion
import org.jetbrains.kotlin.plugin.sandbox.AlsoAddToCompanion

interface A
interface B
interface C

@AddToCompanion("generatedCompanion/A", "generatedCompanion/B")
@AlsoAddToCompanion("generatedCompanion/C")
class Forward

@AlsoAddToCompanion("generatedCompanion/C")
@AddToCompanion("generatedCompanion/B", "generatedCompanion/A")
class Backward

private fun interfacesOf(companion: Any): String =
    companion::class.java.interfaces.joinToString(",") { it.simpleName }

fun box(): String {
    // The order of the supertypes must follow the order of the extensions and the order inside one extension.
    val forward = interfacesOf(Forward.Companion)
    val backward = interfacesOf(Backward.Companion)
    return if (forward == "A,B,C" && backward == "B,A,C") "OK" else "$forward $backward"
}

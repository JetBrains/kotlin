// TARGET_BACKEND: JVM
// WITH_STDLIB
// FULL_JDK

package generatedCompanion

import org.jetbrains.kotlin.plugin.sandbox.AddToCompanion
import org.jetbrains.kotlin.plugin.sandbox.AlsoAddToCompanion

interface Provider<T>

// The first plugin is prioritized
@AddToCompanion("generatedCompanion/Provider<kotlin/String>")
@AlsoAddToCompanion("generatedCompanion/Provider<kotlin/Int>")
class Owner

fun box(): String {
    val provider: Provider<String> = Owner.Companion
    return "OK"
}

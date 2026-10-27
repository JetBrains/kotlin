// TARGET_BACKEND: JVM
// WITH_STDLIB
// FULL_JDK

package generatedCompanion

import org.jetbrains.kotlin.plugin.sandbox.AddToCompanion
import org.jetbrains.kotlin.plugin.sandbox.AlsoAddToCompanion

@Repeatable
@Retention(AnnotationRetention.RUNTIME)
annotation class Marker

@AddToCompanion("@generatedCompanion/Marker")
@AlsoAddToCompanion("@generatedCompanion/Marker")
class Owner

fun box(): String {
    // `Marker` is repeatable, so the companion must keep both copies.
    val count = Owner.Companion::class.java.getAnnotationsByType(Marker::class.java).size
    return if (count == 2) "OK" else "Marker count: $count"
}

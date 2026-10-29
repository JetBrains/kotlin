// TARGET_BACKEND: JVM
// WITH_STDLIB
// FULL_JDK

package generatedCompanion

import org.jetbrains.kotlin.plugin.sandbox.AddToCompanion
import org.jetbrains.kotlin.plugin.sandbox.AlsoAddToCompanion

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class Marker

@AddToCompanion("@generatedCompanion/Marker")
@AlsoAddToCompanion("@generatedCompanion/Marker")
class Owner

fun box(): String {
    val annotations = Owner.Companion::class.java.declaredAnnotations
        .map { it.annotationClass.qualifiedName }
        .filter { it != "kotlin.Metadata" }
    // Because Marker is not repeatable, it has no Container
    // But the compiler still emits the annotation with Container
    // At runtime, the JVM simply ignores the unknown Container
    return if (annotations.isEmpty()) "OK" else annotations.toString()
}

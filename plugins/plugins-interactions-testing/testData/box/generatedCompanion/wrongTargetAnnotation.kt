// TARGET_BACKEND: JVM
// WITH_STDLIB
// FULL_JDK

package generatedCompanion

import org.jetbrains.kotlin.plugin.sandbox.AddToCompanion

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class FunctionOnly

@AddToCompanion("@generatedCompanion/FunctionOnly")
class Owner

fun box(): String {
    val annotations = Owner.Companion::class.java.declaredAnnotations
        .map { it.annotationClass.qualifiedName }
        .filter { it != "kotlin.Metadata" }
    return if (annotations == listOf("generatedCompanion.FunctionOnly")) "OK" else annotations.toString()
}

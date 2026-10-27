// TARGET_BACKEND: JVM
// EXPECT_EXCEPTION: ANY
// WITH_STDLIB
// FULL_JDK

package generatedCompanion

import org.jetbrains.kotlin.plugin.sandbox.AddToCompanion

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class RequiredArgument(val value: String)

@AddToCompanion("@generatedCompanion/RequiredArgument")
class Owner

fun box(): String {
    val annotations = Owner.Companion::class.java.declaredAnnotations
        .map { it.annotationClass.qualifiedName }
        .filter { it != "kotlin.Metadata" }
    // IR validation fails (see the exception dump), but the annotation is still emitted on the companion.
    return if (annotations == listOf("generatedCompanion.RequiredArgument")) "OK" else annotations.toString()
}

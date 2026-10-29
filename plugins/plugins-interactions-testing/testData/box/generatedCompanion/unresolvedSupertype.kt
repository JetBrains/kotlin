// TARGET_BACKEND: JVM
// WITH_STDLIB

package generatedCompanion

import org.jetbrains.kotlin.plugin.sandbox.AddToCompanion

// The frontend reports an error only where the companion is used, so the code below compiles without errors.
@AddToCompanion("generatedCompanion/Missing")
class Owner

fun box(): String {
    // The backend drops the unresolved supertype silently.
    val companionClass = Class.forName("generatedCompanion.Owner\$Companion")
    val supertypes = listOf(companionClass.superclass) + companionClass.interfaces
    return if (supertypes == listOf(Any::class.java)) "OK" else supertypes.toString()
}

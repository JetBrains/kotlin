// WITH_STDLIB

package generatedCompanion

import org.jetbrains.kotlin.plugin.sandbox.AddToCompanion

@AddToCompanion("generatedCompanion/Missing")
class Owner

fun box(): String = Owner.Companion.<!MISSING_DEPENDENCY_SUPERCLASS!>toString<!>().let { "OK" }

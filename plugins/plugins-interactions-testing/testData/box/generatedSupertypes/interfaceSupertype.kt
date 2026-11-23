// WITH_STDLIB

package generatedCompanion

import org.jetbrains.kotlin.plugin.sandbox.AddToCompanion
import org.jetbrains.kotlin.plugin.sandbox.AlsoAddToCompanion
import org.jetbrains.kotlin.plugin.sandbox.CompanionWithFoo

interface Base {
    fun value(): String = "OK"
}

@AddToCompanion("generatedCompanion/Base")
@CompanionWithFoo
class Owner {
    companion object
}

fun box(): String = if (Owner.value() == "OK" && Owner.foo() == 10) "OK" else "FAIL"

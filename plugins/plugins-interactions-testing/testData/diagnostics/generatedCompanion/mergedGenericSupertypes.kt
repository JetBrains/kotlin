package generatedCompanion

import org.jetbrains.kotlin.plugin.sandbox.AddToCompanion
import org.jetbrains.kotlin.plugin.sandbox.AlsoAddToCompanion

interface Provider<T>

// The first plugin is prioritized
@AddToCompanion("generatedCompanion/Provider<kotlin/String>")
@AlsoAddToCompanion("generatedCompanion/Provider<kotlin/Int>")
class Owner

fun acceptStringProvider(value: Provider<String>) {}
fun acceptIntProvider(value: Provider<Int>) {}

fun test() {
    acceptStringProvider(Owner)
    acceptIntProvider(<!ARGUMENT_TYPE_MISMATCH!>Owner<!>)
}

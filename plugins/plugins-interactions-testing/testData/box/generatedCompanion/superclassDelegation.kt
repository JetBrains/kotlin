// WITH_STDLIB

package generatedCompanion

import org.jetbrains.kotlin.plugin.sandbox.AddToCompanion

open class Base(val value: String = "OK")

@AddToCompanion("generatedCompanion/Base")
class Owner

fun box(): String {
    return Owner.Companion.value
}

// WITH_STDLIB

package generatedCompanion

import org.jetbrains.kotlin.plugin.sandbox.AddToCompanion

// This is valid in source code: `open class Owner { companion object : Owner() }`.
@AddToCompanion("generatedCompanion/Owner")
open class Owner {
    fun value(): String = "OK"
}

fun box(): String {
    val owner: Owner = Owner.Companion
    return owner.value()
}

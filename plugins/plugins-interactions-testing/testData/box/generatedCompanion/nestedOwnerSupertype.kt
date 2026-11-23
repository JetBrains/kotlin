// WITH_STDLIB

package generatedCompanion

import org.jetbrains.kotlin.plugin.sandbox.AddToCompanion

// This is valid in source code: `class Owner { interface Api; companion object : Api }`.
@AddToCompanion("generatedCompanion/Owner.Api")
class Owner {
    interface Api {
        fun value(): String = "OK"
    }
}

fun box(): String {
    val api: Owner.Api = Owner.Companion
    return api.value()
}

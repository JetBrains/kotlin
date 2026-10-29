// WITH_STDLIB

package generatedCompanion

import org.jetbrains.kotlin.plugin.sandbox.AddToCompanion

interface Base {
    fun value(): String = "OK"
}

// The owner already has a companion, so the plugin cannot contribute to it.
@AddToCompanion("generatedCompanion/Base")
class Owner {
    companion object
}

fun box(): String {
    val companion: Any = Owner.Companion
    return if (companion is Base) "Supertype is added to the user companion" else "OK"
}

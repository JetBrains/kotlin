// WITH_STDLIB

package generatedCompanion

import org.jetbrains.kotlin.plugin.sandbox.AddToCompanion

interface Provider<T>

@AddToCompanion("generatedCompanion/Provider<generatedCompanion/Owner>")
class Owner<T>

fun box(): String {
    return "OK"
}

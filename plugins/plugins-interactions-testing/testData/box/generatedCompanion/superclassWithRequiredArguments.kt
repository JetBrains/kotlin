// EXPECT_EXCEPTION: JVM
// WITH_STDLIB

package generatedCompanion

import org.jetbrains.kotlin.plugin.sandbox.AddToCompanion

// `Base` has no constructor that can be called without arguments, so the companion object gets no constructor.
// The sandbox plugin does not generate a constructor in IR.
open class Base(val value: String)

@AddToCompanion("generatedCompanion/Base")
class Owner

fun box(): String {
    // On JS, the companion object is created without a call to the `Base` constructor, so `value` is not initialized.
    @Suppress("SENSELESS_COMPARISON")
    return if (Owner.Companion.value == null) "OK" else "Fail"
}

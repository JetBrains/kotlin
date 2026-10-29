// EXPECT_EXCEPTION: JS_IR
// WITH_STDLIB

package generatedCompanion

import org.jetbrains.kotlin.plugin.sandbox.AddToCompanion

interface Left {
    fun value(): String = "Left"
}

interface Right {
    fun value(): String = "Right"
}

@AddToCompanion("generatedCompanion/Left", "generatedCompanion/Right")
class Owner

fun box(): String {
    val left: Left = Owner.Companion
    val right: Right = Owner.Companion
    val result = "${left.value()} ${right.value()}"
    // On JVM, the conflict is resolved silently in favour of `Left`. On JS, the linker fails (see the exception dump).
    return if (result == "Left Left") "OK" else result
}

// EXPECT_EXCEPTION: JS_IR
// WITH_STDLIB

package generatedCompanion

import org.jetbrains.kotlin.plugin.sandbox.AddToCompanion
import kotlin.test.assertFails
import kotlin.test.assertEquals

class FinalBase {
    fun value(): String = "OK"
}

@AddToCompanion("generatedCompanion/FinalBase")
class Owner

fun box(): String {
    val failure = assertFails {
        val base: FinalBase = Owner.Companion
        base.value()
    }
    assertEquals("IncompatibleClassChangeError", failure::class.simpleName)
    return "OK"
}

// EXPECT_EXCEPTION: JS_IR
// WITH_STDLIB

package generatedCompanion

import org.jetbrains.kotlin.plugin.sandbox.AddToCompanion
import kotlin.test.assertFails
import kotlin.test.assertEquals

interface Base {
    fun value(): String
}

@AddToCompanion("generatedCompanion/Base")
class Owner

fun box(): String {
    val failure = assertFails { Owner.Companion.value() }
    assertEquals("AbstractMethodError", failure::class.simpleName)
    return "OK"
}

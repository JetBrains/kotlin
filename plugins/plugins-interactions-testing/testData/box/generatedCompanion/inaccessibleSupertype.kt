// EXPECT_EXCEPTION: ANY
// WITH_STDLIB

// FILE: hidden.kt
package other

private interface Hidden {
    fun value(): String = "OK"
}

// FILE: main.kt
package generatedCompanion

import org.jetbrains.kotlin.plugin.sandbox.AddToCompanion

// `other.Hidden` is private in another file and package, so the companion cannot see it.
@AddToCompanion("other/Hidden")
class Owner

fun box(): String = Owner.Companion.toString().let { "OK" }

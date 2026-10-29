// TARGET_BACKEND: JVM
// WITH_STDLIB

package generatedCompanion

import org.jetbrains.kotlin.plugin.sandbox.AddToCompanion
import org.jetbrains.kotlin.plugin.sandbox.CompanionWithFoo
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

interface FooApi {
    fun foo(): String = "OK"
}

// The generated `foo(): Int` has the same signature as `FooApi.foo(): String`, but a different return type.
@AddToCompanion("generatedCompanion/FooApi")
@CompanionWithFoo
class Owner

fun box(): String {
    val api: FooApi = Owner.Companion
    assertEquals(10, Owner.Companion.foo())
    // `FooApi.foo()` calls a bridge that casts the result of the generated `foo(): Int` to `String`.
    assertFailsWith<ClassCastException> { api.foo() }
    return "OK"
}

// TARGET_BACKEND: JS_IR
// WITH_STDLIB

package generatedCompanion

import org.jetbrains.kotlin.plugin.sandbox.AddToCompanion
import org.jetbrains.kotlin.plugin.sandbox.CompanionWithFoo
import kotlin.test.assertEquals

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
    val apiResult: Any = api.foo()
    // `FooApi.foo()` runs the generated `foo(): Int` and returns an `Int`.
    assertEquals<Any>(10, apiResult)
    return "OK"
}

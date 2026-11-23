// WITH_STDLIB

package generatedCompanion

import org.jetbrains.kotlin.plugin.sandbox.AddToCompanion

open class Foo {
    fun value(): String = "OK"
}
@AddToCompanion("generatedCompanion/Foo")
open class Owner

fun box(): String {
    val foo: Foo = Owner.Companion
    return foo.value()
}

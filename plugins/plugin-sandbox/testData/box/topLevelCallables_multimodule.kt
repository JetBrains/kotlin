// IGNORE_BACKEND: JS, NATIVE
// IGNORE_HMPP: JS
// MUTE_LL_FIR: KT-68878
// ^in AA tests plugin generate declarations both for `lib` and `main` modules

// MODULE: lib
// FILE: foo/some.kt
package foo

import org.jetbrains.kotlin.plugin.sandbox.DummyFunction

@DummyFunction("first.kt")
class First

@DummyFunction("first")
class OtherFirst

@DummyFunction("second")
class Second

@DummyFunction
class Third

// FILE: bar/other.kt
package bar

import org.jetbrains.kotlin.plugin.sandbox.DummyFunction

@DummyFunction("first.kt")
class First

// MODULE: main(lib)
// FILE: foo/main.kt
package foo

fun box(): String {
    val result1 = dummyFirst(First())
    if (result1 != "foo.First") return "Error: $result1"

    val result2 = dummySecond(Second())
    if (result2 != "foo.Second") return "Error: $result2"

    return "OK"
}

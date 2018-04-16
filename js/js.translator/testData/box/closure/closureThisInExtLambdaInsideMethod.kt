// IGNORE_BACKEND: JS_IR
// EXPECTED_REACHABLE_NODES: 1115
// KT-4600 Generated wrong code when capturing `this` in extension function inside a method

package foo

public class Foo(val trigger: () -> Any) {
    fun test() = myRun { trigger() };
}

fun box() = Foo({ "OK" }).test()

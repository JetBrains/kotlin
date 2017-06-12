// EXPECTED_REACHABLE_NODES: 494
// KT-4600 Generated wrong code when capturing `this` in extension function inside a method

package foo

public class Foo(val trigger: () -> Any) {
    fun test() = myRun { trigger() };
}

fun box() = Foo({ "OK" }).test()

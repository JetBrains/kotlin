// KT-4600 Generated wrong code when capturing `this` in extension function inside a method

package foo

fun run<T>(f: Int.() -> T) = 1.f()

public class Foo(val trigger: () -> Any) {
    fun test() = run {trigger()};
}

fun box() = Foo({ "OK" }).test()

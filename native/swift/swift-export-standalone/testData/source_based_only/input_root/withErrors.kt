import a.b.Fake

// I is unresolved.
fun foo(): I {}

// We don't support closures yet.
val x: () -> Unit = {
    println("hello")
}

typealias X = Foo

class MyClass {
    val x: Unresolved = error()

    val y = unknown()

    fun method(arg: Int): Unresolved {}
}

// We don't support type parameters
fun <T> parametrized(): T {
    return TODO()
}

// Inline declarations are actually skipped. S
inline fun <reified T> parametrized(): T {
    return TODO()
}

val fakeUsage: Fake = TODO()
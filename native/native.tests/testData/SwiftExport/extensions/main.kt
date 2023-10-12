// part-1: extension for class
class MyClass
fun MyClass.foo() = print("$this")
fun MyClass.bar(arg: Int) = print("$this - arg: $arg")

// part-2: extension for build ins
// Kotlin does not allow extensions of Int to mutate itself, so we can ignore that case
fun Int.foo() = print("$this")
fun Int.bar(arg: Double) = print("$this - $arg")

// part-3: nexted extensions
class Bar {
    fun Int.foo1() = 123
    fun Int.foo2(arg: Double) = 321
}

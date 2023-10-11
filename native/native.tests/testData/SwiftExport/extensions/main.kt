

// part-1: extension for class
class MyClass
fun MyClass.foo() = print("$this")
fun MyClass.bar(arg: Int) = print("$this - arg: $arg")

// part-2: extension for build ins
// public fun Int.foo() = print("$this")

// part-3: nexted extensions
//class Bar {
//    public fun Int.foo(arg: Double): String {
//        return "$this"
//    }
//}

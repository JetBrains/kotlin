fun JavaClass.foo(x: String) {
    println("extension")
}

fun main(args: Array<String>) {
    JavaClass().foo("str")
}
//KT-21534
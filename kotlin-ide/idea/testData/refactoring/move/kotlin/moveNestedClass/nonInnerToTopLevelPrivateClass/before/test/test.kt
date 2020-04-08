package test

class SomeClass {
    class <caret>NestedClass {
        val valInNestedC = 42
    }
}

fun main(args: Array<String>) {
    val test = SomeClass.NestedClass().valInNestedC
}